package com.rightfull.plan.repository;

import com.rightfull.plan.entity.LicensePlan;
import com.rightfull.plan.entity.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for workspace-isolated license plans.
 */
@Repository
public interface LicensePlanRepository extends JpaRepository<LicensePlan, UUID> {

    List<LicensePlan> findByWorkspaceIdAndProductIdOrderByCreatedAtDesc(UUID workspaceId, UUID productId);

    List<LicensePlan> findByWorkspaceIdAndProductIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, UUID productId, PlanStatus status);

    Page<LicensePlan> findByWorkspaceIdAndProductId(UUID workspaceId, UUID productId, Pageable pageable);

    Page<LicensePlan> findByWorkspaceIdAndProductIdAndStatus(UUID workspaceId, UUID productId, PlanStatus status, Pageable pageable);

    Optional<LicensePlan> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<LicensePlan> findByIdAndProductIdAndWorkspaceId(UUID id, UUID productId, UUID workspaceId);

    boolean existsByProductIdAndNameIgnoreCase(UUID productId, String name);

    boolean existsByProductIdAndNameIgnoreCaseAndIdNot(UUID productId, String name, UUID id);
}

