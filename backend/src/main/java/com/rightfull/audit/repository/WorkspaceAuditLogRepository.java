package com.rightfull.audit.repository;

import com.rightfull.audit.entity.WorkspaceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for compliance and security audit logs.
 */
@Repository
public interface WorkspaceAuditLogRepository extends JpaRepository<WorkspaceAuditLog, UUID> {

    List<WorkspaceAuditLog> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<WorkspaceAuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId);
}

