package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.xalkinn.swgohmanager.modele.EtatSysteme;
import fr.xalkinn.swgohmanager.service.HealthService;

@RestController
@RequestMapping("/api/actualisation")
public class JoueurGuildeStatusControleur {

    private final HealthService service;

    public JoueurGuildeStatusControleur(
            HealthService service) {

        this.service = service;
    }


    @GetMapping("/status")
    public EtatSysteme status() {

        return service.getEtat();

    }
}