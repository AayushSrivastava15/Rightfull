package com.rightfull.plan.service;

import com.rightfull.audit.WorkspaceAuditEvent;
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
import com.rightfull.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("License Plan Service Unit Tests")
class LicensePlanServiceTest {

    @Mock
    private LicensePlanRepository planRepository;

    @Mock
    private LicensePlanVersionRepository versionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private EntitlementValidator entitlementValidator;
    private LicensePlanService planService;

    private UUID workspaceId;
    private UUID productId;
    private UUID actorId;
    private Product activeProduct;

    @BeforeEach
    void setUp() {
        entitlementValidator = new EntitlementValidator();
        planService = new LicensePlanService(
                planRepository,
                versionRepository,
                productRepository,
                entitlementValidator,
                auditService
        );

        workspaceId = UUID.randomUUID();
        productId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        activeProduct = new Product(workspaceId, "Rightful Enterprise", "rightful-enterprise", "Core Product", "1.0", actorId);
        activeProduct.setId(productId);
    }

    @Test
    @DisplayName("Create plan successfully saves plan, records initial version snapshot, and records audit event")
    void testCreatePlanSuccess() {
        CreatePlanRequest request = new CreatePlanRequest(
                "Pro Annual",
                "Full tier access",
                365,
                3,
                5,
                25200,
                300,
                Map.of("export_pdf", true, "max_seats", 5)
        );

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(activeProduct));
        when(planRepository.existsByProductIdAndNameIgnoreCase(productId, "Pro Annual")).thenReturn(false);
        when(planRepository.save(any(LicensePlan.class))).thenAnswer(invocation -> {
            LicensePlan p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PlanResponse response = planService.createPlan(workspaceId, productId, request, actorId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Pro Annual");
        assertThat(response.durationDays()).isEqualTo(365);
        assertThat(response.maximumActivations()).isEqualTo(3);
        assertThat(response.seatLimit()).isEqualTo(5);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(PlanStatus.ACTIVE);

        verify(versionRepository, times(1)).save(any(LicensePlanVersion.class));
        verify(auditService, times(1)).recordEvent(any(WorkspaceAuditEvent.class));
    }

    @Test
    @DisplayName("Create plan fails if product is archived")
    void testCreatePlanArchivedProductFails() {
        activeProduct.archive();
        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(activeProduct));

        CreatePlanRequest request = new CreatePlanRequest(
                "Pro Annual", "Desc", 365, 1, 1, 25200, 300, Map.of()
        );

        assertThatThrownBy(() -> planService.createPlan(workspaceId, productId, request, actorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create a license plan for an archived product");
    }

    @Test
    @DisplayName("Create plan fails on duplicate plan name under same product")
    void testCreatePlanDuplicateNameFails() {
        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(activeProduct));
        when(planRepository.existsByProductIdAndNameIgnoreCase(productId, "Existing Plan")).thenReturn(true);

        CreatePlanRequest request = new CreatePlanRequest(
                "Existing Plan", "Desc", 365, 1, 1, 25200, 300, Map.of()
        );

        assertThatThrownBy(() -> planService.createPlan(workspaceId, productId, request, actorId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists for this product");
    }

    @Test
    @DisplayName("Updating plan entitlements increments plan version and stores immutable version snapshot")
    void testUpdatePlanIncrementsVersionAndStoresSnapshot() {
        LicensePlan existingPlan = new LicensePlan(
                workspaceId, productId, "Original Plan", "Desc", 365, 1, 1, 25200, 300, Map.of("feature_a", true)
        );
        UUID planId = UUID.randomUUID();
        existingPlan.setId(planId);
        existingPlan.setVersion(1);

        UpdatePlanRequest request = new UpdatePlanRequest(
                "Updated Plan Name",
                "New description",
                365,
                2, // Activations changed
                1,
                25200,
                300,
                Map.of("feature_a", true, "feature_b", true) // Entitlements changed
        );

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(activeProduct));
        when(planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)).thenReturn(Optional.of(existingPlan));
        when(planRepository.existsByProductIdAndNameIgnoreCaseAndIdNot(productId, "Updated Plan Name", planId)).thenReturn(false);
        when(planRepository.save(any(LicensePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponse updated = planService.updatePlan(workspaceId, productId, planId, request, actorId);

        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.maximumActivations()).isEqualTo(2);
        assertThat(updated.name()).isEqualTo("Updated Plan Name");
        verify(versionRepository, times(1)).save(any(LicensePlanVersion.class));
        verify(auditService, times(1)).recordEvent(any(WorkspaceAuditEvent.class));
    }

    @Test
    @DisplayName("Updating plan fails if product is archived")
    void testUpdatePlanArchivedProductFails() {
        activeProduct.archive();
        UUID planId = UUID.randomUUID();

        UpdatePlanRequest request = new UpdatePlanRequest(
                "Updated Plan", "Desc", 365, 1, 1, 25200, 300, Map.of()
        );

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> planService.updatePlan(workspaceId, productId, planId, request, actorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update a license plan for an archived product");
    }

    @Test
    @DisplayName("Updating an archived plan fails")
    void testUpdateArchivedPlanFails() {
        LicensePlan plan = new LicensePlan(
                workspaceId, productId, "Archived Plan", "Desc", 365, 1, 1, 25200, 300, Map.of()
        );
        plan.archive();
        UUID planId = UUID.randomUUID();
        plan.setId(planId);

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(activeProduct));
        when(planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)).thenReturn(Optional.of(plan));

        UpdatePlanRequest request = new UpdatePlanRequest(
                "Updated Plan", "Desc", 365, 1, 1, 25200, 300, Map.of()
        );

        assertThatThrownBy(() -> planService.updatePlan(workspaceId, productId, planId, request, actorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update an archived license plan");
    }

    @Test
    @DisplayName("Historical plan versions are recoverable")
    void testGetHistoricalPlanVersions() {
        UUID planId = UUID.randomUUID();
        LicensePlan plan = new LicensePlan(workspaceId, productId, "Plan", "Desc", 365, 1, 1, 25200, 300, Map.of());
        plan.setId(planId);

        LicensePlanVersion v1 = new LicensePlanVersion(plan, actorId);
        LicensePlanVersion v2 = new LicensePlanVersion(plan, actorId);

        when(planRepository.findByIdAndProductIdAndWorkspaceId(planId, productId, workspaceId)).thenReturn(Optional.of(plan));
        when(versionRepository.findByPlanIdAndWorkspaceIdOrderByVersionDesc(planId, workspaceId)).thenReturn(List.of(v2, v1));

        List<PlanVersionResponse> versions = planService.getPlanVersions(workspaceId, productId, planId);

        assertThat(versions).hasSize(2);
    }

    @Test
    @DisplayName("Get plan snapshot produces immutable snapshot of plan state")
    void testGetPlanSnapshot() {
        LicensePlan plan = new LicensePlan(
                workspaceId, productId, "Pro Tier", "Desc", 365, 2, 5, 25200, 300, Map.of("export_pdf", true)
        );
        plan.setId(UUID.randomUUID());

        when(planRepository.findByIdAndProductIdAndWorkspaceId(plan.getId(), productId, workspaceId)).thenReturn(Optional.of(plan));

        PlanSnapshot snapshot = planService.getPlanSnapshot(workspaceId, productId, plan.getId());

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.planId()).isEqualTo(plan.getId());
        assertThat(snapshot.planName()).isEqualTo("Pro Tier");
        assertThat(snapshot.entitlements()).containsEntry("export_pdf", true);
        assertThatThrownBy(() -> snapshot.entitlements().put("new_key", "illegal"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
