package com.castlecsr;

import com.castlecsr.config.EnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CastlecsrBackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(CastlecsrBackendApplication.class);
		app.addListeners(new EnvConfig());
		app.run(args);
	}

}
