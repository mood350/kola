package com.dogaa.backend.modules.user.service;

import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.user.dto.UpdateProfileRequest;
import com.dogaa.backend.modules.user.dto.UserResponse;
import com.dogaa.backend.modules.user.dto.RecipientLookupResponse;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.event.UserProfileUpdatedEvent;
import com.dogaa.backend.modules.user.mapper.UserMapper;
import com.dogaa.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the {@link User} aggregate. Other modules (auth, wallet, kyc, ...) must go through
 * this service rather than touching {@code UserRepository} directly.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Transactional(readOnly = true)
    public boolean phoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID id) {
        return userMapper.toResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public RecipientLookupResponse lookupRecipient(String phone) {
        User user = findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte DOGAA associé à ce numéro"));
        String masked = phone.length() >= 4
                ? phone.substring(0, Math.max(0, phone.length() - 4)).replaceAll("\\d", "•") + phone.substring(phone.length() - 4)
                : phone;
        return new RecipientLookupResponse(user.getId(), user.getFirstName() + " " + user.getLastName(), masked);
    }

    @Transactional(readOnly = true)
    public List<UUID> findActiveUserIds() {
        return userRepository.findActiveIds();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    // --- Admin aggregates (DOGAA.md 4.5) --------------------------

    /** Total accounts. Read-only metric for dogaa-admin. */
    @Transactional(readOnly = true)
    public long totalUsers() {
        return userRepository.count();
    }

    /** New accounts since a point in time — "croissance des utilisateurs". */
    @Transactional(readOnly = true)
    public long newUsersSince(Instant since) {
        return userRepository.countByCreatedAtAfter(since);
    }

    /**
     * Counts a wrong PIN and locks the account once the ceiling is reached.
     *
     * <p>Runs in its own transaction on purpose: the caller aborts by throwing, and a failed
     * login must still leave its trace behind. Without {@code REQUIRES_NEW} the rollback would
     * erase the counter and the lockout would never trigger.
     *
     * @return the persisted state after the attempt was recorded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User registerFailedPinAttempt(UUID userId, int maxAttempts, Duration lockDuration) {
        User user = getById(userId);
        int attempts = user.getFailedPinAttempts() + 1;
        if (attempts >= maxAttempts) {
            user.setFailedPinAttempts(0);
            user.setLockedUntil(Instant.now().plus(lockDuration));
        } else {
            user.setFailedPinAttempts(attempts);
        }
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearPinAttempts(UUID userId) {
        User user = getById(userId);
        user.setFailedPinAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = getById(id);
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.email() != null) {
            String email = request.email().trim().toLowerCase();
            if (!email.equalsIgnoreCase(user.getEmail())) {
                user.setEmail(email);
                // A new address has to be verified again before it can raise the KYC tier.
                user.setEmailVerified(false);
            }
        }
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.address() != null) {
            user.setAddress(request.address().trim());
        }
        if (request.city() != null) {
            user.setCity(request.city().trim());
        }
        if (request.country() != null) {
            user.setCountry(request.country().toUpperCase());
        }
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        // Address, city and country decide KYC tier 1, so a profile edit can move the tier.
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(id));
        return response;
    }
}
