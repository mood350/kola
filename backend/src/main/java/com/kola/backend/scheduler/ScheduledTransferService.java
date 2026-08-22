package com.kola.backend.scheduler;

import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.scheduler.ScheduledTransfer.ScheduledStatus;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.User;
import com.kola.backend.vault.Vault;
import com.kola.backend.vault.VaultRepository;
import com.kola.backend.vault.VaultStatus;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletRepository;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final WalletService walletService;
    // Ouvre une transaction par virement dans le batch, sans dépendre du proxy
    // Spring (inopérant sur un appel interne). Bean fourni par
    // TransactionAutoConfiguration de Spring Boot.
    private final TransactionTemplate transactionTemplate;

    // ═══════════════════════════════════════════════════════════════
    //  CRUD UTILISATEUR
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public ScheduledTransferResponse create(User currentUser, CreateScheduledTransferRequest request) {
        // Vérifie l'appartenance du wallet (protection IDOR réutilisée).
        Wallet wallet = walletService.findOwnedWalletOrThrow(currentUser, request.walletId());

        validateExecutionDay(request.frequency(), request.executionDay());

        Vault targetVault = null;
        if (request.targetVaultId() != null) {
            targetVault = vaultRepository.findById(request.targetVaultId())
                    .orElseThrow(() -> new EntityNotFoundException("Coffre-fort introuvable"));

            if (!targetVault.getOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Ce coffre-fort ne vous appartient pas");
            }
            if (targetVault.getStatus() != VaultStatus.ACTIVE) {
                throw new IllegalStateException("Impossible de programmer un virement vers un coffre inactif ou fermé.");
            }
        }

        ScheduledTransfer st = ScheduledTransfer.builder()
                .frequency(request.frequency())
                .executionDay(request.executionDay())
                .amount(request.amount())
                .currency(wallet.getCurrency())
                .description(request.description())
                .status(ScheduledStatus.ACTIVE)
                .owner(currentUser)
                .wallet(wallet)
                .targetVault(targetVault)
                .build();

        st = scheduledRepository.save(st);
        scheduleNextExecution(st); // calcule et persiste la première échéance

        return ScheduledTransferResponse.fromEntity(st);
    }

    @Transactional
    public List<ScheduledTransferResponse> getMine(User currentUser) {
        return scheduledRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(ScheduledTransferResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ScheduledTransferResponse pause(User currentUser, Long id) {
        ScheduledTransfer st = findOwnedOrThrow(currentUser, id);
        st.setStatus(ScheduledStatus.PAUSED);
        return ScheduledTransferResponse.fromEntity(scheduledRepository.save(st));
    }

    @Transactional
    public ScheduledTransferResponse resume(User currentUser, Long id) {
        ScheduledTransfer st = findOwnedOrThrow(currentUser, id);
        st.setStatus(ScheduledStatus.ACTIVE);
        // Replanifie : la prochaine échéance stockée est probablement dépassée
        // pendant la pause, ce qui déclencherait une exécution immédiate.
        scheduleNextExecution(st);
        return ScheduledTransferResponse.fromEntity(st);
    }

    @Transactional
    public void delete(User currentUser, Long id) {
        scheduledRepository.delete(findOwnedOrThrow(currentUser, id));
    }

    private ScheduledTransfer findOwnedOrThrow(User currentUser, Long id) {
        ScheduledTransfer st = scheduledRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Virement programmé introuvable"));

        if (!st.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce virement programmé ne vous appartient pas");
        }
        return st;
    }

    private void validateExecutionDay(ScheduledTransfer.Frequency frequency, int executionDay) {
        if (frequency == ScheduledTransfer.Frequency.WEEKLY && (executionDay < 1 || executionDay > 7)) {
            throw new IllegalArgumentException(
                    "Pour une fréquence hebdomadaire, le jour doit être entre 1 (lundi) et 7 (dimanche).");
        }
    }

    /**
     * Orchestrateur du batch. Volontairement NON transactionnel : chaque
     * virement doit vivre dans sa propre transaction pour qu'un échec isolé
     * (solde insuffisant, coffre fermé) n'annule pas les virements déjà
     * traités dans le même passage.
     *
     * Les unités de travail sont ouvertes via TransactionTemplate et non par
     * un simple appel à une méthode @Transactional de cette même classe :
     * un appel `this.methode()` ne traverse pas le proxy Spring, l'annotation
     * était donc purement décorative. Sans transaction ambiante, chaque appel
     * au repository ouvrait puis refermait son propre EntityManager, si bien
     * que le Wallet et le Vault renvoyés étaient DÉTACHÉS : les
     * `setBalance()` / `setLockedBalance()` / `setCurrentAmount()` plus bas ne
     * produisaient aucun UPDATE. Le batch écrivait une ligne `transactions`
     * en SUCCESS sans jamais débiter le moindre franc, et le verrou
     * pessimiste posé par findByIdForUpdate() était relâché aussitôt.
     */
    public void processScheduledTransfers() {
        LocalDateTime currentDate = LocalDateTime.now();

        // On ne conserve que les identifiants : les entités chargées ici le
        // sont hors transaction, donc détachées. Les recharger dans l'unité de
        // travail est indispensable pour que le dirty checking s'applique, et
        // évite un LazyInitializationException sur st.getOwner() (relation
        // LAZY, aucune session ouverte — open-in-view est désactivé et un job
        // @Scheduled n'a de toute façon pas de requête web).
        List<Long> dueIds = scheduledRepository
                .findDueTransfers(ScheduledStatus.ACTIVE, currentDate)
                .stream()
                .map(ScheduledTransfer::getId)
                .toList();

        log.info("Execution des virements programmes : {} trouves", dueIds.size());

        for (Long id : dueIds) {
            try {
                transactionTemplate.executeWithoutResult(
                        status -> processOneScheduledTransfer(id, currentDate));
            } catch (InsufficientFundsException e) {
                log.warn("Solde insuffisant pour le virement programme ID {} : {}",
                        id, e.getMessage());
                recordFailure(id, currentDate, e.getMessage());
            } catch (Exception e) {
                log.error("Echec du virement programme ID {} : {}", id, e.getMessage());
                recordFailure(id, currentDate, e.getMessage());
            }
        }
    }

    /**
     * Trace l'échec dans sa propre transaction. Encapsulé ici pour qu'une
     * erreur pendant l'enregistrement de l'échec (BDD indisponible, etc.)
     * n'interrompe pas le traitement des virements suivants.
     */
    private void recordFailure(Long scheduledTransferId, LocalDateTime executionDate, String reason) {
        try {
            transactionTemplate.executeWithoutResult(
                    status -> handleFailedTransfer(scheduledTransferId, executionDate, reason));
        } catch (Exception e) {
            log.error("Impossible d'enregistrer l'echec du virement programme ID {} : {}",
                    scheduledTransferId, e.getMessage());
        }
    }

    /**
     * Unité de travail d'un virement. Pas de @Transactional : la transaction
     * est ouverte par l'appelant via TransactionTemplate (cf. explication sur
     * processScheduledTransfers). Les entités sont donc managées et les
     * modifications persistées par dirty checking, sans save() explicite —
     * même convention que TransactionService et VaultService.
     */
    void processOneScheduledTransfer(Long scheduledTransferId, LocalDateTime executionDate) {
        ScheduledTransfer st = scheduledRepository.findById(scheduledTransferId)
                .orElseThrow(() -> new EntityNotFoundException("Virement programmé introuvable"));

        // 1. Verrouiller le wallet
        Wallet wallet = walletRepository.findByIdForUpdate(st.getWallet().getId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        // 2. Vérifier les fonds
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(st.getAmount()) < 0) {
            throw new InsufficientFundsException("Solde insuffisant pour le virement programme");
        }

        // 3. Appliquer le mouvement selon la destination
        if (st.getTargetVault() != null) {
            Vault vault = vaultRepository.findById(st.getTargetVault().getId())
                    .orElseThrow(() -> new RuntimeException("Vault introuvable"));

            if (vault.getStatus() != VaultStatus.ACTIVE) {
                throw new IllegalStateException("Impossible d'alimenter un vault inactif ou fermé.");
            }

            // Alimentation d'un coffre = immobilisation, pas sortie d'argent.
            // Même invariant que VaultService.addFunds. Auparavant on faisait
            // `balance -= montant` sans jamais toucher lockedBalance : au
            // déblocage, VaultService faisait `lockedBalance -= montant` sur une
            // somme jamais immobilisée → solde bloqué négatif.
            wallet.setLockedBalance(wallet.getLockedBalance().add(st.getAmount()));
            vault.setCurrentAmount(vault.getCurrentAmount().add(st.getAmount()));
        } else {
            // Pas de coffre cible : l'argent quitte réellement le wallet.
            wallet.setBalance(wallet.getBalance().subtract(st.getAmount()));
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

        // 6. Dernière exécution ET prochaine échéance, dans la MÊME transaction
        //    que le débit. Replanifier après coup, hors transaction, laissait
        //    nextExecutionDate dans le passé si le batch s'interrompait entre
        //    les deux : le virement était rejoué — et le wallet redébité — au
        //    passage suivant.
        st.setLastExecutedAt(executionDate);
        scheduleNextExecution(st);
    }

    /**
     * Enregistre une transaction en échec (fonds insuffisants ou autre erreur)
     * et replanifie la prochaine échéance pour éviter une boucle de réessai
     * à chaque exécution du scheduler.
     *
     * Comme processOneScheduledTransfer : transaction fournie par l'appelant.
     */
    void handleFailedTransfer(Long scheduledTransferId, LocalDateTime executionDate, String reason) {
        ScheduledTransfer st = scheduledRepository.findById(scheduledTransferId)
                .orElseThrow(() -> new EntityNotFoundException("Virement programmé introuvable"));

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