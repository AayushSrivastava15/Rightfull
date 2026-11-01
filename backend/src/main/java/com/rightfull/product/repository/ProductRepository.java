package com.rightfull.product.repository;

import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for workspace-isolated software products.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<Product> findByWorkspaceIdAndSlug(UUID workspaceId, String slug);

    Page<Product> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    Page<Product> findByWorkspaceIdAndStatus(UUID workspaceId, ProductStatus status, Pageable pageable);

    boolean existsByWorkspaceIdAndSlug(UUID workspaceId, String slug);

    long countByWorkspaceIdAndStatus(UUID workspaceId, ProductStatus status);
}

