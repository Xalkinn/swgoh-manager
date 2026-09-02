package fr.xalkinn.swgohmanager;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WebInterfaceSwgohApplication {

	public static void main(String[] args) {
        TimeZone.setDefault(
                TimeZone.getTimeZone(
                        fr.xalkinn.swgohmanager.config.EnvironmentConfig.get(
                                "APP_TIMEZONE", "Europe/Paris")));

        SpringApplication.run(WebInterfaceSwgohApplication.class, args);
    }

}
