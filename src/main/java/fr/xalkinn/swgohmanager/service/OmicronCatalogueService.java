package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.client.SwgohGameDataClient;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameSkill;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.GameUnit;
import fr.xalkinn.swgohmanager.client.SwgohGameDataClient.SkillReference;
import jakarta.annotation.PostConstruct;

/**
 * Référentiel autonome des Omicrons disponibles dans le jeu.
 *
 * Le catalogue est construit depuis Game Data et ne contient aucune liste de
 * personnages codée en dur.
 */
@Service
public class OmicronCatalogueService {

    public static final List<String> CATEGORIES_GESTION =
            List.of("TW", "TB", "GAC", "RAID", "CONQUETE");

    private final JdbcTemplate jdbc;
    private final SwgohGameDataClient gameDataClient;

    public OmicronCatalogueService(JdbcTemplate jdbc, SwgohGameDataClient gameDataClient) {
        this.jdbc = jdbc;
        this.gameDataClient = gameDataClient;
    }

    @PostConstruct
    public void initialiserSchema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS omicron_definition (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                base_id VARCHAR(120) NOT NULL,
                personnage_name_key VARCHAR(255) NULL,
                skill_id VARCHAR(180) NOT NULL,
                skill_name_key VARCHAR(255) NULL,
                type_capacite VARCHAR(40) NULL,
                numero_capacite INT NULL,
                omicron_mode_code INT NULL,
                mode_technique VARCHAR(100) NULL,
                categorie VARCHAR(40) NULL,
                tier_requis INT NULL,
                game_version VARCHAR(160) NULL,
                actif BOOLEAN NOT NULL DEFAULT TRUE,
                date_decouverte DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                date_derniere_detection DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_omicron_definition (base_id, skill_id),
                INDEX idx_omicron_definition_categorie (categorie, actif),
                INDEX idx_omicron_definition_skill (skill_id)
            )
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS omicron_priorite (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                omicron_definition_id BIGINT NOT NULL,
                priorite TINYINT NOT NULL,
                actif BOOLEAN NOT NULL DEFAULT TRUE,
                commentaire VARCHAR(500) NULL,
                date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                date_modification DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_omicron_priorite_definition (omicron_definition_id),
                INDEX idx_omicron_priorite (priorite, actif)
            )
            """);

        initialiserTablePossessionLegacy();
    }

    /**
     * Fait évoluer l'ancienne table `omicron` sans perdre les données déjà
     * présentes. Cette table reste la source de compatibilité pour le dashboard
     * joueur et les compteurs existants.
     */
    private void initialiserTablePossessionLegacy() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS omicron (
                id INT AUTO_INCREMENT PRIMARY KEY,
                joueur_id VARCHAR(100) NOT NULL,
                nom_joueur VARCHAR(100),
                personnage VARCHAR(120),
                skill_id VARCHAR(180) NULL,
                type_capacite VARCHAR(40) NULL,
                mode_omicron VARCHAR(100) NULL,
                categorie VARCHAR(40) NULL,
                actif BOOLEAN,
                date_maj DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_omicron_skill (joueur_id, skill_id)
            )
            """);

        ajouterColonneSiAbsente("omicron", "skill_id", "VARCHAR(180) NULL AFTER personnage");
        ajouterColonneSiAbsente("omicron", "mode_omicron", "VARCHAR(100) NULL AFTER type_capacite");
        ajouterColonneSiAbsente("omicron", "categorie", "VARCHAR(40) NULL AFTER mode_omicron");

        // L'ancien ENUM ne permettait que LEADER / UNIQUE / SPECIAL et ne
        // supportait pas plusieurs capacités du même type.
        try {
            jdbc.execute("ALTER TABLE omicron MODIFY COLUMN type_capacite VARCHAR(40) NULL");
        } catch (Exception ignored) {
            // La table peut ne pas autoriser l'ALTER dans certains environnements;
            // l'application reste utilisable tant que les valeurs rentrent.
        }

        if (indexExiste("omicron", "uk_omicron")) {
            try {
                jdbc.execute("ALTER TABLE omicron DROP INDEX uk_omicron");
            } catch (Exception ignored) {
                // Déjà supprimé par une autre instance.
            }
        }
        if (!indexExiste("omicron", "uk_omicron_skill")) {
            try {
                jdbc.execute("ALTER TABLE omicron ADD UNIQUE KEY uk_omicron_skill (joueur_id, skill_id)");
            } catch (Exception ignored) {
                // Ne bloque pas le démarrage si l'index existe sous une autre forme.
            }
        }
    }

    public String synchroniserCatalogue() throws Exception {
        return synchroniserCatalogue(false);
    }

    @Transactional
    public String synchroniserCatalogue(boolean forcer) throws Exception {
        String version = gameDataClient.getLatestGameVersion();
        String versionActuelle = getVersionCatalogue();
        int nbActifs = compterDefinitionsActives();

        if (!forcer && nbActifs > 0 && version.equals(versionActuelle)) {
            return "Catalogue Omicron déjà à jour (Game Data " + version + ", "
                    + nbActifs + " définition(s)).";
        }

        List<GameSkill> skills = gameDataClient.getSkills(version);
        Map<String, GameSkill> skillsOmicron = new HashMap<>();
        for (GameSkill skill : skills) {
            if (skill != null && skill.getId() != null && skill.hasOmicron()) {
                skillsOmicron.put(skill.getId(), skill);
            }
        }

        if (skillsOmicron.isEmpty()) {
            throw new IllegalStateException(
                    "Aucune compétence Omicron détectée dans Game Data. Le catalogue existant n'a pas été modifié.");
        }

        List<GameUnit> units = gameDataClient.getUnits(version);
        Map<String, Detection> detections = new LinkedHashMap<>();

        for (GameUnit unit : units) {
            if (unit == null || unit.getBaseId() == null || unit.getBaseId().isBlank()) continue;
            if (unit.getCombatType() != null && unit.getCombatType() != 1) continue; // personnages uniquement

            Map<String, Integer> numerosParType = new HashMap<>();

            for (SkillReference ref : unit.getSkillReferenceList()) {
                if (ref == null || ref.getSkillId() == null) continue;

                String type = typeCapacite(ref.getSkillId());
                int numero = numerosParType.merge(type, 1, Integer::sum);

                GameSkill skill = skillsOmicron.get(ref.getSkillId());
                if (skill == null) continue;

                Integer modeCode = skill.getOmicronMode();
                String categorie = categorie(modeCode);
                String modeTechnique = modeTechnique(modeCode);

                Detection d = new Detection(
                        unit.getBaseId(),
                        unit.getNameKey(),
                        skill.getId(),
                        skill.getNameKey(),
                        type,
                        numero,
                        modeCode,
                        modeTechnique,
                        categorie,
                        skill.tierOmicronRequis(),
                        version);

                detections.put(unit.getBaseId() + "|" + skill.getId(), d);
            }
        }

        if (detections.isEmpty()) {
            throw new IllegalStateException(
                    "Aucun Omicron n'a pu être relié aux personnages Game Data. Le catalogue existant n'a pas été modifié.");
        }

        // On ne désactive l'ancien catalogue qu'après avoir validé que le nouveau
        // jeu de données est exploitable.
        jdbc.update("UPDATE omicron_definition SET actif = 0");

        List<Object[]> batch = new ArrayList<>();
        for (Detection d : detections.values()) {
            batch.add(new Object[] {
                    d.baseId, d.personnageNameKey, d.skillId, d.skillNameKey,
                    d.typeCapacite, d.numeroCapacite, d.modeCode,
                    d.modeTechnique, d.categorie, d.tierRequis, d.gameVersion
            });
        }

        jdbc.batchUpdate("""
            INSERT INTO omicron_definition
                (base_id, personnage_name_key, skill_id, skill_name_key,
                 type_capacite, numero_capacite, omicron_mode_code,
                 mode_technique, categorie, tier_requis, game_version,
                 actif, date_derniere_detection)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                personnage_name_key = VALUES(personnage_name_key),
                skill_name_key = VALUES(skill_name_key),
                type_capacite = VALUES(type_capacite),
                numero_capacite = VALUES(numero_capacite),
                omicron_mode_code = VALUES(omicron_mode_code),
                mode_technique = VALUES(mode_technique),
                categorie = VALUES(categorie),
                tier_requis = VALUES(tier_requis),
                game_version = VALUES(game_version),
                actif = TRUE,
                date_derniere_detection = CURRENT_TIMESTAMP
            """, batch);

        int gerables = compterDefinitionsGestion();
        return "Catalogue Omicron synchronisé : " + detections.size()
                + " définition(s) détectée(s), dont " + gerables
                + " pour TW / TB / Grande Arène / Raid / Conquête (Game Data " + version + ").";
    }

    public List<Definition> trouverDefinitionsActives() {
        return jdbc.query("""
                SELECT id, base_id, personnage_name_key, skill_id, skill_name_key,
                       type_capacite, numero_capacite, omicron_mode_code,
                       mode_technique, categorie, tier_requis, game_version
                FROM omicron_definition
                WHERE actif = 1
                ORDER BY categorie, base_id, type_capacite, numero_capacite, skill_id
                """,
                (rs, rowNum) -> new Definition(
                        rs.getLong("id"),
                        rs.getString("base_id"),
                        rs.getString("personnage_name_key"),
                        rs.getString("skill_id"),
                        rs.getString("skill_name_key"),
                        rs.getString("type_capacite"),
                        nullableInt(rs.getObject("numero_capacite")),
                        nullableInt(rs.getObject("omicron_mode_code")),
                        rs.getString("mode_technique"),
                        rs.getString("categorie"),
                        nullableInt(rs.getObject("tier_requis")),
                        rs.getString("game_version")));
    }

    public int compterDefinitionsActives() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM omicron_definition WHERE actif = 1",
                Integer.class);
        return n == null ? 0 : n;
    }

    public int compterDefinitionsGestion() {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM omicron_definition
                WHERE actif = 1 AND categorie IN ('TW','TB','GAC','RAID','CONQUETE')
                """, Integer.class);
        return n == null ? 0 : n;
    }

    public String getVersionCatalogue() {
        try {
            return jdbc.queryForObject("""
                    SELECT MAX(game_version)
                    FROM omicron_definition
                    WHERE actif = 1
                    """, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static String categorieLabel(String categorie) {
        if (categorie == null) return "Autre";
        return switch (categorie) {
            case "TW" -> "Guerre de territoires";
            case "TB" -> "Bataille de territoire";
            case "RAID" -> "Raid";
            case "CONQUETE" -> "Conquête";
            case "GAC" -> "Grande Arène";
            case "DEFI_GALACTIQUE" -> "Défi galactique";
            case "GUERRE_GALACTIQUE" -> "Guerre galactique";
            case "EVENEMENT_PVE" -> "Événement JcE";
            case "PVE" -> "JcE";
            case "PVP" -> "JcJ";
            case "TOUS" -> "Tous les modes";
            default -> "Autre";
        };
    }

    public static String libelleType(String type, Integer numero) {
        String label = switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "LEADER" -> "Chef";
            case "UNIQUE" -> "Unique";
            case "SPECIAL" -> "Spéciale";
            case "BASIC" -> "Basique";
            default -> "Capacité";
        };
        if (numero != null && numero > 1) {
            return label + " " + numero;
        }
        return label;
    }

    private String typeCapacite(String skillId) {
        String id = skillId == null ? "" : skillId.toLowerCase(Locale.ROOT);
        if (id.startsWith("leaderskill_")) return "LEADER";
        if (id.startsWith("uniqueskill_")) return "UNIQUE";
        if (id.startsWith("specialskill_")) return "SPECIAL";
        if (id.startsWith("basicskill_")) return "BASIC";
        return "AUTRE";
    }

    private String categorie(Integer mode) {
        if (mode == null) return "AUTRE";
        return switch (mode) {
            case 1 -> "TOUS";
            case 2 -> "PVE";
            case 3 -> "PVP";
            case 4 -> "RAID";
            case 5, 6, 7 -> "TB";
            case 8 -> "TW";
            case 9, 14, 15 -> "GAC";
            case 10 -> "GUERRE_GALACTIQUE";
            case 11 -> "CONQUETE";
            case 12, 16, 17 -> "DEFI_GALACTIQUE";
            case 13 -> "EVENEMENT_PVE";
            default -> "AUTRE";
        };
    }

    private String modeTechnique(Integer mode) {
        if (mode == null) return null;
        return switch (mode) {
            case 0 -> "DEFAULT";
            case 1 -> "ALL_OMICRON";
            case 2 -> "PVE_OMICRON";
            case 3 -> "PVP_OMICRON";
            case 4 -> "GUILD_RAID_OMICRON";
            case 5 -> "TERRITORY_STRIKE_OMICRON";
            case 6 -> "TERRITORY_COVERT_OMICRON";
            case 7 -> "TERRITORY_BATTLE_BOTH_OMICRON";
            case 8 -> "TERRITORY_WAR_OMICRON";
            case 9 -> "TERRITORY_TOURNAMENT_OMICRON";
            case 10 -> "WAR_OMICRON";
            case 11 -> "CONQUEST_OMICRON";
            case 12 -> "GALACTIC_CHALLENGE_OMICRON";
            case 13 -> "PVE_EVENT_OMICRON";
            case 14 -> "TERRITORY_TOURNAMENT_3_OMICRON";
            case 15 -> "TERRITORY_TOURNAMENT_5_OMICRON";
            case 16 -> "GALACTIC_CHALLENGE_3_OMICRON";
            case 17 -> "GALACTIC_CHALLENGE_5_OMICRON";
            default -> "OMICRON_MODE_" + mode;
        };
    }

    private void ajouterColonneSiAbsente(String table, String colonne, String definition) {
        if (!colonneExiste(table, colonne)) {
            try {
                jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + colonne + " " + definition);
            } catch (Exception ignored) {
                // Une autre instance peut avoir appliqué la migration simultanément.
            }
        }
    }

    private boolean colonneExiste(String table, String colonne) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, table, colonne);
        return n != null && n > 0;
    }

    private boolean indexExiste(String table, String index) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, table, index);
        return n != null && n > 0;
    }

    private Integer nullableInt(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private record Detection(
            String baseId,
            String personnageNameKey,
            String skillId,
            String skillNameKey,
            String typeCapacite,
            Integer numeroCapacite,
            Integer modeCode,
            String modeTechnique,
            String categorie,
            Integer tierRequis,
            String gameVersion) {}

    public record Definition(
            long id,
            String baseId,
            String personnageNameKey,
            String skillId,
            String skillNameKey,
            String typeCapacite,
            Integer numeroCapacite,
            Integer omicronModeCode,
            String modeTechnique,
            String categorie,
            Integer tierRequis,
            String gameVersion) {}
}
