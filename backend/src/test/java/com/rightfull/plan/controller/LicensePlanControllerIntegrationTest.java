package com.rightfull.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.plan.dto.CreatePlanRequest;
import com.rightfull.plan.dto.PlanResponse;
import com.rightfull.plan.entity.PlanStatus;
import com.rightfull.plan.service.LicensePlanService;
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
@DisplayName("License Plan REST API Integration Tests")
class LicensePlanControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private LicensePlanService planService;

    @MockitoBean
    private WorkspaceSecurityEvaluator workspaceSecurityEvaluator;

    private UUID workspaceId;
    private UUID productId;
    private UUID planId;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        workspaceId = UUID.randomUUID();
        productId = UUID.randomUUID();
        planId = UUID.randomUUID();
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
    @DisplayName("POST /api/v1/products/{productId}/plans with ADMIN role returns 201 Created")
    void testCreatePlanSuccess() throws Exception {
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        CreatePlanRequest request = new CreatePlanRequest(
                "Standard Plan",
                "Standard tier",
                30,
                1,
                2,
                25200,
                300,
                Map.of("seats", 2)
        );

        PlanResponse response = new PlanResponse(
                planId, workspaceId, productId, "Standard Plan", "Standard tier",
                30, 1, 2, 25200, 300, Map.of("seats", 2),
                PlanStatus.ACTIVE, 1, Instant.now(), Instant.now(), null
        );

        when(planService.createPlan(eq(workspaceId), eq(productId), any(CreatePlanRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/products/{productId}/plans", productId)
                        .param("workspaceId", workspaceId.toString())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Standard Plan")))
                .andExpect(jsonPath("$.data.version", is(1)));
    }

    @Test
    @DisplayName("POST /api/v1/products/{productId}/plans with insufficient role returns 403 Forbidden")
    void testCreatePlanForbidden() throws Exception {
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(false);

        CreatePlanRequest request = new CreatePlanRequest(
                "Standard Plan", "Desc", 30, 1, 1, 25200, 300, Map.of()
        );

        mockMvc.perform(post("/api/v1/products/{productId}/plans", productId)
                        .param("workspaceId", workspaceId.toString())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId}/plans with VIEWER role returns 200 OK")
    void testListPlansSuccess() throws Exception {
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(true);

        PlanResponse response = new PlanResponse(
                planId, workspaceId, productId, "Standard Plan", "Desc",
                30, 1, 1, 25200, 300, Map.of(),
                PlanStatus.ACTIVE, 1, Instant.now(), Instant.now(), null
        );

        when(planService.listPlansPaged(eq(workspaceId), eq(productId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/products/{productId}/plans", productId)
                        .param("workspaceId", workspaceId.toString())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].name", is("Standard Plan")));
    }
}

