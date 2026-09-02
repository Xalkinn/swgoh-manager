package fr.xalkinn.swgohmanager.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.client.SwgohLocalizationClient;

/**
 * Cache en mémoire des localisations françaises SWGOH.
 *
 * La BDD personnage conserve volontairement les identifiants bruts.
 * Les traductions sont mises en cache puis persistées une seule fois par
 * personnage dans la table personnage_definition.
 */
@Service
public class LocalizationService {

    private static final Logger log = LoggerFactory.getLogger(LocalizationService.class);
    private static final Duration DELAI_NOUVEL_ESSAI = Duration.ofMinutes(1);

    private final SwgohLocalizationClient swgohLocalizationClient;
    private final PersonnageDefinitionService personnageDefinitionService;
    private final Object verrou = new Object();

    private volatile Map<String, String> traductionsEnCache = Collections.emptyMap();
    private volatile boolean cacheCharge = false;
    private volatile String versionChargee;
    private volatile Instant prochainEssai = Instant.EPOCH;

    public LocalizationService(
            SwgohLocalizationClient swgohLocalizationClient,
            PersonnageDefinitionService personnageDefinitionService) {
        this.swgohLocalizationClient = swgohLocalizationClient;
        this.personnageDefinitionService = personnageDefinitionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prechargerAuDemarrage() {
        try {
            rafraichir();
        } catch (Exception e) {
            prochainEssai = Instant.now().plus(DELAI_NOUVEL_ESSAI);
            log.warn("Localisations françaises indisponibles au démarrage : {}", e.getMessage());
        }
    }

    public Map<String, String> getTraductions() {
        chargerSiNecessaire();
        return traductionsEnCache;
    }

    public String rafraichir() {
        synchronized (verrou) {
            String version = swgohLocalizationClient.getLatestLocalizationVersion();
            Map<String, String> traductions =
                    swgohLocalizationClient.getLocalizationFrancaise(version);

            traductionsEnCache = Map.copyOf(traductions);
            versionChargee = version;
            cacheCharge = true;
            prochainEssai = Instant.EPOCH;

            // Alimente le référentiel SQL une seule fois par personnage.
            personnageDefinitionService.synchroniserDepuisPersonnages(
                    traductionsEnCache, versionChargee);

            String resultat = String.format(
                    "%d traduction(s) française(s) chargée(s) (version %s)",
                    traductionsEnCache.size(), version);
            log.info(resultat);
            return resultat;
        }
    }

    public String traduire(String nameKey) {
        if (nameKey == null || nameKey.isBlank()) {
            return nameKey;
        }

        return getTraductions().getOrDefault(nameKey, nameKey);
    }

    /**
     * Traduit un identifiant de personnage brut lorsque seul celui-ci est
     * disponible (ex: AHSOKATANO ou AHSOKATANO:SEVEN_STAR).
     */
    public String traduirePersonnage(String identifiantOuNom) {
        return traduirePersonnage(identifiantOuNom, identifiantOuNom);
    }

    /**
     * Version privilégiée lorsque le baseId est disponible.
     * Le fallback est le nom brut actuellement présent en BDD.
     */
    public String traduirePersonnage(String baseId, String fallback) {
        if ((baseId == null || baseId.isBlank()) &&
                (fallback == null || fallback.isBlank())) {
            return fallback;
        }

        Map<String, String> traductions = getTraductions();

        String traduction = chercherTraductionPersonnage(traductions, baseId);
        if (traduction != null) {
            return traduction;
        }

        traduction = chercherTraductionPersonnage(traductions, fallback);
        if (traduction != null) {
            return traduction;
        }

        return fallback != null && !fallback.isBlank() ? fallback : baseId;
    }


    /**
     * Resynchronise personnage_definition à partir du cache déjà chargé.
     * Utile juste après un Batch Roster : les nouveaux personnages importés
     * sont alors ajoutés au référentiel sans retélécharger le bundle Comlink.
     */
    public int synchroniserDefinitionsDepuisCache() {
        chargerSiNecessaire();
        return personnageDefinitionService.synchroniserDepuisPersonnages(
                traductionsEnCache, versionChargee);
    }

    public String getVersionChargee() {
        return versionChargee;
    }

    public boolean isCacheCharge() {
        return cacheCharge;
    }

    private void chargerSiNecessaire() {
        if (cacheCharge || Instant.now().isBefore(prochainEssai)) {
            return;
        }

        synchronized (verrou) {
            if (cacheCharge || Instant.now().isBefore(prochainEssai)) {
                return;
            }

            try {
                rafraichir();
            } catch (Exception e) {
                prochainEssai = Instant.now().plus(DELAI_NOUVEL_ESSAI);
                log.warn(
                        "Impossible de charger les localisations françaises. " +
                        "Les noms bruts seront utilisés temporairement : {}",
                        e.getMessage());
            }
        }
    }

    private String chercherTraductionPersonnage(
            Map<String, String> traductions,
            String valeur) {

        if (valeur == null || valeur.isBlank() || traductions.isEmpty()) {
            return null;
        }

        String propre = valeur.trim();

        // Si le code reçu est déjà une vraie clé de localisation.
        if (propre.startsWith("UNIT_") && propre.endsWith("_NAME")) {
            String direct = traductions.get(propre);
            if (direct != null && !direct.isBlank()) {
                return direct;
            }
        }

        // Les definitionId de roster peuvent contenir un suffixe après ':'.
        int deuxPoints = propre.indexOf(':');
        if (deuxPoints > 0) {
            propre = propre.substring(0, deuxPoints);
        }

        // Certains anciens imports peuvent contenir une virgule finale.
        propre = propre.replace(",", "").trim();
        if (propre.isBlank()) {
            return null;
        }

        String cle = "UNIT_" + propre + "_NAME";
        String traduction = traductions.get(cle);
        if (traduction != null && !traduction.isBlank()) {
            return traduction;
        }

        // Sécurité pour les identifiants éventuellement stockés en minuscules.
        String cleMajuscule = "UNIT_" + propre.toUpperCase(Locale.ROOT) + "_NAME";
        traduction = traductions.get(cleMajuscule);
        return traduction == null || traduction.isBlank() ? null : traduction;
    }
}
