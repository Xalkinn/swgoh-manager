package fr.xalkinn.swgohmanager.controleur;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.Detail;
import fr.xalkinn.swgohmanager.dto.TerritoryBattleView.SummaryRow;
import fr.xalkinn.swgohmanager.service.TerritoryBattleService;
import fr.xalkinn.swgohmanager.service.TerritoryBattleStatsService;

@Controller
public class TerritoryBattleControleur {

    private final TerritoryBattleStatsService statsService;
    private final TerritoryBattleService territoryBattleService;

    public TerritoryBattleControleur(
            TerritoryBattleStatsService statsService,
            TerritoryBattleService territoryBattleService) {
        this.statsService = statsService;
        this.territoryBattleService = territoryBattleService;
    }

    @GetMapping("/tb")
    public String afficher(
            @RequestParam(name = "tbId", required = false) Long tbId,
            Model model) {

        List<SummaryRow> historique = statsService.getHistoriqueComplet();
        Detail detail = statsService.getDetail(tbId);

        model.addAttribute("tbHistorique", historique);
        model.addAttribute("tbDetail", detail);
        model.addAttribute("tbSelectionneeId", detail != null ? detail.getTb().getId() : null);
        return "tb";
    }

    @PostMapping("/tb/importer")
    public String importer(RedirectAttributes redirectAttributes) {
        try {
            String resultat = territoryBattleService.synchroniserDepuisComlink();
            redirectAttributes.addFlashAttribute("tbMessage", resultat);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "tbErreur",
                    "Impossible d'importer la dernière TB : " + message(e));
        }
        return "redirect:/tb";
    }

    private String message(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
