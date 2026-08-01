package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class JoueurControleur {


    @GetMapping("/joueurs")
    public String joueurs() {

        return "joueurs";

    }

}