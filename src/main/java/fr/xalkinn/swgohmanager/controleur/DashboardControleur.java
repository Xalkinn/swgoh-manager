package fr.xalkinn.swgohmanager.controleur;


import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.OmicronRepository;
import fr.xalkinn.swgohmanager.service.HealthService;


@Controller
public class DashboardControleur {
	private final HealthService healthService;
    private final JoueurRepository joueurRepository;
    private final OmicronRepository omicronRepository;
    public DashboardControleur(
            JoueurRepository joueurRepository,
            OmicronRepository omicronRepository,
            HealthService healthService) {

		this.joueurRepository = joueurRepository;
        this.omicronRepository = omicronRepository;
        this.healthService = healthService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
    	System.out.println(
    		    healthService.getEtat()
    		);
        long nombreJoueurs = joueurRepository.count();
        long nombreOmicrons = omicronRepository.count();
        LocalDateTime derniereMaj = joueurRepository.derniereMiseAJour();
        model.addAttribute(
                "nombreJoueurs",
                nombreJoueurs
        );
        model.addAttribute(
                "nombreOmicrons",
                nombreOmicrons
        );
        model.addAttribute(
                "derniereMaj",
                derniereMaj
        );
        model.addAttribute(
                "etatSysteme",
                healthService.getEtat()
        );
        return "dashboard";
    }
}