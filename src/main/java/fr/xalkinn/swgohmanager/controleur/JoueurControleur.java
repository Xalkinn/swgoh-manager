package fr.xalkinn.swgohmanager.controleur;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.xalkinn.swgohmanager.modele.EvolutionPersonnage;
import fr.xalkinn.swgohmanager.modele.Extraction;
import fr.xalkinn.swgohmanager.repository.ExtractionRepository;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;


@Controller
public class JoueurControleur {
    private final JoueurRepository joueurRepository;
    private final PersonnageRepository personnageRepository;
    private final ExtractionRepository extractionRepository;
    
    public JoueurControleur(JoueurRepository joueurRepository, PersonnageRepository personnageRepository, ExtractionRepository extractionRepository) {
        this.joueurRepository = joueurRepository;
		this.personnageRepository = personnageRepository;
		this.extractionRepository = extractionRepository;
    }
    
    @GetMapping("/joueurs")
    public String joueurs(
            @RequestParam(required = false) Integer joueurId, 
            @RequestParam(required = false) Integer ancienneExtraction,
            @RequestParam(required = false) Integer nouvelleExtraction,
            @RequestParam(required = false, defaultValue = "base") String onglet,
            Model model
    ) {
        model.addAttribute("joueurs", joueurRepository.trouverTousLesJoueursParNom());
        model.addAttribute("extractions", extractionRepository.findExtractionsDisponibles());
        model.addAttribute("onglet", onglet);
        model.addAttribute("joueurId", joueurId);
        model.addAttribute("ancienneExtraction", ancienneExtraction);
        model.addAttribute("nouvelleExtraction", nouvelleExtraction);
        System.out.println("Comparaison demandée");
        System.out.println("joueurId = " + joueurId);
        System.out.println("ancienne = " + ancienneExtraction);
        System.out.println("nouvelle = " + nouvelleExtraction);
        //extractionRepository.findAllOrderByDate().forEach(e -> 
        //    System.out.println(
        //        e.getId() 
        //        + " | " 
        //        + e.getDateFin()
        //        + " | "
        //        + e.getDateFinFormatee()));
        //System.out.println(extractionRepository.findAll());
        if (joueurId != null) {


            List<EvolutionPersonnage> evolutions;


            // ==========================
            // RECHERCHE AVANCEE
            // ==========================
            if (ancienneExtraction != null 
                    && nouvelleExtraction != null) {
                evolutions =
                        personnageRepository.comparerExtraction(
                                joueurId,
                                ancienneExtraction,
                                nouvelleExtraction
                        );
            }

            // ==========================
            // RECHERCHE CLASSIQUE
            // ==========================

            else {


                Integer derniereExtraction =
                        personnageRepository.trouverDerniereExtraction(joueurId);


                Integer ancienneExtractionAuto =
                        personnageRepository.trouverAncienneExtraction(
                                joueurId,
                                derniereExtraction
                        );


                evolutions =
                        personnageRepository.trouverEvolution(
                                joueurId,
                                ancienneExtractionAuto,
                                derniereExtraction
                        );

            }


            System.out.println(
                    "Nombre résultats : " 
                    + evolutions.size()
            );


            model.addAttribute(
                    "evolutions",
                    evolutions
            );
        }
        return "joueurs";
    }
}