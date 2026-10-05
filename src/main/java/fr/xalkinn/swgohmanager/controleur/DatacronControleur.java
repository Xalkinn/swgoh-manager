package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.xalkinn.swgohmanager.service.DatacronCatalogueService;
import fr.xalkinn.swgohmanager.service.DatacronPlanService;
import fr.xalkinn.swgohmanager.service.DatacronService;

@Controller
public class DatacronControleur {

    private final DatacronService datacronService;
    private final DatacronCatalogueService catalogueService;
    private final DatacronPlanService planService;

    public DatacronControleur(
            DatacronService datacronService,
            DatacronCatalogueService catalogueService,
            DatacronPlanService planService) {
        this.datacronService = datacronService;
        this.catalogueService = catalogueService;
        this.planService = planService;
    }

    @GetMapping("/datacrons")
    public String datacrons(
            @RequestParam(name = "joueurId", required = false) Integer joueurId,
            @RequestParam(name = "catalogue", required = false) String catalogue,
            @RequestParam(name = "inventorySet", required = false) Integer inventorySet,
            Model model) {
        model.addAttribute("datacronPage", datacronService.construireVue(joueurId));
        model.addAttribute("datacronInventory", datacronService.construireInventaireParSet(inventorySet));
        model.addAttribute("catalogueDtc", catalogueService.getEtat());
        model.addAttribute("catalogueMessage", catalogue);
        model.addAttribute("datacronPlanSets", planService.getSetsDisponibles());
        model.addAttribute("datacronPlanOptions", planService.getOptionsGestion());
        model.addAttribute("datacronPlan", planService.getPlan());
        model.addAttribute("datacronPlanCount", planService.compterPlan());
        return "datacrons";
    }

    @GetMapping("/datacrons/joueur/{joueurId}")
    public String detailJoueur(@PathVariable Integer joueurId, Model model) {
        model.addAttribute("datacronPage", datacronService.construireVue(joueurId));
        return "datacrons-detail";
    }

    @PostMapping("/datacrons/catalogue/actualiser")
    public String actualiserCatalogue(RedirectAttributes redirect) {
        try {
            String resultat = catalogueService.actualiserCatalogue();
            redirect.addFlashAttribute("datacronMessage", "Catalogue DTC actualisé : " + resultat);
        } catch (Exception e) {
            redirect.addFlashAttribute("datacronErreur", "Catalogue DTC non actualisé : " + message(e));
        }
        return "redirect:/datacrons";
    }

    @PostMapping("/datacrons/plan/ajouter")
    public String ajouterPlan(
            @RequestParam("setId") int setId,
            @RequestParam("t3OptionId") long t3OptionId,
            @RequestParam("t6OptionId") long t6OptionId,
            @RequestParam("t9OptionId") long t9OptionId,
            RedirectAttributes redirect) {
        try {
            planService.ajouter(setId, t3OptionId, t6OptionId, t9OptionId);
            redirect.addFlashAttribute("datacronMessage", "Recommandation DTC ajoutée au plan de guilde.");
        } catch (Exception e) {
            redirect.addFlashAttribute("datacronErreur", message(e));
        }
        return "redirect:/datacrons";
    }

    @PostMapping("/datacrons/plan/{id}/supprimer")
    public String supprimerPlan(
            @PathVariable("id") long id,
            RedirectAttributes redirect) {
        planService.supprimer(id);
        redirect.addFlashAttribute("datacronMessage", "Recommandation DTC retirée du plan de guilde.");
        return "redirect:/datacrons";
    }

    private String message(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName()
                : e.getMessage();
    }
}
