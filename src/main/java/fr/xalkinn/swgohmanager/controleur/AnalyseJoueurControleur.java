package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.GetMapping;

public class AnalyseJoueurControleur {
	@GetMapping("/analyse")
    public String afficherAnalyse() {
    	System.out.println("Affichage d'objectif");
        return "analyse";
    }
}
