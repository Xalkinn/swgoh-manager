package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.dto.DatacronPlanView.AffixOption;
import fr.xalkinn.swgohmanager.dto.DatacronPlanView.JoueurMatch;
import fr.xalkinn.swgohmanager.dto.DatacronPlanView.PlanRow;
import fr.xalkinn.swgohmanager.util.DatacronDisplayUtil;

/**
 * Plan de recommandations Datacron de la guilde.
 *
 * Le catalogue Game Data fournit les choix possibles T3/T6/T9. Le plan est
 * volontairement séparé des DTC physiques des joueurs : ceux-ci servent
 * uniquement à calculer automatiquement le niveau de correspondance.
 */
@Service
public class DatacronPlanService {

    private final JdbcTemplate jdbc;
    private final LocalizationService localizationService;

    public DatacronPlanService(
            JdbcTemplate jdbc,
            LocalizationService localizationService,
            DatacronCatalogueService catalogueService) {
        this.jdbc = jdbc;
        this.localizationService = localizationService;
        // La dépendance catalogueService garantit que le schéma catalogue est
        // initialisé avant celui du plan.
        creerTableSiNecessaire();
    }

    private void creerTableSiNecessaire() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_plan_recommandation (
                id BIGINT NOT NULL AUTO_INCREMENT,
                set_id INT NOT NULL,
                personnage_base_id VARCHAR(180) NOT NULL,
                t3_ability_id VARCHAR(220) NOT NULL,
                t3_target_rule VARCHAR(220) NULL,
                t3_tags TEXT NULL,
                t6_ability_id VARCHAR(220) NOT NULL,
                t6_target_rule VARCHAR(220) NULL,
                t6_tags TEXT NULL,
                t9_ability_id VARCHAR(220) NOT NULL,
                t9_target_rule VARCHAR(220) NULL,
                t9_tags TEXT NULL,
                date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                date_modification DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_dtc_plan_set (set_id),
                KEY idx_dtc_plan_personnage (personnage_base_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    /** Sets réellement présents dans les inventaires de la guilde. */
    public List<Integer> getSetsDisponibles() {
        LinkedHashSet<Integer> sets = new LinkedHashSet<>();
        sets.addAll(jdbc.queryForList("""
                SELECT DISTINCT d.set_id
                FROM player_datacron_actuel d
                JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
                WHERE d.set_id > 0
                ORDER BY d.set_id DESC
                """, Integer.class));

        if (sets.isEmpty()) {
            sets.addAll(jdbc.queryForList("""
                    SELECT DISTINCT set_id
                    FROM datacron_template
                    WHERE set_id IS NOT NULL AND set_id > 0
                    ORDER BY set_id DESC
                    LIMIT 3
                    """, Integer.class));
        }
        return new ArrayList<>(sets);
    }

    /**
     * Choix T3/T6/T9 disponibles pour le configurateur.
     *
     * On privilégie le catalogue Game Data. Certains templates récents ne
     * renseignent toutefois pas set_id dans le payload Game Data : dans ce cas,
     * on rattache le template au Set grâce aux DTC réellement remontés par le
     * Batch DTC (template_id -> set_id).
     *
     * Enfin, les mécaniques présentes dans les inventaires sont ajoutées comme
     * filet de sécurité. Le configurateur reste donc utilisable même si EA
     * modifie à nouveau le lien template <-> Set dans Game Data.
     */
    public List<AffixOption> getOptionsGestion() {
        List<Integer> setsDisponibles = getSetsDisponibles();
        if (setsDisponibles.isEmpty()) return List.of();
        Set<Integer> sets = Set.copyOf(setsDisponibles);

        Map<String, AffixOption> options = new LinkedHashMap<>();

        // 1) Catalogue Game Data, avec Set direct OU déduit depuis les DTC des joueurs.
        List<AffixOption> catalogue = jdbc.query("""
                SELECT
                    MIN(a.id) AS option_id,
                    COALESCE(dt.set_id, inv.set_id) AS set_id_effectif,
                    tt.tier_index,
                    a.ability_id,
                    a.target_rule,
                    a.tags,
                    MAX(COALESCE(a.required_relic_tier, 0)) AS required_relic_tier,
                    MAX(def.nom_francais) AS ability_nom,
                    MAX(def.description_fr) AS ability_description
                FROM datacron_template dt
                LEFT JOIN (
                    SELECT template_id, MAX(set_id) AS set_id
                    FROM player_datacron_actuel
                    WHERE template_id IS NOT NULL
                      AND template_id <> ''
                      AND set_id > 0
                    GROUP BY template_id
                ) inv ON inv.template_id = dt.template_id
                JOIN datacron_tier_template tt ON tt.template_id = dt.template_id
                JOIN datacron_affix_template a
                  ON a.affix_set_id = tt.affix_template_set_id
                  OR a.affix_set_id = tt.initial_affix_template_set_id
                LEFT JOIN datacron_ability_definition def ON def.ability_id = a.ability_id
                WHERE COALESCE(dt.set_id, inv.set_id) IS NOT NULL
                  AND tt.tier_index IN (3, 6, 9)
                  AND a.ability_id IS NOT NULL
                  AND a.ability_id <> ''
                GROUP BY COALESCE(dt.set_id, inv.set_id),
                         tt.tier_index, a.ability_id, a.target_rule, a.tags
                ORDER BY set_id_effectif DESC, tt.tier_index, option_id
                """, (rs, rowNum) -> {
                    int setId = rs.getInt("set_id_effectif");
                    if (!sets.contains(setId)) return null;
                    return construireOption(
                            rs.getLong("option_id"),
                            setId,
                            rs.getInt("tier_index"),
                            rs.getString("ability_id"),
                            rs.getString("target_rule"),
                            rs.getString("tags"),
                            rs.getString("ability_nom"),
                            rs.getString("ability_description"));
                });

        for (AffixOption option : catalogue) {
            if (option != null) {
                options.putIfAbsent(cleOption(option), option);
            }
        }

        // 2) Filet de sécurité : mécaniques réellement observées dans la guilde.
        // Les IDs sont négatifs pour distinguer une ligne d'inventaire d'une ligne
        // datacron_affix_template lors de l'ajout au plan.
        List<AffixOption> inventaire = jdbc.query("""
                SELECT
                    MIN(a.id) AS affix_id,
                    d.set_id,
                    a.position_affix AS tier_index,
                    a.ability_id,
                    a.target_rule,
                    a.tags,
                    MAX(def.nom_francais) AS ability_nom,
                    MAX(def.description_fr) AS ability_description
                FROM player_datacron_actuel d
                JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
                JOIN player_datacron_affix_actuel a ON a.player_datacron_id = d.id
                LEFT JOIN datacron_ability_definition def ON def.ability_id = a.ability_id
                WHERE d.set_id > 0
                  AND a.position_affix IN (3, 6, 9)
                  AND a.ability_id IS NOT NULL
                  AND a.ability_id <> ''
                GROUP BY d.set_id, a.position_affix, a.ability_id, a.target_rule, a.tags
                ORDER BY d.set_id DESC, a.position_affix, affix_id
                """, (rs, rowNum) -> {
                    int setId = rs.getInt("set_id");
                    if (!sets.contains(setId)) return null;
                    return construireOption(
                            -rs.getLong("affix_id"),
                            setId,
                            rs.getInt("tier_index"),
                            rs.getString("ability_id"),
                            rs.getString("target_rule"),
                            rs.getString("tags"),
                            rs.getString("ability_nom"),
                            rs.getString("ability_description"));
                });

        for (AffixOption option : inventaire) {
            if (option != null) {
                options.putIfAbsent(cleOption(option), option);
            }
        }

        return options.values().stream()
                .sorted(Comparator
                        .comparingInt(AffixOption::getSetId).reversed()
                        .thenComparingInt(AffixOption::getTier)
                        .thenComparing(o -> premierNonVide(o.getSelectLabel(), o.getAbilityId()),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String cleOption(AffixOption option) {
        return option.getSetId() + "|"
                + option.getTier() + "|"
                + chaineCle(option.getAbilityId()) + "|"
                + chaineCle(option.getTargetRule()) + "|"
                + chaineCle(option.getTags());
    }

    private String chaineCle(String value) {
        return value == null ? "" : value;
    }

    public List<PlanRow> getPlan() {
        List<PlanRow> rows = jdbc.query("""
                SELECT p.*,
                       d3.nom_francais AS t3_nom, d3.description_fr AS t3_description,
                       d6.nom_francais AS t6_nom, d6.description_fr AS t6_description,
                       d9.nom_francais AS t9_nom, d9.description_fr AS t9_description
                FROM datacron_plan_recommandation p
                LEFT JOIN datacron_ability_definition d3 ON d3.ability_id = p.t3_ability_id
                LEFT JOIN datacron_ability_definition d6 ON d6.ability_id = p.t6_ability_id
                LEFT JOIN datacron_ability_definition d9 ON d9.ability_id = p.t9_ability_id
                ORDER BY p.set_id DESC, p.id
                """, (rs, rowNum) -> {
                    PlanRow row = new PlanRow();
                    row.setId(rs.getLong("id"));
                    row.setSetId(rs.getInt("set_id"));
                    row.setPersonnageBaseId(rs.getString("personnage_base_id"));
                    row.setT3(construireOption(
                            0L, row.getSetId(), 3,
                            rs.getString("t3_ability_id"), rs.getString("t3_target_rule"), rs.getString("t3_tags"),
                            rs.getString("t3_nom"), rs.getString("t3_description")));
                    row.setT6(construireOption(
                            0L, row.getSetId(), 6,
                            rs.getString("t6_ability_id"), rs.getString("t6_target_rule"), rs.getString("t6_tags"),
                            rs.getString("t6_nom"), rs.getString("t6_description")));
                    row.setT9(construireOption(
                            0L, row.getSetId(), 9,
                            rs.getString("t9_ability_id"), rs.getString("t9_target_rule"), rs.getString("t9_tags"),
                            rs.getString("t9_nom"), rs.getString("t9_description")));

                    String personnageNom = row.getT9().getPersonnageNom();
                    if (personnageNom == null || personnageNom.isBlank()) {
                        String baseId = row.getPersonnageBaseId();
                        if (baseId != null && !baseId.startsWith("T9_") && !baseId.startsWith("DATACRON_")) {
                            personnageNom = localizationService.traduirePersonnage(baseId, row.getT9().getCible());
                        }
                    }
                    if (personnageNom == null || personnageNom.isBlank()) {
                        personnageNom = premierNonVide(row.getT9().getCible(), row.getT9().getTitre(), "T9");
                    }
                    row.setPersonnageNom(personnageNom);
                    row.setModalId("datacronPlanModal-" + row.getId());
                    return row;
                });

        for (PlanRow row : rows) {
            List<JoueurMatch> statuts = chargerStatutsJoueurs(row);
            List<JoueurMatch> complets = statuts.stream()
                    .filter(JoueurMatch::isComplet)
                    .toList();
            List<JoueurMatch> manquants = statuts.stream()
                    .filter(j -> !j.isComplet())
                    .sorted(Comparator.comparingInt(JoueurMatch::getTiersOk).reversed()
                            .thenComparing(JoueurMatch::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            row.setTotalJoueurs(statuts.size());
            row.setPossesseurs(complets.size());
            row.setManquants(manquants.size());
            row.setManqueUnTier((int) manquants.stream().filter(JoueurMatch::isManqueUnTier).count());
            row.setPossesseursDetails(complets);
            row.setManquantsDetails(manquants);
        }
        return rows;
    }

    /**
     * Ajout volontairement manuel : Set -> T3 -> T6 -> T9.
     * Le personnage éventuel est déduit du T9 et n'est plus un choix séparé.
     */
    @Transactional
    public void ajouter(int setId, long t3OptionId, long t6OptionId, long t9OptionId) {
        if (setId <= 0) throw new IllegalArgumentException("Sélectionne un Set Datacron.");

        AffixSnapshot t3 = chargerSnapshot(t3OptionId, setId, 3);
        AffixSnapshot t6 = chargerSnapshot(t6OptionId, setId, 6);
        AffixSnapshot t9 = chargerSnapshot(t9OptionId, setId, 9);

        Integer doublon = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM datacron_plan_recommandation
                WHERE set_id = ?
                  AND t3_ability_id = ? AND t3_target_rule <=> ? AND t3_tags <=> ?
                  AND t6_ability_id = ? AND t6_target_rule <=> ? AND t6_tags <=> ?
                  AND t9_ability_id = ? AND t9_target_rule <=> ? AND t9_tags <=> ?
                """, Integer.class,
                setId,
                t3.abilityId(), t3.targetRule(), t3.tags(),
                t6.abilityId(), t6.targetRule(), t6.tags(),
                t9.abilityId(), t9.targetRule(), t9.tags());
        if (doublon != null && doublon > 0) {
            throw new IllegalArgumentException("Cette combinaison DTC existe déjà dans le plan.");
        }

        String personnage = personnageDepuisTarget(t9.abilityId(), t9.targetRule());
        if (personnage == null || personnage.isBlank()) {
            personnage = cleTechniqueT9(t9.abilityId());
        }

        jdbc.update("""
                INSERT INTO datacron_plan_recommandation
                    (set_id, personnage_base_id,
                     t3_ability_id, t3_target_rule, t3_tags,
                     t6_ability_id, t6_target_rule, t6_tags,
                     t9_ability_id, t9_target_rule, t9_tags,
                     date_creation, date_modification)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                setId, personnage,
                t3.abilityId(), t3.targetRule(), t3.tags(),
                t6.abilityId(), t6.targetRule(), t6.tags(),
                t9.abilityId(), t9.targetRule(), t9.tags());
    }

    @Transactional
    public void supprimer(long id) {
        jdbc.update("DELETE FROM datacron_plan_recommandation WHERE id = ?", id);
    }

    public int compterPlan() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM datacron_plan_recommandation", Integer.class);
        return n == null ? 0 : n;
    }

    private AffixSnapshot chargerSnapshot(long optionId, int setId, int tier) {
        if (optionId < 0) {
            long affixId = -optionId;
            List<AffixSnapshot> snapshots = jdbc.query("""
                    SELECT a.ability_id, a.target_rule, a.tags
                    FROM player_datacron_affix_actuel a
                    JOIN player_datacron_actuel d ON d.id = a.player_datacron_id
                    JOIN joueur j ON j.id = d.joueur_id AND j.dans_guilde = 1
                    WHERE a.id = ?
                      AND d.set_id = ?
                      AND a.position_affix = ?
                      AND a.ability_id IS NOT NULL
                      AND a.ability_id <> ''
                    LIMIT 1
                    """, (rs, rowNum) -> new AffixSnapshot(
                            rs.getString("ability_id"),
                            rs.getString("target_rule"),
                            rs.getString("tags")),
                    affixId, setId, tier);
            if (!snapshots.isEmpty()) {
                return snapshots.getFirst();
            }
        } else {
            // Un affixe catalogue peut appartenir à un template dont set_id n'est
            // pas renseigné par Game Data. Le Set a déjà été validé lors de la
            // construction de la liste ; ici on vérifie uniquement le tier.
            List<AffixSnapshot> snapshots = jdbc.query("""
                    SELECT a.ability_id, a.target_rule, a.tags
                    FROM datacron_affix_template a
                    JOIN datacron_tier_template tt
                      ON a.affix_set_id = tt.affix_template_set_id
                      OR a.affix_set_id = tt.initial_affix_template_set_id
                    WHERE a.id = ?
                      AND tt.tier_index = ?
                      AND a.ability_id IS NOT NULL
                      AND a.ability_id <> ''
                    LIMIT 1
                    """, (rs, rowNum) -> new AffixSnapshot(
                            rs.getString("ability_id"),
                            rs.getString("target_rule"),
                            rs.getString("tags")),
                    optionId, tier);
            if (!snapshots.isEmpty()) {
                return snapshots.getFirst();
            }
        }

        throw new IllegalArgumentException(
                "Choix T" + tier + " introuvable pour le Set " + setId
                + ". Relance le Batch DTC puis actualise le catalogue.");
    }

    /**
     * Pour chaque joueur, on retient le DTC du Set qui ressemble le plus au plan.
     * Cela permet d'afficher précisément T3/T6/T9 OK ou manquant, même si le joueur
     * ne possède pas encore la combinaison complète.
     */
    private List<JoueurMatch> chargerStatutsJoueurs(PlanRow row) {
        List<Map<String, Object>> lignes = jdbc.queryForList("""
                SELECT j.id AS joueur_id,
                       j.player_id,
                       COALESCE(j.nom, j.player_id) AS player_name,
                       d.datacron_id,
                       COALESCE(d.niveau, 0) AS niveau,
                       CASE WHEN a3.ability_id = ? AND a3.target_rule <=> ? AND a3.tags <=> ? THEN 1 ELSE 0 END AS t3_ok,
                       CASE WHEN a6.ability_id = ? AND a6.target_rule <=> ? AND a6.tags <=> ? THEN 1 ELSE 0 END AS t6_ok,
                       CASE WHEN a9.ability_id = ? AND a9.target_rule <=> ? AND a9.tags <=> ? THEN 1 ELSE 0 END AS t9_ok
                FROM joueur j
                LEFT JOIN player_datacron_actuel d
                  ON d.joueur_id = j.id AND d.set_id = ?
                LEFT JOIN player_datacron_affix_actuel a3
                  ON a3.player_datacron_id = d.id AND a3.position_affix = 3
                LEFT JOIN player_datacron_affix_actuel a6
                  ON a6.player_datacron_id = d.id AND a6.position_affix = 6
                LEFT JOIN player_datacron_affix_actuel a9
                  ON a9.player_datacron_id = d.id AND a9.position_affix = 9
                WHERE j.dans_guilde = 1
                ORDER BY player_name, niveau DESC, d.datacron_id
                """,
                row.getT3().getAbilityId(), row.getT3().getTargetRule(), row.getT3().getTags(),
                row.getT6().getAbilityId(), row.getT6().getTargetRule(), row.getT6().getTags(),
                row.getT9().getAbilityId(), row.getT9().getTargetRule(), row.getT9().getTags(),
                row.getSetId());

        Map<Integer, JoueurMatch> meilleurs = new LinkedHashMap<>();
        for (Map<String, Object> ligne : lignes) {
            int joueurId = nombre(ligne.get("joueur_id"));
            boolean t3Ok = nombre(ligne.get("t3_ok")) == 1;
            boolean t6Ok = nombre(ligne.get("t6_ok")) == 1;
            boolean t9Ok = nombre(ligne.get("t9_ok")) == 1;
            int niveau = nombre(ligne.get("niveau"));
            String datacronId = chaine(ligne.get("datacron_id"));

            JoueurMatch candidat = new JoueurMatch(
                    joueurId,
                    chaine(ligne.get("player_id")),
                    chaine(ligne.get("player_name")),
                    datacronId);
            candidat.setMeilleurDatacronId(datacronId);
            candidat.setMeilleurDatacronNiveau(niveau);
            candidat.setT3Ok(t3Ok);
            candidat.setT6Ok(t6Ok);
            candidat.setT9Ok(t9Ok);

            JoueurMatch courant = meilleurs.get(joueurId);
            if (courant == null || meilleurQue(candidat, courant)) {
                meilleurs.put(joueurId, candidat);
            }
        }

        return new ArrayList<>(meilleurs.values());
    }

    private boolean meilleurQue(JoueurMatch candidat, JoueurMatch courant) {
        if (candidat.getTiersOk() != courant.getTiersOk()) {
            return candidat.getTiersOk() > courant.getTiersOk();
        }
        if (candidat.getMeilleurDatacronId() != null && courant.getMeilleurDatacronId() == null) {
            return true;
        }
        return candidat.getMeilleurDatacronNiveau() > courant.getMeilleurDatacronNiveau();
    }

    private AffixOption construireOption(
            long optionId,
            int setId,
            int tier,
            String abilityId,
            String targetRule,
            String tags,
            String abilityNom,
            String abilityDescription) {

        AffixOption option = new AffixOption();
        option.setOptionId(optionId);
        option.setSetId(setId);
        option.setTier(tier);
        option.setAbilityId(abilityId);
        option.setTargetRule(targetRule);
        option.setTags(tags);

        String cible = DatacronDisplayUtil.cibleLisible(targetRule, tags);
        String personnageBaseId = personnageDepuisTarget(abilityId, targetRule);
        String personnageNom = null;
        if (personnageBaseId != null) {
            personnageNom = localizationService.traduirePersonnage(personnageBaseId, cible);
            cible = personnageNom;
        }

        String titre = DatacronDisplayUtil.remplacerCible(
                DatacronDisplayUtil.titreMecanique(abilityNom), cible);
        String description = DatacronDisplayUtil.remplacerCible(
                DatacronDisplayUtil.nettoyerDescription(abilityDescription), cible);

        option.setCible(cible);
        option.setTitre(titre);
        option.setDescription(description);
        option.setPersonnageBaseId(personnageBaseId);
        option.setPersonnageNom(personnageNom);
        option.setSelectLabel(construireLibelleSelect(tier, cible, personnageNom, titre, description));
        return option;
    }

    private String construireLibelleSelect(int tier, String cible, String personnageNom, String titre, String description) {
        String prefixe = tier == 9 && personnageNom != null ? personnageNom : cible;
        String detail = description != null && !description.isBlank() ? description : titre;

        if (detail != null && prefixe != null && detail.toLowerCase(Locale.ROOT).startsWith(prefixe.toLowerCase(Locale.ROOT))) {
            prefixe = null;
        }
        if (detail != null && detail.length() > 115) {
            detail = detail.substring(0, 112).trim() + "…";
        }
        if (prefixe != null && !prefixe.isBlank() && detail != null && !detail.isBlank()) {
            return prefixe + " · " + detail;
        }
        if (detail != null && !detail.isBlank()) return detail;
        if (prefixe != null && !prefixe.isBlank()) return prefixe;
        return "Mécanique T" + tier;
    }

    private String personnageDepuisTarget(String abilityId, String targetRule) {
        if (abilityId == null || !abilityId.startsWith("datacron_character_")) return null;
        String id = DatacronDisplayUtil.identifiantCible(targetRule);
        return id == null || id.isBlank() ? null : normaliserBaseId(id);
    }

    private String normaliserBaseId(String baseId) {
        if (baseId == null) return null;
        String value = baseId.trim();
        int colon = value.indexOf(':');
        if (colon >= 0) value = value.substring(0, colon);
        return value.toUpperCase(Locale.ROOT);
    }

    private String cleTechniqueT9(String abilityId) {
        String value = abilityId == null || abilityId.isBlank() ? "T9_GENERIC" : abilityId.toUpperCase(Locale.ROOT);
        value = value.replaceAll("[^A-Z0-9_\\-]", "_");
        if (!value.startsWith("T9_")) value = "T9_" + value;
        return value.length() > 180 ? value.substring(0, 180) : value;
    }

    private String premierNonVide(String... valeurs) {
        for (String valeur : valeurs) {
            if (valeur != null && !valeur.isBlank()) return valeur;
        }
        return "";
    }

    private int nombre(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException e) { return 0; }
    }

    private String chaine(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record AffixSnapshot(String abilityId, String targetRule, String tags) {}
}
