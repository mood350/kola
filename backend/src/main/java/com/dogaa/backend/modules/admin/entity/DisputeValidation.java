package com.dogaa.backend.modules.admin.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One administrator signing off a chargeback (BACKEND.md 9).
 *
 * <p>Rows, not a counter: a counter cannot answer "which two people approved this", and it cannot
 * stop one person approving twice. The unique constraint on (dispute, admin) is what makes the
 * four-eyes rule structural rather than a check someone can forget to write.
 */
@Entity
@Table(name = "dispute_validations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dispute_validation_admin", columnNames = {"dispute_id", "admin_id"}),
        indexes = @Index(name = "idx_dispute_validation_dispute", columnList = "dispute_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeValidation extends BaseEntity {

    @Column(name = "dispute_id", nullable = false)
    private UUID disputeId;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    /** Denormalised so the note stays readable after an account is renamed or removed. */
    @Column(name = "admin_name", nullable = false, length = 120)
    private String adminName;
}
