package com.programacion2.catalogandsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogandsyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogandsyncApplication.class, args);
	}

}
