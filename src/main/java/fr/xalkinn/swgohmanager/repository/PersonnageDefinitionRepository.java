package fr.xalkinn.swgohmanager.repository;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import fr.xalkinn.swgohmanager.modele.PersonnageDefinition;

public interface PersonnageDefinitionRepository
        extends ListCrudRepository<PersonnageDefinition, String> {

    @Query("""
        SELECT base_id, nom_brut, nom_francais, cle_localisation,
               version_localisation, date_maj
        FROM personnage_definition
        WHERE nom_brut = :nomBrut
        LIMIT 1
        """)
    Optional<PersonnageDefinition> trouverParNomBrut(@Param("nomBrut") String nomBrut);
}
