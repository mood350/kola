package com.kola.backend.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // CORRECTION : On ne cherche plus juste par le code, on vérifie qu'il n'est PAS encore validé
    // et que sa date d'expiration est dans le futur.
    @Query("SELECT t FROM Token t WHERE t.token = :token AND t.validatedAt IS NULL AND t.expiresAt > :now")
    Optional<Token> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
}