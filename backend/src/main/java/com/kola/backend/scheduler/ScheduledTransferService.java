package com.kola.backend.scheduler;

import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.vault.VaultStatus;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    // CORRECTION 1 : On enlève @Transactional d'ici !
    public void processScheduledTransfers() {
        LocalDateTime currentDate = LocalDateTime.now();

        // CORRECTION REPO : On passe le vrai enum maintenant
        List<ScheduledTransfer> dueTransfers = scheduledRepository.findDueTransfers(ScheduledTransfer.ScheduledStatus.ACTIVE, currentDate);

        log.info("Execution des virements programmes : {} trouves", dueTransfers.size());

        for (ScheduledTransfer st : dueTransfers) {
            try {
                executeTransfer(st, currentDate);
                scheduleNextExecution(st);
            } catch (Exception e) {
                log.error("Echec du virement programme ID {} pour l'utilisateur {} : {}",
                        st.getId(), st.getOwner().getEmail(), e.getMessage());
            }
        }
    }

    // CORRECTION 1 : On met @Transactional ICI. Ainsi, si le virement de B échoue, seul B est annulé, A est préservé.
    @Transactional
    public void executeTransfer(ScheduledTransfer st, LocalDateTime executionDate) {
        // 1. Verrouiller le wallet
        Wallet wallet = walletRepository.findByIdForUpdate(st.getWallet().getId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        // 2. Verifier les fonds
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(st.getAmount()) < 0) {
            throw new InsufficientFundsException("Solde insuffisant pour le virement programme");
        }

        // 3. Débiter le wallet
        wallet.setBalance(wallet.getBalance().subtract(st.getAmount()));

        // CORRECTION 2 : Créditer le Vault si spécifié (L'argent ne doit pas disparaître !)
        if (st.getTargetVault() != null) {
            if (st.getTargetVault().getStatus() == VaultStatus.ACTIVE) {
                st.getTargetVault().setCurrentAmount(st.getTargetVault().getCurrentAmount().add(st.getAmount()));
                // Pas besoin de sauvegarder le vault, Hibernate le fait automatiquement car il est chargé dans la transaction
            } else {
                throw new IllegalStateException("Impossible d'alimenter un vault qui n'est plus actif.");
            }
        }

        // 4. Créer la trace transactionnelle
        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.SCHEDULED_TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .amount(st.getAmount())
                .fee(BigDecimal.ZERO)
                .currency(st.getCurrency())
                .wallet(wallet)
                .sender(st.getOwner())
                .description("VIREMENT PROGRAMME : " + st.getDescription())
                .build();
        transactionRepository.save(tx);

        // 5. Mettre à jour la date
        st.setLastExecutedAt(executionDate);
        scheduledRepository.save(st);
    }

    private void scheduleNextExecution(ScheduledTransfer st) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate;

        if (st.getFrequency() == ScheduledTransfer.Frequency.MONTHLY) {
            // CORRECTION 3A : Validation du jour du mois (max 31)
            int day = Math.min(st.getExecutionDay(), 31);
            if (day < 1) day = 1; // Sécurité supplémentaire

            nextDate = today.withDayOfMonth(day);

            if (!nextDate.isAfter(today)) {
                nextDate = nextDate.plusMonths(1);
            }
        } else { // WEEKLY
            // CORRECTION 3B : Validation stricte du jour de la semaine (1 à 7 uniquement)
            int dayOfWeek = st.getExecutionDay();
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw new IllegalArgumentException("Le jour d'exécution hebdomadaire doit être entre 1 (Lundi) et 7 (Dimanche).");
            }
            DayOfWeek targetDay = DayOfWeek.of(dayOfWeek);
            nextDate = today.with(targetDay);

            if (!nextDate.isAfter(today)) {
                nextDate = nextDate.plusWeeks(1);
            }
        }

        st.setNextExecutionDate(nextDate.atStartOfDay());
        scheduledRepository.save(st);
    }
}