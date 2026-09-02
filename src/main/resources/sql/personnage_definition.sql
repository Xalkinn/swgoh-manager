-- Script de référence uniquement.
-- L'application crée automatiquement cette table au démarrage via
-- PersonnageDefinitionService.creerTableSiNecessaire().

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Si la table existait déjà avec une collation différente de personnage.base_id,
-- l'application la réaligne automatiquement au démarrage.
-- Pour une correction manuelle sur une base où personnage.base_id est en
-- utf8mb4_0900_ai_ci :
-- ALTER TABLE personnage_definition
--   CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
