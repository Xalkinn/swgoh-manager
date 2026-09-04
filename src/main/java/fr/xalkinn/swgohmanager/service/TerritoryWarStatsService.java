package fr.xalkinn.swgohmanager.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.TerritoryWarView.MatchRow;
import fr.xalkinn.swgohmanager.dto.TerritoryWarView.OmicronMissingRow;
import fr.xalkinn.swgohmanager.dto.TerritoryWarView.Page;
import fr.xalkinn.swgohmanager.dto.TerritoryWarView.PreparationPlayerRow;
import fr.xalkinn.swgohmanager.dto.TerritoryWarView.PreparationSummary;
import fr.xalkinn.swgohmanager.dto.TerritoryWarView.Summary;

@Service
public class TerritoryWarStatsService {

    private final JdbcTemplate jdbc;

    public TerritoryWarStatsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page getPage() {
        Page page = new Page();
        List<MatchRow> historique = chargerHistorique(100);
        Summary summary = calculerSummary(historique);
        List<PreparationPlayerRow> joueurs = chargerPreparationJoueurs();
        PreparationSummary preparation = calculerPreparationSummary(joueurs);
        List<OmicronMissingRow> omicronsManquants = chargerOmicronsManquantsTw();

        page.setHistorique(historique);
        page.setSummary(summary);
        page.setJoueurs(joueurs);
        page.setPreparation(preparation);
        page.setOmicronsManquants(omicronsManquants);
        return page;
    }

