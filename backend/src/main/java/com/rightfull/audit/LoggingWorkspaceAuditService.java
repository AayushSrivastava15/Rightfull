package com.rightfull.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default structured logger implementation of WorkspaceAuditService.
 * Formats events for security monitoring and SIEM integration.
 */
@Service
public class LoggingWorkspaceAuditService implements WorkspaceAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.WORKSPACE");

    @Override
    public void recordEvent(WorkspaceAuditEvent event) {
        auditLog.info("WORKSPACE_AUDIT_EVENT eventId={} timestamp={} type={} workspaceId={} actorId={} targetId={} details={}",
                event.eventId(),
                event.timestamp(),
                event.eventType(),
                event.workspaceId(),
                event.actorUserId(),
                event.targetUserId() != null ? event.targetUserId() : "N/A",
                event.details());
    }
}

