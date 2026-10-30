package com.rightful.common.controller;

import com.rightful.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health & System", description = "Public system observability and runtime status endpoints")
public class HealthController {

    @Value("${rightful.licensing.default-grace-period-hours:7}")
    private int defaultGracePeriodHours;

    @Value("${rightful.licensing.default-validation-interval-minutes:5}")
    private int defaultValidationIntervalMinutes;

    @GetMapping
    @Operation(summary = "Check application runtime health and platform version")
    public ApiResponse<Map<String, Object>> getHealth() {
        Map<String, Object> healthData = Map.of(
                "status", "UP",
                "product", "Rightful",
                "version", "0.0.1",
                "javaVersion", System.getProperty("java.version"),
                "springBootVersion", SpringBootVersion.getVersion(),
                "defaultOfflineGracePeriodHours", defaultGracePeriodHours,
                "defaultValidationIntervalMinutes", defaultValidationIntervalMinutes,
                "timestamp", Instant.now().toString()
        );

        return ApiResponse.ok(healthData, "Rightful platform backend is operational");
    }
}
