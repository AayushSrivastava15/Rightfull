package com.rightfull.customer.dto;

import com.rightfull.customer.entity.Customer;
import com.rightfull.customer.entity.CustomerStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Customer profile returned by REST endpoints.
 */
public record CustomerResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String email,
        String company,
        String externalReference,
        Map<String, Object> metadata,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getWorkspaceId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCompany(),
                customer.getExternalReference(),
                customer.getMetadata(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getArchivedAt()
        );
    }
}

