package com.rightfull.customer.service;

import com.rightfull.audit.WorkspaceAuditEvent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Service Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private MetadataValidator metadataValidator;
    private CustomerService customerService;

    private UUID workspaceId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        metadataValidator = new MetadataValidator();
        customerService = new CustomerService(customerRepository, metadataValidator, auditService);

        workspaceId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create customer registers customer and emits audit event")
    void testCreateCustomerSuccess() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Acme Corp",
                "billing@acme.com",
                "Acme Inc",
                "EXT-1001",
                Map.of("region", "US")
        );

        when(customerRepository.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, "billing@acme.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CustomerResponse response = customerService.createCustomer(workspaceId, request, actorId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Acme Corp");
        assertThat(response.email()).isEqualTo("billing@acme.com");
        assertThat(response.company()).isEqualTo("Acme Inc");
        assertThat(response.externalReference()).isEqualTo("EXT-1001");
        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);

        verify(auditService, times(1)).recordEvent(any(WorkspaceAuditEvent.class));
    }

    @Test
    @DisplayName("Create customer fails on duplicate email in same workspace")
    void testCreateCustomerDuplicateEmailFails() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Acme Corp",
                "duplicate@acme.com",
                "Acme Inc",
                null,
                null
        );

        when(customerRepository.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, "duplicate@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(workspaceId, request, actorId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists in this workspace");
    }

    @Test
    @DisplayName("List customers with search query filters results")
    void testListCustomersWithSearch() {
        Customer c1 = new Customer(workspaceId, "John Doe", "john@example.com", "Example Co", null, null);
        Page<Customer> page = new PageImpl<>(List.of(c1), PageRequest.of(0, 10), 1);

        when(customerRepository.searchCustomers(workspaceId, "john", PageRequest.of(0, 10))).thenReturn(page);

        Page<CustomerResponse> result = customerService.listCustomers(workspaceId, "john", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Update customer saves changes and emits audit event")
    void testUpdateCustomerSuccess() {
        Customer customer = new Customer(workspaceId, "Old Name", "old@acme.com", "Old Co", "REF-1", null);
        UUID customerId = UUID.randomUUID();
        customer.setId(customerId);

        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "New Name",
                "new@acme.com",
                "New Co",
                "REF-2",
                Map.of("updated", true)
        );

        when(customerRepository.findByIdAndWorkspaceId(customerId, workspaceId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByWorkspaceIdAndEmailIgnoreCaseAndIdNot(workspaceId, "new@acme.com", customerId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.updateCustomer(workspaceId, customerId, updateRequest, actorId);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("new@acme.com");
        assertThat(response.company()).isEqualTo("New Co");

        verify(auditService, times(1)).recordEvent(any(WorkspaceAuditEvent.class));
    }

    @Test
    @DisplayName("Archive customer transitions status to ARCHIVED")
    void testArchiveCustomer() {
        Customer customer = new Customer(workspaceId, "Acme", "acme@acme.com", null, null, null);
        UUID customerId = UUID.randomUUID();
        customer.setId(customerId);

        when(customerRepository.findByIdAndWorkspaceId(customerId, workspaceId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.archiveCustomer(workspaceId, customerId, actorId);

        assertThat(response.status()).isEqualTo(CustomerStatus.ARCHIVED);
        assertThat(response.archivedAt()).isNotNull();

        verify(auditService, times(1)).recordEvent(any(WorkspaceAuditEvent.class));
    }
}

