package com.rightfull.identity.repository;

import com.rightfull.identity.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    @Query("SELECT p FROM UserPreferences p LEFT JOIN FETCH p.defaultWorkspace WHERE p.user.id = :userId")
    Optional<UserPreferences> findByUserIdWithWorkspace(@Param("userId") UUID userId);

    Optional<UserPreferences> findByUserId(UUID userId);
}

