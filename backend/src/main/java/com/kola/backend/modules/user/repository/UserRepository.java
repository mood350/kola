package com.kola.backend.modules.user.repository;

import com.kola.backend.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    /** Ids only: the nightly scoring pass walks every active account and needs nothing else. */
    @Query("select u.id from User u where u.status = com.kola.backend.common.enums.UserStatus.ACTIVE")
    List<UUID> findActiveIds();

    /** New accounts since a point in time — "croissance des utilisateurs" (KOLA.md 4.5). */
    long countByCreatedAtAfter(Instant since);
}
