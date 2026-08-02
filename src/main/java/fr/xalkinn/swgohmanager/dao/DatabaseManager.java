package fr.xalkinn.swgohmanager.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    // Remplace par tes infos OVH
    private static final String HOST = "127.0.0.1"; // Nom d’hôte exact fourni par OVH
    private static final int PORT = 3333;                        // Port MySQL (par défaut 3306)
    private static final String DATABASE = "swgoh";      // Nom de ta base
    private static final String USER = "root";               // Identifiant MySQL
    private static final String PASSWORD = "root";   // Mot de passe MySQL

    // URL JDBC complète
    private static final String URL = String.format(
        "jdbc:mysql://%s:%d/%s?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );

    /**
     * Retourne une connexion MySQL à la base OVH.
     * @return Connection active
     * @throws SQLException si problème de connexion
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Charger le driver MySQL (optionnel pour JDBC 4+)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL non trouvé !");
            e.printStackTrace();
        }

        // Retourne la connexion
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Test rapide de connexion
     */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connexion réussie !");
            }
        } catch (SQLException e) {
            System.err.println("❌ Échec de la connexion !");
            e.printStackTrace();
        }
    }
}
