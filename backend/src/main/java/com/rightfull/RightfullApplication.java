package com.rightfull;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Rightful - SaaS Software Licensing Management Platform
 * Main Spring Boot Application Entry Point.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RightfullApplication {

    public static void main(String[] args) {
        SpringApplication.run(RightfullApplication.class, args);
    }
}


