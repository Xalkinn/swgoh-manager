package fr.xalkinn.swgohmanager.service;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.client.SwgohGuildClient;
import fr.xalkinn.swgohmanager.dto.TerritoryWarLiveDiagnosticView;
import fr.xalkinn.swgohmanager.modele.GuildeConfiguration;
import jakarta.annotation.PostConstruct;

/**
 * Diagnostic non intrusif des données TW exposées par le /guild Comlink.
 *
 * Ce service ne modifie aucun batch roster et n'essaie pas d'inventer des
 * données de combat. Il capture la réponse réellement reçue afin de savoir si
 * territoryWarStatus (ou une structure voisine) est disponible sur l'instance
 * Comlink utilisée par SWGOH Manager.
 */
@Service
public class TerritoryWarLiveDiagnosticService {

    private static final String SOURCE = "COMLINK /guild";
    private static final int MAX_CANDIDATE_PATHS = 140;
    private static final int MAX_SCAN_DEPTH = 14;

    private final JdbcTemplate jdbc;
    private final GuildeService guildeService;
    private final SwgohGuildClient guildClient;

    public TerritoryWarLiveDiagnosticService(
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
            CREATE TABLE IF NOT EXISTS territory_war_live_snapshot (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                guild_id VARCHAR(100) NOT NULL,
                source VARCHAR(80) NOT NULL,
                status_key_found TINYINT(1) NOT NULL DEFAULT 0,
                status_item_count INT NOT NULL DEFAULT 0,
                recent_result_count INT NOT NULL DEFAULT 0,
                member_count INT NOT NULL DEFAULT 0,
                raw_size_bytes INT NOT NULL DEFAULT 0,
                top_level_keys LONGTEXT NULL,
                candidate_paths LONGTEXT NULL,
                raw_json LONGTEXT NOT NULL,
                captured_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_tw_live_snapshot_guild (guild_id),
                INDEX idx_tw_live_snapshot_date (captured_at)
            )
            """);
    }

    @Transactional
    public TerritoryWarLiveDiagnosticView capturerDepuisComlink() throws Exception {
        GuildeConfiguration guilde = guildeService.getGuildeActiveOuErreur();
        JSONObject root = guildClient.recupererGuildeComplete(guilde.getGuildId());
        JSONObject guild = root.optJSONObject("guild");
        if (guild == null) guild = root;

        String rawJson = root.toString(2);
        List<String> topLevelKeys = collecterClesPrincipales(root, guild);
        List<String> candidatePaths = new ArrayList<>();
        scannerCandidats(root, "$", 0, candidatePaths);

        ExactKeyScan statusScan = new ExactKeyScan();
        rechercherCleExacte(root, "territoryWarStatus", statusScan, 0);

        int recentResultCount = longueur(guild.opt("recentTerritoryWarResult"));
        if (recentResultCount == 0 && guild != root) {
            recentResultCount = longueur(root.opt("recentTerritoryWarResult"));
        }

        int memberCount = longueur(guild.opt("member"));
        if (memberCount == 0 && guild != root) {
            memberCount = longueur(root.opt("member"));
        }

        int rawSizeBytes = rawJson.getBytes(StandardCharsets.UTF_8).length;

        jdbc.update("""
            INSERT INTO territory_war_live_snapshot (
                guild_id, source,
                status_key_found, status_item_count,
                recent_result_count, member_count,
                raw_size_bytes, top_level_keys, candidate_paths,
                raw_json, captured_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """,
            guilde.getGuildId(),
            SOURCE,
            statusScan.found ? 1 : 0,
            statusScan.itemCount,
            recentResultCount,
            memberCount,
            rawSizeBytes,
            String.join("\n", topLevelKeys),
            String.join("\n", candidatePaths),
            rawJson);

        return getDernierPourGuilde(guilde.getGuildId())
                .orElseThrow(() -> new IllegalStateException("Le snapshot TW Live vient d'être créé mais n'a pas pu être relu."));
    }

    public Optional<TerritoryWarLiveDiagnosticView> getDernier() {
        try {
            GuildeConfiguration guilde = guildeService.getGuildeActiveOuErreur();
            return getDernierPourGuilde(guilde.getGuildId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<TerritoryWarLiveDiagnosticView> getParId(long id) {
        List<TerritoryWarLiveDiagnosticView> rows = jdbc.query("""
                SELECT id, guild_id, source, status_key_found, status_item_count,
                       recent_result_count, member_count, raw_size_bytes,
                       top_level_keys, candidate_paths, raw_json, captured_at
                FROM territory_war_live_snapshot
                WHERE id = ?
                LIMIT 1
                """,
                this::mapper,
                id);
        return rows.stream().findFirst();
    }

    private Optional<TerritoryWarLiveDiagnosticView> getDernierPourGuilde(String guildId) {
        List<TerritoryWarLiveDiagnosticView> rows = jdbc.query("""
                SELECT id, guild_id, source, status_key_found, status_item_count,
                       recent_result_count, member_count, raw_size_bytes,
                       top_level_keys, candidate_paths, raw_json, captured_at
                FROM territory_war_live_snapshot
                WHERE guild_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                this::mapper,
                guildId);
        return rows.stream().findFirst();
    }

