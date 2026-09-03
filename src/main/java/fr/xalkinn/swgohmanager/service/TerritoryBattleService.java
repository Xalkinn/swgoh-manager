package fr.xalkinn.swgohmanager.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.client.SwgohGuildClient;
import fr.xalkinn.swgohmanager.modele.GuildeConfiguration;
import fr.xalkinn.swgohmanager.util.MapStatIdParser;
import fr.xalkinn.swgohmanager.util.MapStatIdParser.ParsedMapStat;
import jakarta.annotation.PostConstruct;

/**
 * Historise les résultats finaux de Territory Battle.
 *
 * Une TB est identifiée par son instance_id, unique en base. La synchronisation
 * peut donc être relancée autant de fois que nécessaire sans créer de doublon.
 */
@Service
public class TerritoryBattleService {

    private final JdbcTemplate jdbc;
    private final GuildeService guildeService;
    private final SwgohGuildClient guildClient;

    public TerritoryBattleService(
            JdbcTemplate jdbc,
            GuildeService guildeService,
            SwgohGuildClient guildClient) {
        this.jdbc = jdbc;
        this.guildeService = guildeService;
        this.guildClient = guildClient;
    }

    @PostConstruct
    public void initialiser() {
        jdbc.execute("""
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
            )
            """);

        jdbc.execute("""
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
            )
            """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tb_score_joueur (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                tb_activite_id BIGINT NOT NULL,
                player_id VARCHAR(100) NOT NULL,
                score BIGINT NULL,
                UNIQUE KEY uk_tb_score_joueur (tb_activite_id, player_id),
                INDEX idx_tb_score_player (player_id),
                INDEX idx_tb_score_activite (tb_activite_id)
            )
            """);
    }

    /**
     * Récupère /guild puis importe tous les résultats TB terminés présents dans
     * recentTerritoryBattleResult. En pratique Comlink renvoie surtout la plus
     * récente, mais la boucle rend l'import compatible avec plusieurs résultats.
     */
    @Transactional
    public String synchroniserDepuisComlink() throws Exception {
        GuildeConfiguration guilde = guildeService.getGuildeActiveOuErreur();
        JSONObject root = guildClient.recupererGuildeComplete(guilde.getGuildId());
        return synchroniserDepuisJson(root, guilde.getGuildId());
    }

    /**
     * Variante séparée utile pour les tests et pour garder le parsing indépendant
     * de l'appel HTTP.
     */
    @Transactional
    public String synchroniserDepuisJson(JSONObject root, String guildId) {
        JSONObject guild = root.optJSONObject("guild");
        if (guild == null) {
            guild = root;
        }

        JSONArray results = guild.optJSONArray("recentTerritoryBattleResult");
        if (results == null || results.length() == 0) {
            return "Aucune bataille de territoire terminée disponible dans Comlink.";
        }

        int nouvelles = 0;
        int verifiees = 0;
        int activites = 0;
        int scores = 0;

        for (int i = 0; i < results.length(); i++) {
            JSONObject tb = results.optJSONObject(i);
            if (tb == null) continue;

            String instanceId = tb.optString("instanceId", "").trim();
            if (instanceId.isBlank()) continue;

            // Une TB déjà importée ne génère plus aucune écriture.
            // Avec la transaction, une première importation est soit complète,
            // soit entièrement annulée en cas d'erreur.
            if (existe(instanceId)) {
                verifiees++;
                continue;
            }

            long tbId = upsertTerritoryBattle(tb, guildId);
            nouvelles++;

            ImportCompteur compteur = importerActivitesEtScores(tbId, tb);
            activites += compteur.activites;
            scores += compteur.scores;
        }

        if (nouvelles == 0 && verifiees > 0) {
            return "Bataille de territoire déjà historisée : aucun nouvel enregistrement créé.";
        }

        return "Historique des batailles de territoire mis à jour : " + nouvelles + " nouvelle(s) bataille(s), "
                + activites + " activité(s), " + scores + " score(s) joueur.";
    }

    private boolean existe(String instanceId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM territory_battle WHERE instance_id = ?",
                Integer.class,
                instanceId);
        return count != null && count > 0;
    }

    private long upsertTerritoryBattle(JSONObject tb, String guildId) {
        String instanceId = tb.optString("instanceId", "").trim();
        String definitionId = tb.optString("definitionId", null);
        Timestamp start = timestamp(tb.optString("startTime", null));
        Timestamp end = timestamp(tb.optString("endTime", null));
        int stars = parseInt(tb.optString("totalStars", "0"));

        jdbc.update("""
            INSERT INTO territory_battle
                (guild_id, instance_id, definition_id, start_time, end_time, total_stars, date_import)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                guild_id = VALUES(guild_id),
                definition_id = VALUES(definition_id),
                start_time = VALUES(start_time),
                end_time = VALUES(end_time),
                total_stars = VALUES(total_stars)
            """,
            guildId,
            instanceId,
            definitionId,
            start,
            end,
            stars);

        Long id = jdbc.queryForObject(
                "SELECT id FROM territory_battle WHERE instance_id = ?",
                Long.class,
                instanceId);

        if (id == null) {
            throw new IllegalStateException("Impossible de retrouver la bataille de territoire " + instanceId + " après import.");
        }
        return id;
    }

    private ImportCompteur importerActivitesEtScores(long tbId, JSONObject tb) {
        JSONArray finalStats = tb.optJSONArray("finalStat");
        if (finalStats == null) {
            return new ImportCompteur(0, 0);
        }

        int activites = 0;
        int scores = 0;

        for (int i = 0; i < finalStats.length(); i++) {
            JSONObject fs = finalStats.optJSONObject(i);
            if (fs == null) continue;

            String mapStatId = fs.optString("mapStatId", "").trim();
            if (mapStatId.isBlank()) continue;

            ParsedMapStat parsed = MapStatIdParser.parse(mapStatId);

            jdbc.update("""
                INSERT INTO tb_activite
                    (territory_battle_id, map_stat_id, stat_type, phase, conflict_num, bonus, covert_num, round_num)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    stat_type = VALUES(stat_type),
                    phase = VALUES(phase),
                    conflict_num = VALUES(conflict_num),
                    bonus = VALUES(bonus),
                    covert_num = VALUES(covert_num),
                    round_num = VALUES(round_num)
                """,
                tbId,
                mapStatId,
                parsed.statType(),
                parsed.phase(),
                parsed.conflict(),
                parsed.bonus(),
                parsed.covertNum(),
                parsed.roundNum());

            Long activiteId = jdbc.queryForObject(
                    "SELECT id FROM tb_activite WHERE territory_battle_id = ? AND map_stat_id = ?",
                    Long.class,
                    tbId,
                    mapStatId);

            if (activiteId == null) continue;
            activites++;

            JSONArray playerStats = fs.optJSONArray("playerStat");
            if (playerStats == null || playerStats.length() == 0) continue;

            List<Object[]> batch = new ArrayList<>();
            for (int j = 0; j < playerStats.length(); j++) {
                JSONObject ps = playerStats.optJSONObject(j);
                if (ps == null) continue;

                String playerId = ps.optString("memberId", "").trim();
                if (playerId.isBlank()) continue;

                Long score = parseLongNullable(ps.optString("score", null));
                batch.add(new Object[] { activiteId, playerId, score });
            }

            if (!batch.isEmpty()) {
                jdbc.batchUpdate("""
                    INSERT INTO tb_score_joueur (tb_activite_id, player_id, score)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE score = VALUES(score)
                    """, batch);
                scores += batch.size();
            }
        }

        return new ImportCompteur(activites, scores);
    }

    private Timestamp timestamp(String raw) {
        Long value = parseLongNullable(raw);
        if (value == null || value <= 0) return null;

        // Comlink renvoie les TB en millisecondes. La détection rend le code
        // tolérant si une future réponse utilise des secondes Unix.
        Instant instant = value > 100_000_000_000L
                ? Instant.ofEpochMilli(value)
                : Instant.ofEpochSecond(value);
        return Timestamp.from(instant);
    }

    private int parseInt(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long parseLongNullable(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class ImportCompteur {
        private final int activites;
        private final int scores;

        private ImportCompteur(int activites, int scores) {
            this.activites = activites;
            this.scores = scores;
        }
    }
}
