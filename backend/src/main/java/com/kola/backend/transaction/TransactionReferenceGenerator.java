package com.kola.backend.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Génère des références de transaction uniques au format KLA-YYYY-XXXXXXXX.
 * Extrait en composant partagé pour éviter la duplication entre
 * TransactionService et VaultService (toute création de Transaction passe
 * par ici).
 */
@Component
@RequiredArgsConstructor
public class TransactionReferenceGenerator {

    private final TransactionRepository transactionRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        int year = LocalDate.now().getYear();
        String reference = build(year);

        // Re-tirage en cas de collision (très improbable : 1/10^8) plutôt
        // que de compter uniquement sur la contrainte unique en BDD.
        while (transactionRepository.findByReference(reference).isPresent()) {
            reference = build(year);
        }
        return reference;
    }

    private String build(int year) {
        String suffix = String.format("%08d", RANDOM.nextInt(100_000_000));
        return "KLA-" + year + "-" + suffix;
    }
}
