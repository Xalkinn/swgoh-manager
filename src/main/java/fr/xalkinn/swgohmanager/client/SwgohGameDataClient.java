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
 * Client Game Data en streaming pour les catalogues Omicron et Datacron.
 *
 * On ne charge jamais le JSON complet en mémoire : les segments /data sont
 * lus en streaming et seules les collections utiles (skill / units / Datacrons) sont
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
     * Lit le segment 4 Game Data et n'en conserve que les définitions DTC.
     */
    public DatacronGameData getDatacrons(String version) throws Exception {
        try (InputStream in = ouvrirSegment(version, 4)) {
            return extraireDatacrons(in);
        }
    }

    /**
     * Les descriptions des mécaniques DTC sont des AbilityDefinitions.
     * Dans les segments Comlink, ces définitions sont exposées dans le segment 2
     * (et non le segment 1 qui contient notamment les SkillDefinitions).
     */
    public List<GameAbility> getDatacronAbilities(String version) throws Exception {
        try (InputStream in = ouvrirSegment(version, 2)) {
            return extraireDatacronAbilities(in);
        }
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

    private DatacronGameData extraireDatacrons(InputStream in) throws IOException {
        List<GameDatacronTemplate> templates = new ArrayList<>();
        List<GameDatacronAffixTemplateSet> affixSets = new ArrayList<>();

        try (JsonParser parser = jsonMapper.createParser(in)) {
            verifierDebutObjet(parser);

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                JsonToken valeur = parser.nextToken();

                if ("datacronTemplate".equals(champ) && valeur == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            templates.add(lireDatacronTemplate(parser));
                        } else {
                            parser.skipChildren();
                        }
                    }
                } else if ("datacronAffixTemplateSet".equals(champ) && valeur == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            affixSets.add(lireDatacronAffixTemplateSet(parser));
                        } else {
                            parser.skipChildren();
                        }
                    }
                } else {
                    parser.skipChildren();
                }
            }
        }

        return new DatacronGameData(templates, affixSets);
    }

    private GameDatacronTemplate lireDatacronTemplate(JsonParser parser) throws IOException {
        GameDatacronTemplate template = new GameDatacronTemplate();
        List<GameDatacronTier> tiers = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();
            if (champ == null) {
                parser.skipChildren();
                continue;
            }

            switch (champ) {
                case "id" -> template.setId(lireTexte(parser, valeur));
                case "setId" -> template.setSetId(lireEntier(parser, valeur));
                case "referenceTemplateId" -> template.setReferenceTemplateId(lireTexte(parser, valeur));
                case "tier" -> {
                    if (valeur == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                tiers.add(lireDatacronTier(parser));
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

        template.setTier(tiers);
        return template;
    }

    private GameDatacronTier lireDatacronTier(JsonParser parser) throws IOException {
        GameDatacronTier tier = new GameDatacronTier();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();
            if (champ == null) {
                parser.skipChildren();
                continue;
            }
            switch (champ) {
                case "affixTemplateSetId" -> tier.setAffixTemplateSetId(lireTexte(parser, valeur));
                case "initialAffixTemplateSetId" -> tier.setInitialAffixTemplateSetId(lireTexte(parser, valeur));
                case "requiredUnitTier" -> tier.setRequiredUnitTier(lireEntier(parser, valeur));
                case "requiredRelicTier" -> tier.setRequiredRelicTier(lireEntier(parser, valeur));
                default -> parser.skipChildren();
            }
        }
        return tier;
    }

    private GameDatacronAffixTemplateSet lireDatacronAffixTemplateSet(JsonParser parser) throws IOException {
        GameDatacronAffixTemplateSet set = new GameDatacronAffixTemplateSet();
        List<GameDatacronAffix> affixes = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();
            if (champ == null) {
                parser.skipChildren();
                continue;
            }
            switch (champ) {
                case "id" -> set.setId(lireTexte(parser, valeur));
                case "affix" -> {
                    if (valeur == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                affixes.add(lireDatacronAffix(parser));
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

        set.setAffix(affixes);
        return set;
    }

    private GameDatacronAffix lireDatacronAffix(JsonParser parser) throws IOException {
        GameDatacronAffix affix = new GameDatacronAffix();
        List<String> tags = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();
            if (champ == null) {
                parser.skipChildren();
                continue;
            }
            switch (champ) {
                case "tag" -> {
                    if (valeur == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken().isScalarValue()) {
                                String tag = parser.getValueAsString();
                                if (tag != null && !tag.isBlank()) tags.add(tag);
                            } else {
                                parser.skipChildren();
                            }
                        }
                    } else {
                        parser.skipChildren();
                    }
                }
                case "targetRule" -> affix.setTargetRule(lireTexte(parser, valeur));
                case "abilityId" -> affix.setAbilityId(lireTexte(parser, valeur));
                case "statType" -> affix.setStatType(lireEntier(parser, valeur));
                case "statValue" -> affix.setStatValue(lireTexte(parser, valeur));
                case "requiredUnitTier" -> affix.setRequiredUnitTier(lireEntier(parser, valeur));
                case "requiredRelicTier" -> affix.setRequiredRelicTier(lireEntier(parser, valeur));
                case "scopeIcon" -> affix.setScopeIcon(lireTexte(parser, valeur));
                default -> parser.skipChildren();
            }
        }

        affix.setTag(tags);
        return affix;
    }

    private List<GameAbility> extraireDatacronAbilities(InputStream in) throws IOException {
        List<GameAbility> resultat = new ArrayList<>();

        try (JsonParser parser = jsonMapper.createParser(in)) {
            verifierDebutObjet(parser);
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                JsonToken valeur = parser.nextToken();
                if ("ability".equals(champ) && valeur == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            GameAbility ability = lireAbility(parser);
                            if (ability.getId() != null && ability.getId().startsWith("datacron_")) {
                                resultat.add(ability);
                            }
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

    private GameAbility lireAbility(JsonParser parser) throws IOException {
        GameAbility ability = new GameAbility();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String champ = parser.currentName();
            JsonToken valeur = parser.nextToken();
            if (champ == null) {
                parser.skipChildren();
                continue;
            }
            switch (champ) {
                case "id" -> ability.setId(lireTexte(parser, valeur));
                case "nameKey" -> ability.setNameKey(lireTexte(parser, valeur));
                case "descKey" -> ability.setDescKey(lireTexte(parser, valeur));
                default -> parser.skipChildren();
            }
        }
        return ability;
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

    public static class DatacronGameData {
        private final List<GameDatacronTemplate> templates;
        private final List<GameDatacronAffixTemplateSet> affixTemplateSets;

        public DatacronGameData(
                List<GameDatacronTemplate> templates,
                List<GameDatacronAffixTemplateSet> affixTemplateSets) {
            this.templates = templates == null ? List.of() : templates;
            this.affixTemplateSets = affixTemplateSets == null ? List.of() : affixTemplateSets;
        }

        public List<GameDatacronTemplate> getTemplates() { return templates; }
        public List<GameDatacronAffixTemplateSet> getAffixTemplateSets() { return affixTemplateSets; }
    }

    public static class GameDatacronTemplate {
        private String id;
        private Integer setId;
        private String referenceTemplateId;
        private List<GameDatacronTier> tier = List.of();
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Integer getSetId() { return setId; }
        public void setSetId(Integer setId) { this.setId = setId; }
        public String getReferenceTemplateId() { return referenceTemplateId; }
        public void setReferenceTemplateId(String referenceTemplateId) { this.referenceTemplateId = referenceTemplateId; }
        public List<GameDatacronTier> getTier() { return tier == null ? List.of() : tier; }
        public void setTier(List<GameDatacronTier> tier) { this.tier = tier; }
    }

    public static class GameDatacronTier {
        private String affixTemplateSetId;
        private String initialAffixTemplateSetId;
        private Integer requiredUnitTier;
        private Integer requiredRelicTier;
        public String getAffixTemplateSetId() { return affixTemplateSetId; }
        public void setAffixTemplateSetId(String affixTemplateSetId) { this.affixTemplateSetId = affixTemplateSetId; }
        public String getInitialAffixTemplateSetId() { return initialAffixTemplateSetId; }
        public void setInitialAffixTemplateSetId(String initialAffixTemplateSetId) { this.initialAffixTemplateSetId = initialAffixTemplateSetId; }
        public Integer getRequiredUnitTier() { return requiredUnitTier; }
        public void setRequiredUnitTier(Integer requiredUnitTier) { this.requiredUnitTier = requiredUnitTier; }
        public Integer getRequiredRelicTier() { return requiredRelicTier; }
        public void setRequiredRelicTier(Integer requiredRelicTier) { this.requiredRelicTier = requiredRelicTier; }
    }

    public static class GameDatacronAffixTemplateSet {
        private String id;
        private List<GameDatacronAffix> affix = List.of();
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public List<GameDatacronAffix> getAffix() { return affix == null ? List.of() : affix; }
        public void setAffix(List<GameDatacronAffix> affix) { this.affix = affix; }
    }

    public static class GameDatacronAffix {
        private List<String> tag = List.of();
        private String targetRule;
        private String abilityId;
        private Integer statType;
        private String statValue;
        private Integer requiredUnitTier;
        private Integer requiredRelicTier;
        private String scopeIcon;
        public List<String> getTag() { return tag == null ? List.of() : tag; }
        public void setTag(List<String> tag) { this.tag = tag; }
        public String getTargetRule() { return targetRule; }
        public void setTargetRule(String targetRule) { this.targetRule = targetRule; }
        public String getAbilityId() { return abilityId; }
        public void setAbilityId(String abilityId) { this.abilityId = abilityId; }
        public Integer getStatType() { return statType; }
        public void setStatType(Integer statType) { this.statType = statType; }
        public String getStatValue() { return statValue; }
        public void setStatValue(String statValue) { this.statValue = statValue; }
        public Integer getRequiredUnitTier() { return requiredUnitTier; }
        public void setRequiredUnitTier(Integer requiredUnitTier) { this.requiredUnitTier = requiredUnitTier; }
        public Integer getRequiredRelicTier() { return requiredRelicTier; }
        public void setRequiredRelicTier(Integer requiredRelicTier) { this.requiredRelicTier = requiredRelicTier; }
        public String getScopeIcon() { return scopeIcon; }
        public void setScopeIcon(String scopeIcon) { this.scopeIcon = scopeIcon; }
    }

    public static class GameAbility {
        private String id;
        private String nameKey;
        private String descKey;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNameKey() { return nameKey; }
        public void setNameKey(String nameKey) { this.nameKey = nameKey; }
        public String getDescKey() { return descKey; }
        public void setDescKey(String descKey) { this.descKey = descKey; }
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
