package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.dto.DatacronView;
import fr.xalkinn.swgohmanager.dto.DatacronInventoryView;
import fr.xalkinn.swgohmanager.dto.DatacronInventoryView.PlayerRow;
import fr.xalkinn.swgohmanager.dto.DatacronView.AffixDetail;
import fr.xalkinn.swgohmanager.dto.DatacronView.DatacronDetail;
import fr.xalkinn.swgohmanager.dto.DatacronView.DatacronSetGroupe;
import fr.xalkinn.swgohmanager.dto.DatacronView.JoueurResume;
import fr.xalkinn.swgohmanager.dto.DatacronView.StatResume;
import fr.xalkinn.swgohmanager.util.DatacronDisplayUtil;

/**
 * Persistance de l'état courant des Datacrons.
 *
 * Cette classe ne dépend pas du Batch Roster. Un joueur est remplacé de façon
 * atomique à chaque passage du Batch DTC dédié.
 */
@Service
public class DatacronService {

    private final JdbcTemplate jdbc;
    private final LocalizationService localizationService;

    public DatacronService(JdbcTemplate jdbc, LocalizationService localizationService) {
        this.jdbc = jdbc;
        this.localizationService = localizationService;
        creerTablesSiNecessaire();
    }

    private void creerTablesSiNecessaire() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS player_datacron_actuel (
                id BIGINT NOT NULL AUTO_INCREMENT,
                joueur_id INT NOT NULL,
                datacron_id VARCHAR(120) NOT NULL,
                set_id INT NOT NULL DEFAULT 0,
                template_id VARCHAR(180) NULL,
                niveau INT NOT NULL DEFAULT 0,
                focused BOOLEAN NOT NULL DEFAULT FALSE,
                locked BOOLEAN NOT NULL DEFAULT FALSE,
                reroll_index INT NOT NULL DEFAULT 0,
                reroll_count INT NOT NULL DEFAULT 0,
                tags TEXT NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_player_datacron (joueur_id, datacron_id),
                KEY idx_player_datacron_joueur (joueur_id),
                KEY idx_player_datacron_set (set_id),
                KEY idx_player_datacron_niveau (niveau)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS player_datacron_affix_actuel (
                id BIGINT NOT NULL AUTO_INCREMENT,
                player_datacron_id BIGINT NOT NULL,
                position_affix INT NOT NULL,
                tags TEXT NULL,
                target_rule VARCHAR(220) NULL,
                ability_id VARCHAR(220) NULL,
                stat_type INT NULL,
                stat_value VARCHAR(80) NULL,
                required_unit_tier INT NOT NULL DEFAULT 0,
                required_relic_tier INT NOT NULL DEFAULT 0,
                scope_icon VARCHAR(220) NULL,
                PRIMARY KEY (id),
                UNIQUE KEY uk_player_datacron_affix (player_datacron_id, position_affix),
                KEY idx_datacron_affix_parent (player_datacron_id),
                KEY idx_datacron_affix_ability (ability_id),
                KEY idx_datacron_affix_stat (stat_type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    @Transactional
    public int synchroniserJoueur(int joueurId, JSONObject player) {
        if (joueurId <= 0) {
            throw new IllegalArgumentException("joueurId invalide pour le Batch DTC.");
        }

        supprimerEtatJoueur(joueurId);

        if (player == null) return 0;
        JSONArray datacrons = player.optJSONArray("datacron");
        if (datacrons == null || datacrons.length() == 0) return 0;

        int inseres = 0;
        for (int i = 0; i < datacrons.length(); i++) {
            JSONObject d = datacrons.optJSONObject(i);
            if (d == null) continue;

            String datacronId = texte(d, "id");
            if (datacronId.isBlank()) continue;

            JSONArray affixes = d.optJSONArray("affix");
            int niveau = affixes == null ? 0 : affixes.length();

            jdbc.update("""
                INSERT INTO player_datacron_actuel
                    (joueur_id, datacron_id, set_id, template_id, niveau,
                     focused, locked, reroll_index, reroll_count, tags, date_maj)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                joueurId,
                datacronId,
                d.optInt("setId", 0),
                nullSiVide(texte(d, "templateId")),
                niveau,
                d.optBoolean("focused", false),
                d.optBoolean("locked", false),
                d.optInt("rerollIndex", 0),
                d.optInt("rerollCount", 0),
                tags(d.optJSONArray("tag")));

            Long parentId = jdbc.queryForObject("""
                SELECT id
                FROM player_datacron_actuel
                WHERE joueur_id = ? AND datacron_id = ?
                """, Long.class, joueurId, datacronId);

            if (parentId != null && affixes != null) {
                for (int pos = 0; pos < affixes.length(); pos++) {
                    JSONObject a = affixes.optJSONObject(pos);
                    if (a == null) continue;
                    jdbc.update("""
                        INSERT INTO player_datacron_affix_actuel
                            (player_datacron_id, position_affix, tags, target_rule,
                             ability_id, stat_type, stat_value, required_unit_tier,
                             required_relic_tier, scope_icon)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        parentId,
                        pos + 1,
                        tags(a.optJSONArray("tag")),
                        nullSiVide(texte(a, "targetRule")),
                        nullSiVide(texte(a, "abilityId")),
                        a.has("statType") && !a.isNull("statType") ? a.optInt("statType") : null,
                        nullSiVide(texte(a, "statValue")),
                        a.optInt("requiredUnitTier", 0),
                        a.optInt("requiredRelicTier", 0),
                        nullSiVide(texte(a, "scopeIcon")));
                }
            }
            inseres++;
        }

        return inseres;
    }

    @Transactional
    public void purgerJoueursHorsGuilde() {
        jdbc.update("""
            DELETE a
            FROM player_datacron_affix_actuel a
            JOIN player_datacron_actuel d ON d.id = a.player_datacron_id
            LEFT JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
            WHERE j.id IS NULL
            """);
        jdbc.update("""
            DELETE d
            FROM player_datacron_actuel d
            LEFT JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
            WHERE j.id IS NULL
            """);
    }

    @Transactional
    public void supprimerEtatJoueur(int joueurId) {
        jdbc.update("""
            DELETE a
            FROM player_datacron_affix_actuel a
            JOIN player_datacron_actuel d ON d.id = a.player_datacron_id
            WHERE d.joueur_id = ?
            """, joueurId);
        jdbc.update("DELETE FROM player_datacron_actuel WHERE joueur_id = ?", joueurId);
    }

    public DatacronStats getStatsJoueur(int joueurId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(CASE WHEN niveau >= 9 THEN 1 ELSE 0 END), 0) AS t9,
                   COALESCE(SUM(CASE WHEN focused = 1 THEN 1 ELSE 0 END), 0) AS focused
            FROM player_datacron_actuel
            WHERE joueur_id = ?
            """, joueurId);
        if (rows.isEmpty()) return new DatacronStats(0, 0, 0);
        Map<String, Object> row = rows.get(0);
        return new DatacronStats(nombre(row.get("total")), nombre(row.get("t9")), nombre(row.get("focused")));
    }

    public DatacronView construireVue(Integer joueurId) {
        DatacronView vue = new DatacronView();

        List<Map<String, Object>> global = jdbc.queryForList("""
            SELECT COUNT(d.id) AS total,
                   COALESCE(SUM(CASE WHEN d.niveau >= 9 THEN 1 ELSE 0 END), 0) AS t9,
                   COALESCE(SUM(CASE WHEN d.focused = 1 THEN 1 ELSE 0 END), 0) AS focused,
                   COUNT(DISTINCT d.joueur_id) AS joueurs
            FROM player_datacron_actuel d
            JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
            """);
        if (!global.isEmpty()) {
            Map<String, Object> row = global.get(0);
            vue.setTotal(nombre(row.get("total")));
            vue.setTotalT9Plus(nombre(row.get("t9")));
            vue.setTotalFocused(nombre(row.get("focused")));
            vue.setJoueursAvecDatacrons(nombre(row.get("joueurs")));
        }

        List<JoueurResume> joueurs = jdbc.query("""
            SELECT j.id, j.nom,
                   COUNT(d.id) AS total,
                   COALESCE(SUM(CASE WHEN d.niveau >= 3 THEN 1 ELSE 0 END), 0) AS t3,
                   COALESCE(SUM(CASE WHEN d.niveau >= 6 THEN 1 ELSE 0 END), 0) AS t6,
                   COALESCE(SUM(CASE WHEN d.niveau >= 9 THEN 1 ELSE 0 END), 0) AS t9,
                   COALESCE(SUM(CASE WHEN d.niveau >= 12 THEN 1 ELSE 0 END), 0) AS t12,
                   COALESCE(SUM(CASE WHEN d.niveau >= 15 THEN 1 ELSE 0 END), 0) AS t15,
                   COALESCE(SUM(CASE WHEN d.focused = 1 THEN 1 ELSE 0 END), 0) AS focused
            FROM joueur j
            LEFT JOIN player_datacron_actuel d ON d.joueur_id = j.id
            WHERE j.dans_guilde = 1
            GROUP BY j.id, j.nom
            ORDER BY j.nom
            """, (rs, rowNum) -> {
                JoueurResume r = new JoueurResume();
                r.setId(rs.getInt("id"));
                r.setNom(rs.getString("nom"));
                r.setTotal(rs.getInt("total"));
                r.setT3(rs.getInt("t3"));
                r.setT6(rs.getInt("t6"));
                r.setT9(rs.getInt("t9"));
                r.setT12(rs.getInt("t12"));
                r.setT15(rs.getInt("t15"));
                r.setFocused(rs.getInt("focused"));
                return r;
            });
        vue.setJoueurs(joueurs);

        if (joueurId == null) return vue;

        String joueurNom = null;
        for (JoueurResume joueur : joueurs) {
            if (joueur.getId() == joueurId.intValue()) {
                joueurNom = joueur.getNom();
                break;
            }
        }
        if (joueurNom == null) return vue;

        vue.setJoueurSelectionneId(joueurId);
        vue.setJoueurSelectionneNom(joueurNom);

        List<DatacronDetail> details = jdbc.query("""
            SELECT id, datacron_id, set_id, template_id, niveau,
                   focused, locked, reroll_count, tags
            FROM player_datacron_actuel
            WHERE joueur_id = ?
            ORDER BY set_id DESC, focused DESC, niveau DESC, datacron_id
            """, (rs, rowNum) -> {
                DatacronDetail d = new DatacronDetail();
                d.setId(rs.getLong("id"));
                d.setDatacronId(rs.getString("datacron_id"));
                d.setSetId(rs.getInt("set_id"));
                d.setTemplateId(rs.getString("template_id"));
                d.setNiveau(rs.getInt("niveau"));
                d.setFocused(rs.getBoolean("focused"));
                d.setLocked(rs.getBoolean("locked"));
                d.setRerollCount(rs.getInt("reroll_count"));
                d.setTags(rs.getString("tags"));
                return d;
            }, joueurId);

        for (DatacronDetail detail : details) {
            detail.setAffixes(chargerAffixes(detail.getId()));
        }
        vue.setDatacrons(details);
        vue.setSets(regrouperParSet(details));
        vue.setStats(chargerStats(joueurId));
        return vue;
    }

    /**
     * Statistiques d'inventaire centrées sur un Set précis.
     * Les compteurs T3/T6/T9 représentent des Datacrons ayant atteint au moins
     * le palier concerné, et non un nombre de joueurs.
     */
    public DatacronInventoryView construireInventaireParSet(Integer setDemande) {
        DatacronInventoryView vue = new DatacronInventoryView();

        List<Integer> sets = jdbc.queryForList("""
                SELECT DISTINCT d.set_id
                FROM player_datacron_actuel d
                JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
                WHERE d.set_id > 0
                ORDER BY d.set_id DESC
                """, Integer.class);
        vue.setSets(sets);

        Integer totalJoueurs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM joueur WHERE dans_guilde = 1", Integer.class);
        vue.setTotalJoueurs(totalJoueurs == null ? 0 : totalJoueurs);

        if (sets.isEmpty()) return vue;

        int setId = setDemande != null && sets.contains(setDemande)
                ? setDemande
                : sets.getFirst();
        vue.setSelectedSetId(setId);

        List<Map<String, Object>> global = jdbc.queryForList("""
                SELECT COUNT(d.id) AS total,
                       COUNT(DISTINCT d.joueur_id) AS joueurs,
                       COALESCE(SUM(CASE WHEN d.niveau >= 3 THEN 1 ELSE 0 END), 0) AS t3,
                       COALESCE(SUM(CASE WHEN d.niveau >= 6 THEN 1 ELSE 0 END), 0) AS t6,
                       COALESCE(SUM(CASE WHEN d.niveau >= 9 THEN 1 ELSE 0 END), 0) AS t9,
                       COALESCE(SUM(CASE WHEN d.focused = 1 THEN 1 ELSE 0 END), 0) AS focused
                FROM player_datacron_actuel d
                JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
                WHERE d.set_id = ?
                """, setId);
        if (!global.isEmpty()) {
            Map<String, Object> row = global.getFirst();
            vue.setTotalDatacrons(nombre(row.get("total")));
            vue.setJoueursAvecDatacrons(nombre(row.get("joueurs")));
            vue.setT3Plus(nombre(row.get("t3")));
            vue.setT6Plus(nombre(row.get("t6")));
            vue.setT9Plus(nombre(row.get("t9")));
            vue.setFocused(nombre(row.get("focused")));
        }

        List<PlayerRow> joueurs = jdbc.query("""
                SELECT j.id, COALESCE(j.nom, j.player_id) AS nom,
                       COUNT(d.id) AS total,
                       COALESCE(SUM(CASE WHEN d.niveau >= 3 THEN 1 ELSE 0 END), 0) AS t3,
                       COALESCE(SUM(CASE WHEN d.niveau >= 6 THEN 1 ELSE 0 END), 0) AS t6,
                       COALESCE(SUM(CASE WHEN d.niveau >= 9 THEN 1 ELSE 0 END), 0) AS t9,
                       COALESCE(SUM(CASE WHEN d.focused = 1 THEN 1 ELSE 0 END), 0) AS focused
                FROM joueur j
                LEFT JOIN player_datacron_actuel d
                  ON d.joueur_id = j.id AND d.set_id = ?
                WHERE j.dans_guilde = 1
                GROUP BY j.id, j.nom, j.player_id
                ORDER BY total DESC, t9 DESC, nom
                """, (rs, rowNum) -> {
                    PlayerRow r = new PlayerRow();
                    r.setId(rs.getInt("id"));
                    r.setNom(rs.getString("nom"));
                    r.setTotal(rs.getInt("total"));
                    r.setT3(rs.getInt("t3"));
                    r.setT6(rs.getInt("t6"));
                    r.setT9(rs.getInt("t9"));
                    r.setFocused(rs.getInt("focused"));
                    return r;
                }, setId);
        vue.setJoueurs(joueurs);
        return vue;
    }

    private List<AffixDetail> chargerAffixes(long parentId) {
        return jdbc.query("""
            SELECT a.position_affix, a.tags, a.target_rule, a.ability_id, a.stat_type,
                   a.stat_value, a.required_unit_tier, a.required_relic_tier, a.scope_icon,
                   def.nom_francais AS ability_nom,
                   def.description_fr AS ability_description
            FROM player_datacron_affix_actuel a
            LEFT JOIN datacron_ability_definition def ON def.ability_id = a.ability_id
            WHERE a.player_datacron_id = ?
            ORDER BY a.position_affix
            """, (rs, rowNum) -> {
                AffixDetail a = new AffixDetail();
                a.setPosition(rs.getInt("position_affix"));
                a.setTags(rs.getString("tags"));
                a.setTargetRule(rs.getString("target_rule"));
                a.setAbilityId(rs.getString("ability_id"));
                Object statType = rs.getObject("stat_type");
                a.setStatType(statType == null ? null : ((Number) statType).intValue());
                a.setStatValue(rs.getString("stat_value"));
                a.setRequiredUnitTier(rs.getInt("required_unit_tier"));
                a.setRequiredRelicTier(rs.getInt("required_relic_tier"));
                a.setScopeIcon(rs.getString("scope_icon"));
                String titre = rs.getString("ability_nom");
                String description = rs.getString("ability_description");
                a.setStatNom(DatacronDisplayUtil.nomStat(a.getStatType()));
                a.setStatValeurAffichee(DatacronDisplayUtil.valeurStat(a.getStatType(), a.getStatValue()));
                a.setTagsAffiches(DatacronDisplayUtil.tagsLisibles(a.getTags()));
                a.setCibleAffichee(cibleAffichee(a));

                String titreLisible = DatacronDisplayUtil.titreMecanique(titre);
                String descriptionLisible = DatacronDisplayUtil.nettoyerDescription(description);
                a.setTitre(DatacronDisplayUtil.remplacerCible(titreLisible, a.getCibleAffichee()));
                a.setDescription(DatacronDisplayUtil.remplacerCible(descriptionLisible, a.getCibleAffichee()));
                return a;
            }, parentId);
    }

    private String cibleAffichee(AffixDetail affix) {
        String fallback = DatacronDisplayUtil.cibleLisible(affix.getTargetRule(), affix.getTags());
        String abilityId = affix.getAbilityId();
        if (abilityId == null || !abilityId.startsWith("datacron_character_")) {
            return fallback;
        }

        String identifiant = DatacronDisplayUtil.identifiantCible(affix.getTargetRule());
        if (identifiant == null || identifiant.isBlank()) return fallback;

        return localizationService.traduirePersonnage(identifiant, fallback);
    }

    private List<StatResume> chargerStats(int joueurId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT a.stat_type, a.stat_value
            FROM player_datacron_affix_actuel a
            JOIN player_datacron_actuel d ON d.id = a.player_datacron_id
            WHERE d.joueur_id = ?
              AND a.stat_type IS NOT NULL
              AND a.stat_type <> 1
              AND a.stat_value IS NOT NULL
            """, joueurId);

