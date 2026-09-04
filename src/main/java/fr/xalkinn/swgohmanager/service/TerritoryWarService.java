package fr.xalkinn.swgohmanager.service;

import java.sql.Timestamp;
import java.time.Instant;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.client.SwgohGuildClient;
import fr.xalkinn.swgohmanager.modele.GuildeConfiguration;
import jakarta.annotation.PostConstruct;

/**
 * Historise les résultats finaux de Guerre de territoires exposés par
 * recentTerritoryWarResult dans /guild.
 *
 * Contrairement à la TB, Comlink ne fournit pas ici un instanceId dédié.
 * L'identité stable retenue est donc : guilde + type TW + startTime.
 */
@Service
public class TerritoryWarService {

    private final JdbcTemplate jdbc;
    private final GuildeService guildeService;
    private final SwgohGuildClient guildClient;

    public TerritoryWarService(
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
            CREATE TABLE IF NOT EXISTS territory_war (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                guild_id VARCHAR(100) NOT NULL,
                event_key VARCHAR(255) NOT NULL UNIQUE,
                territory_war_id VARCHAR(100) NULL,
                score INT NOT NULL DEFAULT 0,
                power BIGINT NOT NULL DEFAULT 0,
                opponent_score INT NOT NULL DEFAULT 0,
                opponent_guild_id VARCHAR(100) NULL,
                opponent_guild_name VARCHAR(255) NULL,
                opponent_guild_gp BIGINT NOT NULL DEFAULT 0,
                start_time DATETIME NULL,
                end_time DATETIME NULL,
                date_import DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_tw_end_time (end_time),
                INDEX idx_tw_guild_id (guild_id),
                INDEX idx_tw_opponent (opponent_guild_id)
            )
            """);
    }

    @Transactional
    public String synchroniserDepuisComlink() throws Exception {
        GuildeConfiguration guilde = guildeService.getGuildeActiveOuErreur();
        JSONObject root = guildClient.recupererGuildeComplete(guilde.getGuildId());
        return synchroniserDepuisJson(root, guilde.getGuildId());
    }

    @Transactional
    public String synchroniserDepuisJson(JSONObject root, String guildId) {
        JSONObject guild = root.optJSONObject("guild");
        if (guild == null) guild = root;

        JSONArray results = guild.optJSONArray("recentTerritoryWarResult");
        if (results == null || results.length() == 0) {
            return "Aucune Guerre de territoires terminée disponible dans Comlink.";
        }

        int nouvelles = 0;
        int dejaConnues = 0;

        for (int i = 0; i < results.length(); i++) {
            JSONObject tw = results.optJSONObject(i);
            if (tw == null) continue;

            String twId = tw.optString("territoryWarId", "").trim();
            String startRaw = tw.optString("startTime", "").trim();
            String endRaw = tw.optString("endTimeSeconds", "").trim();

            if (twId.isBlank() && startRaw.isBlank() && endRaw.isBlank()) continue;

            String eventKey = construireEventKey(guildId, twId, startRaw, endRaw);
            if (existe(eventKey)) {
                dejaConnues++;
                continue;
            }

            JSONObject opponent = tw.optJSONObject("opponentGuildProfile");

            jdbc.update("""
                INSERT INTO territory_war (
                    guild_id, event_key, territory_war_id,
                    score, power, opponent_score,
                    opponent_guild_id, opponent_guild_name, opponent_guild_gp,
                    start_time, end_time, date_import
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                guildId,
                eventKey,
                blankToNull(twId),
                parseInt(tw.optString("score", "0")),
                parseLong(tw.optString("power", "0")),
                parseInt(tw.optString("opponentScore", "0")),
                opponent == null ? null : blankToNull(opponent.optString("id", "")),
                opponent == null ? null : blankToNull(opponent.optString("name", "")),
                opponent == null ? 0L : parseLong(opponent.optString("guildGalacticPower", "0")),
                timestamp(startRaw),
                timestamp(endRaw));
            nouvelles++;
        }

        if (nouvelles == 0 && dejaConnues > 0) {
            return "Guerres de territoires déjà historisées : aucun nouvel enregistrement créé.";
        }

        return "Historique des Guerres de territoires mis à jour : " + nouvelles
                + " nouvelle(s) TW, " + dejaConnues + " déjà connue(s).";
    }

    private boolean existe(String eventKey) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM territory_war WHERE event_key = ?",
                Integer.class,
                eventKey);
        return count != null && count > 0;
    }

    private String construireEventKey(String guildId, String twId, String startRaw, String endRaw) {
        String pivot = !startRaw.isBlank() ? startRaw : endRaw;
        return (guildId == null ? "" : guildId.trim()) + ":"
                + (twId == null ? "" : twId.trim()) + ":" + pivot;
    }

    private Timestamp timestamp(String raw) {
        Long value = parseLongNullable(raw);
        if (value == null || value <= 0) return null;

        Instant instant = value > 100_000_000_000L
                ? Instant.ofEpochMilli(value)
                : Instant.ofEpochSecond(value);
        return Timestamp.from(instant);
    }

    private int parseInt(String raw) {
        Long value = parseLongNullable(raw);
        if (value == null) return 0;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return value.intValue();
    }

    private long parseLong(String raw) {
        Long value = parseLongNullable(raw);
        return value == null ? 0L : value;
    }

    private Long parseLongNullable(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
