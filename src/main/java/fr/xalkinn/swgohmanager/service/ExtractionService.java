package fr.xalkinn.swgohmanager.service;


import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import dao.DatabaseManager;
import fr.xalkinn.swgohmanager.modele.ExtractionStatut;


@Service
public class ExtractionService {


    public int debutExtraction() throws Exception {
        try(Connection conn = DatabaseManager.getConnection()) {
            String sql =
                "INSERT INTO extraction(date_debut) VALUES (NOW())";
            PreparedStatement ps =
                conn.prepareStatement(
                    sql,
                    PreparedStatement.RETURN_GENERATED_KEYS
                );
            ps.executeUpdate();
            ResultSet rs =
                ps.getGeneratedKeys();
            if(rs.next()) {
                return rs.getInt(1);
            }
            throw new Exception(
                "Impossible de récupérer extraction_id"
            );
        }
    }

    public void finExtraction(int id) throws Exception {

        try(Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement ps =
                conn.prepareStatement(
                    "UPDATE extraction SET date_fin=NOW(), progression=100, statut = 'TERMINE' WHERE id=?"
                );

            ps.setInt(1,id);

            ps.executeUpdate();
        }
    }
    public void majProgression(int id, int progression) throws Exception {

        try(Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement ps =
                conn.prepareStatement(
                    "UPDATE extraction SET progression=? WHERE id=?"
                );

            ps.setInt(1, progression);
            ps.setInt(2, id);

            ps.executeUpdate();
        }
    }
    public Map<String, Object> getExtractionEnCours() throws Exception {

        try(Connection conn = DatabaseManager.getConnection()) {

            String sql = """
                SELECT id, date_debut, date_fin, progression
                FROM extraction
                WHERE date_fin IS NULL
                ORDER BY id DESC
                LIMIT 1
            """;

            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {

                Map<String, Object> extraction = new HashMap<>();

                extraction.put("id", rs.getInt("id"));
                extraction.put("date_debut", rs.getTimestamp("date_debut"));
                extraction.put("date_fin", rs.getTimestamp("date_fin"));
                extraction.put("progression", rs.getInt("progression"));

                return extraction;
            }

            return null;
        }
    }
}