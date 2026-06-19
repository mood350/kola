package com.kola.backend.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("SELECT t FROM Token t WHERE t.token = :token AND t.tokenType = :type AND t.validatedAt IS NULL AND t.expiresAt > :now")
    Optional<Token> findValidToken(@Param("token") String token, @Param("type") TokenType type, @Param("now") LocalDateTime now);
}