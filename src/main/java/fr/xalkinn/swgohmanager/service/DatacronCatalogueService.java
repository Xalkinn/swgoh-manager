package fr.xalkinn.swgohmanager.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.client.SwgohGameDataClient;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.DatacronGameData;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameAbility;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameDatacronAffix;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameDatacronAffixTemplateSet;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameDatacronTemplate;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameDatacronTier;
import fr.xalkinn.swgohmanager.dto.DatacronView.CatalogueEtat;

/**
 * Référentiel Game Data des Datacrons.
 *
 * Il est indépendant du Batch DTC : le batch collecte les DTC physiques des
 * joueurs, tandis que ce service traduit leurs IDs techniques en définitions
 * de set / palier / mécanique.
 */
@Service
public class DatacronCatalogueService {

    private static final Pattern SET_DANS_TEMPLATE = Pattern.compile("datacron_set_(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_AFFICHAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbc;
    private final SwgohGameDataClient gameDataClient;
    private final LocalizationService localizationService;

    public DatacronCatalogueService(
            JdbcTemplate jdbc,
            SwgohGameDataClient gameDataClient,
            LocalizationService localizationService) {
        this.jdbc = jdbc;
        this.gameDataClient = gameDataClient;
        this.localizationService = localizationService;
        initialiserSchema();
    }

    public void initialiserSchema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_template (
                template_id VARCHAR(220) PRIMARY KEY,
                set_id INT NULL,
                reference_template_id VARCHAR(220) NULL,
                focused BOOLEAN NOT NULL DEFAULT FALSE,
                game_version VARCHAR(120) NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_datacron_template_set (set_id),
                INDEX idx_datacron_template_focused (focused)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_tier_template (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                template_id VARCHAR(220) NOT NULL,
                tier_index INT NOT NULL,
                affix_template_set_id VARCHAR(240) NULL,
                initial_affix_template_set_id VARCHAR(240) NULL,
                required_unit_tier INT NULL,
                required_relic_tier INT NULL,
                game_version VARCHAR(120) NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_datacron_tier (template_id, tier_index),
                INDEX idx_datacron_tier_affix_set (affix_template_set_id),
                INDEX idx_datacron_tier_initial_set (initial_affix_template_set_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_tier_affix_set_template (
                affix_set_id VARCHAR(240) PRIMARY KEY,
                nombre_affixes INT NOT NULL DEFAULT 0,
                game_version VARCHAR(120) NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_affix_template (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                affix_set_id VARCHAR(240) NOT NULL,
                affix_index INT NOT NULL,
                tags TEXT NULL,
                target_rule VARCHAR(220) NULL,
                ability_id VARCHAR(220) NULL,
                stat_type INT NULL,
                stat_value VARCHAR(100) NULL,
                required_unit_tier INT NULL,
                required_relic_tier INT NULL,
                scope_icon VARCHAR(255) NULL,
                game_version VARCHAR(120) NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_datacron_affix_template (affix_set_id, affix_index),
                INDEX idx_datacron_affix_ability (ability_id),
                INDEX idx_datacron_affix_stat (stat_type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_ability_definition (
                ability_id VARCHAR(220) PRIMARY KEY,
                name_key VARCHAR(255) NULL,
                desc_key VARCHAR(255) NULL,
                nom_francais TEXT NULL,
                description_fr MEDIUMTEXT NULL,
                game_version VARCHAR(120) NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS datacron_catalogue_meta (
                meta_key VARCHAR(80) PRIMARY KEY,
                meta_value TEXT NULL,
                date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    /**
     * Télécharge d'abord toutes les données nécessaires puis remplace le
     * référentiel SQL. Le Batch Roster n'intervient jamais dans ce flux.
     */
    public synchronized String actualiserCatalogue() throws Exception {
        String version = gameDataClient.getLatestGameVersion();
        DatacronGameData gameData = gameDataClient.getDatacrons(version);

        if (gameData.getTemplates().isEmpty()) {
            throw new IllegalStateException("Aucun datacronTemplate reçu depuis Game Data.");
        }
        if (gameData.getAffixTemplateSets().isEmpty()) {
            throw new IllegalStateException("Aucun datacronAffixTemplateSet reçu depuis Game Data.");
        }

        List<GameAbility> abilities = gameDataClient.getDatacronAbilities(version);
        if (abilities.isEmpty()) {
            throw new IllegalStateException(
                    "Aucune AbilityDefinition Datacron reçue depuis le segment 2 Game Data.");
        }

        Map<String, String> traductions = localizationService.getTraductions();
        if (traductions == null || traductions.isEmpty()) {
            throw new IllegalStateException(
                    "Le bundle de localisation française est indisponible : catalogue DTC non remplacé.");
        }

        // Les téléchargements étant terminés, on peut maintenant remplacer le catalogue.
        jdbc.update("DELETE FROM datacron_tier_template");
        jdbc.update("DELETE FROM datacron_affix_template");
        jdbc.update("DELETE FROM datacron_tier_affix_set_template");
        jdbc.update("DELETE FROM datacron_template");
        jdbc.update("DELETE FROM datacron_ability_definition");

        insererTemplates(gameData.getTemplates(), version);
        insererAffixSets(gameData.getAffixTemplateSets(), version);
        insererAbilities(abilities, traductions, version);

        enregistrerMeta("game_version", version);
        enregistrerMeta("date_maj", LocalDateTime.now().toString());

        int affixes = gameData.getAffixTemplateSets().stream()
                .mapToInt(set -> set.getAffix().size())
                .sum();
        return "%d template(s), %d jeu(x) d'affixes, %d affixe(s) et %d capacité(s) Datacron chargé(s)"
                .formatted(gameData.getTemplates().size(), gameData.getAffixTemplateSets().size(), affixes, abilities.size());
    }

    public CatalogueEtat getEtat() {
        CatalogueEtat etat = new CatalogueEtat();
        etat.setGameVersion(meta("game_version"));
        etat.setDateMaj(formaterDate(meta("date_maj")));
        etat.setTemplates(compter("datacron_template"));
        etat.setAffixSets(compter("datacron_tier_affix_set_template"));
        etat.setAffixes(compter("datacron_affix_template"));
        etat.setAbilities(compter("datacron_ability_definition"));
        return etat;
    }

    private void insererTemplates(List<GameDatacronTemplate> templates, String version) {
        for (GameDatacronTemplate template : templates) {
            if (template == null || template.getId() == null || template.getId().isBlank()) continue;

            Integer setId = template.getSetId() != null ? template.getSetId() : deduireSetId(template.getId());
            boolean focused = template.getId().toLowerCase(Locale.ROOT).contains("_focused_");

            jdbc.update("""
                INSERT INTO datacron_template
                    (template_id, set_id, reference_template_id, focused, game_version, date_maj)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                template.getId(), setId, videVersNull(template.getReferenceTemplateId()), focused, version);

            List<GameDatacronTier> tiers = template.getTier();
            for (int i = 0; i < tiers.size(); i++) {
                GameDatacronTier tier = tiers.get(i);
                if (tier == null) continue;
                jdbc.update("""
                    INSERT INTO datacron_tier_template
                        (template_id, tier_index, affix_template_set_id, initial_affix_template_set_id,
                         required_unit_tier, required_relic_tier, game_version, date_maj)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    template.getId(), i + 1,
                    videVersNull(tier.getAffixTemplateSetId()),
                    videVersNull(tier.getInitialAffixTemplateSetId()),
                    tier.getRequiredUnitTier(), tier.getRequiredRelicTier(), version);
            }
        }
    }

    private void insererAffixSets(List<GameDatacronAffixTemplateSet> sets, String version) {
        for (GameDatacronAffixTemplateSet set : sets) {
            if (set == null || set.getId() == null || set.getId().isBlank()) continue;
            List<GameDatacronAffix> affixes = set.getAffix();
            jdbc.update("""
                INSERT INTO datacron_tier_affix_set_template
                    (affix_set_id, nombre_affixes, game_version, date_maj)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, set.getId(), affixes.size(), version);

            for (int i = 0; i < affixes.size(); i++) {
                GameDatacronAffix a = affixes.get(i);
                if (a == null) continue;
                jdbc.update("""
                    INSERT INTO datacron_affix_template
                        (affix_set_id, affix_index, tags, target_rule, ability_id, stat_type, stat_value,
                         required_unit_tier, required_relic_tier, scope_icon, game_version, date_maj)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    set.getId(), i + 1, joindre(a.getTag()), videVersNull(a.getTargetRule()),
                    videVersNull(a.getAbilityId()), a.getStatType(), videVersNull(a.getStatValue()),
                    a.getRequiredUnitTier(), a.getRequiredRelicTier(), videVersNull(a.getScopeIcon()), version);
            }
        }
    }

    private void insererAbilities(List<GameAbility> abilities, Map<String, String> traductions, String version) {
        for (GameAbility ability : abilities) {
            if (ability == null || ability.getId() == null || ability.getId().isBlank()) continue;
            jdbc.update("""
                INSERT INTO datacron_ability_definition
                    (ability_id, name_key, desc_key, nom_francais, description_fr, game_version, date_maj)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                ability.getId(), videVersNull(ability.getNameKey()), videVersNull(ability.getDescKey()),
                localiser(traductions, ability.getNameKey()),
                localiser(traductions, ability.getDescKey()), version);
        }
    }

    private String localiser(Map<String, String> traductions, String cle) {
        if (cle == null || cle.isBlank() || traductions == null || traductions.isEmpty()) return null;

        String valeur = traductions.get(cle);
        if (valeur == null || valeur.isBlank()) {
            valeur = traductions.get(cle.toUpperCase(Locale.ROOT));
        }
        if (valeur == null || valeur.isBlank()) return null;

        return nettoyerTexteJeu(valeur);
    }

    /**
     * Les chaînes de localisation SWGOH peuvent contenir des balises de mise
     * en forme destinées au client mobile. L'interface web n'en a pas besoin.
     */
    private String nettoyerTexteJeu(String valeur) {
        if (valeur == null) return null;
        String propre = valeur
                .replace("\\n", " ")
                .replace("\n", " ")
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)</?color(?:=[^>]*)?>", "")
                .replaceAll("(?i)</?b>", "")
                .replaceAll("(?i)</?i>", "")
                .replaceAll("\\[/?c(?:=[^\\]]*)?\\]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return propre.isBlank() ? null : propre;
    }

    private void enregistrerMeta(String cle, String valeur) {
        jdbc.update("""
            INSERT INTO datacron_catalogue_meta (meta_key, meta_value, date_maj)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value), date_maj = CURRENT_TIMESTAMP
            """, cle, valeur);
    }

    private String meta(String cle) {
        List<String> rows = jdbc.query(
                "SELECT meta_value FROM datacron_catalogue_meta WHERE meta_key = ?",
                (rs, rowNum) -> rs.getString("meta_value"), cle);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int compter(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null ? 0 : n;
    }

    private Integer deduireSetId(String templateId) {
        if (templateId == null) return null;
        Matcher matcher = SET_DANS_TEMPLATE.matcher(templateId);
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String joindre(List<String> valeurs) {
        if (valeurs == null || valeurs.isEmpty()) return null;
        return String.join(",", valeurs);
    }

    private String videVersNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String formaterDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value).format(DATE_AFFICHAGE);
        } catch (Exception e) {
            return value;
        }
    }
}
