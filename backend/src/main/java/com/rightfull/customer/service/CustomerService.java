package com.rightfull.customer.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.DuplicateResourceException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.customer.dto.CreateCustomerRequest;
import com.rightfull.customer.dto.CustomerResponse;
import com.rightfull.customer.dto.UpdateCustomerRequest;
import com.rightfull.customer.entity.Customer;
import com.rightfull.customer.entity.CustomerStatus;
import com.rightfull.customer.repository.CustomerRepository;
import com.rightfull.customer.validator.MetadataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service managing workspace-scoped customers and client identities.
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final MetadataValidator metadataValidator;
    private final WorkspaceAuditService auditService;

    public CustomerService(
            CustomerRepository customerRepository,
            MetadataValidator metadataValidator,
            WorkspaceAuditService auditService) {
        this.customerRepository = customerRepository;
        this.metadataValidator = metadataValidator;
        this.auditService = auditService;
    }

    @Transactional
    public CustomerResponse createCustomer(UUID workspaceId, CreateCustomerRequest request, UUID actorId) {
        String email = request.email().trim().toLowerCase();

        // 1. Enforce workspace-scoped email uniqueness
        if (customerRepository.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, email)) {
            throw new DuplicateResourceException("A customer with email '" + request.email() + "' already exists in this workspace");
        }

        // 2. Validate metadata
        metadataValidator.validate(request.metadata());

        // 3. Create customer entity
        Customer customer = new Customer(
                workspaceId,
                request.name().trim(),
                email,
                request.company() != null ? request.company().trim() : null,
                request.externalReference() != null ? request.externalReference().trim() : null,
                request.metadata()
        );

        Customer saved = customerRepository.save(customer);
        log.info("Registered customer id={} email='{}' in workspace id={}", saved.getId(), saved.getEmail(), workspaceId);

        // 4. Audit logging
        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.CUSTOMER_CREATED,
                workspaceId,
                actorId,
                Map.of(
                        "customerId", saved.getId().toString(),
                        "customerEmail", saved.getEmail(),
                        "customerName", saved.getName(),
                        "company", saved.getCompany() != null ? saved.getCompany() : ""
                )
        ));

        return CustomerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(UUID workspaceId, String query, CustomerStatus status, Pageable pageable) {
        Page<Customer> page;
        if (query != null && !query.isBlank()) {
            String trimmedQuery = query.trim();
            page = status != null
                    ? customerRepository.searchCustomersWithStatus(workspaceId, trimmedQuery, status, pageable)
                    : customerRepository.searchCustomers(workspaceId, trimmedQuery, pageable);
        } else {
            page = status != null
                    ? customerRepository.findByWorkspaceIdAndStatus(workspaceId, status, pageable)
                    : customerRepository.findByWorkspaceId(workspaceId, pageable);
        }
        return page.map(CustomerResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID workspaceId, UUID customerId) {
        Customer customer = customerRepository.findByIdAndWorkspaceId(customerId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID workspaceId, UUID customerId, UpdateCustomerRequest request, UUID actorId) {
        Customer customer = customerRepository.findByIdAndWorkspaceId(customerId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        if (customer.getStatus() == CustomerStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update an archived customer");
        }

        String email = request.email().trim().toLowerCase();

        // Enforce unique email if modified
        if (!customer.getEmail().equalsIgnoreCase(email) &&
                customerRepository.existsByWorkspaceIdAndEmailIgnoreCaseAndIdNot(workspaceId, email, customerId)) {
            throw new DuplicateResourceException("Another customer with email '" + request.email() + "' already exists in this workspace");
        }

        // Validate metadata
        metadataValidator.validate(request.metadata());

        customer.setName(request.name().trim());
        customer.setEmail(email);
        customer.setCompany(request.company() != null ? request.company().trim() : null);
        customer.setExternalReference(request.externalReference() != null ? request.externalReference().trim() : null);
        customer.setMetadata(request.metadata());

        Customer updated = customerRepository.save(customer);
        log.info("Updated customer id={}", updated.getId());

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.CUSTOMER_UPDATED,
                workspaceId,
                actorId,
                Map.of(
                        "customerId", updated.getId().toString(),
                        "customerEmail", updated.getEmail(),
                        "customerName", updated.getName()
                )
        ));

        return CustomerResponse.fromEntity(updated);
    }

    @Transactional
    public CustomerResponse archiveCustomer(UUID workspaceId, UUID customerId, UUID actorId) {
        Customer customer = customerRepository.findByIdAndWorkspaceId(customerId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        if (customer.getStatus() == CustomerStatus.ARCHIVED) {
            return CustomerResponse.fromEntity(customer);
        }

        customer.archive();
        Customer archived = customerRepository.save(customer);
        log.info("Archived customer id={}", archived.getId());

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.CUSTOMER_ARCHIVED,
                workspaceId,
                actorId,
                Map.of(
                        "customerId", archived.getId().toString(),
                        "customerEmail", archived.getEmail()
                )
        ));

        return CustomerResponse.fromEntity(archived);
    }
}

