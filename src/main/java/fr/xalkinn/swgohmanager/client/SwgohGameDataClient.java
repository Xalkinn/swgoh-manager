package fr.xalkinn.swgohmanager.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

import fr.xalkinn.swgohmanager.config.SwgohProperties;

/**
 * Client Game Data minimal pour le catalogue Omicron.
 *
 * On ne charge jamais le JSON complet en mémoire : les segments /data sont
 * lus en streaming et seules les collections utiles (skill / units) sont
 * désérialisées.
 */
@Component
public class SwgohGameDataClient {

    private final SwgohProperties properties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    public SwgohGameDataClient(SwgohProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getLatestGameVersion() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getComlink().endpoint("/metadata")))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        verifierReponse(response.statusCode(), response.body(), "/metadata");

        JSONObject json = new JSONObject(response.body());
        String version = json.optString("latestGamedataVersion", "").trim();
        if (version.isBlank()) {
            throw new IllegalStateException("latestGamedataVersion absent de /metadata.");
        }
        return version;
    }

    public List<GameSkill> getSkills(String version) throws Exception {
        return streamSkills(version);
    }

    public List<GameUnit> getUnits(String version) throws Exception {
        return streamUnits(version);
    }

    /**
     * Même principe que le client Game Data du projet de référence : on lit le
     * flux /data token par token et on n'extrait que le tableau utile.
     *
     * Important avec Spring Boot 4 / Jackson 3 : on ne fait volontairement
     * aucun jsonMapper.readValue(parser, ...) à l'intérieur d'un tableau.
     * FAIL_ON_TRAILING_TOKENS considérerait l'objet suivant du tableau comme
     * un token parasite. Le mapping est donc réalisé directement depuis les
     * tokens du flux.
     */
    private List<GameSkill> streamSkills(String version) throws Exception {
        try (InputStream in = ouvrirSegment(version, 1)) {
            return extraireSkills(in);
        }
    }

    private List<GameUnit> streamUnits(String version) throws Exception {
        try (InputStream in = ouvrirSegment(version, 3)) {
            return extraireUnits(in);
        }
    }

    private InputStream ouvrirSegment(String version, int segment) throws Exception {
        String body = """
                {"payload":{"version":"%s","includePveUnits":false,"requestSegment":%d}}
                """.formatted(version, segment).trim();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getComlink().endpoint("/data")))
                .timeout(Duration.ofMinutes(4))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() >= 400) {
            try (InputStream ignored = response.body()) {
                // ferme le flux HTTP en erreur
            }
            throw new IllegalStateException(
                    "Comlink /data a répondu HTTP " + response.statusCode()
                    + " pour le segment " + segment + ".");
        }

        return response.body();
    }

    private List<GameSkill> extraireSkills(InputStream in) throws IOException {
        List<GameSkill> resultat = new ArrayList<>();

        try (JsonParser parser = jsonMapper.createParser(in)) {
            verifierDebutObjet(parser);

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                JsonToken valeur = parser.nextToken();

                if ("skill".equals(champ) && valeur == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            resultat.add(lireSkill(parser));
                        } else {
                            parser.skipChildren();
                        }
                    }
                } else {
                    parser.skipChildren();
                }
            }
        }

        return resultat;
    }

    private GameSkill lireSkill(JsonParser parser) throws IOException {
        GameSkill skill = new GameSkill();
        List<GameSkillTier> tiers = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();

            if (champ == null) {
                parser.skipChildren();
                continue;
            }

            switch (champ) {
                case "id" -> skill.setId(lireTexte(parser, valeur));
                case "nameKey" -> skill.setNameKey(lireTexte(parser, valeur));
                case "abilityReference" -> skill.setAbilityReference(lireTexte(parser, valeur));
                case "skillType" -> skill.setSkillType(lireEntier(parser, valeur));
                case "omicronMode" -> skill.setOmicronMode(lireEntier(parser, valeur));
                case "tier" -> {
                    if (valeur == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                tiers.add(lireSkillTier(parser));
                            } else {
                                parser.skipChildren();
                            }
                        }
                    } else {
                        parser.skipChildren();
                    }
                }
                default -> parser.skipChildren();
            }
        }

        skill.setTier(tiers);
        return skill;
    }

    private GameSkillTier lireSkillTier(JsonParser parser) throws IOException {
        GameSkillTier tier = new GameSkillTier();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();

            if ("isOmicronTier".equals(champ)) {
                tier.setIsOmicronTier(lireBooleen(parser, valeur));
            } else {
                parser.skipChildren();
            }
        }
        return tier;
    }

    private List<GameUnit> extraireUnits(InputStream in) throws IOException {
        List<GameUnit> resultat = new ArrayList<>();

        try (JsonParser parser = jsonMapper.createParser(in)) {
            verifierDebutObjet(parser);

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                JsonToken valeur = parser.nextToken();

                if ("units".equals(champ) && valeur == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            resultat.add(lireUnit(parser));
                        } else {
                            parser.skipChildren();
                        }
                    }
                } else {
                    parser.skipChildren();
                }
            }
        }

        return resultat;
    }

    private GameUnit lireUnit(JsonParser parser) throws IOException {
        GameUnit unit = new GameUnit();
        List<SkillReference> refs = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();

            if (champ == null) {
                parser.skipChildren();
                continue;
            }

            switch (champ) {
                case "id" -> unit.setId(lireTexte(parser, valeur));
                case "baseId" -> unit.setBaseId(lireTexte(parser, valeur));
                case "nameKey" -> unit.setNameKey(lireTexte(parser, valeur));
                case "combatType" -> unit.setCombatType(lireCombatType(parser, valeur));
                case "skillReferenceList", "skillReference" -> {
                    if (valeur == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                refs.add(lireSkillReference(parser));
                            } else {
                                parser.skipChildren();
                            }
                        }
                    } else {
                        parser.skipChildren();
                    }
                }
                default -> parser.skipChildren();
            }
        }

        unit.setSkillReferenceList(refs);
        return unit;
    }

    private SkillReference lireSkillReference(JsonParser parser) throws IOException {
        SkillReference ref = new SkillReference();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();

            if (champ == null) {
                parser.skipChildren();
                continue;
            }

            switch (champ) {
                case "skillId" -> ref.setSkillId(lireTexte(parser, valeur));
                case "requiredTier" -> ref.setRequiredTier(lireEntier(parser, valeur));
                case "requiredRarity" -> ref.setRequiredRarity(lireEntier(parser, valeur));
                default -> parser.skipChildren();
            }
        }
        return ref;
    }

    private void verifierDebutObjet(JsonParser parser) throws IOException {
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Réponse /data inattendue : objet JSON attendu.");
        }
    }

    private String lireTexte(JsonParser parser, JsonToken token) throws IOException {
        if (token == null || token == JsonToken.VALUE_NULL) return null;
        if (token.isScalarValue()) return parser.getValueAsString();
        parser.skipChildren();
        return null;
    }

    private Integer lireEntier(JsonParser parser, JsonToken token) throws IOException {
        if (token == null || token == JsonToken.VALUE_NULL) return null;
        try {
            if (token == JsonToken.VALUE_NUMBER_INT) return parser.getIntValue();
            String texte = parser.getValueAsString();
            return texte == null || texte.isBlank() ? null : Integer.valueOf(texte);
        } catch (Exception e) {
            parser.skipChildren();
            return null;
        }
    }

    private Boolean lireBooleen(JsonParser parser, JsonToken token) throws IOException {
        if (token == JsonToken.VALUE_TRUE) return Boolean.TRUE;
        if (token == JsonToken.VALUE_FALSE) return Boolean.FALSE;
        if (token == null || token == JsonToken.VALUE_NULL) return null;
        String texte = parser.getValueAsString();
        return texte == null ? null : Boolean.valueOf(texte);
    }

    private Integer lireCombatType(JsonParser parser, JsonToken token) throws IOException {
        Integer numeric = lireEntier(parser, token);
        if (numeric != null) return numeric;

        String texte = parser.getValueAsString();
        if (texte == null) return null;
        return switch (texte.trim().toUpperCase()) {
            case "CHARACTER" -> 1;
            case "SHIP" -> 2;
            default -> null;
        };
    }

    private void verifierReponse(int status, String body, String endpoint) {
        if (status >= 400) {
            throw new IllegalStateException(
                    "Comlink " + endpoint + " a répondu HTTP " + status + ".");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Réponse vide de Comlink pour " + endpoint + ".");
        }
    }

    public static class GameSkill {
        private String id;
        private String nameKey;
        private String abilityReference;
        private Integer skillType;
        private Integer omicronMode;
        private List<GameSkillTier> tier = List.of();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNameKey() { return nameKey; }
        public void setNameKey(String nameKey) { this.nameKey = nameKey; }
        public String getAbilityReference() { return abilityReference; }
        public void setAbilityReference(String abilityReference) { this.abilityReference = abilityReference; }
        public Integer getSkillType() { return skillType; }
        public void setSkillType(Integer skillType) { this.skillType = skillType; }
        public Integer getOmicronMode() { return omicronMode; }
        public void setOmicronMode(Integer omicronMode) { this.omicronMode = omicronMode; }
        public List<GameSkillTier> getTier() { return tier == null ? List.of() : tier; }
        public void setTier(List<GameSkillTier> tier) { this.tier = tier; }

        public Integer tierOmicronRequis() {
            List<GameSkillTier> tiers = getTier();
            for (int i = 0; i < tiers.size(); i++) {
                if (Boolean.TRUE.equals(tiers.get(i).getIsOmicronTier())) {
                    // Les tiers Game Data sont indexés dans la liste. Le roster
                    // /player expose un tier décalé d'une unité.
                    return i + 1;
                }
            }
            return null;
        }

        public boolean hasOmicron() {
            return tierOmicronRequis() != null;
        }
    }

    public static class GameSkillTier {
        private Boolean isOmicronTier;

        public Boolean getIsOmicronTier() { return isOmicronTier; }
        public void setIsOmicronTier(Boolean isOmicronTier) { this.isOmicronTier = isOmicronTier; }
    }

    public static class GameUnit {
        private String id;
        private String baseId;
        private String nameKey;
        private Integer combatType;

        private List<SkillReference> skillReferenceList = List.of();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getBaseId() { return baseId; }
        public void setBaseId(String baseId) { this.baseId = baseId; }
        public String getNameKey() { return nameKey; }
        public void setNameKey(String nameKey) { this.nameKey = nameKey; }
        public Integer getCombatType() { return combatType; }
        public void setCombatType(Integer combatType) { this.combatType = combatType; }
        public List<SkillReference> getSkillReferenceList() {
            return skillReferenceList == null ? List.of() : skillReferenceList;
        }
        public void setSkillReferenceList(List<SkillReference> skillReferenceList) {
            this.skillReferenceList = skillReferenceList;
        }
    }

    public static class SkillReference {
        private String skillId;
        private Integer requiredTier;
        private Integer requiredRarity;

        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
        public Integer getRequiredTier() { return requiredTier; }
        public void setRequiredTier(Integer requiredTier) { this.requiredTier = requiredTier; }
        public Integer getRequiredRarity() { return requiredRarity; }
        public void setRequiredRarity(Integer requiredRarity) { this.requiredRarity = requiredRarity; }
    }
}
