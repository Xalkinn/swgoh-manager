package fr.xalkinn.swgohmanager.controleur;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.xalkinn.swgohmanager.modele.EvolutionPersonnage;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;


@Controller
public class JoueurControleur {
    private final JoueurRepository joueurRepository;
    private final PersonnageRepository personnageRepository;
    
    public JoueurControleur(JoueurRepository joueurRepository, PersonnageRepository personnageRepository) {
        this.joueurRepository = joueurRepository;
		this.personnageRepository = personnageRepository;
    }
    
    @GetMapping("/joueurs")
    public String joueurs(
            @RequestParam(required = false) Integer joueurId,
            Model model
    ) {
        model.addAttribute(
            "joueurs",
            joueurRepository.findAll()
        );
        
        if(joueurId != null) {
        	
        	Integer derniere = personnageRepository.trouverDerniereExtraction(joueurId);

        	Integer ancienne = personnageRepository.trouverAncienneExtraction(
        	        joueurId,
        	        derniere
        	);
        	List<EvolutionPersonnage> evolutions =
        	        personnageRepository.trouverEvolution(
        	                joueurId,
        	                ancienne,
        	                derniere
        	        );
        	System.out.println("Nombre d'évolutions trouvées : " + evolutions.size());
        	model.addAttribute("evolutions", evolutions);


        	System.out.println("Dernière extraction : " + derniere);
        	System.out.println("Ancienne extraction : " + ancienne);
            System.out.println("Joueur sélectionné : " + joueurId);
        }
        return "joueurs";
    }
}