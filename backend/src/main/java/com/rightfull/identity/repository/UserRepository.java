package com.rightfull.identity.repository;

import com.rightfull.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findBySupabaseUserId(UUID supabaseUserId);

    Optional<User> findByEmail(String email);

    boolean existsBySupabaseUserId(UUID supabaseUserId);

    boolean existsByEmail(String email);
}

