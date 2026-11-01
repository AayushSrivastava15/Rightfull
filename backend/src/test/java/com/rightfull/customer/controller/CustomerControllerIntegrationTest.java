package com.rightfull.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.customer.dto.CreateCustomerRequest;
import com.rightfull.customer.dto.CustomerResponse;
import com.rightfull.customer.entity.CustomerStatus;
import com.rightfull.customer.service.CustomerService;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.security.WorkspaceSecurityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Customer REST API Integration Tests")
class CustomerControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private WorkspaceSecurityEvaluator workspaceSecurityEvaluator;

    private UUID workspaceId;
    private UUID customerId;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        workspaceId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        jwtToken = "mock.jwt.token";

        Jwt mockJwt = Jwt.withTokenValue(jwtToken)
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .claim("email", "owner@company.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtDecoder.decode(any())).thenReturn(mockJwt);
    }

    @Test
    @DisplayName("POST /api/v1/customers with ADMIN role returns 201 Created")
    void testCreateCustomerSuccess() throws Exception {
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        CreateCustomerRequest request = new CreateCustomerRequest(
                "Globex Corp", "billing@globex.com", "Globex", "GLX-101", Map.of()
        );

        CustomerResponse response = new CustomerResponse(
                customerId, workspaceId, "Globex Corp", "billing@globex.com",
                "Globex", "GLX-101", Map.of(), CustomerStatus.ACTIVE,
                Instant.now(), Instant.now(), null
        );

        when(customerService.createCustomer(eq(workspaceId), any(CreateCustomerRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .param("workspaceId", workspaceId.toString())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Globex Corp")))
                .andExpect(jsonPath("$.data.email", is("billing@globex.com")));
    }

    @Test
    @DisplayName("GET /api/v1/customers with VIEWER role returns 200 OK")
    void testListCustomersSuccess() throws Exception {
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(true);

        CustomerResponse response = new CustomerResponse(
                customerId, workspaceId, "Globex Corp", "billing@globex.com",
                "Globex", "GLX-101", Map.of(), CustomerStatus.ACTIVE,
                Instant.now(), Instant.now(), null
        );

        when(customerService.listCustomers(eq(workspaceId), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/customers")
                        .param("workspaceId", workspaceId.toString())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].email", is("billing@globex.com")));
    }

    @Test
    @DisplayName("POST /api/v1/customers with VIEWER role returns 403 Forbidden")
    void testCreateCustomerForbidden() throws Exception {
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(false);

        CreateCustomerRequest request = new CreateCustomerRequest(
                "Globex Corp", "billing@globex.com", "Globex", "GLX-101", Map.of()
        );

        mockMvc.perform(post("/api/v1/customers")
                        .param("workspaceId", workspaceId.toString())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

