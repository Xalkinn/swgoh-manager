package fr.xalkinn.swgohmanager.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Petit lecteur de configuration utilisable également par les anciens DAO
 * qui ne sont pas gérés par Spring.
 *
 * Ordre de priorité :
 * 1. propriété JVM (-DDB_HOST=...)
 * 2. variable d'environnement
 * 3. fichier .env à la racine du projet / répertoire de lancement
 * 4. valeur par défaut
 */
public final class EnvironmentConfig {

    private static final Map<String, String> DOT_ENV = chargerDotEnv();

    private EnvironmentConfig() {
    }

    public static String get(String cle, String valeurParDefaut) {
        String valeur = System.getProperty(cle);

        if (estVide(valeur)) {
            valeur = System.getenv(cle);
        }

        if (estVide(valeur)) {
            valeur = DOT_ENV.get(cle);
        }

        return estVide(valeur) ? valeurParDefaut : valeur;
    }

    public static int getInt(String cle, int valeurParDefaut) {
        String valeur = get(cle, null);
        if (estVide(valeur)) {
            return valeurParDefaut;
        }

        try {
            return Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            return valeurParDefaut;
        }
    }

    private static Map<String, String> chargerDotEnv() {
        Path fichier = Path.of(".env");

        if (!Files.isRegularFile(fichier)) {
            return Collections.emptyMap();
        }

        Map<String, String> valeurs = new HashMap<>();

        try {
            List<String> lignes = Files.readAllLines(fichier);

            for (String ligne : lignes) {
                String propre = ligne.trim();

                if (propre.isEmpty() || propre.startsWith("#")) {
                    continue;
                }

                int egal = propre.indexOf('=');
                if (egal <= 0) {
                    continue;
                }

                String cle = propre.substring(0, egal).trim();
                String valeur = propre.substring(egal + 1).trim();

                if ((valeur.startsWith("\"") && valeur.endsWith("\""))
                        || (valeur.startsWith("'") && valeur.endsWith("'"))) {
                    valeur = valeur.substring(1, valeur.length() - 1);
                }

                valeurs.put(cle, valeur);
            }

        } catch (IOException e) {
            // Le fichier .env est optionnel : les variables système restent utilisables.
        }

        return Collections.unmodifiableMap(valeurs);
    }

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
