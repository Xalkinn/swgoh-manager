package fr.xalkinn.swgohmanager.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.CombatPlayerRow;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.Detail;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.MissionFailureRow;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.MissionRow;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.PlanetRow;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.PlayerRow;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.RoundRow;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.SummaryRow;

@Service
public class TerritoryBattleStatsService {

    private final JdbcTemplate jdbc;
    private final LocalizationService localizationService;

    public TerritoryBattleStatsService(
            JdbcTemplate jdbc,
            LocalizationService localizationService) {
        this.jdbc = jdbc;
        this.localizationService = localizationService;
    }

    public List<SummaryRow> getHistoriqueDashboard(int limite) {
        List<SummaryRow> rows = chargerHistorique(Math.max(1, limite + 1));
        appliquerTendances(rows);
        if (rows.size() > limite) {
            return new ArrayList<>(rows.subList(0, limite));
        }
        return rows;
    }

    public List<SummaryRow> getHistoriqueComplet() {
        List<SummaryRow> rows = chargerHistorique(100);
        appliquerTendances(rows);
        return rows;
    }

    public Detail getDetail(Long tbId) {
        Long id = tbId;
        if (id == null) {
            id = jdbc.query("""
                    SELECT id FROM territory_battle
                    ORDER BY end_time DESC, id DESC
                    LIMIT 1
                    """,
                    rs -> rs.next() ? rs.getLong("id") : null);
        }

        if (id == null) {
            return null;
        }

        SummaryRow tb = chargerTb(id);
        if (tb == null) return null;

        Detail detail = new Detail();
        detail.setTb(tb);

        List<RoundRow> rounds = chargerRounds(id);
        for (RoundRow round : rounds) {
            round.setModalId("tbPhase-" + round.getRoundNum());
            round.setPlanetes(chargerPlanetesPhase(id, round.getRoundNum()));
        }

        List<MissionRow> missions = chargerMissions(id);
        List<PlayerRow> joueurs = chargerJoueurs(id);

        detail.setRounds(rounds);
        detail.setMissions(missions);
        detail.setJoueurs(joueurs);
        detail.setTotalParticipantsActifs(nombreParticipantsActifs(id));

        long totalVagues = rounds.stream().mapToLong(RoundRow::getVagues).sum();
        long totalCombats = rounds.stream().mapToLong(RoundRow::getCombatsTentes).sum();
        long totalMsTentees = missions.stream().mapToLong(MissionRow::getTentees).sum();
        long totalMsReussies = missions.stream().mapToLong(MissionRow::getReussies).sum();
        long totalPower = rounds.stream().mapToLong(RoundRow::getPuissanceDeployee).sum();
        long totalUnites = rounds.stream().mapToLong(RoundRow::getUnitesDonnees).sum();

        detail.setTotalVagues(totalVagues);
        detail.setTotalCombats(totalCombats);
        detail.setTotalMsTentees(totalMsTentees);
        detail.setTotalMsReussies(totalMsReussies);
        detail.setTauxMs(totalMsTentees > 0 ? totalMsReussies * 100.0 / totalMsTentees : 0.0);
        detail.setTotalPuissanceDeployee(totalPower);
        detail.setTotalUnitesDonnees(totalUnites);

        return detail;
    }

