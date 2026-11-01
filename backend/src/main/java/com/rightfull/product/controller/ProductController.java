package com.rightfull.product.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.product.dto.*;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.service.ProductService;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workspace-scoped software product catalog management
 * and Ed25519 cryptographic signing-key rotation.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Catalog", description = "Endpoints for registering software products and managing Ed25519 signing keys")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Register a software product",
            description = "Creates a new software product in the workspace and provisions an initial Ed25519 signing key. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> registerProduct(
            @RequestParam UUID workspaceId,
            @Valid @RequestBody CreateProductRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        ProductResponse response = productService.registerProduct(workspaceId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Product registered successfully with initial Ed25519 key"));
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List workspace products",
            description = "Returns a paginated list of software products belonging to the authorized workspace with optional status filter.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> listProducts(
            @RequestParam UUID workspaceId,
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> products = productService.listProducts(workspaceId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(products, "Workspace products retrieved successfully"));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Retrieve product details",
            description = "Returns complete product details including active public signing key and key rotation history.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetails(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId) {
        ProductDetailResponse response = productService.getProductDetails(workspaceId, productId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product details retrieved successfully"));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Update product metadata",
            description = "Updates product name, description, or version. Slugs and workspace association are immutable. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        ProductResponse response = productService.updateProduct(workspaceId, productId, request, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product updated successfully"));
    }

    @PostMapping("/{productId}/archive")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Archive product",
            description = "Soft-deletes a product, disabling new license issuance while preserving existing key history. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> archiveProduct(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        ProductResponse response = productService.archiveProduct(workspaceId, productId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product archived successfully"));
    }

    @PostMapping("/{productId}/restore")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Restore archived product",
            description = "Restores an archived product to ACTIVE state. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> restoreProduct(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        ProductResponse response = productService.restoreProduct(workspaceId, productId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product restored successfully"));
    }

    @PostMapping("/{productId}/keys/rotate")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Rotate Ed25519 signing key",
            description = "Retires current active signing key and generates a new Ed25519 keypair. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PublicKeyResponse>> rotateSigningKey(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        PublicKeyResponse response = productService.rotateSigningKey(workspaceId, productId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Product signing key rotated successfully"));
    }

    @PostMapping("/{productId}/keys/{keyId}/revoke")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Revoke an Ed25519 signing key",
            description = "Revokes an active or retired signing key due to compromise or invalidation. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PublicKeyResponse>> revokeSigningKey(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable String keyId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        PublicKeyResponse response = productService.revokeSigningKey(workspaceId, productId, keyId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Signing key revoked successfully"));
    }

    @GetMapping("/{productId}/keys")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Retrieve public signing keys",
            description = "Returns public keys for the product suitable for client-side license verification. Never exposes private keys.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<List<PublicKeyResponse>>> getPublicKeys(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId) {
        List<PublicKeyResponse> keys = productService.getPublicKeys(workspaceId, productId);
        return ResponseEntity.ok(ApiResponse.ok(keys, "Public signing keys retrieved successfully"));
    }
}

