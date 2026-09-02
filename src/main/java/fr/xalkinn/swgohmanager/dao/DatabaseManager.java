package fr.xalkinn.swgohmanager.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import fr.xalkinn.swgohmanager.config.EnvironmentConfig;

/**
 * Connexion JDBC utilisée par les anciens DAO du projet.
 *
 * Les paramètres ne sont plus stockés dans le code : ils proviennent
 * de l'environnement ou du fichier .env.
 */
public class DatabaseManager {

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        String host = EnvironmentConfig.get("DB_HOST", "localhost");
        int port = EnvironmentConfig.getInt("DB_PORT", 3306);
        String database = EnvironmentConfig.get("DB_NAME", "swgoh");
        String user = EnvironmentConfig.get("DB_USER", "");
        String password = EnvironmentConfig.get("DB_PASSWORD", "");
        String timezone = EnvironmentConfig.get("APP_TIMEZONE", "Europe/Paris");

        String url = String.format(
                "jdbc:mysql://%s:%d/%s?serverTimezone=%s&useSSL=false&allowPublicKeyRetrieval=true",
                host, port, database, timezone);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé.", e);
        }

        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connexion MySQL réussie.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Échec de la connexion MySQL.");
            e.printStackTrace();
        }
    }
}
