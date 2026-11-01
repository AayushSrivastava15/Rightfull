package com.rightfull.plan.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.DuplicateResourceException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.plan.dto.CreatePlanRequest;
import com.rightfull.plan.dto.PlanResponse;
import com.rightfull.plan.dto.PlanSnapshot;
import com.rightfull.plan.dto.PlanVersionResponse;
import com.rightfull.plan.dto.UpdatePlanRequest;
import com.rightfull.plan.entity.LicensePlan;
import com.rightfull.plan.entity.LicensePlanVersion;
import com.rightfull.plan.entity.PlanStatus;
import com.rightfull.plan.repository.LicensePlanRepository;
import com.rightfull.plan.repository.LicensePlanVersionRepository;
import com.rightfull.plan.validator.EntitlementValidator;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service orchestrating workspace-isolated license plans and structured entitlement validation.
 */
@Service
public class LicensePlanService {

    private static final Logger log = LoggerFactory.getLogger(LicensePlanService.class);

    private final LicensePlanRepository planRepository;
    private final LicensePlanVersionRepository versionRepository;
    private final ProductRepository productRepository;
    private final EntitlementValidator entitlementValidator;
    private final WorkspaceAuditService auditService;

    public LicensePlanService(
            LicensePlanRepository planRepository,
            LicensePlanVersionRepository versionRepository,
            ProductRepository productRepository,
            EntitlementValidator entitlementValidator,
            WorkspaceAuditService auditService) {
        this.planRepository = planRepository;
        this.versionRepository = versionRepository;
        this.productRepository = productRepository;
        this.entitlementValidator = entitlementValidator;
        this.auditService = auditService;
    }

    @Transactional
    public PlanResponse createPlan(UUID workspaceId, UUID productId, CreatePlanRequest request, UUID actorId) {
        // 1. Verify product exists in workspace and is ACTIVE
        Product product = productRepository.findByIdAndWorkspaceId(productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this workspace: " + productId));

        if (product.getStatus() == ProductStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot create a license plan for an archived product");
        }

        // 2. Check for unique plan name under this product
        if (planRepository.existsByProductIdAndNameIgnoreCase(productId, request.name().trim())) {
            throw new DuplicateResourceException("A plan with name '" + request.name() + "' already exists for this product");
        }

        // 3. Validate entitlements
        entitlementValidator.validate(request.entitlements());

        // 4. Create and save plan
        LicensePlan plan = new LicensePlan(
                workspaceId,
                productId,
                request.name().trim(),
                request.description(),
                request.durationDays(),
                request.maximumActivations(),
                request.seatLimit(),
                request.resolvedGracePeriod(),
                request.resolvedRevalidationInterval(),
                request.entitlements()
        );

        LicensePlan saved = planRepository.save(plan);
        log.info("Created license plan id={} name='{}' product id={} workspace id={}",
                saved.getId(), saved.getName(), productId, workspaceId);

        // Record initial immutable version in history
        LicensePlanVersion initialVersion = new LicensePlanVersion(saved, actorId);
        versionRepository.save(initialVersion);

        // 5. Audit event
        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PLAN_CREATED,
                workspaceId,
                actorId,
                Map.of(
                        "planId", saved.getId().toString(),
                        "productId", productId.toString(),
                        "planName", saved.getName(),
                        "durationDays", saved.getDurationDays(),
                        "seatLimit", saved.getSeatLimit(),
                        "maximumActivations", saved.getMaximumActivations()
                )
        ));

