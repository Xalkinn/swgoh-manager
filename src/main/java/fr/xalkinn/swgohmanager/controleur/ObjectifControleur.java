package fr.xalkinn.swgohmanager.controleur;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.xalkinn.swgohmanager.modele.ComparatifObjectif;
import fr.xalkinn.swgohmanager.modele.ObjectifPersonnage;
import fr.xalkinn.swgohmanager.service.ObjectifService;

@Controller
public class ObjectifControleur {

    private final ObjectifService objectifService;


    public ObjectifControleur(ObjectifService objectifService) {
        this.objectifService = objectifService;
    }


    @GetMapping("/objectifs")
    public String afficherObjectifs(Model model) {

        // Liste déroulante des personnages
        model.addAttribute(
            "personnages",
            objectifService.getPersonnagesDisponibles()
        );

        // Objet utilisé par le formulaire
        model.addAttribute(
            "nouvelObjectif",
            new ObjectifPersonnage()
        );
        model.addAttribute(
        	    "objectifs",
        	    objectifService.getObjectifs()
        	);


        return "objectifs";
    }
    
    @PostMapping("/objectifs/supprimer/{id}")
    public String supprimerObjectif(@PathVariable Integer id) {

        objectifService.supprimerObjectif(id);

        return "redirect:/objectifs";
    }
    
    @PostMapping("/objectifs/ajouter")
    public String ajouterObjectif(
            @ModelAttribute ObjectifPersonnage objectif) {

        objectifService.ajouterObjectif(objectif);

        return "redirect:/objectifs";
    }
    
    @GetMapping("/objectifs/{nom}/comparatif")
    public String comparatif(@PathVariable String nom, Model model) {

        model.addAttribute(
            "comparatif",
            objectifService.getComparatif(nom)
        );

        return "comparatif";
    }
    
    
    
}