    private List<MatchRow> chargerHistorique(int limite) {
        List<MatchRow> rows = jdbc.query("""
                SELECT id, territory_war_id, score, power, opponent_score,
                       opponent_guild_id, opponent_guild_name, opponent_guild_gp,
                       start_time, end_time
                FROM territory_war
                ORDER BY end_time DESC, id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> {
                    MatchRow row = new MatchRow();
                    row.setId(rs.getLong("id"));
                    row.setTerritoryWarId(rs.getString("territory_war_id"));
                    row.setScore(rs.getInt("score"));
                    row.setPower(rs.getLong("power"));
                    row.setOpponentScore(rs.getInt("opponent_score"));
                    row.setOpponentGuildId(rs.getString("opponent_guild_id"));
                    String nom = rs.getString("opponent_guild_name");
                    row.setOpponentGuildName(nom == null || nom.isBlank() ? "Adversaire inconnu" : nom);
                    row.setOpponentGuildGp(rs.getLong("opponent_guild_gp"));
                    row.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
                    row.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    row.setEcart(row.getScore() - row.getOpponentScore());
                    if (row.getEcart() > 0) {
                        row.setResultat("WIN");
                        row.setResultatLabel("Victoire");
                    } else if (row.getEcart() < 0) {
                        row.setResultat("LOSS");
                        row.setResultatLabel("Défaite");
                    } else {
                        row.setResultat("DRAW");
                        row.setResultatLabel("Égalité au score");
                    }
                    if (row.getStartTime() != null && row.getEndTime() != null) {
                        row.setDureeHeures(Duration.between(row.getStartTime(), row.getEndTime()).toMinutes() / 60.0);
                    }
                    return row;
                },
                limite);
        return rows;
    }

    private Summary calculerSummary(List<MatchRow> rows) {
        Summary s = new Summary();
        s.setTotal(rows.size());
        if (rows.isEmpty()) {
            s.setSerieLabel("Aucune série");
            return s;
        }

        int wins = 0;
        int losses = 0;
        int draws = 0;
        long scoreSum = 0;
        long opponentSum = 0;
        long marginSum = 0;

        MatchRow closest = null;
        MatchRow biggestWin = null;
        MatchRow biggestLoss = null;
        MatchRow strongestOpponent = null;

        for (MatchRow row : rows) {
            switch (row.getResultat()) {
                case "WIN" -> wins++;
                case "LOSS" -> losses++;
                default -> draws++;
            }
            scoreSum += row.getScore();
            opponentSum += row.getOpponentScore();
            marginSum += row.getEcart();

            if (closest == null || Math.abs(row.getEcart()) < Math.abs(closest.getEcart())) {
                closest = row;
            }
            if (row.getEcart() > 0 && (biggestWin == null || row.getEcart() > biggestWin.getEcart())) {
                biggestWin = row;
            }
            if (row.getEcart() < 0 && (biggestLoss == null || row.getEcart() < biggestLoss.getEcart())) {
                biggestLoss = row;
            }
            if (row.getOpponentGuildGp() > 0
                    && (strongestOpponent == null || row.getOpponentGuildGp() > strongestOpponent.getOpponentGuildGp())) {
                strongestOpponent = row;
            }
        }

        s.setVictoires(wins);
        s.setDefaites(losses);
        s.setNuls(draws);
        s.setWinRate(rows.isEmpty() ? 0.0 : wins * 100.0 / rows.size());
        s.setScoreMoyen(scoreSum * 1.0 / rows.size());
        s.setScoreAdverseMoyen(opponentSum * 1.0 / rows.size());
        s.setEcartMoyen(marginSum * 1.0 / rows.size());
        s.setMatchLePlusSerre(closest);
        s.setPlusGrosseVictoire(biggestWin);
        s.setPlusGrosseDefaite(biggestLoss);
        s.setAdversairePlusPuissant(strongestOpponent);

        if (closest != null) closest.setPlusSerree(true);
        if (biggestWin != null) biggestWin.setPlusGrosseVictoire(true);
        if (biggestLoss != null) biggestLoss.setPlusGrosseDefaite(true);
        if (strongestOpponent != null) strongestOpponent.setAdversairePlusPuissant(true);

        MatchRow latest = rows.get(0);
        String type = latest.getResultat();
        int streak = 0;
        for (MatchRow row : rows) {
            if (!type.equals(row.getResultat())) break;
            streak++;
        }
        s.setSerieActuelle(streak);
        s.setSerieType(type);
        s.setSerieLabel(switch (type) {
            case "WIN" -> streak + " victoire" + (streak > 1 ? "s" : "") + " de suite";
            case "LOSS" -> streak + " défaite" + (streak > 1 ? "s" : "") + " de suite";
            default -> streak + " égalité" + (streak > 1 ? "s" : "") + " de suite";
        });

        return s;
    }

    private List<PreparationPlayerRow> chargerPreparationJoueurs() {
        Map<Integer, Set<String>> priorites = chargerPrioritesTw();
        Map<String, Set<String>> possessions = chargerPossessionsTw();
        Map<Integer, int[]> reliques = chargerProfondeurReliques();
        Map<Integer, ModNumbers> mods = chargerMods();

        List<Map<String, Object>> base = jdbc.queryForList("""
                SELECT id, player_id, nom, pg_perso
                FROM joueur
                WHERE dans_guilde = 1
                ORDER BY nom
                """);

        List<PreparationPlayerRow> result = new ArrayList<>();
        for (Map<String, Object> row : base) {
            PreparationPlayerRow p = new PreparationPlayerRow();
            int joueurId = number(row.get("id"));
            String playerId = string(row.get("player_id"));
            long pgPerso = longNumber(row.get("pg_perso"));

            p.setJoueurId(joueurId);
            p.setPlayerId(playerId);
            String nom = string(row.get("nom"));
            p.setNom(nom == null || nom.isBlank() ? (playerId == null || playerId.isBlank() ? "Joueur" : playerId) : nom);
            p.setPgPerso(pgPerso);

            Set<String> skills = possessions.getOrDefault(playerId, Set.of());
            for (int priorite = 1; priorite <= 4; priorite++) {
                Set<String> plan = priorites.getOrDefault(priorite, Set.of());
                int atteint = 0;
                for (String skillId : plan) {
                    if (skills.contains(skillId)) atteint++;
                }
                switch (priorite) {
                    case 1 -> { p.setP1Total(plan.size()); p.setP1Atteint(atteint); }
                    case 2 -> { p.setP2Total(plan.size()); p.setP2Atteint(atteint); }
                    case 3 -> { p.setP3Total(plan.size()); p.setP3Atteint(atteint); }
                    case 4 -> { p.setP4Total(plan.size()); p.setP4Atteint(atteint); }
                    default -> { }
                }
            }

            int[] r = reliques.getOrDefault(joueurId, new int[] {0, 0, 0});
            p.setR8Plus(r[0]);
            p.setR9Plus(r[1]);
            p.setR10(r[2]);

            ModNumbers m = mods.getOrDefault(joueurId, new ModNumbers());
            p.setMods25Plus(m.nb25Plus);
            p.setModQ(pgPerso > 0
                    ? (((0.8 * m.nb15a19) + m.nb20a24 + (1.2 * m.nb25Plus)) / pgPerso) * 100000.0
                    : 0.0);
            result.add(p);
        }

        result.sort(Comparator
                .comparingInt(PreparationPlayerRow::getP1Manquants).reversed()
                .thenComparing(Comparator.comparingInt(PreparationPlayerRow::getP2Manquants).reversed())
                .thenComparing(PreparationPlayerRow::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    private PreparationSummary calculerPreparationSummary(List<PreparationPlayerRow> joueurs) {
        PreparationSummary s = new PreparationSummary();
        s.setTotalJoueurs(joueurs.size());
        if (joueurs.isEmpty()) return s;

        PreparationPlayerRow first = joueurs.get(0);
        s.setP1Total(first.getP1Total());
        s.setP2Total(first.getP2Total());
        s.setP3Total(first.getP3Total());
        s.setP4Total(first.getP4Total());

        int p1Complets = 0;
        int p2Complets = 0;
        int p3Complets = 0;
        int p4Complets = 0;
        int p1MissingPlayers = 0;
        int p2MissingPlayers = 0;
        int atteint = 0;
        int total = 0;

        for (PreparationPlayerRow p : joueurs) {
            if (p.getP1Total() > 0 && p.isP1Complet()) p1Complets++;
            if (p.getP2Total() > 0 && p.isP2Complet()) p2Complets++;
            if (p.getP3Total() > 0 && p.isP3Complet()) p3Complets++;
            if (p.getP4Total() > 0 && p.isP4Complet()) p4Complets++;
            if (p.getP1Manquants() > 0) p1MissingPlayers++;
            if (p.getP2Manquants() > 0) p2MissingPlayers++;

            atteint += p.getP1Atteint() + p.getP2Atteint() + p.getP3Atteint() + p.getP4Atteint();
            total += p.getP1Total() + p.getP2Total() + p.getP3Total() + p.getP4Total();
        }

        s.setP1Complets(p1Complets);
        s.setP2Complets(p2Complets);
        s.setP3Complets(p3Complets);
        s.setP4Complets(p4Complets);
        s.setJoueursAvecP1Manquante(p1MissingPlayers);
        s.setJoueursAvecP2Manquante(p2MissingPlayers);
        s.setPlanAtteint(atteint);
        s.setPlanTotal(total);
        s.setPlanPourcentage(total > 0 ? atteint * 100.0 / total : 0.0);
        return s;
    }


    private List<OmicronMissingRow> chargerOmicronsManquantsTw() {
        return jdbc.query("""
                SELECT p.priorite,
                       j.id AS joueur_id,
                       j.player_id,
                       COALESCE(NULLIF(j.nom, ''), j.player_id) AS joueur_nom,
                       d.base_id,
                       d.skill_id,
                       d.type_capacite,
                       d.numero_capacite,
                       COALESCE(NULLIF(pd.nom_francais, ''), NULLIF(pd.nom_brut, ''), d.base_id) AS personnage_nom
                FROM omicron_priorite p
                JOIN omicron_definition d
                  ON d.id = p.omicron_definition_id
                 AND d.actif = 1
                CROSS JOIN joueur j
                LEFT JOIN personnage_definition pd
                  ON pd.base_id = d.base_id
                LEFT JOIN omicron o
                  ON o.joueur_id = j.player_id
                 AND o.skill_id = d.skill_id
                 AND o.actif = 1
                WHERE p.actif = 1
                  AND p.priorite BETWEEN 1 AND 4
                  AND d.categorie = 'TW'
                  AND d.skill_id IS NOT NULL
                  AND j.dans_guilde = 1
                  AND o.id IS NULL
                ORDER BY p.priorite, joueur_nom, personnage_nom, d.type_capacite, d.numero_capacite
                """,
                (rs, rowNum) -> {
                    OmicronMissingRow row = new OmicronMissingRow();
                    row.setPriorite(rs.getInt("priorite"));
                    row.setJoueurId(rs.getInt("joueur_id"));
                    row.setPlayerId(rs.getString("player_id"));
                    row.setJoueurNom(rs.getString("joueur_nom"));
                    row.setBaseId(rs.getString("base_id"));
                    row.setPersonnageNom(rs.getString("personnage_nom"));
                    row.setSkillId(rs.getString("skill_id"));
                    Object numero = rs.getObject("numero_capacite");
                    Integer numeroCapacite = numero instanceof Number n ? n.intValue() : null;
                    row.setCapaciteLabel(OmicronCatalogueService.libelleType(
                            rs.getString("type_capacite"), numeroCapacite));
                    return row;
                });
    }

    private Map<Integer, Set<String>> chargerPrioritesTw() {
        Map<Integer, Set<String>> map = new LinkedHashMap<>();
        for (int i = 1; i <= 4; i++) map.put(i, new HashSet<>());

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.priorite, d.skill_id
                FROM omicron_priorite p
                JOIN omicron_definition d ON d.id = p.omicron_definition_id
                WHERE p.actif = 1
                  AND d.actif = 1
                  AND d.categorie = 'TW'
                  AND d.skill_id IS NOT NULL
                """);
        for (Map<String, Object> row : rows) {
            int priorite = number(row.get("priorite"));
            String skillId = string(row.get("skill_id"));
            if (priorite >= 1 && priorite <= 4 && skillId != null && !skillId.isBlank()) {
                map.computeIfAbsent(priorite, ignored -> new HashSet<>()).add(skillId);
            }
        }
        return map;
    }