        return PlanResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listPlans(UUID workspaceId, UUID productId, PlanStatus status) {
        productRepository.findByIdAndWorkspaceId(productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this workspace: " + productId));

        List<LicensePlan> plans = status != null
                ? planRepository.findByWorkspaceIdAndProductIdAndStatusOrderByCreatedAtDesc(workspaceId, productId, status)
                : planRepository.findByWorkspaceIdAndProductIdOrderByCreatedAtDesc(workspaceId, productId);

        return plans.stream().map(PlanResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<PlanResponse> listPlansPaged(UUID workspaceId, UUID productId, PlanStatus status, Pageable pageable) {
        productRepository.findByIdAndWorkspaceId(productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this workspace: " + productId));

        Page<LicensePlan> page = status != null
                ? planRepository.findByWorkspaceIdAndProductIdAndStatus(workspaceId, productId, status, pageable)
                : planRepository.findByWorkspaceIdAndProductId(workspaceId, productId, pageable);

        return page.map(PlanResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlan(UUID workspaceId, UUID productId, UUID planId) {
        LicensePlan plan = planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan not found: " + planId));
        return PlanResponse.fromEntity(plan);
    }

    @Transactional
    public PlanResponse updatePlan(UUID workspaceId, UUID productId, UUID planId, UpdatePlanRequest request, UUID actorId) {
        // 1. Verify product exists and is active
        Product product = productRepository.findByIdAndWorkspaceId(productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this workspace: " + productId));

        if (product.getStatus() == ProductStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update a license plan for an archived product");
        }

        LicensePlan plan = planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan not found: " + planId));

        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update an archived license plan");
        }

        // Check unique name if changed
        if (!plan.getName().equalsIgnoreCase(request.name().trim()) &&
                planRepository.existsByProductIdAndNameIgnoreCaseAndIdNot(productId, request.name().trim(), planId)) {
            throw new DuplicateResourceException("Another plan with name '" + request.name() + "' already exists for this product");
        }

        // Validate entitlements
        entitlementValidator.validate(request.entitlements());

        // Check if version increment is required (entitlements or limits changed)
        boolean limitsOrEntitlementsChanged = !Objects.equals(plan.getDurationDays(), request.durationDays())
                || plan.getSeatLimit() != request.seatLimit()
                || plan.getMaximumActivations() != request.maximumActivations()
                || plan.getOfflineGracePeriodSeconds() != request.offlineGracePeriodSeconds()
                || plan.getRevalidationIntervalSeconds() != request.revalidationIntervalSeconds()
                || !Objects.equals(plan.getEntitlements(), request.entitlements());

        plan.setName(request.name().trim());
        plan.setDescription(request.description());
        plan.setDurationDays(request.durationDays());
        plan.setMaximumActivations(request.maximumActivations());
        plan.setSeatLimit(request.seatLimit());
        plan.setOfflineGracePeriodSeconds(request.offlineGracePeriodSeconds());
        plan.setRevalidationIntervalSeconds(request.revalidationIntervalSeconds());
        plan.setEntitlements(request.entitlements());

        if (limitsOrEntitlementsChanged) {
            plan.incrementVersion();
        }

        LicensePlan updated = planRepository.save(plan);
        log.info("Updated license plan id={} new version={}", updated.getId(), updated.getVersion());

        // Record new immutable version in history
        LicensePlanVersion versionSnapshot = new LicensePlanVersion(updated, actorId);
        versionRepository.save(versionSnapshot);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PLAN_UPDATED,
                workspaceId,
                actorId,
                Map.of(
                        "planId", updated.getId().toString(),
                        "productId", productId.toString(),
                        "newVersion", updated.getVersion(),
                        "planName", updated.getName()
                )
        ));

        return PlanResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<PlanVersionResponse> getPlanVersions(UUID workspaceId, UUID productId, UUID planId) {
        planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan not found: " + planId));
        return versionRepository.findByPlanIdAndWorkspaceIdOrderByVersionDesc(planId, workspaceId)
                .stream()
                .map(PlanVersionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanVersionResponse getPlanVersion(UUID workspaceId, UUID productId, UUID planId, int version) {
        planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan not found: " + planId));
        LicensePlanVersion v = versionRepository.findByPlanIdAndVersionAndWorkspaceId(planId, version, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan version " + version + " not found for plan: " + planId));
        return PlanVersionResponse.fromEntity(v);
    }

    @Transactional
    public PlanResponse archivePlan(UUID workspaceId, UUID productId, UUID planId, UUID actorId) {
        LicensePlan plan = planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan not found: " + planId));

        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            return PlanResponse.fromEntity(plan);
        }

        plan.archive();
        LicensePlan archived = planRepository.save(plan);
        log.info("Archived license plan id={}", archived.getId());

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.PLAN_ARCHIVED,
                workspaceId,
                actorId,
                Map.of(
                        "planId", archived.getId().toString(),
                        "productId", productId.toString(),
                        "planName", archived.getName()
                )
        ));

        return PlanResponse.fromEntity(archived);
    }

    @Transactional(readOnly = true)
    public PlanSnapshot getPlanSnapshot(UUID workspaceId, UUID productId, UUID planId) {
        LicensePlan plan = planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License plan not found: " + planId));
        return PlanSnapshot.fromPlan(plan);
    }
}

