package com.boxoffice.delivery_manager_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.boxoffice.delivery_manager_service", "com.boxoffice.common"})
public class DeliveryManagerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryManagerServiceApplication.class, args);
	}

}
