package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.xalkinn.swgohmanager.modele.BatchStatus;
import fr.xalkinn.swgohmanager.service.BatchRosterStatusService;

@RestController
@RequestMapping("/api/roster")
public class BatchRosterStatusControleur {


    private final BatchRosterStatusService service;


    public BatchRosterStatusControleur(
            BatchRosterStatusService service) {

        this.service = service;
    }


    @GetMapping("/status")
    public BatchStatus status(){

        return service.getStatus();

    }
}