package fr.xalkinn.swgohmanager.controleur;

import fr.xalkinn.swgohmanager.dao.ModQDAOImpl;
import fr.xalkinn.swgohmanager.modele.ModQ;
import fr.xalkinn.swgohmanager.service.MajModQService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ModQControleur {
	private final MajModQService modService;

	public ModQControleur(MajModQService modService) {
	    this.modService = modService;
	}
    private final ModQDAOImpl modQDAO = new ModQDAOImpl();

    @GetMapping("/modq")
    public String afficherModQ(
            @RequestParam(required = false, defaultValue = "modq") String onglet,
            Model model) {
        model.addAttribute("onglet", onglet);
        // Onglet ModQ
        if (!"stats".equals(onglet)) {
            List<ModQ> modQ = modQDAO.findAll();
            model.addAttribute("modq", modQ);
        }
        // Onglet Stats
        if ("stats".equals(onglet)) {
            // Pour l'instant vide
            // On ajoutera les statistiques ici
        }
        return "modq";
    }
    
    @GetMapping("/actions/maj-modq")
    public String majMods() {
        modService.lancerBatch();
        return "redirect:/modq";
    }
}