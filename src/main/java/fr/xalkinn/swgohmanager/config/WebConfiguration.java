package fr.xalkinn.swgohmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final SetupRequiredInterceptor setupRequiredInterceptor;

    public WebConfiguration(SetupRequiredInterceptor setupRequiredInterceptor) {
        this.setupRequiredInterceptor = setupRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(setupRequiredInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/setup",
                        "/setup/**",
                        "/css/**",
                        "/js/**",
                        "/image/**",
                        "/favicon.ico",
                        "/error");
    }
}
