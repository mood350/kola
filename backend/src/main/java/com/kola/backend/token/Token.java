package com.kola.backend.token;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "token", indexes = {
        // findValidToken cherche par (token, tokenType)
        @Index(name = "idx_token_value_type", columnList = "token, tokenType"),
        @Index(name = "idx_token_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Token extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CORRECTION 3 : Retrait de "unique = true".
    // Un code à 6 chiffres finira par se répéter dans l'historique.
    // L'unicité est gérée par la logique métier (un utilisateur a un seul token valide à la fois).
    @Column(length = 6)
    private String token;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private LocalDateTime expiresAt;

    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY) // Ajout du LAZY par défaut (bonne pratique)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}