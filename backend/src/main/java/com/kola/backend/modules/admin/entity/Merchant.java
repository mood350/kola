package com.kola.backend.modules.admin.entity;

import com.kola.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A partner merchant (KOLA.md 4.1, BACKEND.md 10).
 *
 * <p>The register of who may be paid through Kola, and on what terms they were accepted. Payments
 * still address merchants by the counterparty code carried on the transaction; wiring that code to
 * this row is what would let a suspension actually block a payment, and it is not done yet.
 */
@Entity
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchant_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    /** Trade, as the console groups them: "Alimentation", "Transport"… */
    @Column(nullable = false, length = 60)
    private String category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantStatus status = MerchantStatus.PENDING;
}
