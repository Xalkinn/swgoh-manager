package fr.xalkinn.swgohmanager.controleur;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import fr.xalkinn.swgohmanager.client.SwgohGuildClient;
import fr.xalkinn.swgohmanager.config.EnvironmentConfig;
import fr.xalkinn.swgohmanager.config.SwgohProperties;
import fr.xalkinn.swgohmanager.service.GuildeService;
import fr.xalkinn.swgohmanager.service.HealthService;

@Controller
public class AdministrationControleur {

    private final GuildeService guildeService;
    private final SwgohGuildClient guildClient;
    private final SwgohProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final HealthService healthService;

    public AdministrationControleur(
            GuildeService guildeService,
            SwgohGuildClient guildClient,
            SwgohProperties properties,
            JdbcTemplate jdbcTemplate,
            HealthService healthService) {

        this.guildeService = guildeService;
        this.guildClient = guildClient;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.healthService = healthService;
    }

    @GetMapping("/admin")
    public String administration(Model model) {
        model.addAttribute("guilde", guildeService.getGuildeActiveOuErreur());
        model.addAttribute("etatSysteme", healthService.getEtat());
        model.addAttribute("comlinkUrl", properties.getComlink().getBaseUrl());

        model.addAttribute("dbHost", EnvironmentConfig.get("DB_HOST", "localhost"));
        model.addAttribute("dbPort", EnvironmentConfig.getInt("DB_PORT", 3306));
        model.addAttribute("dbName", EnvironmentConfig.get("DB_NAME", "swgoh"));
        model.addAttribute("dbUser", EnvironmentConfig.get("DB_USER", "swgoh"));

        model.addAttribute("compteurs", chargerCompteurs());

        return "administration";
    }

    @PostMapping("/admin/test-comlink")
    public String testerComlink() {
        return "redirect:/admin?comlink=" + (guildClient.testerConnexion() ? "ok" : "ko");
    }

    private Map<String, Long> chargerCompteurs() {
        Map<String, Long> compteurs = new LinkedHashMap<>();

        compteurs.put("Joueurs", compter("joueur"));
        compteurs.put("Personnages", compter("personnage"));
        compteurs.put("Mods", compter("mods"));
        compteurs.put("Omicrons", compter("omicron"));
        compteurs.put("Extractions", compter("extraction"));
        compteurs.put("Définitions personnages", compter("personnage_definition"));
        compteurs.put("Batailles de territoire", compter("territory_battle"));
        compteurs.put("Guerres de territoire", compter("territory_war"));

        return compteurs;
    }

    private long compter(String table) {
        try {
            Long valeur = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table,
                    Long.class);
            return valeur == null ? 0L : valeur;
        } catch (Exception e) {
            return 0L;
        }
    }
}
