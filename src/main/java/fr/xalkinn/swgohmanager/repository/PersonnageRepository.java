package fr.xalkinn.swgohmanager.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import fr.xalkinn.swgohmanager.modele.ComparatifObjectif;
import fr.xalkinn.swgohmanager.modele.EvolutionGuilde;
import fr.xalkinn.swgohmanager.modele.EvolutionPersonnage;
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
	    @Query("""
	    	    SELECT MAX(extraction_id)
	    	    FROM personnage
	    	    WHERE joueur_id = :joueurId
	    	""")
	    Integer trouverDerniereExtraction(Integer joueurId);
	    
	    @Query("""
	    	    SELECT MAX(extraction_id)
	    	    FROM personnage
	    	    WHERE joueur_id = :joueurId
	    	    AND extraction_id < :derniereExtraction
	    	""")
    	Integer trouverAncienneExtraction(
	        Integer joueurId,
	        Integer derniereExtraction
    	);
	    
	    @Query("""
	    		SELECT

	    		    ancien.nom AS nom,

	    		    ancien.gear AS ancien_gear,
	    		    nouveau.gear AS nouveau_gear,

	    		    ancien.relic AS ancienne_relique,
	    		    nouveau.relic AS nouvelle_relique,

	    		    ancien.etoiles AS anciennes_etoiles,
	    		    nouveau.etoiles AS nouvelles_etoiles

	    		FROM personnage ancien
	    		JOIN personnage nouveau

	    		ON ancien.joueur_id = nouveau.joueur_id
	    		AND ancien.nom = nouveau.nom

	    		WHERE ancien.joueur_id = :joueurId
	    		AND ancien.extraction_id = :ancienneExtraction
	    		AND nouveau.extraction_id = :nouvelleExtraction
	    		AND (
	    		    ancien.gear <> nouveau.gear
	    		    OR ancien.relic <> nouveau.relic
	    		    OR ancien.etoiles <> nouveau.etoiles
	    		)

	    		ORDER BY nouveau.nom
	    		""")
	    		List<EvolutionPersonnage> trouverEvolution(
	    		        Integer joueurId,
	    		        Integer ancienneExtraction,
	    		        Integer nouvelleExtraction
	    		);
	    @Query("""
	    		SELECT

	    		j.nom AS joueur,
	    		nouveau.nom AS personnage,
	    		ancien.gear AS ancien_gear,
	    		nouveau.gear AS nouveau_gear,
	    		ancien.relic AS ancienne_relique,
	    		nouveau.relic AS nouvelle_relique
	    		FROM 
	    		(
	    		    SELECT *
	    		    FROM personnage
	    		    WHERE extraction_id = (
	    		        SELECT MAX(extraction_id)-1
	    		        FROM personnage
	    		    )
	    		) ancien
	    		JOIN
	    		(
	    		    SELECT *
	    		    FROM personnage
	    		    WHERE extraction_id = (
	    		        SELECT MAX(extraction_id)
	    		        FROM personnage
	    		    )
	    		) nouveau
	    		ON ancien.joueur_id = nouveau.joueur_id
	    		AND ancien.nom = nouveau.nom
	    		JOIN joueur j
	    		ON j.id = nouveau.joueur_id
	    		WHERE 
	    		ancien.gear <> nouveau.gear
	    		OR ancien.relic <> nouveau.relic
	    		ORDER BY j.nom, nouveau.nom
	    		""")
	    		List<EvolutionGuilde> trouverDerniersUpsGuilde();
	    
	    @Query("""
	    		SELECT

	    		    ancien.nom AS nom,

	    		    ancien.gear AS ancien_gear,
	    		    nouveau.gear AS nouveau_gear,

	    		    ancien.relic AS ancienne_relique,
	    		    nouveau.relic AS nouvelle_relique,

	    		    ancien.etoiles AS anciennes_etoiles,
	    		    nouveau.etoiles AS nouvelles_etoiles

	    		FROM personnage ancien

	    		JOIN personnage nouveau

	    		ON ancien.joueur_id = nouveau.joueur_id
	    		AND ancien.nom = nouveau.nom

	    		WHERE ancien.joueur_id = :joueurId

	    		AND ancien.extraction_id = :ancienneExtraction

	    		AND nouveau.extraction_id = :nouvelleExtraction

	    		AND (
	    		    ancien.gear <> nouveau.gear
	    		    OR ancien.relic <> nouveau.relic
	    		    OR ancien.etoiles <> nouveau.etoiles
	    		)

	    		ORDER BY nouveau.nom
	    		""")
	    		List<EvolutionPersonnage> comparerExtraction(
	    		        Integer joueurId,
	    		        Integer ancienneExtraction,
	    		        Integer nouvelleExtraction
	    		);
}