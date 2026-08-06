package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.xalkinn.swgohmanager.modele.BatchStatus;
import fr.xalkinn.swgohmanager.service.ModsStatusService;


@RestController
@RequestMapping("/api/mods")
public class ModsStatusControleur {


    private final ModsStatusService service;

    public ModsStatusControleur(ModsStatusService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public BatchStatus status() {

        return service.getStatus();

    }

}