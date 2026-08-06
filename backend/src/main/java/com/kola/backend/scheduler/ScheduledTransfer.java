package com.kola.backend.scheduler;

import com.kola.backend.user.User;
import com.kola.backend.vault.Vault;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scheduled_transfers", indexes = {
        // findDueTransfers, appelé à chaque réveil du job
        @Index(name = "idx_sched_status_next", columnList = "status, nextExecutionDate"),
        @Index(name = "idx_sched_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTransfer extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(nullable = false)
    private int executionDay;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScheduledStatus status = ScheduledStatus.ACTIVE;

    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecutionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_vault_id")
    private Vault targetVault;

    public enum Frequency {
        MONTHLY, WEEKLY
    }

    public enum ScheduledStatus {
        ACTIVE, PAUSED, FAILED_PERMANENTLY
    }
}