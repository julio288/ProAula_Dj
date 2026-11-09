package com.taller.registro_login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RegistroLoginApplication {

	public static void main(String[] args) {
		SpringApplication.run(RegistroLoginApplication.class, args);
	}

}
