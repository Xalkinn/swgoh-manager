package fr.xalkinn.swgohmanager.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import fr.xalkinn.swgohmanager.modele.Extraction;


public interface ExtractionRepository 
        extends ListCrudRepository<Extraction, Integer> {


    List<Extraction> findByStatutOrderByDateFinDesc(String statut);
    
    @Query("""
            SELECT *
            FROM extraction
            ORDER BY id DESC
            """)
        List<Extraction> findAllOrderByDate();
    
    @Query("""
            SELECT DISTINCT e.*
            FROM extraction e
            JOIN personnage p
            ON p.extraction_id = e.id
            WHERE e.statut = 'TERMINE'
            ORDER BY e.date_fin DESC
        """)
        List<Extraction> findExtractionsDisponibles();
    
    @Query("""
    	    SELECT *
    	    FROM extraction
    	    WHERE statut = 'TERMINE'
    	    ORDER BY id DESC
    	    LIMIT 2
    	""")
    	List<Extraction> trouverDernieresExtractions();

}