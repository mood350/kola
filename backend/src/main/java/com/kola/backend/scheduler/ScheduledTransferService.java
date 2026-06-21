package com.kola.backend.scheduler;

import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.scheduler.ScheduledTransfer.ScheduledStatus;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.vault.Vault;
import com.kola.backend.vault.VaultRepository;
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
    private final VaultRepository vaultRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    public void processScheduledTransfers() {
        LocalDateTime currentDate = LocalDateTime.now();
        List<ScheduledTransfer> dueTransfers =
                scheduledRepository.findDueTransfers(ScheduledStatus.ACTIVE, currentDate);

        log.info("Execution des virements programmes : {} trouves", dueTransfers.size());

        for (ScheduledTransfer st : dueTransfers) {
            try {
                processOneScheduledTransfer(st, currentDate);
                // Succès : on planifie la prochaine échéance normale
                scheduleNextExecution(st);
            } catch (InsufficientFundsException e) {
                log.warn("Solde insuffisant pour le virement programme ID {} (utilisateur {}) : {}",
                        st.getId(), st.getOwner().getEmail(), e.getMessage());
                handleFailedTransfer(st, currentDate, e.getMessage());
            } catch (Exception e) {
                log.error("Echec du virement programme ID {} pour l'utilisateur {} : {}",
                        st.getId(), st.getOwner().getEmail(), e.getMessage());
                handleFailedTransfer(st, currentDate, e.getMessage());
            }
        }
    }

    @Transactional
    public void processOneScheduledTransfer(ScheduledTransfer st, LocalDateTime executionDate) {
        // 1. Verrouiller le wallet
        Wallet wallet = walletRepository.findByIdForUpdate(st.getWallet().getId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        // 2. Vérifier les fonds
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(st.getAmount()) < 0) {
            throw new InsufficientFundsException("Solde insuffisant pour le virement programme");
        }

        // 3. Débiter le wallet
        wallet.setBalance(wallet.getBalance().subtract(st.getAmount()));

        // 4. Créditer le Vault si spécifié
        if (st.getTargetVault() != null) {
            Vault vault = vaultRepository.findById(st.getTargetVault().getId())
                    .orElseThrow(() -> new RuntimeException("Vault introuvable"));

            if (vault.getStatus() != VaultStatus.ACTIVE) {
                throw new IllegalStateException("Impossible d'alimenter un vault inactif ou fermé.");
            }

            vault.setCurrentAmount(vault.getCurrentAmount().add(st.getAmount()));
        }

        // 5. Créer la trace transactionnelle
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

        // 6. Mettre à jour la date de dernière exécution
        st.setLastExecutedAt(executionDate);
        scheduledRepository.save(st);
    }

    /**
     * Enregistre une transaction en échec (fonds insuffisants ou autre erreur)
     * et replanifie la prochaine échéance pour éviter une boucle de réessai
     * à chaque exécution du scheduler.
     */
    @Transactional
    public void handleFailedTransfer(ScheduledTransfer st, LocalDateTime executionDate, String reason) {
        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.SCHEDULED_TRANSFER)
                .status(TransactionStatus.FAILED)
                .amount(st.getAmount())
                .fee(BigDecimal.ZERO)
                .currency(st.getCurrency())
                .wallet(st.getWallet())
                .sender(st.getOwner())
                .description("VIREMENT PROGRAMME ECHOUE : " + reason)
                .build();
        transactionRepository.save(tx);

        // On replanifie quand même la prochaine échéance pour ne pas retenter
        // en boucle avant la date normale suivante.
        scheduleNextExecution(st);
    }

    private void scheduleNextExecution(ScheduledTransfer st) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate;

        if (st.getFrequency() == ScheduledTransfer.Frequency.MONTHLY) {
            int targetDay = st.getExecutionDay();

            // Calcul sécurisé pour gérer les mois à 28, 29, 30 ou 31 jours
            LocalDate tentativeDate = today.withDayOfMonth(Math.min(targetDay, today.lengthOfMonth()));

            // Si la date est dépassée, on passe au mois suivant
            if (!tentativeDate.isAfter(today)) {
                LocalDate nextMonth = today.plusMonths(1);
                nextDate = nextMonth.withDayOfMonth(Math.min(targetDay, nextMonth.lengthOfMonth()));
            } else {
                nextDate = tentativeDate;
            }
        } else { // WEEKLY
            int dayOfWeek = st.getExecutionDay();

            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw new IllegalArgumentException("Le jour de la semaine doit être entre 1 (Lundi) et 7 (Dimanche).");
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