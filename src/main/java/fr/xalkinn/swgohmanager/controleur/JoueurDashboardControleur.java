package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import fr.xalkinn.swgohmanager.dto.JoueurDashboard;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.service.JoueurDashboardService;

@Controller
public class JoueurDashboardControleur {

    private final JoueurDashboardService service;
    private final JoueurRepository joueurRepository;

    public JoueurDashboardControleur(
            JoueurDashboardService service,
            JoueurRepository joueurRepository) {
        this.service = service;
        this.joueurRepository = joueurRepository;
    }

    @GetMapping("/dashboard/joueur/{joueurId}")
    public String dashboardJoueur(
            @PathVariable Integer joueurId,
            Model model) {

        JoueurDashboard dashboard = service.getDashboard(joueurId);

        if (dashboard == null) {
            return "redirect:/";
        }

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("joueurs", joueurRepository.trouverTousLesJoueursParNom());
        return "dashboard-joueur";
    }
}
