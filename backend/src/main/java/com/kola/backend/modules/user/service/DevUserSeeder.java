package com.kola.backend.modules.user.service;

import com.kola.backend.common.util.PhoneNumbers;
import com.kola.backend.config.AuthProperties;
import com.kola.backend.config.DevUserSeedProperties;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.event.UserRegisteredEvent;
import com.kola.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Creates one ready-to-use client account on an empty phone number, so a developer never has to
 * run the registration OTP flow just to get a JWT to test against.
 *
 * <p>Mirrors what {@code AuthenticationService.register} does after a successful OTP — same
 * {@code TIER_0}, same {@code phoneVerified = true}, same {@link UserRegisteredEvent} so the wallet
 * module still provisions the current and savings accounts — except the PIN is written directly
 * instead of going through {@code PinPolicy}, since a memorable dev PIN like "1234" is exactly what
 * that policy exists to reject for a real user.
 *
 * <p>Runs on every start but only acts once: it checks the phone number, not a row count, so it
 * still fires on a database that already has other users. <b>Turn it off in production</b> with
 * {@code app.dev.seed.enabled=false}.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DevUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DevUserSeedProperties properties;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String phone = PhoneNumbers.normalize(properties.getPhone(), authProperties.getDefaultCallingCode());
        if (userRepository.existsByPhone(phone)) {
            return;
        }

        User user = userRepository.save(User.builder()
                .firstName(properties.getFirstName())
                .lastName(properties.getLastName())
                .phone(phone)
                .pinHash(passwordEncoder.encode(properties.getPin()))
                .dateOfBirth(properties.getDateOfBirth())
                .address(properties.getAddress())
                .city(properties.getCity())
                .country(properties.getCountry())
                .phoneVerified(true)
                .privacyPolicyAcceptedAt(Instant.now())
                .build());

        // Same event AuthenticationService.register publishes: the wallet module needs it to
        // provision the current and savings accounts, otherwise this user has nothing to test with.
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));

        log.warn("""
                Seeded a dev test client: phone {} / PIN {} ({}).
                Sign in at POST /api/v1/auth/login. Set app.dev.seed.enabled=false before production.""",
                phone, properties.getPin(), user.getFullName());
    }
}
