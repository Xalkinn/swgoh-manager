package fr.xalkinn.swgohmanager.controleur;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.xalkinn.swgohmanager.dto.OmicronGestionView.DefinitionOption;
import fr.xalkinn.swgohmanager.service.BatchOmicronService;
import fr.xalkinn.swgohmanager.service.OmicronCatalogueService;
import fr.xalkinn.swgohmanager.service.OmicronExportService;
import fr.xalkinn.swgohmanager.service.OmicronPrioriteService;

@Controller
public class OmicronControleur {

    private final OmicronCatalogueService catalogueService;
    private final OmicronPrioriteService prioriteService;
    private final BatchOmicronService batchOmicronService;
    private final OmicronExportService exportService;

    public OmicronControleur(
            OmicronCatalogueService catalogueService,
            OmicronPrioriteService prioriteService,
            BatchOmicronService batchOmicronService,
            OmicronExportService exportService) {
        this.catalogueService = catalogueService;
        this.prioriteService = prioriteService;
        this.batchOmicronService = batchOmicronService;
        this.exportService = exportService;
    }

    @GetMapping("/omicrons")
    public String omicrons(Model model) {
        List<DefinitionOption> options = prioriteService.getOptionsGestion();
        model.addAttribute("omicronOptions", options);
        model.addAttribute("omicronCategories", prioriteService.getCategoriesDisponibles(options));
        model.addAttribute("omicronPriorites", prioriteService.getPriorites());
        model.addAttribute("catalogueCount", catalogueService.compterDefinitionsActives());
        model.addAttribute("catalogueGestionCount", options.size());
        model.addAttribute("catalogueVersion", catalogueService.getVersionCatalogue());
        model.addAttribute("prioritesCount", prioriteService.compterPriorites());
        return "omicrons";
    }

    @PostMapping("/omicrons/catalogue/actualiser")
    public String actualiserCatalogue(RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("omicronMessage", catalogueService.synchroniserCatalogue(true));
        } catch (Exception e) {
            redirect.addFlashAttribute("omicronErreur", "Catalogue non actualisé : " + message(e));
        }
        return "redirect:/omicrons";
    }

    @PostMapping("/omicrons/joueurs/actualiser")
    public String actualiserJoueurs(RedirectAttributes redirect) {
        batchOmicronService.lancerBatch();
        redirect.addFlashAttribute(
                "omicronMessage",
                "Synchronisation des Omicrons joueurs lancée. Le catalogue Game Data sera vérifié automatiquement.");
        return "redirect:/omicrons";
    }

    @PostMapping("/omicrons/priorites/ajouter")
    public String ajouterPriorite(
            @RequestParam("definitionId") long definitionId,
            @RequestParam("priorite") int priorite,
            RedirectAttributes redirect) {
        try {
            prioriteService.ajouter(definitionId, priorite);
            redirect.addFlashAttribute("omicronMessage", "Omicron ajouté au plan de priorité P" + priorite + ".");
        } catch (Exception e) {
            redirect.addFlashAttribute("omicronErreur", message(e));
        }
        return "redirect:/omicrons";
    }

    @PostMapping("/omicrons/priorites/{id}/modifier")
    public String modifierPriorite(
            @PathVariable("id") long id,
            @RequestParam("priorite") int priorite,
            RedirectAttributes redirect) {
        try {
            prioriteService.modifier(id, priorite);
            redirect.addFlashAttribute("omicronMessage", "Priorité mise à jour en P" + priorite + ".");
        } catch (Exception e) {
            redirect.addFlashAttribute("omicronErreur", message(e));
        }
        return "redirect:/omicrons";
    }

    @PostMapping("/omicrons/priorites/{id}/supprimer")
    public String supprimerPriorite(
            @PathVariable("id") long id,
            RedirectAttributes redirect) {
        prioriteService.supprimer(id);
        redirect.addFlashAttribute("omicronMessage", "Omicron retiré du plan de guilde.");
        return "redirect:/omicrons";
    }

    @GetMapping("/omicrons/export")
    public ResponseEntity<byte[]> exporterOmicrons(
            @RequestParam(name = "format", defaultValue = "excel") String format) throws Exception {
        OmicronExportService.ExportResult export = exportService.genererExport(format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + export.filename() + "\"")
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.content());
    }

    private String message(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName()
                : e.getMessage();
    }
}
