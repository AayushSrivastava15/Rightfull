package com.rightfull.workspace.repository;

import com.rightfull.workspace.entity.WorkspaceInvitation;
import com.rightfull.workspace.entity.WorkspaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for workspace member invitation persistence and token lookup.
 */
@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {

    Optional<WorkspaceInvitation> findByTokenHash(String tokenHash);

    List<WorkspaceInvitation> findByWorkspaceId(UUID workspaceId);

    List<WorkspaceInvitation> findByWorkspaceIdAndStatus(UUID workspaceId, WorkspaceInvitationStatus status);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndEmailAndStatus(UUID workspaceId, String email, WorkspaceInvitationStatus status);

    boolean existsByWorkspaceIdAndEmailAndStatus(UUID workspaceId, String email, WorkspaceInvitationStatus status);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndId(UUID workspaceId, UUID id);
}