    private TerritoryWarLiveDiagnosticView mapper(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        TerritoryWarLiveDiagnosticView row = new TerritoryWarLiveDiagnosticView();
        row.setId(rs.getLong("id"));
        row.setGuildId(rs.getString("guild_id"));
        row.setSource(rs.getString("source"));
        row.setStatusKeyFound(rs.getBoolean("status_key_found"));
        row.setStatusItemCount(rs.getInt("status_item_count"));
        row.setRecentResultCount(rs.getInt("recent_result_count"));
        row.setMemberCount(rs.getInt("member_count"));
        row.setRawSizeBytes(rs.getInt("raw_size_bytes"));
        row.setTopLevelKeys(splitLines(rs.getString("top_level_keys")));
        row.setCandidatePaths(splitLines(rs.getString("candidate_paths")));
        row.setRawJson(rs.getString("raw_json"));
        Timestamp capturedAt = rs.getTimestamp("captured_at");
        row.setCapturedAt(capturedAt == null ? null : capturedAt.toLocalDateTime());
        return row;
    }

    private List<String> collecterClesPrincipales(JSONObject root, JSONObject guild) {
        List<String> keys = new ArrayList<>();
        List<String> rootKeys = clesTriees(root);

        if (guild == root) {
            keys.addAll(rootKeys);
            return keys;
        }

        for (String key : rootKeys) {
            keys.add("root." + key);
        }
        for (String key : clesTriees(guild)) {
            keys.add("guild." + key);
        }
        return keys;
    }

    private List<String> clesTriees(JSONObject object) {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = object.keys();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
        return keys;
    }

    private void scannerCandidats(Object node, String path, int depth, List<String> matches) {
        if (node == null || node == JSONObject.NULL || depth > MAX_SCAN_DEPTH || matches.size() >= MAX_CANDIDATE_PATHS) {
            return;
        }

        if (node instanceof JSONObject object) {
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext() && matches.size() < MAX_CANDIDATE_PATHS) {
                String key = iterator.next();
                Object value = object.opt(key);
                String childPath = path + "." + key;

                if (estCleCandidate(key)) {
                    matches.add(childPath + " → " + decrire(value));
                }
                scannerCandidats(value, childPath, depth + 1, matches);
            }
            return;
        }

        if (node instanceof JSONArray array) {
            int max = Math.min(array.length(), 12);
            for (int i = 0; i < max && matches.size() < MAX_CANDIDATE_PATHS; i++) {
                scannerCandidats(array.opt(i), path + "[" + i + "]", depth + 1, matches);
            }
            if (array.length() > max && matches.size() < MAX_CANDIDATE_PATHS) {
                matches.add(path + "[…] → analyse limitée à " + max + " élément(s) sur " + array.length());
            }
        }
    }

    private boolean estCleCandidate(String key) {
        if (key == null) return false;
        String normalized = key.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");

        return normalized.contains("territorywar")
                || normalized.contains("territorytournament")
                || normalized.contains("warstatus")
                || normalized.contains("conflict")
                || normalized.contains("combat")
                || normalized.contains("battle")
                || normalized.contains("attack")
                || normalized.contains("defense")
                || normalized.contains("defence")
                || normalized.contains("squad")
                || normalized.contains("zone");
    }

    private String decrire(Object value) {
        if (value == null || value == JSONObject.NULL) return "null";
        if (value instanceof JSONArray array) return "tableau [" + array.length() + "]";
        if (value instanceof JSONObject object) return "objet {" + object.length() + " clé(s)}";
        if (value instanceof String string) return "texte (" + string.length() + " caractère(s))";
        if (value instanceof Number) return "nombre";
        if (value instanceof Boolean) return "booléen";
        return value.getClass().getSimpleName();
    }

    private void rechercherCleExacte(Object node, String target, ExactKeyScan result, int depth) {
        if (node == null || node == JSONObject.NULL || depth > MAX_SCAN_DEPTH) return;

        if (node instanceof JSONObject object) {
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = object.opt(key);
                if (target.equals(key)) {
                    result.found = true;
                    result.itemCount += longueur(value);
                }
                rechercherCleExacte(value, target, result, depth + 1);
            }
            return;
        }

        if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                rechercherCleExacte(array.opt(i), target, result, depth + 1);
            }
        }
    }

    private int longueur(Object value) {
        if (value == null || value == JSONObject.NULL) return 0;
        if (value instanceof JSONArray array) return array.length();
        if (value instanceof JSONObject object) return object.length() == 0 ? 0 : 1;
        if (value instanceof String string) return string.isBlank() ? 0 : 1;
        return 1;
    }

    private List<String> splitLines(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return new ArrayList<>(List.of(raw.split("\\R")));
    }

    private static class ExactKeyScan {
        private boolean found;
        private int itemCount;
    }
}
