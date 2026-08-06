package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.xalkinn.swgohmanager.modele.BatchStatus;
import fr.xalkinn.swgohmanager.service.SynchronisationGuildeStatusService;


@RestController
@RequestMapping("/api/guilde")
public class SynchronisationGuildeStatusControleur {

    private final SynchronisationGuildeStatusService service;

    public SynchronisationGuildeStatusControleur(
            SynchronisationGuildeStatusService service) {

        this.service = service;
    }

    @GetMapping("/status")
    public BatchStatus status() {
        return service.getStatus();

    }
}