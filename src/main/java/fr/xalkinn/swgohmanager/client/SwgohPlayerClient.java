package fr.xalkinn.swgohmanager.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import fr.xalkinn.swgohmanager.config.SwgohProperties;

/**
 * Client dédié à l'endpoint /player de swgoh-comlink.
 *
 * Le joueur est volontairement renvoyé sous forme de JSONObject : la réponse
 * Comlink contient beaucoup de données (roster, mods, profils, etc.) et les
 * services consommateurs peuvent extraire uniquement ce dont ils ont besoin.
 */
@Component
public class SwgohPlayerClient {

    private final SwgohProperties properties;
    private final HttpClient httpClient;

    public SwgohPlayerClient(SwgohProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public JSONObject rechercherParAllyCode(String allyCode) throws Exception {
        String allyCodeNormalise = normaliserAllyCode(allyCode);
        return recupererJoueur(new JSONObject().put("allyCode", allyCodeNormalise));
    }

    public JSONObject rechercherParPlayerId(String playerId) throws Exception {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Le Player ID est obligatoire.");
        }

        return recupererJoueur(new JSONObject().put("playerId", playerId.trim()));
    }

    public String normaliserAllyCode(String allyCode) {
        if (allyCode == null || allyCode.isBlank()) {
            throw new IllegalArgumentException("L'Ally Code est obligatoire.");
        }

        // Accepte 599225682, 599-225-682 et 599 225 682.
        String normalise = allyCode.replaceAll("\\D", "");

        if (!normalise.matches("\\d{9}")) {
            throw new IllegalArgumentException(
                    "L'Ally Code doit contenir exactement 9 chiffres.");
        }

        return normalise;
    }

    private JSONObject recupererJoueur(JSONObject payload) throws Exception {
        JSONObject body = new JSONObject().put("payload", payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getComlink().endpoint("/player")))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                    "Impossible de récupérer le joueur depuis Comlink (HTTP "
                    + response.statusCode() + ").");
        }

        if (response.body() == null || response.body().isBlank()) {
            throw new IllegalStateException(
                    "Comlink n'a renvoyé aucune donnée pour ce joueur.");
        }

        JSONObject root = new JSONObject(response.body());
        JSONObject player = root.optJSONObject("player");
        if (player == null) {
            player = root;
        }

        if (player.optString("playerId", "").isBlank()
                && player.optString("name", "").isBlank()) {
            throw new IllegalStateException(
                    "La réponse Comlink ne contient pas de profil joueur exploitable.");
        }

        return player;
    }
}
