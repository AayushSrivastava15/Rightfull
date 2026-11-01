package com.rightfull.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.audit.entity.WorkspaceAuditLog;
import com.rightfull.audit.repository.WorkspaceAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Production persistent audit service.
 * Persists immutable security audit logs to PostgreSQL audit_logs table
 * and forwards events to structured logger for SIEM monitoring.
 */
@Service
@Primary
public class PersistentWorkspaceAuditService implements WorkspaceAuditService {

    private static final Logger log = LoggerFactory.getLogger(PersistentWorkspaceAuditService.class);

    private final WorkspaceAuditLogRepository auditLogRepository;
    private final LoggingWorkspaceAuditService loggingAuditService;
    private final ObjectMapper objectMapper;

    public PersistentWorkspaceAuditService(
            WorkspaceAuditLogRepository auditLogRepository,
            LoggingWorkspaceAuditService loggingAuditService) {
        this.auditLogRepository = auditLogRepository;
        this.loggingAuditService = loggingAuditService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordEvent(WorkspaceAuditEvent event) {
        // 1. Structured logging for operational diagnostics and SIEM ingestion
        loggingAuditService.recordEvent(event);

        // 2. Persist to PostgreSQL audit_logs table
        try {
            Map<String, Object> sanitizedDetails = sanitizeDetails(event.details());
            String detailsJson = objectMapper.writeValueAsString(sanitizedDetails);

            String targetType = resolveTargetType(event.eventType());
            UUID targetId = event.targetUserId();

            WorkspaceAuditLog auditRecord = new WorkspaceAuditLog(
                    event.workspaceId(),
                    event.actorUserId(),
                    event.eventType().name(),
                    targetType,
                    targetId,
                    null, // IP address extracted from RequestContext if present
                    detailsJson
            );

            auditLogRepository.save(auditRecord);
        } catch (Exception ex) {
            log.error("Failed to persist workspace audit event: type={}, workspaceId={}, actorId={}: {}",
                    event.eventType(), event.workspaceId(), event.actorUserId(), ex.getMessage(), ex);
            // In high-compliance environments, rethrow or handle per audit policy
            throw new IllegalStateException("Security audit logging failure: transaction aborted", ex);
        }
    }

    private Map<String, Object> sanitizeDetails(Map<String, Object> rawDetails) {
        if (rawDetails == null || rawDetails.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> clean = new HashMap<>(rawDetails);
        // Scrub any sensitive credential keys if ever present
        clean.remove("password");
        clean.remove("token");
        clean.remove("rawToken");
        clean.remove("licenseKey");
        clean.remove("rawKey");
        clean.remove("secret");
        clean.remove("privateKey");
        clean.remove("encryptedPrivateKey");
        clean.remove("nonce");
        return clean;
    }

    private String resolveTargetType(WorkspaceAuditEventType type) {
        return switch (type) {
            case WORKSPACE_CREATED, WORKSPACE_UPDATED, WORKSPACE_ARCHIVED, WORKSPACE_OWNERSHIP_TRANSFERRED -> "WORKSPACE";
            case MEMBER_INVITED, INVITATION_ACCEPTED, INVITATION_REJECTED, INVITATION_REVOKED -> "INVITATION";
            case MEMBER_ROLE_CHANGED, MEMBER_REMOVED, DEFAULT_WORKSPACE_CHANGED -> "MEMBERSHIP";
            case PRODUCT_REGISTERED, PRODUCT_UPDATED, PRODUCT_ARCHIVED, PRODUCT_RESTORED -> "PRODUCT";
            case SIGNING_KEY_GENERATED, SIGNING_KEY_ROTATED, SIGNING_KEY_REVOKED -> "SIGNING_KEY";
            case PLAN_CREATED, PLAN_UPDATED, PLAN_ARCHIVED -> "PLAN";
            case CUSTOMER_CREATED, CUSTOMER_UPDATED, CUSTOMER_ARCHIVED -> "CUSTOMER";
            case LICENSE_ISSUED, LICENSE_SUSPENDED, LICENSE_RESUMED, LICENSE_REVOKED, LICENSE_RENEWED -> "LICENSE";
            case DEVICE_ACTIVATED, DEVICE_DEACTIVATED -> "ACTIVATION";
        };
    }
}