    private List<SummaryRow> chargerHistorique(int limite) {
        List<SummaryRow> rows = jdbc.query("""
                SELECT id, instance_id, definition_id, start_time, end_time, total_stars
                FROM territory_battle
                ORDER BY end_time DESC, id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> {
                    SummaryRow row = new SummaryRow();
                    row.setId(rs.getLong("id"));
                    row.setInstanceId(rs.getString("instance_id"));
                    row.setDefinitionId(rs.getString("definition_id"));
                    row.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
                    row.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    row.setTotalStars(rs.getInt("total_stars"));
                    return row;
                },
                limite);

        for (SummaryRow row : rows) {
            Map<Integer, RoundRow> rounds = chargerRoundsMap(row.getId());
            row.setVaguesP3(valeurVagues(rounds, 3));
            row.setVaguesP4(valeurVagues(rounds, 4));
            row.setVaguesP5(valeurVagues(rounds, 5));
            row.setVaguesP6(valeurVagues(rounds, 6));
            row.setMsTentees(rounds.values().stream().mapToLong(RoundRow::getMsTentees).sum());
        }

        return rows;
    }

    private SummaryRow chargerTb(long tbId) {
        List<SummaryRow> rows = jdbc.query("""
                SELECT id, instance_id, definition_id, start_time, end_time, total_stars
                FROM territory_battle
                WHERE id = ?
                """,
                (rs, rowNum) -> {
                    SummaryRow row = new SummaryRow();
                    row.setId(rs.getLong("id"));
                    row.setInstanceId(rs.getString("instance_id"));
                    row.setDefinitionId(rs.getString("definition_id"));
                    row.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
                    row.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    row.setTotalStars(rs.getInt("total_stars"));
                    return row;
                },
                tbId);
        return rows.stream().findFirst().orElse(null);
    }

    private List<RoundRow> chargerRounds(long tbId) {
        return new ArrayList<>(chargerRoundsMap(tbId).values());
    }

    private Map<Integer, RoundRow> chargerRoundsMap(long tbId) {
        List<RoundRow> rows = jdbc.query("""
                SELECT
                    a.round_num,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'strike_attempt' THEN s.score ELSE 0 END), 0) AS combats_tentes,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'strike_encounter' THEN s.score ELSE 0 END), 0) AS vagues,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'covert_attempt' THEN s.score ELSE 0 END), 0) AS ms_tentees,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'power' THEN s.score ELSE 0 END), 0) AS puissance_deployee,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'unit_donated' THEN s.score ELSE 0 END), 0) AS unites_donnees
                FROM tb_activite a
                JOIN tb_score_joueur s ON s.tb_activite_id = a.id
                JOIN joueur j
                  ON j.player_id = s.player_id
                 AND j.dans_guilde = 1
                WHERE a.territory_battle_id = ?
                  AND a.round_num IS NOT NULL
                GROUP BY a.round_num
                ORDER BY a.round_num
                """,
                (rs, rowNum) -> {
                    RoundRow row = new RoundRow();
                    row.setRoundNum(rs.getInt("round_num"));
                    row.setCombatsTentes(rs.getLong("combats_tentes"));
                    row.setVagues(rs.getLong("vagues"));
                    row.setMsTentees(rs.getLong("ms_tentees"));
                    row.setPuissanceDeployee(rs.getLong("puissance_deployee"));
                    row.setUnitesDonnees(rs.getLong("unites_donnees"));
                    row.setVaguesParCombat(row.getCombatsTentes() > 0
                            ? row.getVagues() * 1.0 / row.getCombatsTentes()
                            : 0.0);
                    return row;
                },
                tbId);

        Map<Integer, RoundRow> result = new HashMap<>();
        // L'ordre est ensuite reconstruit de P1 à P6.
        Map<Integer, RoundRow> byRound = new HashMap<>();
        for (RoundRow row : rows) byRound.put(row.getRoundNum(), row);

        for (int round = 1; round <= 6; round++) {
            RoundRow row = byRound.get(round);
            if (row == null) {
                row = new RoundRow();
                row.setRoundNum(round);
            }
            result.put(round, row);
        }

        // HashMap ne garantit pas l'ordre ; LinkedHashMap serait préférable pour Thymeleaf.
        Map<Integer, RoundRow> ordered = new java.util.LinkedHashMap<>();
        for (int round = 1; round <= 6; round++) ordered.put(round, result.get(round));
        return ordered;
    }

    /**
     * Détail des combats classiques d'une phase, regroupés par planète / zone.
     * Les missions spéciales sont exclues en ne sélectionnant que les statistiques
     * strike_attempt et strike_encounter.
     */
    private List<PlanetRow> chargerPlanetesPhase(long tbId, int phase) {
        List<PlanetRow> planetes = jdbc.query("""
                SELECT
                    a.conflict_num,
                    a.bonus,
                    COALESCE(SUM(CASE
                        WHEN a.stat_type = 'strike_attempt' AND j.player_id IS NOT NULL
                        THEN COALESCE(s.score, 0) ELSE 0 END), 0) AS combats_tentes,
                    COALESCE(SUM(CASE
                        WHEN a.stat_type = 'strike_encounter' AND j.player_id IS NOT NULL
                        THEN COALESCE(s.score, 0) ELSE 0 END), 0) AS vagues
                FROM tb_activite a
                LEFT JOIN tb_score_joueur s
                  ON s.tb_activite_id = a.id
                LEFT JOIN joueur j
                  ON j.player_id = s.player_id
                 AND j.dans_guilde = 1
                WHERE a.territory_battle_id = ?
                  AND a.phase = ?
                  AND a.conflict_num IS NOT NULL
                  AND a.stat_type IN ('strike_attempt', 'strike_encounter')
                GROUP BY a.conflict_num, a.bonus
                ORDER BY a.conflict_num, a.bonus
                """,
                (rs, rowNum) -> {
                    PlanetRow row = new PlanetRow();
                    row.setPhase(phase);
                    row.setConflict(rs.getInt("conflict_num"));
                    row.setBonus(rs.getBoolean("bonus"));
                    row.setCombatsTentes(rs.getLong("combats_tentes"));
                    row.setVagues(rs.getLong("vagues"));
                    row.setVaguesParCombat(row.getCombatsTentes() > 0
                            ? row.getVagues() * 1.0 / row.getCombatsTentes()
                            : 0.0);
                    row.setLabel(labelPlanete(phase, row.getConflict(), row.isBonus()));
                    return row;
                },
                tbId,
                phase);

        for (PlanetRow planete : planetes) {
            planete.setJoueurs(chargerCombatsJoueursPlanete(tbId, phase, planete));
        }
        return planetes;
    }

    private List<CombatPlayerRow> chargerCombatsJoueursPlanete(
            long tbId,
            int phase,
            PlanetRow planete) {
        return jdbc.query("""
                SELECT
                    j.player_id,
                    COALESCE(j.nom, j.player_id) AS player_name,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'strike_attempt' THEN COALESCE(s.score, 0) ELSE 0 END), 0) AS combats_tentes,
                    COALESCE(SUM(CASE WHEN a.stat_type = 'strike_encounter' THEN COALESCE(s.score, 0) ELSE 0 END), 0) AS vagues
                FROM tb_activite a
                JOIN tb_score_joueur s
                  ON s.tb_activite_id = a.id
                JOIN joueur j
                  ON j.player_id = s.player_id
                 AND j.dans_guilde = 1
                WHERE a.territory_battle_id = ?
                  AND a.phase = ?
                  AND a.conflict_num = ?
                  AND a.bonus = ?
                  AND a.stat_type IN ('strike_attempt', 'strike_encounter')
                GROUP BY j.player_id, j.nom
                HAVING SUM(CASE WHEN a.stat_type = 'strike_attempt' THEN COALESCE(s.score, 0) ELSE 0 END) > 0
                    OR SUM(CASE WHEN a.stat_type = 'strike_encounter' THEN COALESCE(s.score, 0) ELSE 0 END) > 0
                ORDER BY vagues DESC, combats_tentes DESC, player_name
                """,
                (rs, rowNum) -> {
                    CombatPlayerRow row = new CombatPlayerRow();
                    row.setPlayerId(rs.getString("player_id"));
                    row.setPlayerName(rs.getString("player_name"));
                    row.setCombatsTentes(rs.getLong("combats_tentes"));
                    row.setVagues(rs.getLong("vagues"));
                    row.setVaguesParCombat(row.getCombatsTentes() > 0
                            ? row.getVagues() * 1.0 / row.getCombatsTentes()
                            : 0.0);
                    return row;
                },
                tbId,
                phase,
                planete.getConflict(),
                planete.isBonus());
    }

    private List<MissionRow> chargerMissions(long tbId) {
        List<MissionRow> missions = jdbc.query("""
                SELECT
                    a.phase,
                    a.conflict_num,
                    a.bonus,
                    a.covert_num,
                    COUNT(DISTINCT CASE
                        WHEN a.stat_type = 'covert_round_attempted' AND j.player_id IS NOT NULL
                        THEN s.player_id END) AS tentees,
                    COUNT(DISTINCT CASE
                        WHEN a.stat_type = 'covert_complete' AND j.player_id IS NOT NULL
                        THEN s.player_id END) AS reussies
                FROM tb_activite a
                LEFT JOIN tb_score_joueur s ON s.tb_activite_id = a.id
                LEFT JOIN joueur j
                  ON j.player_id = s.player_id
                 AND j.dans_guilde = 1
                WHERE a.territory_battle_id = ?
                  AND a.phase IS NOT NULL
                  AND a.stat_type IN ('covert_round_attempted', 'covert_complete')
                GROUP BY a.phase, a.conflict_num, a.bonus, a.covert_num
                ORDER BY a.phase, a.conflict_num, a.bonus, a.covert_num
                """,
                (rs, rowNum) -> {
                    MissionRow row = new MissionRow();
                    row.setPhase(rs.getInt("phase"));
                    row.setConflict(rs.getInt("conflict_num"));
                    row.setBonus(rs.getBoolean("bonus"));
                    row.setCovertNum(rs.getInt("covert_num"));
                    row.setTentees(rs.getLong("tentees"));
                    row.setReussies(rs.getLong("reussies"));
                    row.setTauxReussite(row.getTentees() > 0
                            ? row.getReussies() * 100.0 / row.getTentees()
                            : 0.0);
                    row.setLabel(labelMission(row.getPhase(), row.getConflict(), row.isBonus(), row.getCovertNum()));
                    row.setModalId("tbMission-" + row.getPhase() + "-" + row.getConflict()
                            + "-" + (row.isBonus() ? 1 : 0) + "-" + row.getCovertNum());
                    return row;
                },
                tbId);

        for (MissionRow mission : missions) {
            mission.setEchecs(chargerEchecsMission(tbId, mission));
            mission.setNonTentes(chargerNonTentesMission(tbId, mission));
        }

        return missions;
    }

    /**
     * Retourne uniquement les joueurs qui ont tenté la mission spéciale mais
     * qui n'ont pas de ligne de réussite correspondante. Les joueurs qui
     * n'ont pas tenté la mission ne sont volontairement pas inclus.
     */
    private List<MissionFailureRow> chargerEchecsMission(long tbId, MissionRow mission) {
        return jdbc.query("""
                SELECT
                    tentative.player_id,
                    COALESCE(MAX(j.nom), tentative.player_id) AS player_name
                FROM tb_score_joueur tentative
                JOIN tb_activite activite_tentative
                  ON activite_tentative.id = tentative.tb_activite_id
                JOIN joueur j
                  ON j.player_id = tentative.player_id
                 AND j.dans_guilde = 1
                WHERE activite_tentative.territory_battle_id = ?
                  AND activite_tentative.stat_type = 'covert_round_attempted'
                  AND activite_tentative.phase = ?
                  AND activite_tentative.conflict_num <=> ?
                  AND activite_tentative.bonus = ?
                  AND activite_tentative.covert_num <=> ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM tb_score_joueur succes
                      JOIN tb_activite activite_succes
                        ON activite_succes.id = succes.tb_activite_id
                      WHERE succes.player_id = tentative.player_id
                        AND activite_succes.territory_battle_id = activite_tentative.territory_battle_id
                        AND activite_succes.stat_type = 'covert_complete'
                        AND activite_succes.phase = activite_tentative.phase
                        AND activite_succes.conflict_num <=> activite_tentative.conflict_num
                        AND activite_succes.bonus = activite_tentative.bonus
                        AND activite_succes.covert_num <=> activite_tentative.covert_num
                  )
                GROUP BY tentative.player_id
                ORDER BY player_name
                """,
                (rs, rowNum) -> new MissionFailureRow(
                        rs.getString("player_id"),
                        rs.getString("player_name")),
                tbId,
                mission.getPhase(),
                mission.getConflict(),
                mission.isBonus(),
                mission.getCovertNum());
    }


    /**
     * Retourne uniquement les participants de cette TB qui sont toujours
     * membres de la guilde (dans_guilde = 1) et qui n'ont pas tenté la mission.
     *
     * La population de référence est donc :
     * participants TB ∩ membres actuels de la guilde.
     */
    private List<MissionFailureRow> chargerNonTentesMission(long tbId, MissionRow mission) {
        return jdbc.query("""
                SELECT
                    j.player_id,
                    COALESCE(j.nom, j.player_id) AS player_name
                FROM joueur j
                WHERE j.dans_guilde = 1
                  AND EXISTS (
                      SELECT 1
                      FROM tb_score_joueur participation
                      JOIN tb_activite activite_participation
                        ON activite_participation.id = participation.tb_activite_id
                      WHERE participation.player_id = j.player_id
                        AND activite_participation.territory_battle_id = ?
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM tb_score_joueur tentative
                      JOIN tb_activite activite_tentative
                        ON activite_tentative.id = tentative.tb_activite_id
                      WHERE tentative.player_id = j.player_id
                        AND activite_tentative.territory_battle_id = ?
                        AND activite_tentative.stat_type = 'covert_round_attempted'
                        AND activite_tentative.phase = ?
                        AND activite_tentative.conflict_num <=> ?
                        AND activite_tentative.bonus = ?
                        AND activite_tentative.covert_num <=> ?
                  )
                ORDER BY player_name
                """,
                (rs, rowNum) -> new MissionFailureRow(
                        rs.getString("player_id"),
                        rs.getString("player_name")),
                tbId,
                tbId,
                mission.getPhase(),
                mission.getConflict(),
                mission.isBonus(),
                mission.getCovertNum());
    }

    private List<PlayerRow> chargerJoueurs(long tbId) {
        return jdbc.query("""
                SELECT
                    j.player_id,
                    COALESCE(j.nom, j.player_id) AS player_name,
                    COALESCE(SUM(CASE WHEN x.round_num IS NOT NULL AND x.stat_type = 'strike_attempt' THEN x.score ELSE 0 END), 0) AS combats_tentes,
                    COALESCE(SUM(CASE WHEN x.round_num IS NOT NULL AND x.stat_type = 'strike_encounter' THEN x.score ELSE 0 END), 0) AS vagues,
                    COUNT(DISTINCT CASE WHEN x.phase IS NOT NULL AND x.stat_type = 'covert_round_attempted' THEN x.activite_id END) AS ms_tentees,
                    COUNT(DISTINCT CASE WHEN x.phase IS NOT NULL AND x.stat_type = 'covert_complete' THEN x.activite_id END) AS ms_reussies,
                    COALESCE(SUM(CASE WHEN x.round_num IS NOT NULL AND x.stat_type = 'power' THEN x.score ELSE 0 END), 0) AS puissance_deployee,
                    COALESCE(SUM(CASE WHEN x.round_num IS NOT NULL AND x.stat_type = 'unit_donated' THEN x.score ELSE 0 END), 0) AS unites_donnees
                FROM (
                    SELECT DISTINCT s.player_id
                    FROM tb_score_joueur s
                    JOIN tb_activite a ON a.id = s.tb_activite_id
                    WHERE a.territory_battle_id = ?
                ) participants
                JOIN joueur j
                  ON j.player_id = participants.player_id
                 AND j.dans_guilde = 1
                LEFT JOIN (
                    SELECT
                        s.player_id,
                        s.score,
                        a.id AS activite_id,
                        a.round_num,
                        a.phase,
                        a.stat_type
                    FROM tb_score_joueur s
                    JOIN tb_activite a ON a.id = s.tb_activite_id
                    WHERE a.territory_battle_id = ?
                ) x ON x.player_id = participants.player_id
                GROUP BY j.player_id, j.nom
                ORDER BY vagues DESC, ms_reussies DESC, player_name
                """,
                (rs, rowNum) -> {
                    PlayerRow row = new PlayerRow();
                    row.setPlayerId(rs.getString("player_id"));
                    row.setPlayerName(rs.getString("player_name"));
                    row.setCombatsTentes(rs.getLong("combats_tentes"));
                    row.setVagues(rs.getLong("vagues"));
                    row.setMsTentees(rs.getLong("ms_tentees"));
                    row.setMsReussies(rs.getLong("ms_reussies"));
                    row.setPuissanceDeployee(rs.getLong("puissance_deployee"));
                    row.setUnitesDonnees(rs.getLong("unites_donnees"));
                    row.setVaguesParCombat(row.getCombatsTentes() > 0
                            ? row.getVagues() * 1.0 / row.getCombatsTentes()
                            : 0.0);
                    row.setTauxMs(row.getMsTentees() > 0
                            ? row.getMsReussies() * 100.0 / row.getMsTentees()
                            : 0.0);
                    return row;
                },
                tbId,
                tbId);
    }

    /**
     * Nombre de joueurs ayant au moins une donnée dans cette TB et qui sont
     * toujours marqués dans la guilde.
     */
    private int nombreParticipantsActifs(long tbId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT s.player_id)
                FROM tb_score_joueur s
                JOIN tb_activite a ON a.id = s.tb_activite_id
                JOIN joueur j
                  ON j.player_id = s.player_id
                 AND j.dans_guilde = 1
                WHERE a.territory_battle_id = ?
                """,
                Integer.class,
                tbId);
        return n == null ? 0 : n;
    }

    private void appliquerTendances(List<SummaryRow> rows) {
        // rows est trié du plus récent au plus ancien. La référence d'une ligne
        // est donc l'élément suivant.
        for (int i = 0; i < rows.size(); i++) {
            SummaryRow current = rows.get(i);
            SummaryRow previous = i + 1 < rows.size() ? rows.get(i + 1) : null;
            if (previous == null) continue;

            current.setStarsTrend(tendance(current.getTotalStars(), previous.getTotalStars()));
            current.setP3Trend(tendance(current.getVaguesP3(), previous.getVaguesP3()));
            current.setP4Trend(tendance(current.getVaguesP4(), previous.getVaguesP4()));
            current.setP5Trend(tendance(current.getVaguesP5(), previous.getVaguesP5()));
            current.setP6Trend(tendance(current.getVaguesP6(), previous.getVaguesP6()));
            current.setMsTrend(tendance(current.getMsTentees(), previous.getMsTentees()));
        }
    }

    private String tendance(long actuel, long precedent) {
        if (actuel > precedent) return "up";
        if (actuel < precedent) return "down";
        return "same";
    }

    private long valeurVagues(Map<Integer, RoundRow> rounds, int round) {
        RoundRow row = rounds.get(round);
        return row != null ? row.getVagues() : 0L;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    /**
     * Libellés des missions spéciales de la Bataille de territoire.
     *
     * Pour les missions liées à un personnage, on utilise le bundle officiel
     * FRE_FR déjà chargé par LocalizationService. Le fallback reste en français
     * afin que la page ne réintroduise pas de libellé anglais si Comlink est
     * momentanément indisponible.
     */
    private String labelMission(int phase, int conflict, boolean bonus, int covert) {
        String key = phase + "-" + conflict + "-" + bonus + "-" + covert;
        return switch (key) {
            case "1-3-false-1" -> nomMissionPersonnage("QIRA", "Qi'ra");
            case "2-1-false-1" -> nomMissionPersonnage("JEDIKNIGHTCAL", "Cal Kestis (chevalier Jedi)");
            case "3-1-false-1" -> nomMissionPersonnage("SAWGERRERA", "Saw Gerrera");
            case "3-3-false-1" -> nomMissionPersonnage("THIRDSISTER", "Troisième Sœur (Reva)");
            case "3-3-false-2" -> nomMissionPersonnage("MANDALORBOKATAN", "Bo-Katan (Mandalore)");
            case "3-2-false-1" -> nomMissionPersonnage("MERRIN", "Merrin");
            case "3-1-true-1" -> "Clones";
            case "4-2-false-1" -> "Inquisiteurs";
            case "4-3-false-1" -> nomMissionPersonnage("L3_37", "L3-37");
            case "5-3-false-1" -> nomMissionPersonnage("YOUNGHAN", "Jeune Han Solo");
            default -> "Phase " + phase + " · Zone " + conflict
                    + (bonus ? " · Bonus" : "")
                    + " · Mission spéciale " + covert;
        };
    }

    private String labelPlanete(int phase, int conflict, boolean bonus) {
        String key = phase + "-" + conflict + "-" + bonus;
        return switch (key) {
            case "1-1-false" -> "R5 · Coruscant";
            case "1-2-false" -> "R5 · Mustafar";
            case "1-3-false" -> "R5 · Corellia";
            case "2-1-false" -> "R6 · Bracca";
            case "2-2-false" -> "R6 · Geonosis";
            case "2-3-false" -> "R6 · Felucia";
            case "3-1-false" -> "R7 · Kashyyyk";
            case "3-1-true" -> "Zeffo";
            case "3-2-false" -> "R7 · Dathomir";
            case "3-3-false" -> "R7 · Tatooine";
            case "4-1-false" -> "R8 · Lothal";
            case "4-2-false" -> "R8 · Station médicale Haven Class";
            case "4-3-false" -> "R8 · Kessel";
            case "4-3-true" -> "Mandalore";
            case "5-1-false" -> "R9 · Anneau de Kafrene";
            case "5-2-false" -> "R9 · Malachor";
            case "5-3-false" -> "R9 · Vandor";
            case "6-1-false" -> "R9 · Scarif";
            case "6-2-false" -> "R9 · Death Star";
            case "6-3-false" -> "R9 · Hoth";
            default -> "Planète / Zone " + conflict + (bonus ? " · Bonus" : "");
        };
    }

    private String nomMissionPersonnage(String baseId, String fallbackFrancais) {
        String nom = localizationService.traduirePersonnage(baseId, fallbackFrancais);
        return nom == null || nom.isBlank() ? fallbackFrancais : nom;
    }
}
