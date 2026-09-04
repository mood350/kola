package com.dogaa.backend.modules.auth.service;

import com.dogaa.backend.common.enums.UserStatus;
import com.dogaa.backend.common.util.PhoneNumbers;
import com.dogaa.backend.config.AuthProperties;
import com.dogaa.backend.exception.AccountLockedException;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.UnauthorizedException;
import com.dogaa.backend.modules.auth.dto.AuthResponse;
import com.dogaa.backend.modules.auth.dto.OtpRequestedResponse;
import com.dogaa.backend.modules.auth.dto.OtpVerifiedResponse;
import com.dogaa.backend.modules.auth.dto.RequestOtpRequest;
import com.dogaa.backend.modules.auth.dto.VerifyOtpRequest;
import com.dogaa.backend.modules.auth.dto.ChangePinRequest;
import com.dogaa.backend.modules.auth.dto.LoginRequest;
import com.dogaa.backend.modules.auth.dto.RegisterRequest;
import com.dogaa.backend.modules.auth.entity.OtpPurpose;
import com.dogaa.backend.modules.auth.entity.RefreshToken;
import com.dogaa.backend.modules.auth.security.JwtService;
import com.dogaa.backend.modules.auth.security.PinPolicy;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.event.UserRegisteredEvent;
import com.dogaa.backend.modules.user.mapper.UserMapper;
import com.dogaa.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Phone + PIN authentication.
 *
 * <p>The identifier is the phone number in E.164 and the secret is a numeric PIN hashed with
 * BCrypt. Because a PIN has a tiny key space, wrong attempts are counted and the account is
 * locked for a cool-down period: that lockout, not the PIN length, is what makes the scheme
 * usable for a wallet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final int MINIMUM_AGE_YEARS = 18;

    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Step 1: send a one-time code to the number. Refuses numbers that already have an account,
     * so the flow cannot be used to text arbitrary people.
     */
    @Transactional
    public OtpRequestedResponse requestRegistrationOtp(RequestOtpRequest request) {
        String phone = PhoneNumbers.normalize(request.phone(), authProperties.getDefaultCallingCode());

        if (userService.phoneExists(phone)) {
            throw new ConflictException("An account already exists for this phone number");
        }

        Instant resendAvailableAt = otpService.requestCode(phone, OtpPurpose.REGISTRATION);
        return new OtpRequestedResponse(PhoneNumbers.mask(phone),
                otpService.codeTtl().toSeconds(), resendAvailableAt);
    }

    /** Step 2: check the code and hand back the single-use proof needed by {@link #register}. */
    public OtpVerifiedResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        String phone = PhoneNumbers.normalize(request.phone(), authProperties.getDefaultCallingCode());
        String token = otpService.verify(phone, request.code(), OtpPurpose.REGISTRATION);
        return new OtpVerifiedResponse(token, otpService.verificationTokenTtl().toSeconds());
    }

    /**
     * Step 3: create the account and its PIN.
     *
     * <p>The phone number is taken from the verification token, never from the payload. Without a
     * token issued against a correct OTP there is no number to register at all, so this step is
     * unreachable — that is what makes the OTP mandatory rather than merely expected.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent, String ip) {
        String phone = otpService.consumeVerificationToken(
                request.verificationToken(), OtpPurpose.REGISTRATION);

        if (!request.pin().equals(request.confirmPin())) {
            throw new BadRequestException("PIN confirmation does not match");
        }
        PinPolicy.validate(request.pin());
        requireAdult(request.dateOfBirth());

        if (userService.phoneExists(phone)) {
            throw new ConflictException("An account already exists for this phone number");
        }
        String email = request.email() == null || request.email().isBlank()
                ? null
                : request.email().trim().toLowerCase();
        if (email != null && userService.emailExists(email)) {
            throw new ConflictException("An account already exists for this email address");
        }

        User user = userService.save(User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(phone)
                .email(email)
                .dateOfBirth(request.dateOfBirth())
                .address(request.address())
                .city(request.city())
                .country(request.country())
                .pinHash(passwordEncoder.encode(request.pin()))
                // The number was proved in step 2; this is what TIER_0 means (DOGAA.md 4.4).
                .phoneVerified(true)
                // Bean validation already rejects a false/missing value; recorded for the
                // compliance trail (DOGAA.md does not cover this, added on request).
                .privacyPolicyAcceptedAt(Instant.now())
                .build());

        log.info("Registered user {} ({})", user.getId(), PhoneNumbers.mask(phone));
        // The wallet module listens and provisions the current and savings accounts.
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));

        return issueTokens(user, userAgent, ip);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ip) {
        String phone = PhoneNumbers.normalize(request.phone(), authProperties.getDefaultCallingCode());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(phone, request.pin()));
        } catch (LockedException ex) {
            throw new AccountLockedException(lockedUntil(phone));
        } catch (DisabledException ex) {
            throw inactiveAccount(phone);
        } catch (BadCredentialsException ex) {
            // Also raised for an unknown phone number: never say which of the two it was.
            throw userService.findByPhone(phone)
                    .map(user -> rejectWrongPin(user, "Invalid phone number or PIN"))
                    .orElseGet(() -> new UnauthorizedException("INVALID_CREDENTIALS",
                            "Invalid phone number or PIN"));
        }

        User user = userService.findByPhone(phone)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS",
                        "Invalid phone number or PIN"));
        user.setFailedPinAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userService.save(user);

        return issueTokens(user, userAgent, ip);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String userAgent, String ip) {
        RefreshToken stored = refreshTokenService.verify(refreshToken);
        User user = userService.getById(stored.getUserId());

        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new UnauthorizedException("ACCOUNT_INACTIVE", "This account is no longer active");
        }

        String rotated = refreshTokenService.rotate(stored, userAgent, ip);
        return AuthResponse.of(jwtService.generateAccessToken(user), rotated,
                jwtService.accessTokenTtlSeconds(), userMapper.toResponse(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public void logoutEverywhere(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    @Transactional
    public void changePin(UUID userId, ChangePinRequest request) {
        User user = userService.getById(userId);

        if (!passwordEncoder.matches(request.currentPin(), user.getPinHash())) {
            throw rejectWrongPin(user, "Current PIN is incorrect");
        }
        if (!request.newPin().equals(request.confirmPin())) {
            throw new BadRequestException("PIN confirmation does not match");
        }
        if (request.newPin().equals(request.currentPin())) {
            throw new BadRequestException("New PIN must differ from the current one");
        }
        PinPolicy.validate(request.newPin());

        user.setPinHash(passwordEncoder.encode(request.newPin()));
        user.setFailedPinAttempts(0);
        user.setLockedUntil(null);
        userService.save(user);

        // A PIN change invalidates every session: if the old PIN leaked, the sessions did too.
        refreshTokenService.revokeAllForUser(userId);
        log.info("PIN changed for user {}", userId);
    }

    private Instant lockedUntil(String phone) {
        return userService.findByPhone(phone)
                .map(User::getLockedUntil)
                .orElse(Instant.now().plus(authProperties.getLockDuration()));
    }

    private UnauthorizedException inactiveAccount(String phone) {
        UserStatus status = userService.findByPhone(phone)
                .map(User::getStatus)
                .orElse(UserStatus.CLOSED);
        return status == UserStatus.SUSPENDED
                ? new UnauthorizedException("ACCOUNT_SUSPENDED", "This account is suspended")
                : new UnauthorizedException("ACCOUNT_CLOSED", "This account has been closed");
    }

    private AuthResponse issueTokens(User user, String userAgent, String ip) {
        return AuthResponse.of(
                jwtService.generateAccessToken(user),
                refreshTokenService.issue(user.getId(), userAgent, ip),
                jwtService.accessTokenTtlSeconds(),
                userMapper.toResponse(user));
    }

    /**
     * Records the wrong PIN, then throws: either the generic credentials error, or the lockout
     * error when this attempt was the one that tripped the ceiling.
     */
    private RuntimeException rejectWrongPin(User user, String message) {
        User updated = userService.registerFailedPinAttempt(user.getId(),
                authProperties.getMaxPinAttempts(), authProperties.getLockDuration());

        if (updated.isLocked()) {
            log.warn("Locked user {} after {} wrong PIN attempts",
                    user.getId(), authProperties.getMaxPinAttempts());
            return new AccountLockedException(updated.getLockedUntil());
        }
        return new UnauthorizedException("INVALID_CREDENTIALS", message);
    }

    private void requireAdult(LocalDate dateOfBirth) {
        if (ChronoUnit.YEARS.between(dateOfBirth, LocalDate.now()) < MINIMUM_AGE_YEARS) {
            throw new BadRequestException("You must be at least " + MINIMUM_AGE_YEARS + " years old");
        }
    }
}
