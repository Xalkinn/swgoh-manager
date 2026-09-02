package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import fr.xalkinn.swgohmanager.service.LocalizationService;
import fr.xalkinn.swgohmanager.service.GuildeService;
import fr.xalkinn.swgohmanager.modele.GuildeConfiguration;

/**
 * Rend le service de localisation disponible dans tous les templates
 * Thymeleaf via la variable ${localization}.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final LocalizationService localizationService;
    private final GuildeService guildeService;

    public GlobalModelAdvice(
            LocalizationService localizationService,
            GuildeService guildeService) {
        this.localizationService = localizationService;
        this.guildeService = guildeService;
    }

    @ModelAttribute("localization")
    public LocalizationService localization() {
        return localizationService;
    }

    @ModelAttribute("guildeActive")
    public GuildeConfiguration guildeActive() {
        return guildeService.getGuildeActive().orElse(null);
    }
}
