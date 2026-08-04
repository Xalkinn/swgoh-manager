package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.xalkinn.swgohmanager.modele.BatchStatus;
import fr.xalkinn.swgohmanager.service.BatchStatusService;

@RestController
public class BatchStatusControleur {

    private final BatchStatusService batchStatusService;

    public BatchStatusControleur(BatchStatusService batchStatusService) {
        this.batchStatusService = batchStatusService;
    }

    @GetMapping("/api/batch/status")
    public BatchStatus status() {
        return batchStatusService.getStatus();
    }

}