        Map<Integer, long[]> agregats = new java.util.TreeMap<>();
        for (Map<String, Object> row : rows) {
            int statType = nombre(row.get("stat_type"));
            long[] agg = agregats.computeIfAbsent(statType, k -> new long[2]);
            agg[0]++;
            try {
                agg[1] += Long.parseLong(String.valueOf(row.get("stat_value")));
            } catch (NumberFormatException ignored) {
                // Valeur non entière : on conserve quand même l'occurrence.
            }
        }

        List<StatResume> resultat = new ArrayList<>();
        agregats.forEach((type, agg) -> {
            StatResume stat = new StatResume(type, (int) agg[0], agg[1]);
            stat.setNom(DatacronDisplayUtil.nomStat(type));
            stat.setValeurAffichee(DatacronDisplayUtil.valeurStat(type, String.valueOf(agg[1])));
            resultat.add(stat);
        });
        return resultat;
    }

    private List<DatacronSetGroupe> regrouperParSet(List<DatacronDetail> details) {
        Map<Integer, DatacronSetGroupe> groupes = new java.util.LinkedHashMap<>();
        for (DatacronDetail detail : details) {
            DatacronSetGroupe groupe = groupes.computeIfAbsent(detail.getSetId(), setId -> {
                DatacronSetGroupe nouveau = new DatacronSetGroupe();
                nouveau.setSetId(setId);
                return nouveau;
            });

            groupe.getDatacrons().add(detail);
            groupe.setTotal(groupe.getTotal() + 1);
            if (detail.getNiveau() >= 9) groupe.setT9Plus(groupe.getT9Plus() + 1);
            if (detail.isFocused()) groupe.setFocused(groupe.getFocused() + 1);
        }
        return new ArrayList<>(groupes.values());
    }

    private String texte(JSONObject json, String cle) {
        Object value = json.opt(cle);
        return value == null || JSONObject.NULL.equals(value) ? "" : String.valueOf(value).trim();
    }

    private String tags(JSONArray tags) {
        if (tags == null || tags.length() == 0) return null;
        List<String> valeurs = new ArrayList<>();
        for (int i = 0; i < tags.length(); i++) {
            String value = tags.optString(i, "").trim();
            if (!value.isBlank()) valeurs.add(value);
        }
        return valeurs.isEmpty() ? null : String.join(",", valeurs);
    }

    private String nullSiVide(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int nombre(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    public record DatacronStats(int total, int t9Plus, int focused) {}
}
