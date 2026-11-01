package com.rightfull.plan.repository;

import com.rightfull.plan.entity.LicensePlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for immutable license plan historical version records.
 */
@Repository
public interface LicensePlanVersionRepository extends JpaRepository<LicensePlanVersion, UUID> {

    List<LicensePlanVersion> findByPlanIdAndWorkspaceIdOrderByVersionDesc(UUID planId, UUID workspaceId);

    Optional<LicensePlanVersion> findByPlanIdAndVersionAndWorkspaceId(UUID planId, int version, UUID workspaceId);
}

