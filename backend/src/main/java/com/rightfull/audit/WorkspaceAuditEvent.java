package com.rightfull.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable security audit record for workspace administrative actions.
 */
public record WorkspaceAuditEvent(
        UUID eventId,
        Instant timestamp,
        WorkspaceAuditEventType eventType,
        UUID workspaceId,
        UUID actorUserId,
        UUID targetUserId,
        Map<String, Object> details
) {
    public static WorkspaceAuditEvent of(
            WorkspaceAuditEventType eventType,
            UUID workspaceId,
            UUID actorUserId,
            Map<String, Object> details) {
        return of(eventType, workspaceId, actorUserId, null, details);
    }

    public static WorkspaceAuditEvent of(
            WorkspaceAuditEventType eventType,
            UUID workspaceId,
            UUID actorUserId,
            UUID targetUserId,
            Map<String, Object> details) {
        return new WorkspaceAuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                workspaceId,
                actorUserId,
                targetUserId,
                details != null ? details : Map.of()
        );
    }
}

