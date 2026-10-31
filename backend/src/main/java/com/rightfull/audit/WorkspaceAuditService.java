package com.rightfull.audit;

/**
 * Service contract for publishing security-sensitive workspace audit events.
 * Provides abstraction for Step 3.8 and future Step 3.9 audit_logs database integration.
 */
public interface WorkspaceAuditService {

    void recordEvent(WorkspaceAuditEvent event);
}

