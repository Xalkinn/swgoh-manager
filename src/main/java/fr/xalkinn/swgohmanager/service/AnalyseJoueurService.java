package fr.xalkinn.swgohmanager.service;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.client.SwgohGuildClient;
import fr.xalkinn.swgohmanager.client.SwgohPlayerClient;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.ComparaisonGuilde;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.MetricComparison;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.ModStats;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.OmicronPriorityDetail;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.OmicronPrioritySummary;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.RosterCharacter;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.TbMissionPerformance;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.TbPerformance;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.TbPhasePerformance;
import fr.xalkinn.swgohmanager.util.MapStatIdParser;
import fr.xalkinn.swgohmanager.util.MapStatIdParser.ParsedMapStat;

@Service
public class AnalyseJoueurService {

    private static final String STAT_PG_TOTAL = "STAT_GALACTIC_POWER_ACQUIRED_NAME";
    private static final String STAT_PG_PERSO = "STAT_CHARACTER_GALACTIC_POWER_ACQUIRED_NAME";
    private static final String STAT_PG_VAISSEAUX = "STAT_SHIP_GALACTIC_POWER_ACQUIRED_NAME";

    private final SwgohPlayerClient playerClient;
    private final SwgohGuildClient guildClient;
    private final PersonnageDefinitionService personnageDefinitionService;
    private final JdbcTemplate jdbc;

    public AnalyseJoueurService(
            SwgohPlayerClient playerClient,
            SwgohGuildClient guildClient,
            PersonnageDefinitionService personnageDefinitionService,
            JdbcTemplate jdbc) {
        this.playerClient = playerClient;
        this.guildClient = guildClient;
        this.personnageDefinitionService = personnageDefinitionService;
        this.jdbc = jdbc;
    }

    /**
     * Analyse ponctuellement un joueur depuis Comlink.
     * Aucune donnée du candidat n'est insérée en base.
     */
    public AnalyseJoueur analyser(String allyCode) throws Exception {
        JSONObject player = playerClient.rechercherParAllyCode(allyCode);

        AnalyseJoueur analyse = new AnalyseJoueur();
        remplirProfil(analyse, player);
        remplirRosterEtMods(analyse, player);
        remplirPlanOmicron(analyse, player);
        analyse.setComparaisonGuilde(construireComparaisonGuilde(analyse));
        remplirPerformancesTb(analyse);

        return analyse;
    }

    private void remplirProfil(AnalyseJoueur analyse, JSONObject player) {
        analyse.setNom(player.optString("name", "Joueur inconnu"));
        analyse.setAllyCode(player.optString("allyCode", ""));
        analyse.setPlayerId(player.optString("playerId", ""));
        analyse.setGuildId(player.optString("guildId", ""));
        analyse.setGuildName(player.optString("guildName", ""));
        analyse.setNiveau(player.optInt("level", 0));

        JSONArray profileStats = player.optJSONArray("profileStat");
        analyse.setPgTotal(lireStat(profileStats, STAT_PG_TOTAL));
        analyse.setPgPerso(lireStat(profileStats, STAT_PG_PERSO));
        analyse.setPgVaisseaux(lireStat(profileStats, STAT_PG_VAISSEAUX));

        JSONArray pvpProfiles = player.optJSONArray("pvpProfile");
        if (pvpProfiles != null) {
            for (int i = 0; i < pvpProfiles.length(); i++) {
                JSONObject pvp = pvpProfiles.optJSONObject(i);
                if (pvp == null) continue;

                int tab = pvp.optInt("tab", 0);
                int rank = pvp.optInt("rank", 0);
                if (tab == 1) {
                    analyse.setRangArenePersonnages(rank);
                } else if (tab == 2) {
                    analyse.setRangAreneFlotte(rank);
                }
            }
        }

        JSONObject playerRating = player.optJSONObject("playerRating");
        if (playerRating != null) {
            JSONObject skill = playerRating.optJSONObject("playerSkillRating");
            if (skill != null) {
                analyse.setSkillRatingGac(skill.optInt("skillRating", 0));
            }

            JSONObject rank = playerRating.optJSONObject("playerRankStatus");
            if (rank != null) {
                analyse.setLigueGac(rank.optString("leagueId", ""));
            }
        }
    }

