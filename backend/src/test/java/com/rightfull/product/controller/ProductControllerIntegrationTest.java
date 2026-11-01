package com.rightfull.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.product.dto.*;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.service.ProductService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Product Catalog REST API Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private WorkspaceSecurityEvaluator workspaceSecurityEvaluator;

    private UUID workspaceId;
    private UUID productId;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        workspaceId = UUID.randomUUID();
        productId = UUID.randomUUID();

        testUser = new User(UUID.randomUUID(), "developer@rightfull.com", "Dev Lead", null);
        testUser.setId(UUID.randomUUID());

        when(userProvisioningService.getOrCreateUser(any())).thenReturn(testUser);
    }

    private Jwt mockJwt() {
        return Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "RS256")
                .claim("sub", testUser.getSupabaseUserId().toString())
                .claim("email", testUser.getEmail())
                .claim("aud", "authenticated")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/products returns HTTP 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/products?workspaceId=" + workspaceId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("POST /api/v1/products with ADMIN role registers product and returns HTTP 201 Created")
    void testRegisterProductSuccess() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        CreateProductRequest request = new CreateProductRequest("Rightful Engine", "rightful-engine", "Licensing Core", "1.0.0");
        ProductResponse response = new ProductResponse(
                productId, workspaceId, "Rightful Engine", "rightful-engine", "Licensing Core", "1.0.0",
                ProductStatus.ACTIVE, "kid_sample123", Instant.now(), Instant.now(), null
        );

        when(productService.registerProduct(eq(workspaceId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(productId.toString())))
                .andExpect(jsonPath("$.data.slug", is("rightful-engine")))
                .andExpect(jsonPath("$.data.activeKeyId", is("kid_sample123")));
    }

    @Test
    @DisplayName("POST /api/v1/products with VIEWER role returns HTTP 403 Forbidden")
    void testRegisterProductForbiddenForViewer() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(false);

        CreateProductRequest request = new CreateProductRequest("Unauthorized Product", "unauth-prod", "Desc", "1.0");

        mockMvc.perform(post("/api/v1/products?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/products with VIEWER role returns paginated products")
    void testListProductsSuccess() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(true);

        ProductResponse prod = new ProductResponse(
                productId, workspaceId, "SDK", "sdk", "Desc", "1.0",
                ProductStatus.ACTIVE, "kid_999", Instant.now(), Instant.now(), null
        );
        when(productService.listProducts(eq(workspaceId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(prod), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/products?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].slug", is("sdk")));
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId} returns complete details including public keys")
    void testGetProductDetails() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(true);

        PublicKeyResponse key = new PublicKeyResponse(
                "kid_active", "ED25519", "b64_pub_key", KeyStatus.ACTIVE, Instant.now(), null
        );
        ProductDetailResponse detail = new ProductDetailResponse(
                productId, workspaceId, "Core", "core", "Core lib", "3.0.0",
                ProductStatus.ACTIVE, key, List.of(key), Instant.now(), Instant.now(), null
        );

        when(productService.getProductDetails(workspaceId, productId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/products/" + productId + "?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeKey.keyId", is("kid_active")))
                .andExpect(jsonPath("$.data.activeKey.algorithm", is("ED25519")))
                .andExpect(jsonPath("$.data.activeKey.publicKey", is("b64_pub_key")));
    }

    @Test
    @DisplayName("POST /api/v1/products/{productId}/keys/rotate rotates signing key and returns new public key")
    void testRotateSigningKeySuccess() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        PublicKeyResponse newKey = new PublicKeyResponse(
                "kid_rotated_222", "ED25519", "new_b64_pub_key", KeyStatus.ACTIVE, Instant.now(), null
        );
        when(productService.rotateSigningKey(eq(workspaceId), eq(productId), any())).thenReturn(newKey);

        mockMvc.perform(post("/api/v1/products/" + productId + "/keys/rotate?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.keyId", is("kid_rotated_222")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/products/{productId}/archive archives the product")
    void testArchiveProduct() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        ProductResponse response = new ProductResponse(
                productId, workspaceId, "Old App", "old-app", null, null,
                ProductStatus.ARCHIVED, null, Instant.now(), Instant.now(), Instant.now()
        );
        when(productService.archiveProduct(eq(workspaceId), eq(productId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products/" + productId + "/archive?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ARCHIVED")));
    }

    @Test
    @DisplayName("POST /api/v1/products/{productId}/restore restores an archived product")
    void testRestoreProduct() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        ProductResponse response = new ProductResponse(
                productId, workspaceId, "Restored App", "restored-app", null, null,
                ProductStatus.ACTIVE, "kid_key", Instant.now(), Instant.now(), null
        );
        when(productService.restoreProduct(eq(workspaceId), eq(productId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products/" + productId + "/restore?workspaceId=" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }
}

