package com.kola.backend.modules.dispute.entity;

import com.kola.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One administrator signing off on a chargeback.
 *
 * <p>The unique constraint is the whole point: <b>the database refuses a second row for the same
 * administrator on the same dispute</b>. A counter can be incremented twice by one person; a
 * unique index cannot, not even under a race between two simultaneous requests. Four-eyes control
 * has to be enforced by something that cannot be talked out of it.
 */
@Entity
@Table(name = "dispute_validations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dispute_validation_admin",
                columnNames = {"dispute_id", "admin_id"}))
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

    /** Kept alongside the id so the note stays readable if the account is later renamed. */
    @Column(name = "admin_name", nullable = false, length = 120)
    private String adminName;
}
