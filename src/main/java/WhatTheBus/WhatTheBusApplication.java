package WhatTheBus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhatTheBusApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatTheBusApplication.class, args);
	}

}
