package fr.xalkinn.swgohmanager.repository;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import fr.xalkinn.swgohmanager.modele.Joueur;

public interface JoueurRepository extends ListCrudRepository<Joueur, Integer> {
	@Query("""
            SELECT MAX(date_maj)
            FROM omicron
            """)
    LocalDateTime derniereMiseAJour();
	
	@Query("SELECT MAX(date_capture) FROM personnage")
	LocalDateTime findDerniereMaj();
	
	@Query("""
		    SELECT *
		    FROM joueur
		    WHERE dans_guilde = 1
		    ORDER BY nom
		""")
		List<Joueur> trouverTousLesJoueursParNom();
}