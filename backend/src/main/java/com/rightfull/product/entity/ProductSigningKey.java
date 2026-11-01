package com.rightfull.product.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Product-specific Ed25519 cryptographic signing key entity.
 * Stores Base64-encoded public key for verification and AES-256-GCM encrypted private key at rest.
 */
@Entity
@Table(
        name = "product_signing_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_product_key_id", columnNames = {"product_id", "key_id"})
        }
)
public class ProductSigningKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "workspace_id", updatable = false, nullable = false)
    private UUID workspaceId;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "key_id", updatable = false, nullable = false, length = 64)
    private String keyId;

    @Column(name = "algorithm", updatable = false, nullable = false, length = 32)
    private String algorithm = "ED25519";

    @Column(name = "public_key", columnDefinition = "TEXT", updatable = false, nullable = false)
    private String publicKey;

    @Column(name = "encrypted_private_key", updatable = false, nullable = false)
    private byte[] encryptedPrivateKey;

    @Column(name = "encryption_nonce", updatable = false, nullable = false)
    private byte[] encryptionNonce;

    @Column(name = "encryption_version", updatable = false, nullable = false, length = 16)
    private String encryptionVersion = "v1";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private KeyStatus status = KeyStatus.ACTIVE;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    public ProductSigningKey() {
    }

    public ProductSigningKey(
            UUID workspaceId,
            UUID productId,
            String keyId,
            String publicKey,
            byte[] encryptedPrivateKey,
            byte[] encryptionNonce,
            String encryptionVersion) {
        this.workspaceId = workspaceId;
        this.productId = productId;
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.encryptionNonce = encryptionNonce;
        this.encryptionVersion = encryptionVersion;
        this.algorithm = "ED25519";
        this.status = KeyStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void retire() {
        this.status = KeyStatus.RETIRED;
        this.retiredAt = Instant.now();
        this.rotatedAt = Instant.now();
    }

    public void revoke() {
        this.status = KeyStatus.REVOKED;
        this.retiredAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == KeyStatus.ACTIVE;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(byte[] encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public byte[] getEncryptionNonce() {
        return encryptionNonce;
    }

    public void setEncryptionNonce(byte[] encryptionNonce) {
        this.encryptionNonce = encryptionNonce;
    }

    public String getEncryptionVersion() {
        return encryptionVersion;
    }

    public void setEncryptionVersion(String encryptionVersion) {
        this.encryptionVersion = encryptionVersion;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public void setStatus(KeyStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    public void setRotatedAt(Instant rotatedAt) {
        this.rotatedAt = rotatedAt;
    }

    public Instant getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Instant retiredAt) {
        this.retiredAt = retiredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductSigningKey that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

