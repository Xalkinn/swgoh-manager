package fr.xalkinn.swgohmanager.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fr.xalkinn.swgohmanager.config.SwgohProperties;

/**
 * Client minimal dédié aux localisations SWGOH exposées par swgoh-comlink.
 *
 * Il ne synchronise pas toute la GameData : il récupère uniquement le bundle
 * de localisation français nécessaire à l'affichage des noms.
 */
@Component
public class SwgohLocalizationClient {

    private final RestClient restClient;

    public SwgohLocalizationClient(SwgohProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getComlink().getBaseUrl())
                .build();
    }

    public String getLatestLocalizationVersion() {
        Map<String, Object> response = restClient.post()
                .uri("/metadata")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response == null) {
            throw new IllegalStateException("Réponse vide de Comlink pour /metadata");
        }

        Object version = response.get("latestLocalizationBundleVersion");
        if (version == null || String.valueOf(version).isBlank()) {
            throw new IllegalStateException(
                    "latestLocalizationBundleVersion absent de la réponse /metadata");
        }

        return String.valueOf(version);
    }

    public Map<String, String> getLocalizationFrancaise(String version) {
        String localizationId = version + ":FRE_FR";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", localizationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payload", payload);
        body.put("unzip", true);

        Map<String, Object> response = restClient.post()
                .uri("/localization")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response == null) {
            throw new IllegalStateException("Réponse vide de Comlink pour /localization");
        }

        Object fichier = response.get("Loc_FRE_FR.txt");
        if (!(fichier instanceof String contenu) || contenu.isBlank()) {
            throw new IllegalStateException(
                    "Loc_FRE_FR.txt absent de la réponse de localisation");
        }

        return extraireTraductions(contenu);
    }

    private Map<String, String> extraireTraductions(String contenu) {
        Map<String, String> traductions = new HashMap<>();

        for (String ligne : contenu.split("\r?\n")) {
            int separateur = ligne.indexOf('|');
            if (separateur <= 0) {
                continue;
            }

            String cle = ligne.substring(0, separateur).trim();
            String valeur = ligne.substring(separateur + 1).trim();

            if (!cle.isBlank() && !valeur.isBlank()) {
                traductions.put(cle, valeur);
            }
        }

        return traductions;
    }
}
