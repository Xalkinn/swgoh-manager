-- =========================================================
-- SWGOH Manager - Historique Territory Battle
-- Les tables sont également créées automatiquement au démarrage
-- par TerritoryBattleService pour faciliter les migrations V2.x.
-- =========================================================

CREATE TABLE IF NOT EXISTS territory_battle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guild_id VARCHAR(100) NOT NULL,
    instance_id VARCHAR(180) NOT NULL UNIQUE,
    definition_id VARCHAR(100) NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    total_stars INT NOT NULL DEFAULT 0,
    date_import DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tb_end_time (end_time),
    INDEX idx_tb_guild_id (guild_id)
);

CREATE TABLE IF NOT EXISTS tb_activite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    territory_battle_id BIGINT NOT NULL,
    map_stat_id VARCHAR(255) NOT NULL,
    stat_type VARCHAR(100) NULL,
    phase INT NULL,
    conflict_num INT NULL,
    bonus BOOLEAN NOT NULL DEFAULT FALSE,
    covert_num INT NULL,
    round_num INT NULL,
    UNIQUE KEY uk_tb_activite (territory_battle_id, map_stat_id),
    INDEX idx_tb_activite_tb (territory_battle_id),
    INDEX idx_tb_activite_type (stat_type),
    INDEX idx_tb_activite_round (round_num)
);

CREATE TABLE IF NOT EXISTS tb_score_joueur (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tb_activite_id BIGINT NOT NULL,
    player_id VARCHAR(100) NOT NULL,
    score BIGINT NULL,
    UNIQUE KEY uk_tb_score_joueur (tb_activite_id, player_id),
    INDEX idx_tb_score_player (player_id),
    INDEX idx_tb_score_activite (tb_activite_id)
);
