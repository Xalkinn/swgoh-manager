package fr.xalkinn.swgohmanager.controleur;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import fr.xalkinn.swgohmanager.service.LocalizationService;

/**
 * Rend le service de localisation disponible dans tous les templates
 * Thymeleaf via la variable ${localization}.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final LocalizationService localizationService;

    public GlobalModelAdvice(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @ModelAttribute("localization")
    public LocalizationService localization() {
        return localizationService;
    }
}
