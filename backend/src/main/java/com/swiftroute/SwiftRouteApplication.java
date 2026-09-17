package com.swiftroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwiftRouteApplication {

	public static void main(String[] args) {
		SpringApplication.run(SwiftRouteApplication.class, args);
	}

}