    private void remplirRosterEtMods(AnalyseJoueur analyse, JSONObject player) {
        JSONArray roster = player.optJSONArray("rosterUnit");
        if (roster == null) {
            throw new IllegalStateException("La réponse Comlink ne contient aucun roster.");
        }

        Map<String, String> nomsFrancais =
                personnageDefinitionService.chargerIndexNomsFrancais();

        List<RosterCharacter> personnages = new ArrayList<>();

        int totalVaisseaux = 0;
        int stars7 = 0;
        int g13 = 0;
        int reliques = 0;
        int r5Plus = 0;
        int r7Plus = 0;
        int r8Plus = 0;
        int r9Plus = 0;
        int r10 = 0;

        ModStats modStats = new ModStats();
        long sommeVitesses = 0L;
        int modsAvecVitesse = 0;

        for (int i = 0; i < roster.length(); i++) {
            JSONObject unit = roster.optJSONObject(i);
            if (unit == null) continue;

            JSONObject relicObject = unit.optJSONObject("relic");

            // Dans la réponse /player actuelle de Comlink, les vaisseaux ont relic=null.
            // Cela permet de ne pas mélanger les vaisseaux aux statistiques personnages.
            if (relicObject == null) {
                totalVaisseaux++;
                continue;
            }

            String definitionId = unit.optString("definitionId", "");
            String baseId = normaliserBaseId(definitionId);
            String nomBrut = baseId;
            String nomFrancais = personnageDefinitionService.nomAffiche(
                    nomsFrancais,
                    baseId,
                    nomBrut);

            int etoiles = unit.optInt("currentRarity", 0);
            int niveau = unit.optInt("currentLevel", 0);
            int gear = unit.optInt("currentTier", 0);
            int relic = calculerRelique(gear, relicObject);

            personnages.add(new RosterCharacter(
                    nomBrut,
                    nomFrancais,
                    baseId,
                    etoiles,
                    niveau,
                    gear,
                    relic));

            if (etoiles >= 7) stars7++;
            if (gear >= 13) g13++;
            if (relic > 0) reliques++;
            if (relic >= 5) r5Plus++;
            if (relic >= 7) r7Plus++;
            if (relic >= 8) r8Plus++;
            if (relic >= 9) r9Plus++;
            if (relic >= 10) r10++;

            JSONArray mods = unit.optJSONArray("equippedStatMod");
            if (mods == null) continue;

            for (int m = 0; m < mods.length(); m++) {
                JSONObject mod = mods.optJSONObject(m);
                if (mod == null) continue;

                modStats.setTotal(modStats.getTotal() + 1);

                int vitesse = extraireVitesseSecondaire(mod);
                if (vitesse <= 0) {
                    modStats.setSansVitesse(modStats.getSansVitesse() + 1);
                } else {
                    sommeVitesses += vitesse;
                    modsAvecVitesse++;

                    if (vitesse <= 14) {
                        modStats.setVitesse1a14(modStats.getVitesse1a14() + 1);
                    } else if (vitesse <= 19) {
                        modStats.setVitesse15a19(modStats.getVitesse15a19() + 1);
                    } else if (vitesse <= 24) {
                        modStats.setVitesse20a24(modStats.getVitesse20a24() + 1);
                    } else {
                        modStats.setVitesse25Plus(modStats.getVitesse25Plus() + 1);
                    }
                }

                long xp = mod.optLong("xp", 0L);
                if (xp == 1105000L || xp == 1105005L) {
                    modStats.setModsOr(modStats.getModsOr() + 1);
                } else {
                    modStats.setModsNormaux(modStats.getModsNormaux() + 1);
                }
            }
        }

        personnages.sort(Comparator.comparing(
                RosterCharacter::getNomFrancais,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        if (modsAvecVitesse > 0) {
            modStats.setVitesseMoyenne((double) sommeVitesses / modsAvecVitesse);
        }

        if (analyse.getPgPerso() > 0) {
            double modQ = (((0.8 * modStats.getVitesse15a19())
                    + modStats.getVitesse20a24()
                    + (1.2 * modStats.getVitesse25Plus()))
                    / analyse.getPgPerso()) * 100000.0;
            modStats.setModQ(modQ);
        }

        analyse.setRoster(personnages);
        analyse.setTotalPersonnages(personnages.size());
        analyse.setTotalVaisseaux(totalVaisseaux);
        analyse.setPersonnages7Etoiles(stars7);
        analyse.setPersonnagesG13(g13);
        analyse.setPersonnagesRelique(reliques);
        analyse.setPersonnagesR5Plus(r5Plus);
        analyse.setPersonnagesR7Plus(r7Plus);
        analyse.setPersonnagesR8Plus(r8Plus);
        analyse.setPersonnagesR9Plus(r9Plus);
        analyse.setPersonnagesR10(r10);
        analyse.setModStats(modStats);
    }


    /**
     * Compare le roster /player du candidat au plan P1-P4 configuré dans la
     * guilde, sans écrire le candidat dans la table omicron.
     */
    private void remplirPlanOmicron(AnalyseJoueur analyse, JSONObject player) {
        List<Map<String, Object>> planRows = jdbc.queryForList("""
                SELECT
                    p.priorite,
                    d.base_id,
                    d.skill_id,
                    d.type_capacite,
                    d.numero_capacite,
                    d.categorie,
                    d.tier_requis
                FROM omicron_priorite p
                JOIN omicron_definition d ON d.id = p.omicron_definition_id
                WHERE p.actif = 1
                  AND d.actif = 1
                ORDER BY p.priorite, d.categorie, d.base_id, d.type_capacite, d.numero_capacite
                """);

        Map<Integer, OmicronPrioritySummary> priorites = new LinkedHashMap<>();
        for (int priorite = 1; priorite <= 4; priorite++) {
            OmicronPrioritySummary summary = new OmicronPrioritySummary();
            summary.setPriorite(priorite);
            priorites.put(priorite, summary);
        }

        if (planRows.isEmpty()) {
            analyse.setOmicronPriorities(new ArrayList<>(priorites.values()));
            return;
        }

        Map<String, Map<String, Integer>> skillsRoster = lireSkillsRoster(player);
        Map<String, String> nomsFrancais = personnageDefinitionService.chargerIndexNomsFrancais();

        int planAtteint = 0;
        int planTotal = 0;

        for (Map<String, Object> row : planRows) {
            int priorite = number(row.get("priorite"));
            OmicronPrioritySummary summary = priorites.get(priorite);
            if (summary == null) continue;

            summary.setTotal(summary.getTotal() + 1);
            planTotal++;

            String baseId = normaliserBaseId(row.get("base_id") == null ? "" : String.valueOf(row.get("base_id")));
            String skillId = row.get("skill_id") == null ? "" : String.valueOf(row.get("skill_id"));
            Integer tierRequis = nullableInt(row.get("tier_requis"));

            Map<String, Integer> skills = skillsRoster.get(baseId);
            Integer tier = skills == null ? null : skills.get(skillId);
            int tierRosterRequis = tierRequis == null ? Integer.MAX_VALUE : Math.max(0, tierRequis - 1);
            boolean possede = tier != null && tier >= tierRosterRequis;

            if (possede) {
                summary.setAtteint(summary.getAtteint() + 1);
                planAtteint++;
                continue;
            }

            String type = row.get("type_capacite") == null ? "AUTRE" : String.valueOf(row.get("type_capacite"));
            Integer numero = nullableInt(row.get("numero_capacite"));
            String categorie = row.get("categorie") == null ? null : String.valueOf(row.get("categorie"));
            String nomPersonnage = personnageDefinitionService.nomAffiche(nomsFrancais, baseId, baseId);

            summary.getManquantsDetails().add(new OmicronPriorityDetail(
                    nomPersonnage,
                    OmicronCatalogueService.libelleType(type, numero),
                    categorie,
                    OmicronCatalogueService.categorieLabel(categorie)));
        }

        List<OmicronPrioritySummary> result = new ArrayList<>();
        for (OmicronPrioritySummary summary : priorites.values()) {
            summary.setManquants(Math.max(0, summary.getTotal() - summary.getAtteint()));
            summary.setPourcentage(summary.getTotal() > 0
                    ? summary.getAtteint() * 100.0 / summary.getTotal()
                    : 0.0);
            result.add(summary);
        }

        analyse.setOmicronPriorities(result);
        analyse.setOmicronPlanAtteint(planAtteint);
        analyse.setOmicronPlanTotal(planTotal);
        analyse.setOmicronPlanPourcentage(planTotal > 0
                ? planAtteint * 100.0 / planTotal
                : 0.0);
    }

    private Map<String, Map<String, Integer>> lireSkillsRoster(JSONObject player) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        JSONArray roster = player.optJSONArray("rosterUnit");
        if (roster == null) return result;

        for (int i = 0; i < roster.length(); i++) {
            JSONObject unit = roster.optJSONObject(i);
            if (unit == null) continue;

            String baseId = normaliserBaseId(unit.optString("definitionId", ""));
            if (baseId.isBlank()) continue;

            JSONArray skills = unit.optJSONArray("skill");
            if (skills == null) continue;

            Map<String, Integer> bySkill = result.computeIfAbsent(baseId, ignored -> new HashMap<>());
            for (int j = 0; j < skills.length(); j++) {
                JSONObject skill = skills.optJSONObject(j);
                if (skill == null) continue;

                String skillId = skill.optString("id", "").trim();
                if (skillId.isBlank()) continue;
                bySkill.put(skillId, skill.optInt("tier", -1));
            }
        }

        return result;
    }

    /**
     * Charge les résultats TB récents de la guilde ACTUELLE du candidat et
     * extrait uniquement les lignes de son playerId. Les données restent en
     * mémoire et ne sont pas importées dans l'historique TB de notre guilde.
     */
    private void remplirPerformancesTb(AnalyseJoueur analyse) {
        if (analyse.getGuildId() == null || analyse.getGuildId().isBlank()) {
            analyse.setTbMessage("Le joueur n'appartient actuellement à aucune guilde : aucune performance TB récente n'est disponible.");
            return;
        }
        if (analyse.getPlayerId() == null || analyse.getPlayerId().isBlank()) {
            analyse.setTbMessage("Player ID absent : impossible de retrouver les performances TB individuelles.");
            return;
        }

        try {
            JSONObject root = guildClient.recupererGuildeComplete(analyse.getGuildId());
            JSONObject guild = root.optJSONObject("guild");
            if (guild == null) guild = root;

            JSONArray results = guild.optJSONArray("recentTerritoryBattleResult");
            if (results == null) results = root.optJSONArray("recentTerritoryBattleResult");

            if (results == null || results.length() == 0) {
                analyse.setTbMessage("La guilde actuelle du candidat n'expose aucun résultat TB récent via Comlink.");
                return;
            }

            Map<String, String> nomsFrancais = personnageDefinitionService.chargerIndexNomsFrancais();
            List<TbPerformance> performances = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject tb = results.optJSONObject(i);
                if (tb == null) continue;

                TbPerformance performance = extrairePerformanceTb(tb, analyse.getPlayerId(), nomsFrancais);
                if (performance != null) performances.add(performance);
            }

            performances.sort(Comparator.comparingLong(TbPerformance::getEndTimeEpoch).reversed());
            if (performances.size() > 3) {
                performances = new ArrayList<>(performances.subList(0, 3));
            }

            analyse.setTbPerformances(performances);
            if (performances.isEmpty()) {
                analyse.setTbMessage("Aucune statistique individuelle n'a été trouvée pour ce joueur dans les TB récentes exposées par sa guilde.");
            } else if (performances.size() == 1) {
                analyse.setTbMessage("Comlink expose actuellement 1 TB récente exploitable pour ce joueur.");
            } else {
                analyse.setTbMessage("Les " + performances.size() + " dernières TB exposées par Comlink sont affichées.");
            }
        } catch (Exception e) {
            String detail = e.getMessage() == null || e.getMessage().isBlank()
                    ? "service Comlink indisponible"
                    : e.getMessage();
            analyse.setTbMessage("Performances TB indisponibles pour le moment : " + detail);
        }
    }

