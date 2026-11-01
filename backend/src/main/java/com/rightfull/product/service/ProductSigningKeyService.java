package com.rightfull.product.service;

import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.product.crypto.Ed25519KeyService;
import com.rightfull.product.crypto.PrivateKeyEncryptionService;
import com.rightfull.product.dto.PublicKeyResponse;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductSigningKey;
import com.rightfull.product.repository.ProductSigningKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing product-specific Ed25519 signing keys, keypair generation,
 * AES-256-GCM encrypted persistence, and secure key rotation.
 */
@Service
public class ProductSigningKeyService {

    private static final Logger log = LoggerFactory.getLogger(ProductSigningKeyService.class);

    private final ProductSigningKeyRepository signingKeyRepository;
    private final Ed25519KeyService ed25519KeyService;
    private final PrivateKeyEncryptionService encryptionService;
    private final WorkspaceAuditService auditService;

    public ProductSigningKeyService(
            ProductSigningKeyRepository signingKeyRepository,
            Ed25519KeyService ed25519KeyService,
            PrivateKeyEncryptionService encryptionService,
            WorkspaceAuditService auditService) {
        this.signingKeyRepository = signingKeyRepository;
        this.ed25519KeyService = ed25519KeyService;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }

    /**
     * Generates and stores the initial Ed25519 signing key for a newly registered product.
     */
    @Transactional
    public ProductSigningKey createInitialSigningKey(Product product, UUID actorId) {
        log.info("Generating initial Ed25519 signing key for product id={} slug={}", product.getId(), product.getSlug());
        ProductSigningKey key = generateAndPersistKey(product);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.SIGNING_KEY_GENERATED,
                product.getWorkspaceId(),
                actorId,
                Map.<String, Object>of(
                        "productId", product.getId().toString(),
                        "keyId", key.getKeyId(),
                        "algorithm", key.getAlgorithm()
                )
        ));

        return key;
    }

    /**
     * Rotates the signing key for a product:
     * Retires the existing ACTIVE key (preserving it for historical license verification),
     * and mints a new ACTIVE Ed25519 keypair.
     */
    @Transactional
    public ProductSigningKey rotateSigningKey(Product product, UUID actorId) {
        log.info("Rotating signing key for product id={} slug={}", product.getId(), product.getSlug());

        Optional<ProductSigningKey> currentActive = signingKeyRepository.findByProductIdAndStatus(
                product.getId(),
                KeyStatus.ACTIVE
        );

        String previousKeyId = null;
        if (currentActive.isPresent()) {
            ProductSigningKey oldKey = currentActive.get();
            oldKey.retire();
            signingKeyRepository.save(oldKey);
            previousKeyId = oldKey.getKeyId();
            log.info("Retired previous signing key id={} (kid={}) for product id={}", oldKey.getId(), oldKey.getKeyId(), product.getId());
        }

        ProductSigningKey newKey = generateAndPersistKey(product);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.SIGNING_KEY_ROTATED,
                product.getWorkspaceId(),
                actorId,
                Map.<String, Object>of(
                        "productId", product.getId().toString(),
                        "newKeyId", newKey.getKeyId(),
                        "previousKeyId", (previousKeyId != null) ? previousKeyId : "NONE"
                )
        ));

        return newKey;
    }

    private ProductSigningKey generateAndPersistKey(Product product) {
        KeyPair keyPair = ed25519KeyService.generateKeyPair();
        String base64PublicKey = ed25519KeyService.encodePublicKey(keyPair.getPublic());
        byte[] pkcs8PrivateKeyBytes = ed25519KeyService.encodePrivateKey(keyPair.getPrivate());

        String keyId = "kid_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        var encryptedPayload = encryptionService.encrypt(
                pkcs8PrivateKeyBytes,
                product.getId(),
                keyId,
                PrivateKeyEncryptionService.DEFAULT_ENCRYPTION_VERSION
        );

        ProductSigningKey signingKey = new ProductSigningKey(
                product.getWorkspaceId(),
                product.getId(),
                keyId,
                base64PublicKey,
                encryptedPayload.ciphertext(),
                encryptedPayload.nonce(),
                encryptedPayload.version()
        );

        return signingKeyRepository.save(signingKey);
    }

    /**
     * Revokes a signing key for a product due to key compromise or retirement.
     * Enforces that the key belongs to the product and workspace and is not already revoked.
     */
    @Transactional
    public ProductSigningKey revokeSigningKey(Product product, String keyId, UUID actorId) {
        ProductSigningKey key = signingKeyRepository.findByProductIdAndKeyId(product.getId(), keyId)
                .orElseThrow(() -> new ResourceNotFoundException("Signing key not found with keyId: " + keyId));

        if (!key.getWorkspaceId().equals(product.getWorkspaceId())) {
            throw new ResourceNotFoundException("Signing key not found with keyId: " + keyId);
        }

        if (key.getStatus() == KeyStatus.REVOKED) {
            throw new BusinessException("Signing key is already revoked", "KEY_ALREADY_REVOKED", HttpStatus.CONFLICT);
        }

        KeyStatus previousStatus = key.getStatus();
        key.revoke();
        ProductSigningKey saved = signingKeyRepository.save(key);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.SIGNING_KEY_REVOKED,
                product.getWorkspaceId(),
                actorId,
                Map.<String, Object>of(
                        "productId", product.getId().toString(),
                        "keyId", saved.getKeyId(),
                        "previousStatus", previousStatus.name()
                )
        ));

        log.warn("Revoked signing key id={} (kid={}) for product id={} by actor={}", saved.getId(), saved.getKeyId(), product.getId(), actorId);
        return saved;
    }

    /**
     * Retrieves the current ACTIVE public key for a product.
     */
    @Transactional(readOnly = true)
    public Optional<PublicKeyResponse> getActivePublicKey(UUID productId) {
        return signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)
                .map(this::toPublicKeyResponse);
    }

    /**
     * Retrieves all public keys for a product (active and historical) in reverse chronological order.
     */
    @Transactional(readOnly = true)
    public List<PublicKeyResponse> getAllPublicKeys(UUID productId) {
        return signingKeyRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toPublicKeyResponse)
                .toList();
    }

    /**
     * Decrypts and reconstructs the active Ed25519 PrivateKey for license signing.
     * Accessible only internally within transactional service boundaries.
     */
    @Transactional(readOnly = true)
    public PrivateKey getActivePrivateKey(UUID productId) {
        ProductSigningKey key = signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active signing key found for product id=" + productId));

        byte[] decryptedPkcs8 = encryptionService.decrypt(
                key.getEncryptedPrivateKey(),
                key.getEncryptionNonce(),
                key.getEncryptionVersion(),
                key.getProductId(),
                key.getKeyId()
        );

        return ed25519KeyService.decodePrivateKey(decryptedPkcs8);
    }

    public PublicKeyResponse toPublicKeyResponse(ProductSigningKey key) {
        return new PublicKeyResponse(
                key.getKeyId(),
                key.getAlgorithm(),
                key.getPublicKey(),
                key.getStatus(),
                key.getCreatedAt(),
                key.getRetiredAt()
        );
    }
}
