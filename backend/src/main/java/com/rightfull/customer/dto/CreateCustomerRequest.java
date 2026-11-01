package com.rightfull.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Request payload for registering a new customer.
 */
public record CreateCustomerRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid customer email address format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 255, message = "Company name must not exceed 255 characters")
        String company,

        @Size(max = 255, message = "External reference must not exceed 255 characters")
        String externalReference,

        Map<String, Object> metadata
) {}

