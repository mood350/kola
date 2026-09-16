package com.kola.backend.modules.qr.service;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.util.PhoneNumbers;
import com.kola.backend.config.QrProperties;
import com.kola.backend.exception.BadRequestException;
import com.kola.backend.exception.ConflictException;
import com.kola.backend.exception.ResourceNotFoundException;
import com.kola.backend.modules.qr.dto.CreatePaymentRequestRequest;
import com.kola.backend.modules.qr.dto.QrCodeResponse;
import com.kola.backend.modules.qr.dto.QrPaymentRequest;
import com.kola.backend.modules.qr.dto.ScannedQrResponse;
import com.kola.backend.modules.qr.entity.QrCode;
import com.kola.backend.modules.qr.entity.QrCodeStatus;
import com.kola.backend.modules.qr.entity.QrCodeType;
import com.kola.backend.modules.qr.mapper.QrCodeMapper;
import com.kola.backend.modules.qr.repository.QrCodeRepository;
import com.kola.backend.modules.transaction.dto.TransferRequest;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * QR codes for receiving money, so a payer scans instead of typing a number.
 *
 * <p>Two guarantees run through everything below.
 *
 * <ol>
 *   <li><b>A code never carries a phone number, only a random reference.</b> QR codes get printed,
 *       photographed and forwarded; a number encoded in one is given away permanently and cannot be
 *       taken back. A reference resolves only for a signed-in caller and can be revoked.</li>
 *   <li><b>Paying a code goes through {@code TransactionService.transfer}</b>, the same path as a
 *       typed transfer. Fees, KYC ceilings, wallet locks and the ledger entry are therefore
 *       identical. A QR is a way to address a payment, never a second kind of payment — a separate
 *       path here would be a way around the limits enforced there.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final QrProperties properties;
    private final QrCodeRepository repository;
    private final QrCodeMapper mapper;
    private final UserService userService;
    private final TransactionService transactionService;

    // --- the user's own codes ---------------------------------------------

    /**
     * The caller's permanent code, created on first use.
     *
     * <p>Get-or-create rather than an explicit "create my code" endpoint: a personal QR is an
     * attribute of the account, not something a user should have to know to ask for.
     */
    @Transactional
    public QrCodeResponse myStaticCode(UUID ownerId) {
        return mapper.toResponse(
                repository.findFirstByOwnerIdAndTypeAndStatus(
                                ownerId, QrCodeType.STATIC, QrCodeStatus.ACTIVE)
                        .orElseGet(() -> createStatic(ownerId)));
    }

    /**
     * Replaces the permanent code with a new one — for a user whose code ended up somewhere they
     * did not intend. The old one is revoked, so anything already printed stops resolving.
     */
    @Transactional
    public QrCodeResponse rotateStaticCode(UUID ownerId) {
        repository.findFirstByOwnerIdAndTypeAndStatus(ownerId, QrCodeType.STATIC, QrCodeStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(QrCodeStatus.REVOKED);
                    repository.save(existing);
                    log.info("Static QR code revoked for user {}", ownerId);
                });
        return mapper.toResponse(createStatic(ownerId));
    }

    private QrCode createStatic(UUID ownerId) {
        QrCode qr = new QrCode();
        qr.setCode(newCode());
        qr.setOwnerId(ownerId);
        qr.setType(QrCodeType.STATIC);
        qr.setStatus(QrCodeStatus.ACTIVE);
        return repository.save(qr);
    }

    /** Opens a claim for a precise amount, payable to the caller and to nobody else. */
    @Transactional
    public QrCodeResponse createPaymentRequest(UUID ownerId, CreatePaymentRequestRequest request) {
        QrCode qr = new QrCode();
        qr.setCode(newCode());
        qr.setOwnerId(ownerId);
        qr.setType(QrCodeType.PAYMENT_REQUEST);
        qr.setStatus(QrCodeStatus.ACTIVE);
        qr.setAmount(request.amount());
        qr.setCurrency(request.currency());
        qr.setLabel(request.label());
        qr.setExpiresAt(Instant.now().plus(resolveTtl(request.expiresInMinutes())));

        log.info("Payment request QR created by {} for {} {}",
                ownerId, request.amount(), request.currency());
        return mapper.toResponse(repository.save(qr));
    }

    private Duration resolveTtl(Integer minutes) {
        if (minutes == null) {
            return properties.getDefaultRequestTtl();
        }
        if (minutes < 1) {
            throw new BadRequestException("La durée de validité doit être d'au moins une minute");
        }
        Duration requested = Duration.ofMinutes(minutes);
        if (requested.compareTo(properties.getMaxRequestTtl()) > 0) {
            throw new BadRequestException("La durée de validité ne peut pas dépasser "
                    + properties.getMaxRequestTtl().toHours() + " heures");
        }
        return requested;
    }

    @Transactional(readOnly = true)
    public List<QrCodeResponse> myPaymentRequests(UUID ownerId) {
        return repository
                .findByOwnerIdAndTypeOrderByCreatedAtDesc(ownerId, QrCodeType.PAYMENT_REQUEST)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** Cancels one of the caller's codes. Someone else's reads as missing, not as forbidden. */
    @Transactional
    public QrCodeResponse revoke(UUID ownerId, String code) {
        QrCode qr = repository.findByCodeAndOwnerId(code, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ce QR code est introuvable"));

        if (qr.getStatus() == QrCodeStatus.USED) {
            throw new ConflictException("Ce QR code a déjà été payé");
        }
        qr.setStatus(QrCodeStatus.REVOKED);
        return mapper.toResponse(repository.save(qr));
    }

    /** The PNG, for sharing or printing. Owner only — a payer renders the payload themselves. */
    @Transactional(readOnly = true)
    public QrCode requireOwned(UUID ownerId, String code) {
        return repository.findByCodeAndOwnerId(code, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ce QR code est introuvable"));
    }

    // --- scanning ---------------------------------------------------------

    /**
     * What the payer sees after scanning, before confirming.
     *
     * <p>An unusable code still answers 200 with {@code payable=false} and a reason. The app can
     * then say "ce code a expiré" instead of showing a generic error on a screen where the user is
     * standing in front of a merchant.
     */
    @Transactional(readOnly = true)
    public ScannedQrResponse scan(UUID scannerId, String code) {
        QrCode qr = repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ce QR code est inconnu"));

        User owner = userService.getById(qr.getOwnerId());
        String reason = unpayableReason(qr, scannerId);

        return new ScannedQrResponse(
                qr.getCode(),
                qr.getType(),
                reason == null,
                reason,
                displayName(owner),
                PhoneNumbers.mask(owner.getPhone()),
                qr.getType() == QrCodeType.PAYMENT_REQUEST,
                qr.getAmount(),
                qr.getCurrency(),
                qr.getLabel(),
                qr.getExpiresAt());
    }

    private String unpayableReason(QrCode qr, UUID scannerId) {
        if (qr.getOwnerId().equals(scannerId)) {
            return "Ce QR code est le vôtre";
        }
        return switch (qr.getStatus()) {
            case REVOKED -> "Ce QR code a été annulé";
            case USED -> "Ce QR code a déjà été payé";
            case ACTIVE -> qr.isExpired(Instant.now()) ? "Ce QR code a expiré" : null;
        };
    }

    // --- paying -----------------------------------------------------------

    /**
     * Pays a scanned code.
     *
     * <p>A payment request is marked used inside this same transaction, before the money moves. Two
     * people scanning the same receipt at once therefore collide on the row's {@code @Version}: one
     * commits, the other rolls the whole payment back. Marking it afterwards would leave a window
     * where both transfers succeed and only the second flag fails.
     */
    @Transactional
    public QrCodeResponse pay(UUID payerId, String code, QrPaymentRequest request) {
        QrCode qr = repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ce QR code est inconnu"));

        String reason = unpayableReason(qr, payerId);
        if (reason != null) {
            throw new ConflictException(reason);
        }

        Currency currency = resolveCurrency(qr, request);
        BigDecimal amount = resolveAmount(qr, request);
        User owner = userService.getById(qr.getOwnerId());

        if (qr.getType() == QrCodeType.PAYMENT_REQUEST) {
            qr.setStatus(QrCodeStatus.USED);
            qr.setPaidAt(Instant.now());
            qr.setPaidByUserId(payerId);
        }

        // The ordinary transfer path: same fee, same KYC ceiling, same ledger entry.
        Transaction transaction = transactionService.transfer(payerId, new TransferRequest(
                currency, amount, owner.getPhone(), descriptionFor(qr, request)));

        qr.setTransactionReference(transaction.getReference());
        log.info("QR code {} paid by {} — transaction {}", code, payerId, transaction.getReference());
        return mapper.toResponse(repository.save(qr));
    }

    /**
     * A value that contradicts the code is refused rather than ignored: a payer who typed one
     * number and was charged another has been lied to, even when the difference favours them.
     */
    private BigDecimal resolveAmount(QrCode qr, QrPaymentRequest request) {
        if (qr.getType() == QrCodeType.STATIC) {
            if (request.amount() == null) {
                throw new BadRequestException("Ce QR code ne fixe pas de montant : précisez-le");
            }
            return request.amount();
        }
        if (request.amount() != null && request.amount().compareTo(qr.getAmount()) != 0) {
            throw new BadRequestException(
                    "Ce QR code demande un montant fixe de " + qr.getAmount() + " " + qr.getCurrency());
        }
        return qr.getAmount();
    }

    private Currency resolveCurrency(QrCode qr, QrPaymentRequest request) {
        if (qr.getType() == QrCodeType.STATIC) {
            if (request.currency() == null) {
                throw new BadRequestException("Ce QR code ne fixe pas de devise : précisez-la");
            }
            return request.currency();
        }
        if (request.currency() != null && request.currency() != qr.getCurrency()) {
            throw new BadRequestException("Ce QR code est libellé en " + qr.getCurrency());
        }
        return qr.getCurrency();
    }

    private static String descriptionFor(QrCode qr, QrPaymentRequest request) {
        if (request.description() != null && !request.description().isBlank()) {
            return request.description();
        }
        return qr.getLabel() != null ? qr.getLabel() : "Paiement par QR code";
    }

    // --- plumbing ---------------------------------------------------------

    /**
     * 128 bits of randomness, URL-safe. A guessable code would let anyone walk the space and
     * resolve strangers' names and masked numbers; the collision retry is belt and braces.
     */
    private String newCode() {
        byte[] bytes = new byte[properties.getCodeBytes()];
        for (int attempt = 0; attempt < 5; attempt++) {
            RANDOM.nextBytes(bytes);
            String candidate = ENCODER.encodeToString(bytes);
            if (!repository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Impossible de générer un code QR unique");
    }

    private static String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().strip();
        String last = user.getLastName() == null ? "" : user.getLastName().strip();
        String full = (first + " " + last).strip();
        return full.isEmpty() ? PhoneNumbers.mask(user.getPhone()) : full;
    }
}
