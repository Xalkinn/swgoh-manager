package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import fr.xalkinn.swgohmanager.service.OmicronService;

@Controller
public class OmicronControleur {
    private final OmicronService service;

    public OmicronControleur(OmicronService service) {
        this.service = service;
    }

    @GetMapping("/omicrons")
    public String omicrons(Model model) {
        model.addAttribute("omicrons",service.getOmicrons());
        return "omicrons";
    }
}