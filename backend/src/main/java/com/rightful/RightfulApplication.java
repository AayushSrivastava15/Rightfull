package com.rightful;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Rightful - SaaS Software Licensing Management Platform
 * Main Spring Boot Application Entry Point.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RightfulApplication {

    public static void main(String[] args) {
        SpringApplication.run(RightfulApplication.class, args);
    }
}
