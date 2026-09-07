package fr.xalkinn.swgohmanager.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mise en forme lisible des données techniques Datacron.
 *
 * Les IDs restent stockés tels quels en BDD. Cette classe ne sert qu'à
 * l'affichage web : noms français des stats, pourcentages et catégories.
 */
public final class DatacronDisplayUtil {

    private static final Map<Integer, String> STATS = new LinkedHashMap<>();
    private static final Set<Integer> STATS_DECIMALES = Set.of(
            16, 17, 18, 23, 24, 25, 26, 27, 29, 30, 31, 32, 33, 34, 35, 36,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61);
    private static final Map<String, String> TAGS = new LinkedHashMap<>();

    static {
        STATS.put(1, "Santé");
        STATS.put(5, "Vitesse");
        STATS.put(6, "Dégâts physiques");
        STATS.put(7, "Dégâts spéciaux");
        STATS.put(8, "Armure");
        STATS.put(9, "Résistance");
        STATS.put(10, "Pénétration d'armure");
        STATS.put(11, "Pénétration de résistance");
        STATS.put(12, "Esquive");
        STATS.put(13, "Déviation");
        STATS.put(14, "Chance de critique physique");
        STATS.put(15, "Chance de critique spécial");
        STATS.put(16, "Dégâts critiques");
        STATS.put(17, "Puissance");
        STATS.put(18, "Ténacité");
        STATS.put(27, "Vol de santé");
        STATS.put(28, "Protection");
        STATS.put(41, "Attaque");
        STATS.put(42, "Défense");
        STATS.put(43, "Pénétration de défense");
        STATS.put(44, "Esquive");
        STATS.put(45, "Chance de critique");
        STATS.put(46, "Précision");
        STATS.put(47, "Évitement critique");
        STATS.put(48, "Attaque");
        STATS.put(49, "Défense");
        STATS.put(50, "Pénétration de défense");
        STATS.put(51, "Esquive");
        STATS.put(52, "Précision");
        STATS.put(53, "Chance de critique");
        STATS.put(54, "Évitement critique");
        STATS.put(55, "Santé");
        STATS.put(56, "Protection");
        STATS.put(57, "Vitesse");
        STATS.put(58, "Contre-attaque");
        STATS.put(60, "Pénétration de défense");
        STATS.put(61, "Maîtrise");

        TAGS.put("lightside", "Côté lumineux");
        TAGS.put("darkside", "Côté obscur");
        TAGS.put("attacker", "Attaquant");
        TAGS.put("tank", "Tank");
        TAGS.put("support", "Soutien");
        TAGS.put("healer", "Soigneur");
        TAGS.put("jedi", "Jedi");
        TAGS.put("sith", "Sith");
        TAGS.put("oldrepublic", "Ancienne République");
        TAGS.put("galacticrepublic", "République Galactique");
        TAGS.put("separatist", "Séparatiste");
        TAGS.put("empire", "Empire");
        TAGS.put("rebel", "Rebelle");
        TAGS.put("resistance", "Résistance");
        TAGS.put("firstorder", "Premier Ordre");
        TAGS.put("bountyhunter", "Chasseur de primes");
        TAGS.put("nightsister", "Sœur de la Nuit");
        TAGS.put("mandalorian", "Mandalorien");
        TAGS.put("tusken", "Tusken");
        TAGS.put("jawa", "Jawa");
        TAGS.put("ewok", "Ewok");
        TAGS.put("phoenix", "Phoenix");
        TAGS.put("inquisitorius", "Inquisiteur");
        TAGS.put("imperialtrooper", "Trooper impérial");
        TAGS.put("scoundrel", "Vaurien");
        TAGS.put("droid", "Droïde");
        TAGS.put("clone", "Clone");
        TAGS.put("unalignedforceuser", "Utilisateur de la Force non aligné");
    }

    private DatacronDisplayUtil() {
    }

    public static String nomStat(Integer statType) {
        if (statType == null) return "Statistique";
        return STATS.getOrDefault(statType, "Statistique " + statType);
    }

