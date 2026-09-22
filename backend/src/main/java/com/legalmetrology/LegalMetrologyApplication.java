package com.legalmetrology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LegalMetrologyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalMetrologyApplication.class, args);
    }
}
