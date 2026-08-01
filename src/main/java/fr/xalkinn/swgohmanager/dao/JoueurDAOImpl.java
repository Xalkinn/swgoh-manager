package fr.xalkinn.swgohmanager.dao;

import java.sql.*;

import dao.DatabaseManager;

public class JoueurDAOImpl implements JoueurDAO {

    @Override
    public int compterJoueurs() {
        String sql = """
                SELECT COUNT(*)
                FROM joueur
                """;
        int total = 0;
        try(Connection con = DatabaseManager.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                total = rs.getInt(1);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

}