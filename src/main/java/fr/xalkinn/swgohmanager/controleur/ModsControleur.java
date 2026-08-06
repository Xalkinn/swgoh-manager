package fr.xalkinn.swgohmanager.controleur;


import fr.xalkinn.swgohmanager.dao.ModsDAOImpl;
import fr.xalkinn.swgohmanager.modele.Mods;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.List;

@Controller
public class ModsControleur {

    private final ModsDAOImpl modsDAO = new ModsDAOImpl();

    @GetMapping("/mods")
    public String afficherMods(Model model) {
        List<Mods> mods = modsDAO.findAll();
        model.addAttribute("mods",mods);
        System.out.println("Nombre de mods : " + mods.size());
        return "mods";
    }
}