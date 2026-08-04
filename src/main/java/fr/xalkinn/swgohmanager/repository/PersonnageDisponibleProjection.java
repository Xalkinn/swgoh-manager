package fr.xalkinn.swgohmanager.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;

public interface PersonnageDisponibleProjection {

    String getBaseId();

    String getNom();
    @Query("""
    		SELECT DISTINCT base_id AS baseId,
    		       nom
    		FROM personnage
    		ORDER BY nom
    		""")
    		List<PersonnageDisponibleProjection> trouverPersonnagesDisponibles();
}