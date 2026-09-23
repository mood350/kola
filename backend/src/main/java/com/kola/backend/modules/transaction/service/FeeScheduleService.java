package com.kola.backend.modules.transaction.service;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.entity.FeeScheduleEntry;
import com.kola.backend.modules.transaction.repository.FeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The fee grid in force (BACKEND.md 10).
 *
 * <p>Holds the last grid the back-office saved and hands it to {@link FeeCalculator}. Until someone
 * saves one, this service reports nothing and the calculator keeps using the configured base rates
 * — so {@code app.fees.*} stays meaningful on a fresh install instead of being silently overruled
 * by a seeded copy of itself.
 *
 * <p>The map is rebuilt on save and on startup, and read on every fee quote: charging a rate is far
 * more frequent than changing one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeScheduleService implements ApplicationRunner {

    private final FeeScheduleRepository repository;

    /** Empty until a grid is saved; {@code null} rates never reach the calculator. */
    private volatile Map<KycTier, FeeScheduleEntry> inForce = new EnumMap<>(KycTier.class);

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        long latest = repository.findLatestVersionNumber().orElse(0L);
        if (latest == 0L) {
            log.info("No saved fee grid; using the configured base rates");
            return;
        }
        inForce = index(repository.findByVersionNumber(latest));
        log.info("Fee grid loaded from version {}", latest);
    }

    /** The saved percentage for this tier and type, or empty when no grid has been saved. */
    public Optional<BigDecimal> percentFor(TransactionType type, KycTier tier) {
        FeeScheduleEntry entry = inForce.get(tier);
        if (entry == null) {
            return Optional.empty();
        }
        return switch (type) {
            case P2P_TRANSFER -> Optional.of(entry.getP2pPercent());
            case MERCHANT_PAYMENT -> Optional.of(entry.getMerchantPercent());
            case CASH_OUT -> Optional.of(entry.getCashOutPercent());
            // Bill payments and cash-in are not on the back-office grid; they keep the base rate.
            default -> Optional.empty();
        };
    }

    /** The grid in force, or empty when the configured base rates still apply. */
    public Map<KycTier, FeeScheduleEntry> current() {
        return inForce;
    }

    @Transactional
    public void replace(List<FeeScheduleEntry> entries, String authorName) {
        long next = repository.findLatestVersionNumber().orElse(0L) + 1;
        entries.forEach(entry -> {
            entry.setVersionNumber(next);
            entry.setAuthorName(authorName);
            repository.save(entry);
        });
        inForce = index(entries);
        log.info("Fee grid replaced by {} as version {}", authorName, next);
    }

    private static Map<KycTier, FeeScheduleEntry> index(List<FeeScheduleEntry> entries) {
        Map<KycTier, FeeScheduleEntry> map = new EnumMap<>(KycTier.class);
        entries.forEach(entry -> map.put(entry.getTier(), entry));
        return map;
    }
}
