package com.dogaa.backend.modules.vault.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Changes to a savings goal. A field left out is left alone.
 *
 * <p><b>The balance is not here, and must never be.</b> Money enters and leaves a vault only
 * through a deposit, a withdrawal or a scheduled payment — each of which writes a transaction. A
 * field that set the balance directly would create money with no trace of where it came from, and
 * the ledger would stop being able to explain itself.
 *
 * <p>The currency is absent for the same kind of reason: it is fixed by the wallet the vault is
 * locked against, and a vault holding 50 000 does not become a vault holding 50 000 of something
 * else because someone edited a field.
 *
 * @param clearTargetAmount true drops the target — {@code null} means "unchanged", so giving up on
 *                          a goal amount needs a word of its own
 * @param clearTargetDate   same, for the deadline
 */
public record UpdateVaultRequest(
        @Size(max = 80) String name,

        @Positive @Digits(integer = 17, fraction = 2) BigDecimal targetAmount,

        LocalDate targetDate,

        @Size(max = 160) String description,

        boolean clearTargetAmount,

        boolean clearTargetDate) {
}
