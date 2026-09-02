package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.xalkinn.swgohmanager.client.SwgohGuildClient;
import fr.xalkinn.swgohmanager.config.SwgohProperties;
import fr.xalkinn.swgohmanager.dto.GuildeDetectee;
import fr.xalkinn.swgohmanager.service.GuildeService;

@Controller
public class SetupControleur {

    private final GuildeService guildeService;
    private final SwgohGuildClient guildClient;
    private final SwgohProperties properties;

    public SetupControleur(
            GuildeService guildeService,
            SwgohGuildClient guildClient,
            SwgohProperties properties) {

        this.guildeService = guildeService;
        this.guildClient = guildClient;
        this.properties = properties;
    }

    @GetMapping("/setup")
    public String setup(
            @RequestParam(name = "reconfigure", defaultValue = "false") boolean reconfigure,
            Model model) {

        if (guildeService.estConfiguree() && !reconfigure) {
            return "redirect:/";
        }

        remplirModeleCommun(model);
        return "setup";
    }

    @PostMapping("/setup")
    public String enregistrer(
            @RequestParam("allyCode") String allyCode,
            Model model) {

        try {
            GuildeDetectee guilde = guildClient.rechercherGuildeParAllyCode(allyCode);

            guildeService.enregistrerGuilde(
                    guilde.getGuildId(),
                    guilde.getNom());

            return "redirect:/?setup=success";

        } catch (Exception e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("allyCode", allyCode);
            remplirModeleCommun(model);
            return "setup";
        }
    }

    private void remplirModeleCommun(Model model) {
        model.addAttribute("comlinkUrl", properties.getComlink().getBaseUrl());
        model.addAttribute("comlinkOk", guildClient.testerConnexion());
        model.addAttribute("guildeActuelle", guildeService.getGuildeActive().orElse(null));
    }
}
