package com.nirkap.consumption;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConsumptionToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumptionToolApplication.class, args);
    }
}
