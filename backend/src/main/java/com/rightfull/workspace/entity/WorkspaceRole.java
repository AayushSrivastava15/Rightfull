package com.rightfull.workspace.entity;

/**
 * Roles for workspace members with hierarchical permissions.
 */
public enum WorkspaceRole {
    OWNER(4),
    ADMIN(3),
    MEMBER(2),
    VIEWER(1);

    private final int level;

    WorkspaceRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasAtLeast(WorkspaceRole requiredRole) {
        return this.level >= requiredRole.level;
    }

    public boolean isAdministrative() {
        return this == OWNER || this == ADMIN;
    }
}

