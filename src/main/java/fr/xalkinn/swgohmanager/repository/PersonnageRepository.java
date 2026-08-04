package fr.xalkinn.swgohmanager.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import fr.xalkinn.swgohmanager.modele.ComparatifObjectif;
import fr.xalkinn.swgohmanager.modele.Personnage;

public interface PersonnageRepository extends CrudRepository<Personnage, Integer> {


    @Query("""
        SELECT relic
        FROM personnage
        WHERE joueur_id = :joueurId
        AND base_id = :baseId
        """)
    Integer trouverRelic(
        Integer joueurId,
        String baseId
    );


    @Query("""
    	    SELECT DISTINCT nom
    	    FROM personnage
    	    ORDER BY nom
    	""")
    	List<Personnage> trouverPersonnagesDisponibles();
    
    @Query("""
		    SELECT 
		    j.nom AS joueur,
		    p.relic AS relic_actuelle,
		    o.relic_cible AS relic_cible
		
		FROM objectif_personnage o
		
		JOIN personnage p
		ON TRIM(TRAILING ',' FROM o.nom) = p.nom
		
		JOIN joueur j
		ON j.id = p.joueur_id
		
		JOIN (
		    SELECT 
		        joueur_id,
		        nom,
		        MAX(id) AS dernier_id
		    FROM personnage
		    GROUP BY joueur_id, nom
		) dernier
		
		ON dernier.dernier_id = p.id
		
		WHERE o.nom = :nom
		
		AND p.relic < o.relic_cible
		
		ORDER BY p.relic ASC;
    	""")
    	List<ComparatifObjectif> getComparatifObjectif(
    	        @Param("nom") String nom
    	);

}