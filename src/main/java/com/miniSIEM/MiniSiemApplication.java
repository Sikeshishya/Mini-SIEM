package com.miniSIEM;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MiniSiemApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniSiemApplication.class, args);
	}
}