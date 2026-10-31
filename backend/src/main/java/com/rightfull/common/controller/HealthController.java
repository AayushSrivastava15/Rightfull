package com.rightfull.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health & System", description = "Public system observability and runtime status endpoints")
public class HealthController {

    @Value("${spring.application.name:rightfull-backend}")
    private String applicationName;

    @GetMapping
    @Operation(summary = "Check application runtime health status")
    public Map<String, String> getHealth() {
        return Map.of(
                "status", "UP",
                "application", applicationName
        );
    }
}
