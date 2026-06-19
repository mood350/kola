package com.kola.backend.credit;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_scores", indexes = {
        // Index pour retrouver rapidement le dernier score d'un utilisateur
        @Index(name = "idx_credit_user_latest", columnList = "user_id, latest")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScore extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTier tier;

    @Column(columnDefinition = "TEXT")
    private String breakdownJson;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal monthlyRate;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean latest = true;
}