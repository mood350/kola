package com.kola.backend.modules.credit.service;

import com.kola.backend.config.CreditProperties;
import com.kola.backend.modules.credit.entity.CreditTierConfig;
import com.kola.backend.modules.credit.repository.CreditTierConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owner of the live leverage ladder (BACKEND.md 7).
 *
 * <p>{@link CreditPolicy} reads the ladder from {@link CreditProperties}, and this service is what
 * keeps that bean truthful: the saved version is loaded into it at startup and replaced on every
 * edit. Lending therefore uses whatever the back-office last approved, without the policy — the one
 * piece of pure arithmetic in the credit module — having to know a database exists.
 *
 * <p>An edit never overwrites: it appends a new version. The terms a loan was granted under stay
 * readable afterwards, which is the whole point of the "version historisée" the screen promises.
 * Version 1 is therefore the first back-office save, not the configured baseline — that one lives
 * in the configuration file, and the audit entry for the first save records what it replaced.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditLadderService implements ApplicationRunner {

    private final CreditTierConfigRepository repository;
    private final CreditProperties properties;

    /**
     * Adopts the last ladder the back-office saved, if there is one.
     *
     * <p>Nothing is written here on an empty table: until someone edits the ladder,
     * {@code app.credit.ladder} in the configuration file stays the source of truth and editing it
     * still works. Seeding a version at startup would silently take that away on the very first
     * boot — the file would keep being read and quietly overruled.
     */
    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        long latest = repository.findLatestVersionNumber().orElse(0L);

        if (latest == 0L) {
            log.info("No saved credit ladder; using the configured one");
            return;
        }

        properties.setLadder(repository.findByVersionNumberOrderByPositionAsc(latest).stream()
                .map(CreditLadderService::toRung)
                .toList());
        log.info("Credit ladder loaded from version {}", latest);
    }

    /** The ladder in force, best rung first. */
    public List<CreditProperties.Rung> current() {
        return properties.getLadder();
    }

    /**
     * Saves a new version and puts it into force immediately.
     *
     * @return the ladder now in force
     */
    @Transactional
    public List<CreditProperties.Rung> replace(List<CreditProperties.Rung> rungs, String authorName) {
        long next = repository.findLatestVersionNumber().orElse(0L) + 1;
        persist(rungs, authorName, next);
        properties.setLadder(List.copyOf(rungs));
        log.info("Credit ladder replaced by {} as version {}", authorName, next);
        return properties.getLadder();
    }

    private void persist(List<CreditProperties.Rung> rungs, String authorName, long versionNumber) {
        for (int position = 0; position < rungs.size(); position++) {
            CreditProperties.Rung rung = rungs.get(position);
            repository.save(CreditTierConfig.builder()
                    .versionNumber(versionNumber)
                    .position(position)
                    .minLoansRepaid(rung.getMinLoansRepaid())
                    .minScore(rung.getMinScore())
                    .leverage(rung.getLeverage())
                    .monthlyRatePercent(rung.getMonthlyRatePercent())
                    .maxAmount(rung.getMaxAmount())
                    .authorName(authorName)
                    .build());
        }
    }

    private static CreditProperties.Rung toRung(CreditTierConfig row) {
        return new CreditProperties.Rung(
                row.getMinLoansRepaid(),
                row.getMinScore(),
                row.getLeverage(),
                row.getMonthlyRatePercent(),
                row.getMaxAmount());
    }
}
