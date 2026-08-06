package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.xalkinn.swgohmanager.service.BatchOmicronService;
import fr.xalkinn.swgohmanager.service.BatchRosterService;
import fr.xalkinn.swgohmanager.service.BatchStatusService;
import fr.xalkinn.swgohmanager.service.SynchronisationGuildeService;

@Controller
@RequestMapping("/actions")
public class ActionControleur {
	
	private final BatchOmicronService batchOmicronService;
	private final BatchRosterService batchRosterService;
	private final SynchronisationGuildeService synchronisationGuildeService;


    public ActionControleur(BatchOmicronService batchOmicronService, BatchRosterService batchRosterService,
    		SynchronisationGuildeService synchronisationGuildeService) {

        this.batchOmicronService = batchOmicronService;
		this.batchRosterService = batchRosterService;
		this.synchronisationGuildeService = synchronisationGuildeService;

    }

    @GetMapping("/batch-omicron")
    public String lancerBatchOmicron() {
        System.out.println("🚀 Lancement du batch Omicron");
        if(BatchStatusService.estEnCours()) {
            System.out.println("⚠ Batch déjà en cours");
            return "redirect:/";
        }
        batchOmicronService.lancerBatch();
        return "redirect:/";
    }
    
    @GetMapping("/batch-roster")
    public String lancerBatchRoster() {
    	System.out.println("🚀 Lancement mise à jour roster");
        batchRosterService.lancerBatch();
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
    
    @GetMapping("/synchronisation-guilde")
    public String synchroniserGuilde() {
    	System.out.println("🚀 Lancement mise à jour joueur");
        synchronisationGuildeService.lancerBatch();
        return "redirect:/";
    }
}