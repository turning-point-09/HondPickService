package com.example.handPick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HandPickApplication {

	public static void main(String[] args) {
		SpringApplication.run(HandPickApplication.class, args);
	}

}