package com.dogaa.backend.exception;

import com.dogaa.backend.common.enums.KycTier;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * A ceiling of the user's KYC tier would be crossed. The message names the tier and the limit so
 * the app can invite the user to upgrade rather than just refusing.
 */
@Getter
public class KycLimitExceededException extends ApiException {

    private final KycTier tier;
    private final String ceiling;
    private final BigDecimal limit;

    public KycLimitExceededException(KycTier tier, String ceiling, BigDecimal limit) {
        super(HttpStatus.FORBIDDEN, "KYC_LIMIT_EXCEEDED",
                "This operation exceeds your " + ceiling + " limit of " + limit.toPlainString()
                        + " XOF for " + tier + ". Upgrade your verification level to raise it.");
        this.tier = tier;
        this.ceiling = ceiling;
        this.limit = limit;
    }
}
