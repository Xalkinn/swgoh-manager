package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.xalkinn.swgohmanager.service.BatchOmicronService;
import fr.xalkinn.swgohmanager.service.BatchStatusService;

@Controller
@RequestMapping("/actions")
public class ActionControleur {
	
	private final BatchOmicronService batchOmicronService;


    public ActionControleur(BatchOmicronService batchOmicronService) {

        this.batchOmicronService = batchOmicronService;

    }

    @GetMapping("/batch-omicron")
    public String lancerBatchOmicron() {
        System.out.println("🚀 Lancement du batch Omicron");
        batchOmicronService.lancerBatch();
        if(BatchStatusService.estEnCours()) {
            System.out.println(
                "⚠ Batch déjà en cours"
            );
            return "redirect:/";

        }
        batchOmicronService.lancerBatch();
        return "redirect:/";
    }
    @GetMapping("/export")
    public String exporterCSV() {
        System.out.println("📄 Export CSV demandé");
        // Appel CsvExport ici
        return "redirect:/";
    }

    @GetMapping("/refresh")
    public String refresh() {
        System.out.println("🔄 Rafraîchissement demandé");
        return "redirect:/";
    }
}