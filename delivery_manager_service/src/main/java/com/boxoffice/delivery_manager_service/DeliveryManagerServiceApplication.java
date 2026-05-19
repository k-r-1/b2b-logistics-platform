package com.boxoffice.delivery_manager_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class DeliveryManagerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryManagerServiceApplication.class, args);
	}

}
