package com.kola.backend.modules.scoring.service;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.util.PhoneNumbers;
import com.kola.backend.config.AuthProperties;
import com.kola.backend.config.DevUserSeedProperties;
import com.kola.backend.config.ScoringProperties;
import com.kola.backend.modules.auth.security.ActorPrincipal;
import com.kola.backend.modules.kyc.dto.KycDocumentResponse;
import com.kola.backend.modules.kyc.dto.ReviewDocumentRequest;
import com.kola.backend.modules.kyc.entity.KycDocumentType;
import com.kola.backend.modules.kyc.service.KycService;
import com.kola.backend.modules.scoring.entity.CreditScore;
import com.kola.backend.modules.scoring.repository.CreditScoreRepository;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import com.kola.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Keeps the dev test client ({@link DevUserSeedProperties}) past every credit gate of
 * {@code CreditService.checkEligibility}, so a loan can be tested without weeks of real activity.
 *
 * <p>Each gate is reached through the path a real user takes, not by writing the outcome:
 * <ul>
 *   <li><b>TIER_2</b> — an identity document is submitted and approved through {@link KycService},
 *       which derives the tier. Setting {@code kycTier} directly would be undone by the next
 *       recompute, since the tier is derived, never assigned.</li>
 *   <li><b>Collateral</b> — a cash-in then a savings deposit through {@link TransactionService},
 *       so the ledger explains every franc on the savings wallet.</li>
 *   <li><b>Score</b> — the only shortcut. The published score is a moving average over 30 days of
 *       activity that cannot be backdated, so a score row is written directly. The nightly
 *       rescoring will pull it back down; the next start restores it.</li>
 * </ul>
 *
 * <p>Runs on every start and only tops up what is missing. Lives in the scoring module because the
 * score row is the one thing it writes itself; everything else goes through the owning service.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class DevCreditReadinessSeeder implements ApplicationRunner {

    private static final Currency CURRENCY = Currency.XOF;

    /** Stable id so every seeded approval traces back to the same, clearly-named actor. */
    private static final ActorPrincipal SEEDER = new ActorPrincipal() {
        private final UUID id = UUID.nameUUIDFromBytes("kola-dev-seeder".getBytes(StandardCharsets.UTF_8));

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public String displayName() {
            return "Seeder de développement";
        }
    };

    private final DevUserSeedProperties properties;
    private final AuthProperties authProperties;
    private final ScoringProperties scoringProperties;
    private final UserService userService;
    private final KycService kycService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final CreditScoreRepository creditScoreRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.isCreditReady()) {
            return;
        }
        String phone = PhoneNumbers.normalize(properties.getPhone(), authProperties.getDefaultCallingCode());
        User user = userService.findByPhone(phone).orElse(null);
        if (user == null) {
            return;
        }

        ensureIdentityVerified(user);
        ensureCollateral(user.getId());
        ensureScore(user.getId());

        log.warn("Dev test client {} is credit-ready: TIER_2, {} {} in savings, score >= {}.",
                phone, properties.getSavingsBalance().toPlainString(), CURRENCY, properties.getCreditScore());
    }

    private void ensureIdentityVerified(User user) {
        if (user.getKycTier().isAtLeast(KycTier.TIER_2)) {
            return;
        }
        KycDocumentResponse document = kycService.submitDocument(
                user.getId(), KycDocumentType.NATIONAL_ID, placeholderDocument());
        kycService.review(document.id(), SEEDER, new ReviewDocumentRequest(true, null));
    }

    private void ensureCollateral(UUID userId) {
        BigDecimal missing = properties.getSavingsBalance()
                .subtract(walletService.getSavingsWallet(userId, CURRENCY).getTotalBalance());
        if (missing.signum() <= 0) {
            return;
        }
        transactionService.cashIn(userId, CURRENCY, missing);
        transactionService.depositToSavings(userId, CURRENCY, missing);
    }

    private void ensureScore(UUID userId) {
        int target = properties.getCreditScore();
        boolean highEnough = creditScoreRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(score -> score.getScoreValue() >= target)
                .orElse(false);
        if (highEnough) {
            return;
        }
        // Axes scaled from the configured weights (30/25/20/15/10) so the breakdown adds up to the
        // published value, as it does for a computed score.
        int savings = target * 30 / 100;
        int stability = target * 25 / 100;
        int inflow = target * 20 / 100;
        int usage = target * 15 / 100;
        int history = target - savings - stability - inflow - usage;

        creditScoreRepository.save(CreditScore.builder()
                .userId(userId)
                .scoreValue(target)
                .rawScoreValue(target)
                .kycTier(userService.getById(userId).getKycTier())
                .savingsDisciplinePoints(savings)
                .financialStabilityPoints(stability)
                .inflowRegularityPoints(inflow)
                .usageIntensityPoints(usage)
                .creditHistoryPoints(history)
                .windowDays(scoringProperties.getWindowDays())
                .build());
    }

    /** A minimal PDF: the upload path validates type and size, and a reviewer may open it. */
    private static MultipartFile placeholderDocument() {
        byte[] content = "%PDF-1.4\n% Piece d'identite fictive - seeder de developpement Kola\n%%EOF\n"
                .getBytes(StandardCharsets.US_ASCII);
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return "cni-dev.pdf";
            }

            @Override
            public String getContentType() {
                return "application/pdf";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return content.length;
            }

            @Override
            public byte[] getBytes() {
                return content.clone();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(content);
            }

            @Override
            public void transferTo(File dest) throws IOException {
                Files.write(dest.toPath(), content);
            }
        };
    }
}
