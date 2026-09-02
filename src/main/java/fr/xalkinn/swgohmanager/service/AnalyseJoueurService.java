package fr.xalkinn.swgohmanager.service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.client.SwgohPlayerClient;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.ComparaisonGuilde;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.MetricComparison;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.ModStats;
import fr.xalkinn.swgohmanager.dto.AnalyseJoueur.RosterCharacter;

@Service
public class AnalyseJoueurService {

    private static final String STAT_PG_TOTAL = "STAT_GALACTIC_POWER_ACQUIRED_NAME";
    private static final String STAT_PG_PERSO = "STAT_CHARACTER_GALACTIC_POWER_ACQUIRED_NAME";
    private static final String STAT_PG_VAISSEAUX = "STAT_SHIP_GALACTIC_POWER_ACQUIRED_NAME";

    private final SwgohPlayerClient playerClient;
    private final PersonnageDefinitionService personnageDefinitionService;
    private final JdbcTemplate jdbc;

    public AnalyseJoueurService(
            SwgohPlayerClient playerClient,
            PersonnageDefinitionService personnageDefinitionService,
            JdbcTemplate jdbc) {
        this.playerClient = playerClient;
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
        analyse.setComparaisonGuilde(construireComparaisonGuilde(analyse));

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
        return baseId;
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