    /**
     * Les valeurs décimales des UnitStats Game Data sont stockées à l'échelle
     * 1e8 : 30 000 000 correspond à 30 %. Pour l'UI, on restitue directement
     * le pourcentage compréhensible par le joueur.
     */
    public static String valeurStat(Integer statType, String valeurBrute) {
        if (valeurBrute == null || valeurBrute.isBlank()) return "—";
        try {
            BigDecimal valeur = new BigDecimal(valeurBrute.trim());
            if (statType != null && STATS_DECIMALES.contains(statType)) {
                BigDecimal pourcentage = valeur.divide(new BigDecimal("1000000"), 2, RoundingMode.HALF_UP);
                return formatNombre(pourcentage) + " %";
            }

            // Les éventuelles stats plates sont généralement encodées à 1e8.
            // Si la valeur est clairement à cette échelle, on la restitue.
            if (valeur.abs().compareTo(new BigDecimal("100000000")) >= 0) {
                BigDecimal plate = valeur.divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP);
                return formatNombre(plate);
            }
            return formatNombre(valeur);
        } catch (NumberFormatException e) {
            return valeurBrute;
        }
    }

    public static String tagsLisibles(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) return null;
        LinkedHashSet<String> libelles = new LinkedHashSet<>();
        for (String brut : tagsCsv.split(",")) {
            String tag = brut == null ? "" : brut.trim();
            if (tag.isBlank()) continue;
            libelles.add(TAGS.getOrDefault(tag.toLowerCase(Locale.ROOT), humaniser(tag)));
        }
        return libelles.isEmpty() ? null : String.join(" · ", libelles);
    }

    public static String cibleLisible(String targetRule, String tagsCsv) {
        if (targetRule != null && !targetRule.isBlank()) {
            String suffixe = extraireCibleTechnique(targetRule);
            String libelleConnu = TAGS.get(suffixe);
            if (libelleConnu != null) return libelleConnu;

            // Pour une mécanique de personnage, le service peut ensuite
            // remplacer ce fallback par le nom officiel issu de FRE_FR.
            String cibleTechnique = humaniser(suffixe);
            if (cibleTechnique != null && !cibleTechnique.isBlank()) return cibleTechnique;
        }

        return tagsLisibles(tagsCsv);
    }

    public static String identifiantCible(String targetRule) {
        if (targetRule == null || targetRule.isBlank()) return null;
        String suffixe = extraireCibleTechnique(targetRule);
        return suffixe == null || suffixe.isBlank() ? null : suffixe;
    }

    private static String extraireCibleTechnique(String targetRule) {
        String suffixe = targetRule.trim().toLowerCase(Locale.ROOT);
        if (suffixe.startsWith("target_datacron_")) {
            suffixe = suffixe.substring("target_datacron_".length());
        }
        return suffixe;
    }

    /**
     * Remplace les paramètres de localisation SWGOH par la cible lisible du
     * Datacron. Exemple : "Bonus mécanique : {0}" devient
     * "Bonus mécanique : Côté obscur".
     */
    public static String remplacerCible(String texte, String cibleAffichee) {
        if (texte == null || texte.isBlank()) return texte;
        if (!texte.contains("{0}")) return texte;

        String cible = cibleAffichee == null || cibleAffichee.isBlank()
                ? "cible concernée"
                : cibleAffichee;
        return texte.replace("{0}", cible);
    }

    public static String titreMecanique(String titreTraduit) {
        if (titreTraduit == null || titreTraduit.isBlank()) return "Mécanique Datacron";
        // Ne jamais remettre une clé technique en gros dans l'interface.
        String min = titreTraduit.toLowerCase(Locale.ROOT);
        if (min.startsWith("datacron_") || min.contains("_name") || min.contains("_desc")) {
            return "Mécanique Datacron";
        }
        return titreTraduit;
    }

    public static String nettoyerDescription(String description) {
        if (description == null || description.isBlank()) return null;
        String propre = description
                .replace("\\n", " ")
                .replace('\n', ' ')
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)</?color(?:=[^>]*)?>", "")
                .replaceAll("(?i)</?b>", "")
                .replaceAll("(?i)</?i>", "")
                .replaceAll("\\[/?c(?:=[^\\]]*)?\\]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return propre.isBlank() ? null : propre;
    }

    private static String formatNombre(BigDecimal valeur) {
        BigDecimal propre = valeur.stripTrailingZeros();
        if (propre.scale() < 0) propre = propre.setScale(0);
        return propre.toPlainString().replace('.', ',');
    }

    private static String humaniser(String valeur) {
        if (valeur == null || valeur.isBlank()) return null;
        String propre = valeur.replace('_', ' ').replace('-', ' ').trim();
        if (propre.isBlank()) return null;

        List<String> mots = new ArrayList<>();
        for (String mot : propre.split("\\s+")) {
            if (mot.isBlank()) continue;
            if (mot.length() == 1) mots.add(mot.toUpperCase(Locale.ROOT));
            else mots.add(Character.toUpperCase(mot.charAt(0)) + mot.substring(1));
        }
        return String.join(" ", mots);
    }
}
