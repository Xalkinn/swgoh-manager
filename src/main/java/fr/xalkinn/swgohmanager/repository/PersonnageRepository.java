package fr.xalkinn.swgohmanager.repository;

import org.springframework.data.repository.ListCrudRepository;

import fr.xalkinn.swgohmanager.modele.Personnage;

public interface PersonnageRepository extends ListCrudRepository<Personnage, Integer> {

}