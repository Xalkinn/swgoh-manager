package fr.xalkinn.swgohmanager.controleur;


import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.xalkinn.swgohmanager.modele.BatchStatus;
import fr.xalkinn.swgohmanager.modele.DashboardStats;
import fr.xalkinn.swgohmanager.modele.EtatSysteme;
import fr.xalkinn.swgohmanager.modele.ExtractionStatut;
import fr.xalkinn.swgohmanager.repository.ExtractionRepository;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.OmicronRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;
import fr.xalkinn.swgohmanager.service.ActualisationService;
import fr.xalkinn.swgohmanager.service.DashboardService;
import fr.xalkinn.swgohmanager.service.ExtractionService;
import fr.xalkinn.swgohmanager.service.HealthService;
import fr.xalkinn.swgohmanager.service.MiseAJourService;
import fr.xalkinn.swgohmanager.service.SynchronisationGuildeStatusService;


@Controller
public class DashboardControleur {
    private final DashboardService dashboardService;
    private final HealthService healthService;
	private final ExtractionService extractionService;
	private final ActualisationService actualisationService;
	private final PersonnageRepository personnageRepository;
	private final ExtractionRepository extractionRepository;
	private final MiseAJourService miseAJourService;


    public DashboardControleur(
            DashboardService dashboardService,
            HealthService healthService, ExtractionService extractionService,
            ActualisationService actualisationService, PersonnageRepository personnageRepository,
            ExtractionRepository extractionRepository, MiseAJourService miseAJourService) {

        this.dashboardService = dashboardService;
        this.healthService = healthService;
		this.extractionService = extractionService;
		this.actualisationService = actualisationService;
		this.personnageRepository = personnageRepository;
		this.extractionRepository = extractionRepository;
		this.miseAJourService = miseAJourService;

    }

    @GetMapping("/")
    public String dashboard(Model model) throws Exception {
        DashboardStats stats = dashboardService.getDashboardStats();
        EtatSysteme etatSysteme = healthService.getEtat();
        Map<String,Object> extraction = extractionService.getExtractionEnCours();
        if(extraction == null) {
            extraction = new HashMap<>();
            extraction.put("progression", 0);
        }

        model.addAttribute("extraction", extraction);
        model.addAttribute("stats", stats);
        model.addAttribute("etatSysteme", etatSysteme);
        model.addAttribute("extraction", extraction);
        model.addAttribute("actualisation", actualisationService.getActualisation());
        model.addAttribute("derniersUps", personnageRepository.trouverDerniersUpsGuilde());
        model.addAttribute("dernieresExtractions",extractionRepository.trouverDernieresExtractions());
        //model.addAttribute("derniersUps", null);
        return "dashboard";
    }
    
    @GetMapping("/api/extraction")
    @ResponseBody
    public Map<String, Object> extractionEnCours() throws Exception {
        return extractionService.getExtractionEnCours();
    }
        
//    @GetMapping("/actions/maj-mods")
//    public String majMods() throws Exception {
//        miseAJourService.miseAJourMods();
//        return "redirect:/";
//    }
//  Version 1.0
//	private final HealthService healthService;
//    private final JoueurRepository joueurRepository;
//    private final OmicronRepository omicronRepository;
//    public DashboardControleur(
//            JoueurRepository joueurRepository,
//            OmicronRepository omicronRepository,
//            HealthService healthService) {
//
//		this.joueurRepository = joueurRepository;
//        this.omicronRepository = omicronRepository;
//        this.healthService = healthService;
//    }
//
//    @GetMapping("/")
//    public String dashboard(Model model) {
//    	System.out.println(
//    		    healthService.getEtat()
//    		);
//        long nombreJoueurs = joueurRepository.count();
//        long nombreOmicrons = omicronRepository.count();
//        LocalDateTime derniereMaj = joueurRepository.derniereMiseAJour();
//        model.addAttribute(
//                "nombreJoueurs",
//                nombreJoueurs
//        );
//        model.addAttribute(
//                "nombreOmicrons",
//                nombreOmicrons
//        );
//        model.addAttribute(
//                "derniereMaj",
//                derniereMaj
//        );
//        model.addAttribute(
//                "etatSysteme",
//                healthService.getEtat()
//        );
//        return "dashboard";
//    }
}