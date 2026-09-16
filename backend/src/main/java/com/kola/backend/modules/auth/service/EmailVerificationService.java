package com.kola.backend.modules.auth.service;

import com.kola.backend.exception.BadRequestException;
import com.kola.backend.exception.ConflictException;
import com.kola.backend.modules.auth.entity.OtpChannel;
import com.kola.backend.modules.auth.entity.OtpPurpose;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Optional confirmation of the email address on a profile.
 *
 * <p>Email is a convenience for receipts and notifications, nothing more: it grants no KYC tier and
 * unlocks no ceiling. That is deliberate — a wallet aimed at users who have a phone and no mailbox
 * cannot make an address the price of entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserService userService;
    private final OtpService otpService;

    /** @return when another code may be requested */
    @Transactional
    public Instant requestCode(UUID userId) {
        User user = userService.getById(userId);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException("Add an email address to your profile first");
        }
        if (user.isEmailVerified()) {
            throw new ConflictException("This email address is already verified");
        }
        return otpService.requestCode(user.getPhone(), OtpPurpose.EMAIL_VERIFICATION,
                OtpChannel.EMAIL, user.getEmail());
    }

    @Transactional
    public void confirm(UUID userId, String code) {
        User user = userService.getById(userId);

        if (user.isEmailVerified()) {
            throw new ConflictException("This email address is already verified");
        }
        // Throws when the code is wrong. The token it returns is not needed here: the act of
        // answering the challenge is the whole point.
        otpService.verify(user.getPhone(), code, OtpPurpose.EMAIL_VERIFICATION);

        user.setEmailVerified(true);
        userService.save(user);
        log.info("Email verified for user {}", userId);
    }
}
