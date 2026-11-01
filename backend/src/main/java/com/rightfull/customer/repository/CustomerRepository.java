package com.rightfull.customer.repository;

import com.rightfull.customer.entity.Customer;
import com.rightfull.customer.entity.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for workspace-isolated customers.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    Page<Customer> findByWorkspaceIdAndStatus(UUID workspaceId, CustomerStatus status, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.workspaceId = :workspaceId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.externalReference) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomers(
            @Param("workspaceId") UUID workspaceId,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.workspaceId = :workspaceId AND c.status = :status AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.externalReference) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomersWithStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("query") String query,
            @Param("status") CustomerStatus status,
            Pageable pageable);

    Optional<Customer> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<Customer> findByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);

    boolean existsByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);

    boolean existsByWorkspaceIdAndEmailIgnoreCaseAndIdNot(UUID workspaceId, String email, UUID id);

    long countByWorkspaceIdAndStatus(UUID workspaceId, CustomerStatus status);
}

