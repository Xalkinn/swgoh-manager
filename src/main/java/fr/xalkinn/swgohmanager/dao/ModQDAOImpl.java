package fr.xalkinn.swgohmanager.dao;

import fr.xalkinn.swgohmanager.modele.ModQ;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ModQDAOImpl {


    public List<ModQ> findAll() {

        List<ModQ> liste = new ArrayList<>();


        String sql = """
                SELECT 
                    j.nom,
                    j.pg_perso,

                    SUM(CASE 
                        WHEN m.vitesse BETWEEN 15 AND 19 THEN 1 
                        ELSE 0 
                    END) AS nb15_19,

                    SUM(CASE 
                        WHEN m.vitesse BETWEEN 20 AND 24 THEN 1 
                        ELSE 0 
                    END) AS nb20_24,

                    SUM(CASE 
                        WHEN m.vitesse >= 25 THEN 1 
                        ELSE 0 
                    END) AS nb25

                FROM joueur j

                JOIN mods m
                ON m.joueur_id = j.id
                WHERE j.dans_guilde = 1

                GROUP BY j.id, j.nom, j.pg_perso

                ORDER BY j.nom
                """;

        try (
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while(rs.next()) {
                ModQ modQ = new ModQ();
                modQ.setJoueur(rs.getString("nom"));
                int nb15_19 = rs.getInt("nb15_19");
                int nb20_24 = rs.getInt("nb20_24");
                int nb25 = rs.getInt("nb25");
                long pgPerso = rs.getLong("pg_perso");
                modQ.setNb15_19(nb15_19);
                modQ.setNb20_24(nb20_24);
                modQ.setNb25(nb25);
                double calcul = 0;
                calcul = ((((0.8 * nb15_19) + nb20_24 + (1.2 * nb25)) / pgPerso) * 100000);
                modQ.setModQ(calcul);
                liste.add(modQ);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }
}