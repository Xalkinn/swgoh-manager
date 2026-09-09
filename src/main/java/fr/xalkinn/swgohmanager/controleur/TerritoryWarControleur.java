package fr.xalkinn.swgohmanager.controleur;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.xalkinn.swgohmanager.dto.TerritoryWarLiveDiagnosticView;
import fr.xalkinn.swgohmanager.dto.TerritoryWarView.Page;
import fr.xalkinn.swgohmanager.service.TerritoryWarLiveDiagnosticService;
import fr.xalkinn.swgohmanager.service.TerritoryWarService;
import fr.xalkinn.swgohmanager.service.TerritoryWarStatsService;

@Controller
public class TerritoryWarControleur {

    private final TerritoryWarStatsService statsService;
    private final TerritoryWarService territoryWarService;
    private final TerritoryWarLiveDiagnosticService liveDiagnosticService;

    public TerritoryWarControleur(
            TerritoryWarStatsService statsService,
            TerritoryWarService territoryWarService,
            TerritoryWarLiveDiagnosticService liveDiagnosticService) {
        this.statsService = statsService;
        this.territoryWarService = territoryWarService;
        this.liveDiagnosticService = liveDiagnosticService;
    }

    @GetMapping("/tw")
    public String afficher(Model model) {
        Page page = statsService.getPage();
        model.addAttribute("twPage", page);
        liveDiagnosticService.getDernier().ifPresent(snapshot -> model.addAttribute("twLiveDiagnostic", snapshot));
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

    @PostMapping("/tw/live/diagnostic")
    public String diagnosticLive(RedirectAttributes redirectAttributes) {
        try {
            TerritoryWarLiveDiagnosticView snapshot = liveDiagnosticService.capturerDepuisComlink();

            String message;
            if (snapshot.hasLiveData()) {
                message = "Diagnostic TW Live : " + snapshot.getStatusItemCount()
                        + " élément(s) territoryWarStatus détecté(s). Le JSON brut a été conservé pour l'analyse.";
            } else if (snapshot.isStatusKeyFound()) {
                message = "Diagnostic TW Live : la clé territoryWarStatus est présente mais vide. "
                        + "Le JSON brut a été conservé pour vérifier les autres structures disponibles.";
            } else {
                message = "Diagnostic TW Live : aucune clé territoryWarStatus n'est renvoyée par /guild. "
                        + "Le JSON brut a été conservé pour rechercher une autre structure exploitable.";
            }

            redirectAttributes.addFlashAttribute("twDiagnosticMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "twDiagnosticErreur",
                    "Impossible d'exécuter le diagnostic TW Live : " + message(e));
        }
        return "redirect:/tw";
    }

    @GetMapping(value = "/tw/live/diagnostic/{id}/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> jsonDiagnostic(@PathVariable long id) {
        var snapshot = liveDiagnosticService.getParId(id);
        if (snapshot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot.get().getRawJson());
    }

    private String message(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
