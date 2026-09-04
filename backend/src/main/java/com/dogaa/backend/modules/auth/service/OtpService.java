package com.dogaa.backend.modules.auth.service;

import com.dogaa.backend.common.util.PhoneNumbers;
import com.dogaa.backend.common.util.Tokens;
import com.dogaa.backend.config.OtpProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.TooManyRequestsException;
import com.dogaa.backend.modules.auth.entity.OtpChannel;
import com.dogaa.backend.modules.auth.entity.OtpCode;
import com.dogaa.backend.modules.auth.entity.OtpPurpose;
import com.dogaa.backend.modules.auth.repository.OtpCodeRepository;
import com.dogaa.backend.modules.notification.service.EmailOtpSender;
import com.dogaa.backend.modules.notification.service.OtpSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Issues and checks the one-time codes that gate phone ownership.
 *
 * <p>Note the absence of a class-level {@code @Transactional}: {@link #verify} records the failed
 * attempt and then throws. Inside a single transaction the rollback would erase the counter and the
 * attempt ceiling would never bite — the same trap as the PIN lockout. Each repository call commits
 * on its own instead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int VERIFICATION_TOKEN_BYTES = 48;

    private final OtpCodeRepository otpCodeRepository;
    private final OtpSender otpSender;
    private final EmailOtpSender emailOtpSender;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;

    /**
     * Sends a fresh code, invalidating any previous one for that number and purpose.
     *
     * @return when the caller may ask for another code
     */
    @Transactional
    public Instant requestCode(String phone, OtpPurpose purpose) {
        return requestCode(phone, purpose, OtpChannel.SMS, phone);
    }

    /**
     * Same challenge, delivered over the given channel. The row stays keyed by the account phone
     * number even when the code travels by email, so one cooldown and one attempt counter cover
     * the account rather than the address of the day.
     */
    @Transactional
    public Instant requestCode(String phone, OtpPurpose purpose, OtpChannel channel, String destination) {
        otpCodeRepository.findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, purpose)
                .filter(previous -> previous.getCreatedAt() != null)
                .ifPresent(previous -> {
                    Instant nextAllowed = previous.getCreatedAt().plus(otpProperties.getResendCooldown());
                    if (nextAllowed.isAfter(Instant.now())) {
                        throw new TooManyRequestsException(
                                "A code was already sent. Try again in "
                                        + Duration.between(Instant.now(), nextAllowed).toSeconds() + "s.");
                    }
                });

        otpCodeRepository.consumeAllForPhone(phone, purpose, Instant.now());

        String code = Tokens.numericCode(otpProperties.getCodeLength());
        otpCodeRepository.save(OtpCode.builder()
                .phone(phone)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(otpProperties.getTtl()))
                .channel(channel)
                .destination(destination)
                .build());

        deliver(channel, destination, code);
        log.info("OTP issued for {} ({})", PhoneNumbers.mask(phone), purpose);
        return Instant.now().plus(otpProperties.getResendCooldown());
    }

    /**
     * Checks the code and, on success, hands back a single-use token proving the number was verified.
     *
     * @return the clear-text verification token; only its hash is stored
     */
    public String verify(String phone, String code, OtpPurpose purpose) {
        OtpCode otp = otpCodeRepository
                .findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, purpose)
                .filter(OtpCode::isPending)
                .orElseThrow(() -> new BadRequestException(
                        "No pending code for this number. Request a new one."));

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= otpProperties.getMaxAttempts()) {
                otp.setConsumedAt(Instant.now());
                otpCodeRepository.save(otp);
                log.warn("Burned OTP for {} after {} wrong attempts",
                        PhoneNumbers.mask(phone), otp.getAttempts());
                throw new BadRequestException("Too many wrong codes. Request a new one.");
            }
            otpCodeRepository.save(otp);
            throw new BadRequestException("Invalid or expired code");
        }

        String token = Tokens.random(VERIFICATION_TOKEN_BYTES);
        otp.setVerifiedAt(Instant.now());
        otp.setTokenHash(Tokens.sha256(token));
        otp.setTokenExpiresAt(Instant.now().plus(otpProperties.getVerificationTokenTtl()));
        otpCodeRepository.save(otp);

        log.info("OTP verified for {} ({})", PhoneNumbers.mask(phone), purpose);
        return token;
    }

    /**
     * Spends a verification token and returns the phone number it was issued for.
     *
     * <p>This is the only way to learn that number: the caller cannot name it themselves, which is
     * what stops anyone from registering a number they never proved they own.
     */
    @Transactional
    public String consumeVerificationToken(String token, OtpPurpose purpose) {
        OtpCode otp = otpCodeRepository.findByTokenHash(Tokens.sha256(token))
                .filter(candidate -> candidate.getPurpose() == purpose)
                .filter(OtpCode::isTokenUsable)
                .orElseThrow(() -> new BadRequestException(
                        "Phone verification is missing or has expired. Start over."));

        otp.setConsumedAt(Instant.now());
        otpCodeRepository.save(otp);
        return otp.getPhone();
    }

    private void deliver(OtpChannel channel, String destination, String code) {
        switch (channel) {
            case SMS -> otpSender.sendOtp(destination, code, otpProperties.getTtl());
            case EMAIL -> emailOtpSender.sendOtp(destination, code, otpProperties.getTtl());
        }
    }

    public Duration codeTtl() {
        return otpProperties.getTtl();
    }

    public Duration verificationTokenTtl() {
        return otpProperties.getVerificationTokenTtl();
    }
}