    private TbPerformance extrairePerformanceTb(
            JSONObject tb,
            String playerId,
            Map<String, String> nomsFrancais) {
        JSONArray finalStats = tb.optJSONArray("finalStat");
        if (finalStats == null || finalStats.length() == 0) return null;

        TbPerformance result = new TbPerformance();
        result.setInstanceId(tb.optString("instanceId", ""));
        result.setDefinitionId(tb.optString("definitionId", ""));
        result.setTotalStars(parseInt(tb.optString("totalStars", "0")));

        long endEpoch = parseEpoch(tb.optString("endTime", "0"));
        result.setEndTimeEpoch(endEpoch);
        result.setDateFin(formaterDateEpoch(endEpoch));

        Map<Integer, TbPhasePerformance> phases = new LinkedHashMap<>();
        Map<String, TbMissionPerformance> missionsSpeciales = new LinkedHashMap<>();
        boolean participe = false;
        boolean zoneZeffoPresente = false;
        long puissanceZones = 0L;
        long donsZones = 0L;

        for (int i = 0; i < finalStats.length(); i++) {
            JSONObject finalStat = finalStats.optJSONObject(i);
            if (finalStat == null) continue;

            ParsedMapStat parsed = MapStatIdParser.parse(finalStat.optString("mapStatId", ""));
            String type = parsed.statType();

            JSONArray zonePlayerStats = finalStat.optJSONArray("playerStat");
            if (parsed.phase() != null
                    && parsed.phase() == 3
                    && parsed.conflict() != null
                    && parsed.conflict() == 1
                    && parsed.bonus()
                    && ("strike_attempt".equals(type)
                        || "strike_encounter".equals(type)
                        || "summary".equals(type)
                        || "power".equals(type))
                    && zonePlayerStats != null
                    && zonePlayerStats.length() > 0) {
                zoneZeffoPresente = true;
            }

            JSONObject playerStat = trouverPlayerStat(zonePlayerStats, playerId);
            if (playerStat == null) continue;
            participe = true;

            long score = parseLong(playerStat.optString("score", "0"));

            if (parsed.phase() != null
                    && parsed.conflict() != null
                    && parsed.covertNum() != null
                    && ("covert_round_attempted".equals(type) || "covert_complete".equals(type))
                    && score > 0) {
                String missionKey = parsed.phase() + "-" + parsed.conflict() + "-"
                        + parsed.bonus() + "-" + parsed.covertNum();
                TbMissionPerformance mission = missionsSpeciales.computeIfAbsent(missionKey, ignored -> {
                    TbMissionPerformance m = new TbMissionPerformance();
                    m.setPhase(parsed.phase());
                    m.setConflict(parsed.conflict());
                    m.setBonus(parsed.bonus());
                    m.setCovertNum(parsed.covertNum());
                    m.setLabel(labelMission(parsed.phase(), parsed.conflict(), parsed.bonus(), parsed.covertNum(), nomsFrancais));
                    return m;
                });

                if ("covert_round_attempted".equals(type)) {
                    mission.setTentee(true);
                } else if ("covert_complete".equals(type)) {
                    // Une réussite implique nécessairement que la mission a été tentée.
                    mission.setTentee(true);
                    mission.setReussie(true);
                }
            }

            if (parsed.roundNum() != null) {
                switch (type) {
                    case "strike_attempt" -> result.setCombatsTentes(result.getCombatsTentes() + score);
                    case "strike_encounter" -> result.setVagues(result.getVagues() + score);
                    case "power" -> result.setPuissanceDeployee(result.getPuissanceDeployee() + score);
                    case "unit_donated" -> result.setUnitesDonnees(result.getUnitesDonnees() + score);
                    default -> { }
                }
            }

            if (parsed.phase() != null) {
                TbPhasePerformance phase = phases.computeIfAbsent(parsed.phase(), phaseNum -> {
                    TbPhasePerformance p = new TbPhasePerformance();
                    p.setPhase(phaseNum);
                    return p;
                });

                switch (type) {
                    case "strike_attempt" -> phase.setCombatsTentes(phase.getCombatsTentes() + score);
                    case "strike_encounter" -> phase.setVagues(phase.getVagues() + score);
                    case "power" -> puissanceZones += score;
                    case "unit_donated" -> donsZones += score;
                    default -> { }
                }
            }
        }

        if (!participe) return null;

        if (zoneZeffoPresente) {
            TbMissionPerformance combatJkckZeffo = new TbMissionPerformance();
            combatJkckZeffo.setPhase(3);
            combatJkckZeffo.setConflict(1);
            combatJkckZeffo.setBonus(true);
            combatJkckZeffo.setCovertNum(0);
            combatJkckZeffo.setLabel("Jedi Knight Cal Kestis — CM Zeffo (1 023 750 pts)");
            combatJkckZeffo.setSuiviExact(false);
            combatJkckZeffo.setCombatPrioritaire(true);
            combatJkckZeffo.setNote("Le résultat final Comlink agrège les combats de Zeffo : la tentative et la réussite de cette mission ne peuvent pas être isolées automatiquement par joueur.");
            missionsSpeciales.put("zeffo-jkck-prioritaire", combatJkckZeffo);
        }

        List<TbMissionPerformance> missionList = new ArrayList<>(missionsSpeciales.values());
        missionList.sort(Comparator
                .comparingInt(TbMissionPerformance::getPhase)
                .thenComparingInt(TbMissionPerformance::getConflict)
                .thenComparing(TbMissionPerformance::isBonus)
                .thenComparingInt(TbMissionPerformance::getCovertNum));

        long totalMsTentees = 0L;
        long totalMsReussies = 0L;
        for (TbMissionPerformance mission : missionList) {
            if (!mission.isSuiviExact()) {
                continue;
            }
            TbPhasePerformance phase = phases.computeIfAbsent(mission.getPhase(), phaseNum -> {
                TbPhasePerformance p = new TbPhasePerformance();
                p.setPhase(phaseNum);
                return p;
            });
            if (mission.isTentee()) {
                totalMsTentees++;
                phase.setMsTentees(phase.getMsTentees() + 1);
            }
            if (mission.isReussie()) {
                totalMsReussies++;
                phase.setMsReussies(phase.getMsReussies() + 1);
            }
        }

        long combatsPhases = 0L;
        long vaguesPhases = 0L;
        List<TbPhasePerformance> phaseList = new ArrayList<>(phases.values());
        phaseList.sort(Comparator.comparingInt(TbPhasePerformance::getPhase));

        for (TbPhasePerformance phase : phaseList) {
            phase.setVaguesParCombat(phase.getCombatsTentes() > 0
                    ? phase.getVagues() * 1.0 / phase.getCombatsTentes()
                    : 0.0);
            combatsPhases += phase.getCombatsTentes();
            vaguesPhases += phase.getVagues();
        }

        // Les totaux ROUND sont la référence afin d'éviter de recompter les
        // mêmes points dans les différentes zones. Fallback sur les zones si
        // une future réponse Comlink ne fournit plus ces agrégats ROUND.
        if (result.getCombatsTentes() == 0 && combatsPhases > 0) {
            result.setCombatsTentes(combatsPhases);
        }
        if (result.getVagues() == 0 && vaguesPhases > 0) {
            result.setVagues(vaguesPhases);
        }
        if (result.getPuissanceDeployee() == 0 && puissanceZones > 0) {
            result.setPuissanceDeployee(puissanceZones);
        }
        if (result.getUnitesDonnees() == 0 && donsZones > 0) {
            result.setUnitesDonnees(donsZones);
        }

        result.setMsTentees(totalMsTentees);
        result.setMsReussies(totalMsReussies);
        result.setVaguesParCombat(result.getCombatsTentes() > 0
                ? result.getVagues() * 1.0 / result.getCombatsTentes()
                : 0.0);
        result.setTauxMs(totalMsTentees > 0
                ? totalMsReussies * 100.0 / totalMsTentees
                : 0.0);
        result.setPhases(phaseList);
        result.setMissionsSpeciales(missionList);
        return result;
    }

