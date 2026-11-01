package com.rightfull.product.service;

import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.DuplicateResourceException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.product.crypto.Ed25519KeyService;
import com.rightfull.product.crypto.PrivateKeyEncryptionService;
import com.rightfull.product.dto.CreateProductRequest;
import com.rightfull.product.dto.ProductResponse;
import com.rightfull.product.dto.UpdateProductRequest;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductSigningKey;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.repository.ProductRepository;
import com.rightfull.product.repository.ProductSigningKeyRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSigningKeyRepository signingKeyRepository;

    @Mock
    private ProductSigningKeyService signingKeyService;

    @Mock
    private WorkspaceAuditService auditService;

    private ProductService productService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                signingKeyRepository,
                signingKeyService,
                auditService
        );
    }

    @Test
    @DisplayName("Registers new product and provisions initial signing key successfully")
    void testRegisterProductSuccess() {
        CreateProductRequest request = new CreateProductRequest("Rightful CLI", "rightful-cli", "Developer CLI", "1.0.0");

        when(productRepository.existsByWorkspaceIdAndSlug(workspaceId, "rightful-cli")).thenReturn(false);

        Product mockProduct = new Product(workspaceId, "Rightful CLI", "rightful-cli", "Developer CLI", "1.0.0", actorId);
        mockProduct.setId(UUID.randomUUID());
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        ProductSigningKey mockKey = new ProductSigningKey(
                workspaceId, mockProduct.getId(), "kid_123", "pubkey_b64", new byte[16], new byte[12], "v1"
        );
        when(signingKeyService.createInitialSigningKey(any(Product.class), eq(actorId))).thenReturn(mockKey);

        ProductResponse response = productService.registerProduct(workspaceId, request, actorId);

        assertNotNull(response);
        assertEquals("Rightful CLI", response.name());
        assertEquals("rightful-cli", response.slug());
        assertEquals("kid_123", response.activeKeyId());
        assertEquals(ProductStatus.ACTIVE, response.status());

        verify(signingKeyService, times(1)).createInitialSigningKey(any(Product.class), eq(actorId));
        verify(auditService, times(1)).recordEvent(any());
    }

    @Test
    @DisplayName("Rejects product registration if slug is duplicate within workspace")
    void testRegisterProductDuplicateSlug() {
        CreateProductRequest request = new CreateProductRequest("Rightful CLI", "rightful-cli", "CLI", "1.0.0");
        when(productRepository.existsByWorkspaceIdAndSlug(workspaceId, "rightful-cli")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                productService.registerProduct(workspaceId, request, actorId)
        );

        verify(productRepository, never()).save(any());
        verify(signingKeyService, never()).createInitialSigningKey(any(), any());
    }

    @Test
    @DisplayName("Retrieves product details with active and historical public keys")
    void testGetProductDetails() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(workspaceId, "Rightful SDK", "rightful-sdk", "SDK", "2.0.0", actorId);
        product.setId(productId);

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(signingKeyService.getActivePublicKey(productId)).thenReturn(Optional.empty());
        when(signingKeyService.getAllPublicKeys(productId)).thenReturn(List.of());

        var details = productService.getProductDetails(workspaceId, productId);
        assertNotNull(details);
        assertEquals("Rightful SDK", details.name());
        assertEquals(ProductStatus.ACTIVE, details.status());
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when product does not belong to authorized workspace")
    void testGetProductCrossTenantIsolation() {
        UUID productId = UUID.randomUUID();
        UUID attackerWorkspaceId = UUID.randomUUID();

        when(productRepository.findByIdAndWorkspaceId(productId, attackerWorkspaceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductDetails(attackerWorkspaceId, productId)
        );
    }

    @Test
    @DisplayName("Archives product, soft-deleting it while preserving history")
    void testArchiveProduct() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(workspaceId, "Rightful Agent", "rightful-agent", "Agent", "1.0.0", actorId);
        product.setId(productId);

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.archiveProduct(workspaceId, productId, actorId);

        assertEquals(ProductStatus.ARCHIVED, response.status());
        assertNotNull(response.archivedAt());
        verify(auditService, times(1)).recordEvent(any());
    }

    @Test
    @DisplayName("Restores archived product back to ACTIVE status")
    void testRestoreProduct() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(workspaceId, "Rightful Agent", "rightful-agent", "Agent", "1.0.0", actorId);
        product.setId(productId);
        product.archive();

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.restoreProduct(workspaceId, productId, actorId);

        assertEquals(ProductStatus.ACTIVE, response.status());
        assertNull(response.archivedAt());
        verify(auditService, times(1)).recordEvent(any());
    }

    @Test
    @DisplayName("Updates product name and description while slug remains immutable")
    void testUpdateProduct() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(workspaceId, "Old Name", "permanent-slug", "Old Desc", "1.0.0", actorId);
        product.setId(productId);

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest updateRequest = new UpdateProductRequest("New Name", "New Desc", "1.1.0");
        ProductResponse response = productService.updateProduct(workspaceId, productId, updateRequest, actorId);

        assertEquals("New Name", response.name());
        assertEquals("permanent-slug", response.slug(), "Slug must remain strictly immutable");
        assertEquals("New Desc", response.description());
        assertEquals("1.1.0", response.version());
    }
}

