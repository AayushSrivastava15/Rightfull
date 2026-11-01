package com.rightfull.activation.repository;

import com.rightfull.activation.entity.ActivationStatus;
import com.rightfull.activation.entity.LicenseActivation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for hardware activations bound to software licenses.
 */
@Repository
public interface LicenseActivationRepository extends JpaRepository<LicenseActivation, UUID> {

    Optional<LicenseActivation> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<LicenseActivation> findByLicenseIdAndDeviceIdentifierHash(UUID licenseId, String deviceIdentifierHash);

    List<LicenseActivation> findByLicenseIdAndStatus(UUID licenseId, ActivationStatus status);

    Page<LicenseActivation> findByLicenseId(UUID licenseId, Pageable pageable);

    Page<LicenseActivation> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    Page<LicenseActivation> findByWorkspaceIdAndStatus(UUID workspaceId, ActivationStatus status, Pageable pageable);

    long countByLicenseIdAndStatus(UUID licenseId, ActivationStatus status);

    long countByWorkspaceId(UUID workspaceId);

    long countByWorkspaceIdAndStatus(UUID workspaceId, ActivationStatus status);
}

