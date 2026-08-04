package fr.xalkinn.swgohmanager.repository;


import java.time.LocalDateTime;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import fr.xalkinn.swgohmanager.modele.Omicron;


public interface OmicronRepository extends ListCrudRepository<Omicron, Integer> {
	 @Query("SELECT MAX(date_maj) FROM omicron")
	    LocalDateTime findDerniereMaj();

}