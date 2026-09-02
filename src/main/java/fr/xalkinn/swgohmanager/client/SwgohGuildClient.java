package fr.xalkinn.swgohmanager.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import fr.xalkinn.swgohmanager.config.SwgohProperties;
import fr.xalkinn.swgohmanager.dto.GuildeDetectee;

@Component
public class SwgohGuildClient {

    private final SwgohProperties properties;
    private final SwgohPlayerClient playerClient;
    private final HttpClient httpClient;

    public SwgohGuildClient(SwgohProperties properties, SwgohPlayerClient playerClient) {
        this.properties = properties;
        this.playerClient = playerClient;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Recherche automatiquement la guilde d'un joueur à partir de son Ally Code.
     *
     * Comlink /player renvoie directement guildId et guildName dans le profil du
     * joueur. Une fois le guildId récupéré, on interroge /guild pour vérifier la
     * guilde et récupérer ses informations complètes.
     */
    public GuildeDetectee rechercherGuildeParAllyCode(String allyCode) throws Exception {
        JSONObject player = playerClient.rechercherParAllyCode(allyCode);

        String guildId = player.optString("guildId", "").trim();
        String guildName = player.optString("guildName", "").trim();
        String playerName = player.optString("name", "").trim();

        if (guildId.isBlank()) {
            String prefixe = playerName.isBlank()
                    ? "Le joueur correspondant à cet Ally Code"
                    : "Le joueur " + playerName;

            throw new IllegalStateException(
                    prefixe + " n'appartient actuellement à aucune guilde.");
        }

        try {
            return rechercherGuilde(guildId);
        } catch (Exception e) {
            // /player contient déjà les informations essentielles. Si /guild
            // est temporairement indisponible, on garde la guilde détectée.
            if (!guildName.isBlank()) {
                return new GuildeDetectee(guildId, guildName, 0);
            }
            throw e;
        }
    }

    public GuildeDetectee rechercherGuilde(String guildId) throws Exception {
        if (guildId == null || guildId.isBlank()) {
            throw new IllegalArgumentException("Le Guild ID est obligatoire.");
        }

        JSONObject payload = new JSONObject()
                .put("guildId", guildId.trim())
                .put("includeRecentGuildActivityInfo", true);

        JSONObject body = new JSONObject().put("payload", payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getComlink().endpoint("/guild")))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                    "Comlink a répondu avec le code HTTP " + response.statusCode());
        }

        JSONObject root = new JSONObject(response.body());

        JSONObject guild = root.optJSONObject("guild");
        if (guild == null) {
            guild = root;
        }

        JSONArray membres = guild.optJSONArray("member");
        if (membres == null) {
            membres = root.optJSONArray("member");
        }

        if (membres == null) {
            throw new IllegalStateException(
                    "La réponse Comlink ne contient pas de liste de membres.");
        }

        String nom = extraireNomGuilde(guild);

        if ((nom == null || nom.isBlank()) && membres.length() > 0) {
            JSONObject premier = membres.optJSONObject(0);
            if (premier != null) {
                String playerId = premier.optString("playerId", null);
                if (playerId != null && !playerId.isBlank()) {
                    nom = recupererNomGuildeDepuisJoueur(playerId);
                }
            }
        }

        if (nom == null || nom.isBlank()) {
            nom = "Guilde " + guildId.trim();
        }

        return new GuildeDetectee(guildId.trim(), nom, membres.length());
    }

    public boolean testerConnexion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getComlink().endpoint("")))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            return response.statusCode() < 500;

        } catch (Exception e) {
            return false;
        }
    }

    private String extraireNomGuilde(JSONObject guild) {
        String nom = guild.optString("name", null);

        if (nom != null && !nom.isBlank()) {
            return nom;
        }

        JSONObject profile = guild.optJSONObject("profile");
        if (profile != null) {
            nom = profile.optString("name", null);
        }

        return nom;
    }

    private String recupererNomGuildeDepuisJoueur(String playerId) throws Exception {
        JSONObject joueur = playerClient.rechercherParPlayerId(playerId);
        return joueur.optString("guildName", null);
    }
}
