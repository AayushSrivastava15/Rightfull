package com.rightfull.customer.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.customer.dto.CreateCustomerRequest;
import com.rightfull.customer.dto.CustomerResponse;
import com.rightfull.customer.dto.UpdateCustomerRequest;
import com.rightfull.customer.entity.CustomerStatus;
import com.rightfull.customer.service.CustomerService;
import com.rightfull.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for workspace-scoped customer directory management.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Endpoints for managing workspace-scoped customers and client identity assignment")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Register customer",
            description = "Registers a new customer in the workspace. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @RequestParam UUID workspaceId,
            @Valid @RequestBody CreateCustomerRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        CustomerResponse response = customerService.createCustomer(workspaceId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Customer registered successfully"));
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List customers",
            description = "Returns a paginated list of customers in the authorized workspace with search query and status filtering. Accessible to all members (VIEWER+).",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> listCustomers(
            @RequestParam UUID workspaceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CustomerStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CustomerResponse> page = customerService.listCustomers(workspaceId, query, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Customers retrieved successfully"));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get customer details",
            description = "Retrieves full profile and metadata for a specific customer.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @RequestParam UUID workspaceId,
            @PathVariable UUID customerId) {
        CustomerResponse response = customerService.getCustomer(workspaceId, customerId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customer retrieved successfully"));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Update customer",
            description = "Updates customer name, company, external reference, or metadata. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @RequestParam UUID workspaceId,
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        CustomerResponse response = customerService.updateCustomer(workspaceId, customerId, request, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customer updated successfully"));
    }

    @PostMapping("/{customerId}/archive")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Archive customer",
            description = "Transitions a customer to ARCHIVED status. Hard delete is denied. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<CustomerResponse>> archiveCustomer(
            @RequestParam UUID workspaceId,
            @PathVariable UUID customerId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        CustomerResponse response = customerService.archiveCustomer(workspaceId, customerId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Customer archived successfully"));
    }
}

