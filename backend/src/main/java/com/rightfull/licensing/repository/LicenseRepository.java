package com.rightfull.licensing.repository;

import com.rightfull.licensing.entity.License;
import com.rightfull.licensing.entity.LicenseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for workspace-isolated software licenses.
 */
@Repository
public interface LicenseRepository extends JpaRepository<License, UUID> {

    Optional<License> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<License> findByProductIdAndLicenseKeyHash(UUID productId, String licenseKeyHash);

    Optional<License> findByWorkspaceIdAndIdempotencyKey(UUID workspaceId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM License l WHERE l.id = :id")
    Optional<License> findByIdWithPessimisticLock(@Param("id") UUID id);

    @Query("SELECT l FROM License l WHERE l.workspaceId = :workspaceId " +
            "AND (:productId IS NULL OR l.productId = :productId) " +
            "AND (:customerId IS NULL OR l.customerId = :customerId) " +
            "AND (:status IS NULL OR l.status = :status) " +
            "AND (:search IS NULL OR LOWER(l.licenseKeyPrefix) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(l.externalReference) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<License> findWithFilters(
            @Param("workspaceId") UUID workspaceId,
            @Param("productId") UUID productId,
            @Param("customerId") UUID customerId,
            @Param("status") LicenseStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    Page<License> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    Page<License> findByWorkspaceIdAndProductId(UUID workspaceId, UUID productId, Pageable pageable);

    long countByWorkspaceId(UUID workspaceId);

    long countByWorkspaceIdAndStatus(UUID workspaceId, LicenseStatus status);

    long countByWorkspaceIdAndProductId(UUID workspaceId, UUID productId);

    long countByCustomerId(UUID customerId);
}

