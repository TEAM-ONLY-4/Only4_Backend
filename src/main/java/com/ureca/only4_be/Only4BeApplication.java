package com.ureca.only4_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Only4BeApplication {

	public static void main(String[] args) {
		SpringApplication.run(Only4BeApplication.class, args);
	}

}
