package fr.xalkinn.swgohmanager.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.xalkinn.swgohmanager.dto.AnalyseJoueur;
import fr.xalkinn.swgohmanager.service.AnalyseJoueurService;

@Controller
public class AnalyseJoueurControleur {

    private final AnalyseJoueurService analyseJoueurService;

    public AnalyseJoueurControleur(AnalyseJoueurService analyseJoueurService) {
        this.analyseJoueurService = analyseJoueurService;
    }

    @GetMapping("/analyse")
    public String afficherAnalyse(Model model) {
        return "analyse";
    }

    @PostMapping("/analyse")
    public String analyser(
            @RequestParam("allyCode") String allyCode,
            Model model) {

        model.addAttribute("allyCode", allyCode);

        try {
            AnalyseJoueur analyse = analyseJoueurService.analyser(allyCode);
            model.addAttribute("analyse", analyse);
            model.addAttribute("allyCode", analyse.getAllyCode());
        } catch (Exception e) {
            model.addAttribute("erreur", messageErreur(e));
        }

        return "analyse";
    }

    private String messageErreur(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return "Impossible d'analyser ce joueur pour le moment.";
    }
}
