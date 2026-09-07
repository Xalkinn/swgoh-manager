package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.xalkinn.swgohmanager.modele.BatchStatus;
import fr.xalkinn.swgohmanager.service.BatchDatacronStatusService;

@RestController
@RequestMapping("/api/datacron")
public class BatchDatacronStatusControleur {

    private final BatchDatacronStatusService service;

    public BatchDatacronStatusControleur(BatchDatacronStatusService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public BatchStatus status() {
        return service.getStatus();
    }
}
