package fr.xalkinn.swgohmanager.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import fr.xalkinn.swgohmanager.service.GuildeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SetupRequiredInterceptor implements HandlerInterceptor {

    private final GuildeService guildeService;

    public SetupRequiredInterceptor(GuildeService guildeService) {
        this.guildeService = guildeService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        if (!guildeService.estConfiguree()) {
            response.sendRedirect(request.getContextPath() + "/setup");
            return false;
        }

        return true;
    }
}
