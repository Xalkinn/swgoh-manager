package fr.xalkinn.swgohmanager;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebInterfaceSwgohApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebInterfaceSwgohApplication.class, args);
		TimeZone.setDefault(
	            TimeZone.getTimeZone("Europe/Paris")
	        );
	}

}
