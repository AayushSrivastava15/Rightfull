package com.rightfull.product.repository;

import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.ProductSigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for product Ed25519 cryptographic signing keys.
 */
@Repository
public interface ProductSigningKeyRepository extends JpaRepository<ProductSigningKey, UUID> {

    Optional<ProductSigningKey> findByProductIdAndStatus(UUID productId, KeyStatus status);

    Optional<ProductSigningKey> findByProductIdAndKeyId(UUID productId, String keyId);

    List<ProductSigningKey> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<ProductSigningKey> findByWorkspaceIdAndProductIdOrderByCreatedAtDesc(UUID workspaceId, UUID productId);
}

