package com.kola.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRolesRoleName(String roleName);

    long countByEnabledTrueAndAccountLockedFalse();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(u.countryCode, 'UN'), COUNT(u) FROM User u GROUP BY COALESCE(u.countryCode, 'UN') ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByCountry();

    @Query("SELECT u.kycLevel, COUNT(u) FROM User u GROUP BY u.kycLevel ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByKycLevel();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
}
