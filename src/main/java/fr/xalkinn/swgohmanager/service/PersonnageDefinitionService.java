package fr.xalkinn.swgohmanager.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.PersonnageDisponible;

/**
 * Gère le référentiel personnage_definition.
 *
 * La table personnage reste une table d'historique de roster. Ici, on stocke
 * une seule fois les informations communes à un personnage, notamment son nom
 * français officiel.
 */
@Service
public class PersonnageDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(PersonnageDefinitionService.class);

    /**
     * Certains personnages ont exactement le même libellé dans la localisation
     * officielle française. Ces alias ne changent pas les identifiants SWGOH :
     * ils servent uniquement à rendre l'affichage compréhensible.
     *
     * Cette logique reprend le principe utilisé dans le projet de référence.
     */
    private static final Map<String, String> NOMS_AFFICHAGE_PERSONNALISES = Map.of(
            "AHSOKATANO", "Ahsoka Tano (Chipie)",
            "R2D2_LEGENDARY", "R2-D2",
            "REY", "Rey (Pilleuse)"
    );

    private final JdbcTemplate jdbc;
    private volatile boolean tableVerifiee = false;

    public PersonnageDefinitionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    /**
     * Le référentiel doit exister même si Comlink est indisponible.
     * On crée donc la table au démarrage et on l'alimente au minimum avec
     * les noms bruts déjà présents dans personnage.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialiserAuDemarrage() {
        try {
            synchroniserDepuisPersonnages(Map.of(), null);
        } catch (Exception e) {
            log.warn("Impossible d'initialiser personnage_definition : {}", e.getMessage());
        }
    }

    /**
     * Création automatique de la table pour éviter une migration manuelle.
     */
    public void creerTableSiNecessaire() {
        if (tableVerifiee) {
            return;
        }

        synchronized (this) {
            if (tableVerifiee) {
                return;
            }

            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS personnage_definition (
                    base_id VARCHAR(128) NOT NULL,
                    nom_brut VARCHAR(255) NULL,
                    nom_francais VARCHAR(255) NULL,
                    cle_localisation VARCHAR(255) NULL,
                    version_localisation VARCHAR(128) NULL,
                    date_maj DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (base_id),
                    INDEX idx_personnage_definition_nom_brut (nom_brut),
                    INDEX idx_personnage_definition_nom_francais (nom_francais)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

            // Important : personnage est une table déjà existante dans le projet.
            // Sa collation peut différer de la collation par défaut de MySQL.
            // On aligne donc automatiquement personnage_definition sur la colonne
            // personnage.base_id afin d'éviter les erreurs "Illegal mix of collations"
            // lors des JOIN.
            alignerCollationSurPersonnage();

            tableVerifiee = true;
        }
    }


    /**
     * Aligne le charset/collation du référentiel sur personnage.base_id.
     *
     * Ceci corrige aussi automatiquement une table personnage_definition déjà
     * créée avec une ancienne collation (par exemple utf8mb4_unicode_ci alors
     * que personnage.base_id est en utf8mb4_0900_ai_ci).
     */
    private void alignerCollationSurPersonnage() {
        try {
            Map<String, Object> source = jdbc.queryForMap("""
                SELECT CHARACTER_SET_NAME AS charset_source,
                       COLLATION_NAME AS collation_source
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'personnage'
                  AND COLUMN_NAME = 'base_id'
                """);

            String charset = asString(source.get("charset_source"));
            String collation = asString(source.get("collation_source"));

            if (charset == null || collation == null) {
                log.warn("Impossible de déterminer la collation de personnage.base_id");
                return;
            }

            // Les valeurs proviennent d'INFORMATION_SCHEMA mais on garde une
            // validation stricte avant de les intégrer dans une commande DDL.
            if (!charset.matches("[A-Za-z0-9_]+") || !collation.matches("[A-Za-z0-9_]+")) {
                log.warn("Charset/collation inattendu : {} / {}", charset, collation);
                return;
            }

            String collationActuelle = jdbc.queryForObject("""
                SELECT COLLATION_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'personnage_definition'
                  AND COLUMN_NAME = 'base_id'
                """, String.class);

            if (collation.equalsIgnoreCase(collationActuelle)) {
                return;
            }

            jdbc.execute("ALTER TABLE personnage_definition CONVERT TO CHARACTER SET "
                    + charset + " COLLATE " + collation);

            log.info("Collation personnage_definition alignée sur personnage.base_id : {} / {}",
                    charset, collation);

        } catch (Exception e) {
            log.warn("Impossible d'aligner la collation de personnage_definition : {}",
                    e.getMessage());
        }
    }

    /**
     * Alimente / met à jour le référentiel à partir des base_id déjà connus
     * dans les rosters et du bundle de localisation français.
     */
    public int synchroniserDepuisPersonnages(
            Map<String, String> traductions,
            String versionLocalisation) {

        creerTableSiNecessaire();

        List<Map<String, Object>> personnages = jdbc.queryForList("""
            SELECT
                SUBSTRING_INDEX(base_id, ':', 1) AS base_id,
                MAX(nom) AS nom_brut
            FROM personnage
            WHERE base_id IS NOT NULL
              AND TRIM(base_id) <> ''
            GROUP BY SUBSTRING_INDEX(base_id, ':', 1)
            ORDER BY SUBSTRING_INDEX(base_id, ':', 1)
            """);

        List<LigneDefinition> lignes = new ArrayList<>();

        for (Map<String, Object> row : personnages) {
            String baseId = asString(row.get("base_id"));
            String nomBrut = asString(row.get("nom_brut"));

            if (baseId == null || baseId.isBlank()) {
                continue;
            }

            String cle = construireCleNom(baseId);
            String nomFrancais = traductions == null ? null : traductions.get(cle);

            // Sécurité si Comlink renvoie les clés dans une casse différente.
            if ((nomFrancais == null || nomFrancais.isBlank()) && traductions != null) {
                nomFrancais = traductions.get(cle.toUpperCase(Locale.ROOT));
            }

            // Certaines traductions officielles sont ambiguës (plusieurs unités
            // portent exactement le même nom). On applique alors un libellé
            // d'affichage explicite en fonction du baseId.
            nomFrancais = appliquerNomAffichagePersonnalise(baseId, nomFrancais);

            lignes.add(new LigneDefinition(
                    normaliserBaseId(baseId),
                    nomBrut,
                    videVersNull(nomFrancais),
                    cle,
                    versionLocalisation));
        }

        if (lignes.isEmpty()) {
            log.info("Aucun personnage à synchroniser dans personnage_definition");
            return 0;
        }

        String sql = """
            INSERT INTO personnage_definition
                (base_id, nom_brut, nom_francais, cle_localisation,
                 version_localisation, date_maj)
            VALUES (?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                nom_brut = VALUES(nom_brut),
                nom_francais = COALESCE(VALUES(nom_francais), nom_francais),
                cle_localisation = VALUES(cle_localisation),
                version_localisation = COALESCE(VALUES(version_localisation), version_localisation),
                date_maj = NOW()
            """;

        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LigneDefinition ligne = lignes.get(i);
                ps.setString(1, ligne.baseId());
                ps.setString(2, ligne.nomBrut());
                ps.setString(3, ligne.nomFrancais());
                ps.setString(4, ligne.cleLocalisation());
                ps.setString(5, ligne.versionLocalisation());
            }

            @Override
            public int getBatchSize() {
                return lignes.size();
            }
        });

        long traduits = lignes.stream()
                .filter(l -> l.nomFrancais() != null && !l.nomFrancais().isBlank())
                .count();

        log.info("personnage_definition synchronisée : {} personnage(s), {} traduction(s) FR",
                lignes.size(), traduits);

        return lignes.size();
    }

    /**
     * Charge un index utilisable par les services qui manipulent encore un
     * identifiant brut (ex. omicrons).
     * La clé peut être soit base_id, soit nom_brut.
     */
    public Map<String, String> chargerIndexNomsFrancais() {
        creerTableSiNecessaire();

        Map<String, String> index = new HashMap<>();
        jdbc.query("""
            SELECT base_id, nom_brut, cle_localisation, nom_francais
            FROM personnage_definition
            WHERE nom_francais IS NOT NULL
              AND TRIM(nom_francais) <> ''
            """, rs -> {
                String baseId = rs.getString("base_id");
                String nomBrut = rs.getString("nom_brut");
                String cleLocalisation = rs.getString("cle_localisation");
                String nomFrancais = rs.getString("nom_francais");

                if (baseId != null) {
                    index.put(baseId, nomFrancais);
                    index.put(baseId.toUpperCase(Locale.ROOT), nomFrancais);
                }
                if (nomBrut != null && !nomBrut.isBlank()) {
                    index.put(nomBrut, nomFrancais);
                    index.put(nomBrut.toUpperCase(Locale.ROOT), nomFrancais);
                }
                if (cleLocalisation != null && !cleLocalisation.isBlank()) {
                    index.put(cleLocalisation, nomFrancais);
                    index.put(cleLocalisation.toUpperCase(Locale.ROOT), nomFrancais);
                }
            });
        return index;
    }

    public String nomAffiche(
            Map<String, String> index,
            String identifiant,
            String fallback) {

        if (index != null && identifiant != null && !identifiant.isBlank()) {
            String propre = normaliserBaseId(identifiant);
            String valeur = index.get(propre);
            if (valeur == null) {
                valeur = index.get(propre.toUpperCase(Locale.ROOT));
            }
            if (valeur != null && !valeur.isBlank()) {
                return valeur;
            }
        }

        if (index != null && fallback != null && !fallback.isBlank()) {
            String valeur = index.get(fallback);
            if (valeur == null) {
                valeur = index.get(fallback.toUpperCase(Locale.ROOT));
            }
            if (valeur != null && !valeur.isBlank()) {
                return valeur;
            }
        }

        return fallback != null && !fallback.isBlank() ? fallback : identifiant;
    }



    public List<PersonnageDisponible> listerPersonnagesDisponibles() {
        creerTableSiNecessaire();

        return jdbc.query("""
            SELECT DISTINCT
                p.nom AS nom_brut,
                SUBSTRING_INDEX(p.base_id, ':', 1) AS base_id,
                COALESCE(pd.nom_francais, p.nom) AS nom_francais
            FROM personnage p
            LEFT JOIN personnage_definition pd
              ON pd.base_id = SUBSTRING_INDEX(p.base_id, ':', 1)
            WHERE p.base_id IS NOT NULL
              AND TRIM(p.base_id) <> ''
            ORDER BY nom_francais
            """, (rs, rowNum) -> new PersonnageDisponible(
                    rs.getString("nom_brut"),
                    rs.getString("base_id"),
                    rs.getString("nom_francais")));
    }

    public String trouverBaseIdParNomBrut(String nomBrut) {
        if (nomBrut == null || nomBrut.isBlank()) {
            return null;
        }

        creerTableSiNecessaire();
        List<String> ids = jdbc.query("""
            SELECT base_id
            FROM personnage_definition
            WHERE REPLACE(TRIM(nom_brut), ',', '') =
                  REPLACE(TRIM(?), ',', '')
            LIMIT 1
            """, (rs, rowNum) -> rs.getString(1), nomBrut);

        return ids.isEmpty() ? null : ids.get(0);
    }

    public String trouverNomFrancaisParNomBrut(String nomBrut) {
        if (nomBrut == null || nomBrut.isBlank()) {
            return nomBrut;
        }

        creerTableSiNecessaire();
        List<String> noms = jdbc.query("""
            SELECT COALESCE(nom_francais, nom_brut)
            FROM personnage_definition
            WHERE nom_brut = ? OR base_id = ?
            ORDER BY CASE WHEN base_id = ? THEN 0 ELSE 1 END
            LIMIT 1
            """, (rs, rowNum) -> rs.getString(1), nomBrut, nomBrut, nomBrut);

        return noms.isEmpty() ? nomBrut : noms.get(0);
    }

    private String appliquerNomAffichagePersonnalise(String baseId, String nomTraduit) {
        String id = normaliserBaseId(baseId).toUpperCase(Locale.ROOT);
        return NOMS_AFFICHAGE_PERSONNALISES.getOrDefault(id, nomTraduit);
    }

    private String construireCleNom(String baseId) {
        return "UNIT_" + normaliserBaseId(baseId).toUpperCase(Locale.ROOT) + "_NAME";
    }

    private String normaliserBaseId(String valeur) {
        String propre = valeur == null ? "" : valeur.trim();
        int deuxPoints = propre.indexOf(':');
        if (deuxPoints > 0) {
            propre = propre.substring(0, deuxPoints);
        }
        return propre.replace(",", "").trim();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String videVersNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record LigneDefinition(
            String baseId,
            String nomBrut,
            String nomFrancais,
            String cleLocalisation,
            String versionLocalisation) {
    }
}
