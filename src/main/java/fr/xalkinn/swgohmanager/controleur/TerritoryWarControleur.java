package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.xalkinn.swgohmanager.dto.TerritoryWarView.Page;
import fr.xalkinn.swgohmanager.service.TerritoryWarService;
import fr.xalkinn.swgohmanager.service.TerritoryWarStatsService;

@Controller
public class TerritoryWarControleur {

    private final TerritoryWarStatsService statsService;
    private final TerritoryWarService territoryWarService;

    public TerritoryWarControleur(
            TerritoryWarStatsService statsService,
            TerritoryWarService territoryWarService) {
        this.statsService = statsService;
        this.territoryWarService = territoryWarService;
    }

    @GetMapping("/tw")
    public String afficher(Model model) {
        Page page = statsService.getPage();
        model.addAttribute("twPage", page);
        return "tw";
    }

    @PostMapping("/tw/importer")
    public String importer(RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute(
                    "twMessage",
                    territoryWarService.synchroniserDepuisComlink());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "twErreur",
                    "Impossible d'importer les dernières TW : " + message(e));
        }
        return "redirect:/tw";
    }

    private String message(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
