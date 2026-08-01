package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import fr.xalkinn.swgohmanager.service.BatchService;

@Controller
public class BatchControleur {
    private final BatchService service;

    public BatchControleur(BatchService service) {
        this.service = service;
    }

    @GetMapping("/batch/omicron")
    public String lancerBatch() {
        service.lancerAnalyseOmicron();
        return "redirect:/";
    }
}