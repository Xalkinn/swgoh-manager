package fr.xalkinn.swgohmanager.dao;

import fr.xalkinn.swgohmanager.modele.Mods;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModsDAOImpl {


    public List<Mods> findAll() {

        List<Mods> mods = new ArrayList<>();
        String sql = """
                SELECT *
                FROM mods
                ORDER BY vitesse DESC
                """;

        try (
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while(rs.next()) {
                Mods mod = new Mods();
                mod.setId(rs.getInt("id"));
                mod.setModId(rs.getString("mod_id"));
                mod.setJoueurId(rs.getInt("joueur_id"));
                mod.setPersonnage(rs.getString("personnage"));
                mod.setEmplacement(rs.getString("emplacement"));
                mod.setRarete(rs.getInt("rarete"));
                mod.setTypeMod( rs.getString("type_mod"));
                mod.setVitesse(rs.getInt("vitesse"));
                mods.add(mod);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return mods;
    }
}