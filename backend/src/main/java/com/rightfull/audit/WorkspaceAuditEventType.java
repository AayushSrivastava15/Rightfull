package com.rightfull.audit;

/**
 * Enumeration of security-sensitive workspace lifecycle and authorization events.
 */
public enum WorkspaceAuditEventType {
    WORKSPACE_CREATED,
    WORKSPACE_UPDATED,
    WORKSPACE_ARCHIVED,
    WORKSPACE_OWNERSHIP_TRANSFERRED,
    MEMBER_INVITED,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    INVITATION_REVOKED,
    MEMBER_ROLE_CHANGED,
    MEMBER_REMOVED,
    DEFAULT_WORKSPACE_CHANGED
}