    private String labelMission(
            int phase,
            int conflict,
            boolean bonus,
            int covert,
            Map<String, String> nomsFrancais) {
        String key = phase + "-" + conflict + "-" + bonus + "-" + covert;
        return switch (key) {
            case "1-3-false-1" -> nomMissionPersonnage(nomsFrancais, "QIRA", "Qi'ra");
            case "2-1-false-1" -> nomMissionPersonnage(nomsFrancais, "CEREJUNDA", "Cere Junda") + " + Cal Kestis";
            case "3-1-false-1" -> nomMissionPersonnage(nomsFrancais, "SAWGERRERA", "Saw Gerrera");
            case "3-3-false-1" -> nomMissionPersonnage(nomsFrancais, "THIRDSISTER", "Troisième Sœur (Reva)");
            case "3-3-false-2" -> nomMissionPersonnage(nomsFrancais, "MANDALORBOKATAN", "Bo-Katan (Mandalore)");
            case "3-2-false-1" -> nomMissionPersonnage(nomsFrancais, "MERRIN", "Merrin");
            case "3-1-true-1" -> "Clones";
            case "4-2-false-1" -> "Inquisiteurs";
            case "4-3-false-1" -> nomMissionPersonnage(nomsFrancais, "L3_37", "L3-37");
            case "5-3-false-1" -> nomMissionPersonnage(nomsFrancais, "YOUNGHAN", "Jeune Han Solo");
            default -> "Phase " + phase + " · Zone " + conflict
                    + (bonus ? " · Bonus" : "")
                    + " · Mission spéciale " + covert;
        };
    }

