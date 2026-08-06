package fr.xalkinn.swgohmanager.controleur;


import fr.xalkinn.swgohmanager.dao.ModQDAOImpl;
import fr.xalkinn.swgohmanager.modele.ModQ;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Controller
public class ModQControleur {
    private final ModQDAOImpl modQDAO = new ModQDAOImpl();

    @GetMapping("/modq")
    public String afficherModQ(Model model) {
        List<ModQ> modQ = modQDAO.findAll();
        System.out.println("Nombre de joueurs ModQ : " + modQ.size());
        model.addAttribute("modq", modQ);
        return "modq";
    }
}