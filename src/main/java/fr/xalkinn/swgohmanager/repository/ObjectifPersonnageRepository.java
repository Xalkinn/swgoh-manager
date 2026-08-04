package fr.xalkinn.swgohmanager.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;

import fr.xalkinn.swgohmanager.modele.ObjectifPersonnage;
import fr.xalkinn.swgohmanager.modele.Personnage;

public interface ObjectifPersonnageRepository extends CrudRepository<ObjectifPersonnage, Integer> {

    @Query("""
        SELECT DISTINCT nom
        FROM personnage
        ORDER BY nom
        """)
    List<String> trouverPersonnagesDisponibles();


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

}