    private String nomMissionPersonnage(
            Map<String, String> nomsFrancais,
            String baseId,
            String fallbackFrancais) {
        String nom = personnageDefinitionService.nomAffiche(nomsFrancais, baseId, fallbackFrancais);
        return nom == null || nom.isBlank() ? fallbackFrancais : nom;
    }

    private JSONObject trouverPlayerStat(JSONArray playerStats, String playerId) {
        if (playerStats == null) return null;
        for (int i = 0; i < playerStats.length(); i++) {
            JSONObject stat = playerStats.optJSONObject(i);
            if (stat != null && playerId.equals(stat.optString("memberId", ""))) {
                return stat;
            }
        }
        return null;
    }

    private long parseEpoch(String raw) {
        long value = parseLong(raw);
        if (value <= 0) return 0L;
        return value > 100_000_000_000L ? value : value * 1000L;
    }

    private String formaterDateEpoch(long epochMillis) {
        if (epochMillis <= 0) return "Date inconnue";
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int parseInt(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Integer nullableInt(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private ComparaisonGuilde construireComparaisonGuilde(AnalyseJoueur analyse) {
        ComparaisonGuilde comparaison = new ComparaisonGuilde();

        List<Map<String, Object>> joueurs = jdbc.queryForList("""
                SELECT id, nom, pg_perso
                FROM joueur
                WHERE dans_guilde = 1
                ORDER BY nom
                """);

        if (joueurs.isEmpty()) {
            return comparaison;
        }

        Map<Integer, GuildMetrics> metrics = new LinkedHashMap<>();
        for (Map<String, Object> row : joueurs) {
            int joueurId = number(row.get("id"));
            GuildMetrics gm = new GuildMetrics();
            gm.pgPerso = longNumber(row.get("pg_perso"));
            metrics.put(joueurId, gm);
        }

        List<Map<String, Object>> relicRows = jdbc.queryForList("""
                SELECT
                    p.joueur_id,
                    SUM(CASE WHEN p.relic >= 7 THEN 1 ELSE 0 END) AS r7_plus,
                    SUM(CASE WHEN p.relic >= 8 THEN 1 ELSE 0 END) AS r8_plus,
                    SUM(CASE WHEN p.relic >= 9 THEN 1 ELSE 0 END) AS r9_plus,
                    SUM(CASE WHEN p.relic >= 10 THEN 1 ELSE 0 END) AS r10
                FROM personnage p
                INNER JOIN (
                    SELECT joueur_id, MAX(extraction_id) AS extraction_id
                    FROM personnage
                    GROUP BY joueur_id
                ) latest
                    ON latest.joueur_id = p.joueur_id
                   AND latest.extraction_id = p.extraction_id
                GROUP BY p.joueur_id
                """);

        for (Map<String, Object> row : relicRows) {
            GuildMetrics gm = metrics.get(number(row.get("joueur_id")));
            if (gm == null) continue;
            gm.r7Plus = number(row.get("r7_plus"));
            gm.r8Plus = number(row.get("r8_plus"));
            gm.r9Plus = number(row.get("r9_plus"));
            gm.r10 = number(row.get("r10"));
        }

        List<Map<String, Object>> modRows = jdbc.queryForList("""
                SELECT
                    joueur_id,
                    SUM(CASE WHEN vitesse BETWEEN 15 AND 19 THEN 1 ELSE 0 END) AS m15_19,
                    SUM(CASE WHEN vitesse BETWEEN 20 AND 24 THEN 1 ELSE 0 END) AS m20_24,
                    SUM(CASE WHEN vitesse >= 25 THEN 1 ELSE 0 END) AS m25_plus
                FROM mods
                GROUP BY joueur_id
                """);

        for (Map<String, Object> row : modRows) {
            GuildMetrics gm = metrics.get(number(row.get("joueur_id")));
            if (gm == null) continue;
            gm.mods15a19 = number(row.get("m15_19"));
            gm.mods20a24 = number(row.get("m20_24"));
            gm.mods25Plus = number(row.get("m25_plus"));
        }

        List<GuildMetrics> guild = new ArrayList<>(metrics.values());
        comparaison.setNombreJoueurs(guild.size());

        ModStats candidatMods = analyse.getModStats();
        List<MetricComparison> lignes = new ArrayList<>();

        lignes.add(metricEntiere(
                "pg",
                "PG personnages",
                analyse.getPgPerso(),
                valeurs(guild, g -> g.pgPerso)));

        lignes.add(metricEntiere(
                "r7",
                "Personnages R7+",
                analyse.getPersonnagesR7Plus(),
                valeurs(guild, g -> g.r7Plus)));

        lignes.add(metricEntiere(
                "r8",
                "Personnages R8+",
                analyse.getPersonnagesR8Plus(),
                valeurs(guild, g -> g.r8Plus)));

        lignes.add(metricEntiere(
                "r9",
                "Personnages R9+",
                analyse.getPersonnagesR9Plus(),
                valeurs(guild, g -> g.r9Plus)));

        lignes.add(metricEntiere(
                "r10",
                "Personnages R10",
                analyse.getPersonnagesR10(),
                valeurs(guild, g -> g.r10)));

        lignes.add(metricEntiere(
                "mods20",
                "Mods vitesse 20+",
                candidatMods.getVitesse20Plus(),
                valeurs(guild, g -> g.mods20a24 + g.mods25Plus)));

        lignes.add(metricEntiere(
                "mods25",
                "Mods vitesse 25+",
                candidatMods.getVitesse25Plus(),
                valeurs(guild, g -> g.mods25Plus)));

        List<Double> modQGuilde = new ArrayList<>();
        for (GuildMetrics gm : guild) {
            modQGuilde.add(gm.modQ());
        }
        lignes.add(metricDecimale(
                "modq",
                "Score ModQ",
                candidatMods.getModQ(),
                modQGuilde));

        comparaison.setMetriques(lignes);
        return comparaison;
    }

    private MetricComparison metricEntiere(
            String code,
            String label,
            double candidat,
            List<Double> guilde) {
        return creerMetric(code, label, candidat, guilde, false);
    }

    private MetricComparison metricDecimale(
            String code,
            String label,
            double candidat,
            List<Double> guilde) {
        return creerMetric(code, label, candidat, guilde, true);
    }

    private MetricComparison creerMetric(
            String code,
            String label,
            double candidat,
            List<Double> guilde,
            boolean decimal) {

        double moyenne = moyenne(guilde);
        double mediane = mediane(guilde);
        double ecart = mediane == 0.0
                ? (candidat == 0.0 ? 0.0 : 100.0)
                : ((candidat - mediane) / mediane) * 100.0;

        String tendance;
        String libelle;
        if (ecart >= 10.0) {
            tendance = "above";
            libelle = "Au-dessus";
        } else if (ecart <= -10.0) {
            tendance = "below";
            libelle = "En dessous";
        } else {
            tendance = "near";
            libelle = "Proche de la médiane";
        }

        return new MetricComparison(
                code,
                label,
                decimal ? formatDecimal(candidat) : formatInteger(candidat),
                decimal ? formatDecimal(moyenne) : formatInteger(moyenne),
                decimal ? formatDecimal(mediane) : formatInteger(mediane),
                formatEcart(ecart),
                tendance,
                libelle);
    }

    private List<Double> valeurs(List<GuildMetrics> guild, ValueExtractor extractor) {
        List<Double> values = new ArrayList<>();
        for (GuildMetrics gm : guild) {
            values.add(extractor.get(gm));
        }
        return values;
    }

    private long lireStat(JSONArray stats, String nameKey) {
        if (stats == null) return 0L;

        for (int i = 0; i < stats.length(); i++) {
            JSONObject stat = stats.optJSONObject(i);
            if (stat == null) continue;

            if (nameKey.equals(stat.optString("nameKey"))) {
                return stat.optLong("value", 0L);
            }
        }

        return 0L;
    }

    private int calculerRelique(int gear, JSONObject relicObject) {
        if (gear < 13 || relicObject == null) return 0;
        return Math.max(0, relicObject.optInt("currentTier", 0) - 2);
    }

    private int extraireVitesseSecondaire(JSONObject mod) {
        JSONArray secondaryStats = mod.optJSONArray("secondaryStat");
        if (secondaryStats == null) return 0;

        for (int i = 0; i < secondaryStats.length(); i++) {
            JSONObject secondary = secondaryStats.optJSONObject(i);
            if (secondary == null) continue;

            JSONObject stat = secondary.optJSONObject("stat");
            if (stat == null || stat.optInt("unitStatId", -1) != 5) continue;

            // Comlink expose la vitesse sous la forme 250000 pour +25.
            long statValue = stat.optLong("statValueDecimal", 0L);
            return (int) (statValue / 10000L);
        }

        return 0;
    }

    private String normaliserBaseId(String definitionId) {
        if (definitionId == null) return "";
        String baseId = definitionId.trim();
        int separator = baseId.indexOf(':');
        if (separator >= 0) {
            baseId = baseId.substring(0, separator);
        }
        return baseId.replace(",", "").trim();
    }

    private int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private long longNumber(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private double moyenne(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double total = 0.0;
        for (double value : values) total += value;
        return total / values.size();
    }

    private double mediane(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) return sorted.get(size / 2);
        return (sorted.get((size / 2) - 1) + sorted.get(size / 2)) / 2.0;
    }

    private String formatInteger(double value) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.FRANCE);
        nf.setMaximumFractionDigits(0);
        return nf.format(Math.round(value));
    }

    private String formatDecimal(double value) {
        return String.format(Locale.FRANCE, "%.2f", value);
    }

    private String formatEcart(double value) {
        return String.format(Locale.FRANCE, "%+.1f %%", value);
    }

    @FunctionalInterface
    private interface ValueExtractor {
        double get(GuildMetrics metrics);
    }

    private static class GuildMetrics {
        private long pgPerso;
        private int r7Plus;
        private int r8Plus;
        private int r9Plus;
        private int r10;
        private int mods15a19;
        private int mods20a24;
        private int mods25Plus;

        private double modQ() {
            if (pgPerso <= 0) return 0.0;
            return (((0.8 * mods15a19) + mods20a24 + (1.2 * mods25Plus))
                    / pgPerso) * 100000.0;
        }
    }
}
