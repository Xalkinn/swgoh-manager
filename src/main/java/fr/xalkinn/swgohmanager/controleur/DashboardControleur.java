package fr.xalkinn.swgohmanager.controleur;


import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.OmicronRepository;


@Controller
public class DashboardControleur {
    private final JoueurRepository joueurRepository;
    private final OmicronRepository omicronRepository;
    public DashboardControleur(
            JoueurRepository joueurRepository,
            OmicronRepository omicronRepository) {

        this.joueurRepository = joueurRepository;
        this.omicronRepository = omicronRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
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
        return "dashboard";
    }
}