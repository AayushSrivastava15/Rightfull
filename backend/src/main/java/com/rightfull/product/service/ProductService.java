package com.rightfull.product.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.DuplicateResourceException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.product.dto.*;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductSigningKey;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.repository.ProductRepository;
import com.rightfull.product.repository.ProductSigningKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service managing workspace software product registration, lifecycle states,
 * and integration with product-specific cryptographic signing keys.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ProductRepository productRepository;
    private final ProductSigningKeyRepository signingKeyRepository;
    private final ProductSigningKeyService signingKeyService;
    private final WorkspaceAuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            ProductSigningKeyRepository signingKeyRepository,
            ProductSigningKeyService signingKeyService,
            WorkspaceAuditService auditService) {
        this.productRepository = productRepository;
        this.signingKeyRepository = signingKeyRepository;
        this.signingKeyService = signingKeyService;
        this.auditService = auditService;
    }

    /**
     * Registers a new product in the specified workspace and automatically
     * provisions
     * an initial Ed25519 signing key.
     */
    @Transactional
    public ProductResponse registerProduct(UUID workspaceId, CreateProductRequest request, UUID actorId) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? request.slug().trim().toLowerCase(Locale.ROOT)
                : generateSlug(request.name().trim());

        if (productRepository.existsByWorkspaceIdAndSlug(workspaceId, slug)) {
            throw new DuplicateResourceException("Product with slug '" + slug + "' already exists in this workspace");
        }

        Product product = new Product(
                workspaceId,
                request.name().trim(),
                slug,
                request.description() != null ? request.description().trim() : null,
                request.version() != null ? request.version().trim() : null,
                actorId);

        Product savedProduct = productRepository.save(product);
        log.info("Registered product id={} slug='{}' in workspace id={}", savedProduct.getId(), savedProduct.getSlug(),
                workspaceId);

        ProductSigningKey initialKey = signingKeyService.createInitialSigningKey(savedProduct, actorId);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PRODUCT_REGISTERED,
                workspaceId,
                actorId,
                Map.<String, Object>of(
                        "productId", savedProduct.getId().toString(),
                        "name", savedProduct.getName(),
                        "slug", savedProduct.getSlug(),
                        "keyId", initialKey.getKeyId())));

        return toProductResponse(savedProduct, initialKey.getKeyId());
    }

    /**
     * Retrieves a page of products for a workspace, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(UUID workspaceId, ProductStatus status, Pageable pageable) {
        Page<Product> products = (status != null)
                ? productRepository.findByWorkspaceIdAndStatus(workspaceId, status, pageable)
                : productRepository.findByWorkspaceId(workspaceId, pageable);

        return products.map(p -> {
            String activeKeyId = signingKeyRepository.findByProductIdAndStatus(p.getId(), KeyStatus.ACTIVE)
                    .map(ProductSigningKey::getKeyId)
                    .orElse(null);
            return toProductResponse(p, activeKeyId);
        });
    }

    /**
     * Retrieves full product details including active public key and historical
     * rotation keys.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetails(UUID workspaceId, UUID productId) {
        Product product = findProductOrThrow(workspaceId, productId);
        PublicKeyResponse activeKey = signingKeyService.getActivePublicKey(productId).orElse(null);
        List<PublicKeyResponse> historicalKeys = signingKeyService.getAllPublicKeys(productId);

        return new ProductDetailResponse(
                product.getId(),
                product.getWorkspaceId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getVersion(),
                product.getStatus(),
                activeKey,
                historicalKeys,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getArchivedAt());
    }

    /**
     * Updates editable product metadata (name, description, version).
     * Slug and workspace are strictly immutable.
     */
    @Transactional
    public ProductResponse updateProduct(UUID workspaceId, UUID productId, UpdateProductRequest request, UUID actorId) {
        Product product = findProductOrThrow(workspaceId, productId);

        product.setName(request.name().trim());
        product.setDescription(request.description() != null ? request.description().trim() : null);
        product.setVersion(request.version() != null ? request.version().trim() : null);

        Product updated = productRepository.save(product);
        log.info("Updated product id={} in workspace id={}", productId, workspaceId);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PRODUCT_UPDATED,
                workspaceId,
                actorId,
                Map.<String, Object>of(
                        "productId", updated.getId().toString(),
                        "name", updated.getName())));

        String activeKeyId = signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)
                .map(ProductSigningKey::getKeyId)
                .orElse(null);

        return toProductResponse(updated, activeKeyId);
    }

    /**
     * Archives a product (soft-delete). Preserves historical records for
     * verification.
     */
    @Transactional
    public ProductResponse archiveProduct(UUID workspaceId, UUID productId, UUID actorId) {
        Product product = findProductOrThrow(workspaceId, productId);
        if (product.getStatus() == ProductStatus.ARCHIVED) {
            return toProductResponse(product, null);
        }

        product.archive();
        Product saved = productRepository.save(product);
        log.info("Archived product id={} in workspace id={}", productId, workspaceId);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PRODUCT_ARCHIVED,
                workspaceId,
                actorId,
                Map.<String, Object>of("productId", saved.getId().toString())));

        return toProductResponse(saved, null);
    }

    /**
     * Restores an archived product to ACTIVE state.
     */
    @Transactional
    public ProductResponse restoreProduct(UUID workspaceId, UUID productId, UUID actorId) {
        Product product = findProductOrThrow(workspaceId, productId);
        if (product.getStatus() == ProductStatus.ACTIVE) {
            return toProductResponse(product, null);
        }

        product.restore();
        Product saved = productRepository.save(product);
        log.info("Restored product id={} in workspace id={}", productId, workspaceId);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PRODUCT_RESTORED,
                workspaceId,
                actorId,
                Map.<String, Object>of("productId", saved.getId().toString())));

        String activeKeyId = signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)
                .map(ProductSigningKey::getKeyId)
                .orElse(null);

        return toProductResponse(saved, activeKeyId);
    }

    /**
     * Rotates the signing key for a product.
     */
    @Transactional
    public PublicKeyResponse rotateSigningKey(UUID workspaceId, UUID productId, UUID actorId) {
        Product product = findProductOrThrow(workspaceId, productId);
        ProductSigningKey newKey = signingKeyService.rotateSigningKey(product, actorId);
        return signingKeyService.toPublicKeyResponse(newKey);
    }

    /**
     * Revokes a signing key for a product.
     */
    @Transactional
    public PublicKeyResponse revokeSigningKey(UUID workspaceId, UUID productId, String keyId, UUID actorId) {
        Product product = findProductOrThrow(workspaceId, productId);
        ProductSigningKey revokedKey = signingKeyService.revokeSigningKey(product, keyId, actorId);
        return signingKeyService.toPublicKeyResponse(revokedKey);
    }

    /**
     * Retrieves public keys for client-side license verification.
     */
    @Transactional(readOnly = true)
    public List<PublicKeyResponse> getPublicKeys(UUID workspaceId, UUID productId) {
        findProductOrThrow(workspaceId, productId);
        return signingKeyService.getAllPublicKeys(productId);
    }

    private Product findProductOrThrow(UUID workspaceId, UUID productId) {
        return productRepository.findByIdAndWorkspaceId(productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private ProductResponse toProductResponse(Product product, String activeKeyId) {
        return new ProductResponse(
                product.getId(),
                product.getWorkspaceId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getVersion(),
                product.getStatus(),
                activeKeyId,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getArchivedAt());
    }

    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ROOT);
        return slug.replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
