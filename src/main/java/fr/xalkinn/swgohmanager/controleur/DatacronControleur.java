package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

import fr.xalkinn.swgohmanager.service.DatacronService;
import fr.xalkinn.swgohmanager.service.DatacronCatalogueService;

@Controller
public class DatacronControleur {

    private final DatacronService datacronService;
    private final DatacronCatalogueService catalogueService;

    public DatacronControleur(
            DatacronService datacronService,
            DatacronCatalogueService catalogueService) {
        this.datacronService = datacronService;
        this.catalogueService = catalogueService;
    }

    @GetMapping("/datacrons")
    public String datacrons(
            @RequestParam(name = "joueurId", required = false) Integer joueurId,
            @RequestParam(name = "catalogue", required = false) String catalogue,
            Model model) {
        model.addAttribute("datacronPage", datacronService.construireVue(joueurId));
        model.addAttribute("catalogueDtc", catalogueService.getEtat());
        model.addAttribute("catalogueMessage", catalogue);
        return "datacrons";
    }

    @GetMapping("/datacrons/joueur/{joueurId}")
    public String detailJoueur(@PathVariable Integer joueurId, Model model) {
        model.addAttribute("datacronPage", datacronService.construireVue(joueurId));
        return "datacrons-detail";
    }

    @PostMapping("/datacrons/catalogue/actualiser")
    public String actualiserCatalogue() {
        try {
            catalogueService.actualiserCatalogue();
            return "redirect:/datacrons?catalogue=ok";
        } catch (Exception e) {
            return "redirect:/datacrons?catalogue=erreur";
        }
    }
}
