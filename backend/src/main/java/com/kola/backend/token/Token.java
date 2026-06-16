package com.kola.backend.token;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    Token.java                               ║
 * ║     Entité représentant un token de confirmation/reset      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Un token est un code à 6 chiffres + un lien UUID.
 * Il est lié à un utilisateur et a une date d'expiration.
 *
 * POURQUOI les deux (code + lien) ?
 *  - Lien  → expérience fluide sur desktop (clic direct)
 *  - Code  → expérience mobile (copier-coller le code)
 *  C'est le standard bancaire (ex: Revolut, N26)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Token extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Code OTP à 6 chiffres ex: "847291"
    @Column(unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    // Date d'expiration du token
    private LocalDateTime expiresAt;

    // Date de validation (null si pas encore validé)
    private LocalDateTime validatedAt;

    // L'utilisateur auquel appartient ce token
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}