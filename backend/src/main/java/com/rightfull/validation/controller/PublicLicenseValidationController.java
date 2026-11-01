package com.rightfull.validation.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.validation.dto.*;
import com.rightfull.validation.service.LicenseValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public high-throughput runtime endpoints for software client license validation,
 * device activations, deactivations, and recurring heartbeat checks.
 * Does not require Supabase user authentication.
 */
@RestController
@RequestMapping("/api/v1/licenses")
@Tag(name = "License Validation API", description = "Public runtime endpoints for desktop, server, and client license verification and Ed25519 token minting")
public class PublicLicenseValidationController {

    private final LicenseValidationService validationService;

    public PublicLicenseValidationController(LicenseValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate")
    @Operation(
            summary = "Validate a software license",
            description = "Public endpoint for external software applications to check license validity and obtain a signed Ed25519 JWS token."
    )
    public ResponseEntity<ApiResponse<ValidateLicenseResponse>> validateLicense(
            @Valid @RequestBody ValidateLicenseRequest request) {
        ValidateLicenseResponse response = validationService.validate(request);
        HttpStatus status = response.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status)
                .body(ApiResponse.ok(response, response.message()));
    }

    @PostMapping("/activate")
    @Operation(
            summary = "Activate a client device node",
            description = "Registers and binds a client machine to an issued license under atomic concurrency control."
    )
    public ResponseEntity<ApiResponse<ActivateDeviceResponse>> activateDevice(
            @Valid @RequestBody ActivateDeviceRequest request) {
        ActivateDeviceResponse response = validationService.activateDevice(request);
        HttpStatus status = response.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status)
                .body(ApiResponse.ok(response, response.message()));
    }

    @PostMapping("/deactivate")
    @Operation(
            summary = "Deactivate a client device node",
            description = "Releases an activation slot from a client machine."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivateDevice(
            @Valid @RequestBody DeactivateDeviceRequest request) {
        boolean success = validationService.deactivateDevice(request);
        String message = success ? "Device deactivated successfully" : "Device was not active or not found";
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", success), message));
    }

    @PostMapping("/heartbeat")
    @Operation(
            summary = "Heartbeat & license refresh",
            description = "Periodically checks license status, updates client last-seen timestamp, and renews signed entitlement token."
    )
    public ResponseEntity<ApiResponse<ValidateLicenseResponse>> heartbeat(
            @Valid @RequestBody HeartbeatRequest request) {
        ValidateLicenseResponse response = validationService.heartbeat(request);
        HttpStatus status = response.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status)
                .body(ApiResponse.ok(response, response.message()));
    }
}

