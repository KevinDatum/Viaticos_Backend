package com.viaticos.backend_viaticos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendViaticosApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendViaticosApplication.class, args);
	}

}
