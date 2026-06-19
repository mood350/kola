package com.kola.backend.scheduler;

import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
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

    @Transactional
    public void processScheduledTransfers() {
        LocalDateTime currentDate = LocalDateTime.now();
        List<ScheduledTransfer> dueTransfers = scheduledRepository.findDueTransfers(currentDate);

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

    private void executeTransfer(ScheduledTransfer st, LocalDateTime executionDate) {
        // 1. Verrouiller le wallet (Pessimistic Write)
        Wallet wallet = walletRepository.findByIdForUpdate(st.getWallet().getId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        // 2. Verifier les fonds (On ne prend pas l'argent bloque dans les vaults)
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(st.getAmount()) < 0) {
            log.warn("Fonds insuffisants pour le virement programme de {} pour le user {}",
                    st.getAmount(), st.getOwner().getEmail());
            throw new InsufficientFundsException("Solde insuffisant pour le virement programme");
        }

        // 3. Executer le mouvement (Ici on suppose qu'on alimente un Vault)
        if (st.getTargetVault() != null) {
            // logique simplifiee : on debite le wallet
            wallet.setBalance(wallet.getBalance().subtract(st.getAmount()));
        }

        // 4. Creer la trace transactionnelle
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

        // 5. Mettre a jour la date de derniere execution
        st.setLastExecutedAt(executionDate);
        scheduledRepository.save(st);
    }

    private void scheduleNextExecution(ScheduledTransfer st) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate;

        if (st.getFrequency() == ScheduledTransfer.Frequency.MONTHLY) {
            // Construire la date cible (ex: le 5 du mois courant)
            nextDate = today.withDayOfMonth(st.getExecutionDay());

            // Si le jour cible est deja passe ce mois-ci, on passe au mois prochain
            if (!nextDate.isAfter(today)) {
                nextDate = nextDate.plusMonths(1);
            }
        } else { // WEEKLY
            // Convertir l'entier (1 = Lundi, 7 = Dimanche) en enum Java
            DayOfWeek targetDay = DayOfWeek.of(st.getExecutionDay());
            nextDate = today.with(targetDay);

            // Si le jour cible est deja passe cette semaine, on passe a la semaine prochaine
            if (!nextDate.isAfter(today)) {
                nextDate = nextDate.plusWeeks(1);
            }
        }

        // Se declenche a minuit
        st.setNextExecutionDate(nextDate.atStartOfDay());
        scheduledRepository.save(st);
    }
}