package br.com.fiap.greenlify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenlifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(GreenlifyApplication.class, args);
	}

}
