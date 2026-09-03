-- Référence de schéma. Les tables/migrations sont également créées automatiquement
-- par OmicronCatalogueService au démarrage.

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
    UNIQUE KEY uk_omicron_definition (base_id, skill_id)
);

CREATE TABLE IF NOT EXISTS omicron_priorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    omicron_definition_id BIGINT NOT NULL,
    priorite TINYINT NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire VARCHAR(500) NULL,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_omicron_priorite_definition (omicron_definition_id)
);