    private Map<String, Set<String>> chargerPossessionsTw() {
        Map<String, Set<String>> map = new HashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT o.joueur_id, o.skill_id
                FROM omicron o
                JOIN joueur j ON j.player_id = o.joueur_id
                JOIN omicron_definition d ON d.skill_id = o.skill_id AND d.actif = 1
                WHERE j.dans_guilde = 1
                  AND o.actif = 1
                  AND d.categorie = 'TW'
                  AND o.skill_id IS NOT NULL
                """);
        for (Map<String, Object> row : rows) {
            String playerId = string(row.get("joueur_id"));
            String skillId = string(row.get("skill_id"));
            if (playerId == null || skillId == null) continue;
            map.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(skillId);
        }
        return map;
    }

    private Map<Integer, int[]> chargerProfondeurReliques() {
        Map<Integer, int[]> map = new HashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.joueur_id,
                       SUM(CASE WHEN p.relic >= 8 THEN 1 ELSE 0 END) AS r8_plus,
                       SUM(CASE WHEN p.relic >= 9 THEN 1 ELSE 0 END) AS r9_plus,
                       SUM(CASE WHEN p.relic >= 10 THEN 1 ELSE 0 END) AS r10
                FROM personnage p
                JOIN joueur j ON j.id = p.joueur_id AND j.dans_guilde = 1
                JOIN (
                    SELECT joueur_id, MAX(extraction_id) AS extraction_id
                    FROM personnage
                    GROUP BY joueur_id
                ) latest
                  ON latest.joueur_id = p.joueur_id
                 AND latest.extraction_id = p.extraction_id
                GROUP BY p.joueur_id
                """);
        for (Map<String, Object> row : rows) {
            map.put(number(row.get("joueur_id")), new int[] {
                    number(row.get("r8_plus")),
                    number(row.get("r9_plus")),
                    number(row.get("r10"))
            });
        }
        return map;
    }

    private Map<Integer, ModNumbers> chargerMods() {
        Map<Integer, ModNumbers> map = new HashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT j.id AS joueur_id,
                       SUM(CASE WHEN m.vitesse BETWEEN 15 AND 19 THEN 1 ELSE 0 END) AS nb15_19,
                       SUM(CASE WHEN m.vitesse BETWEEN 20 AND 24 THEN 1 ELSE 0 END) AS nb20_24,
                       SUM(CASE WHEN m.vitesse >= 25 THEN 1 ELSE 0 END) AS nb25
                FROM joueur j
                LEFT JOIN mods m ON m.joueur_id = j.id
                WHERE j.dans_guilde = 1
                GROUP BY j.id
                """);
        for (Map<String, Object> row : rows) {
            ModNumbers m = new ModNumbers();
            m.nb15a19 = number(row.get("nb15_19"));
            m.nb20a24 = number(row.get("nb20_24"));
            m.nb25Plus = number(row.get("nb25"));
            map.put(number(row.get("joueur_id")), m);
        }
        return map;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private long longNumber(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static class ModNumbers {
        int nb15a19;
        int nb20a24;
        int nb25Plus;
    }
}
