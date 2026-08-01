package fr.xalkinn.swgohmanager.dao;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import fr.xalkinn.swgohmanager.modele.Omicron;

@Repository
public class OmicronDAOImpl implements OmicronDAO {

    private final JdbcTemplate jdbc;

    public OmicronDAOImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Omicron> findAll() {
        String sql = """
                SELECT 
                    id,
                    joueur_id,
                    nom_joueur,
                    personnage,
                    type_capacite,
                    actif
                FROM omicron
                ORDER BY nom_joueur
                """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Omicron o = new Omicron();
            o.setId(rs.getInt("id"));
            o.setJoueurId(rs.getString("joueur_id"));
            o.setNomJoueur(rs.getString("nom_joueur"));
            o.setPersonnage(rs.getString("personnage"));
            o.setTypeCapacite(rs.getString("type_capacite"));
            o.setActif(rs.getBoolean("actif"));
            return o;
        });
    }
}