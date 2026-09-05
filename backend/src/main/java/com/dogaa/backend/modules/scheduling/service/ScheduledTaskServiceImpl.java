package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import com.dogaa.backend.modules.vault.entity.VaultStatus;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.modules.transaction.service.BillerCatalog;
import com.dogaa.backend.modules.vault.entity.Vault;
import com.dogaa.backend.modules.vault.service.VaultService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.dogaa.backend.common.enums.Biller;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.modules.scheduling.dto.UpdateScheduledTaskRequest;
import java.time.Instant;
import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.scheduling.mapper.ScheduledTaskMapper;
import com.dogaa.backend.modules.scheduling.repository.ScheduledTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private final ScheduledTaskRepository repository;
    private final VaultService vaultService;
    private final BillerCatalog billerCatalog;

    public ScheduledTaskServiceImpl(ScheduledTaskRepository repository,
                                    VaultService vaultService,
                                    BillerCatalog billerCatalog) {
        this.repository = repository;
        this.vaultService = vaultService;
        this.billerCatalog = billerCatalog;
    }

    @Override
    public ScheduledTaskResponse create(UUID ownerId, ScheduledTaskRequest request) {
        ScheduledTask task = new ScheduledTask();
        // The owner comes from the token, not from the payload: trusting the body
        // would let anyone schedule a transfer out of someone else's wallet.
        task.setUserId(ownerId);
        task.setType(request.type());
        task.setFrequency(request.frequency());
        task.setAmount(request.amount());
        task.setCurrency(request.currency());
        task.setBeneficiaryReference(beneficiaryFor(request));
        task.setBiller(request.type() == ScheduledTaskType.BILL_PAYMENT ? request.biller() : null);
        task.setNextRunAt(request.firstRunAt());
        task.setEndDate(request.endDate());
        task.setMaxOccurrences(request.maxOccurrences());
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        task.setFundingVaultId(fundingVaultFor(ownerId, request));
        task.setDayOfMonth(dayOfMonthFor(request));
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse update(UUID ownerId, UUID taskId,
                                        UpdateScheduledTaskRequest request) {
        ScheduledTask task = requireOwner(ownerId, getOrThrow(taskId));
        requireEditable(task);

        if (request.amount() != null) {
            task.setAmount(request.amount());
        }
        if (request.fundingVaultId() != null) {
            task.setFundingVaultId(resolveVault(ownerId, request.fundingVaultId(), task.getCurrency()));
        }
        applyBeneficiary(task, request);
        applyRecurrence(task, request);

        // Cleared explicitly rather than by sending null: on a partial update null means
        // "unchanged", so "no end date" needs a word of its own or it cannot be said at all.
        if (request.clearEndDate()) {
            task.setEndDate(null);
        } else if (request.endDate() != null) {
            task.setEndDate(request.endDate());
        }
        if (request.clearMaxOccurrences()) {
            task.setMaxOccurrences(null);
        } else if (request.maxOccurrences() != null) {
            requireNotAlreadyPast(task, request.maxOccurrences());
            task.setMaxOccurrences(request.maxOccurrences());
        }

        log.info("Scheduled task {} edited by its owner", taskId);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    /**
     * A cancelled or completed schedule is history, and history is not edited — reviving one by
     * changing its amount would make the trail lie about what was agreed and when.
     */
    private void requireEditable(ScheduledTask task) {
        if (task.getStatus() != ScheduledTaskStatus.ACTIVE
                && task.getStatus() != ScheduledTaskStatus.PAUSED) {
            throw new ConflictException("Une planification " + task.getStatus()
                    + " ne peut plus être modifiée : créez-en une nouvelle.");
        }
    }

    /**
     * The beneficiary and the biller move together: a Canal+ card number is meaningless once the
     * biller becomes Togocom, so changing one re-validates against the other rather than trusting
     * whichever half arrived.
     */
    private void applyBeneficiary(ScheduledTask task, UpdateScheduledTaskRequest request) {
        if (request.biller() == null && request.beneficiaryReference() == null) {
            return;
        }
        if (task.getType() != ScheduledTaskType.BILL_PAYMENT) {
            if (request.beneficiaryReference() != null) {
                task.setBeneficiaryReference(request.beneficiaryReference());
            }
            return;
        }

        Biller biller = request.biller() != null ? request.biller() : task.getBiller();
        String reference = request.beneficiaryReference() != null
                ? request.beneficiaryReference()
                : task.getBeneficiaryReference();

        billerCatalog.requireSchedulable(biller);
        task.setBiller(biller);
        task.setBeneficiaryReference(billerCatalog.normaliseIdentifier(biller, reference));
    }

    private void applyRecurrence(ScheduledTask task, UpdateScheduledTaskRequest request) {
        if (request.nextRunAt() != null) {
            if (!request.nextRunAt().isAfter(Instant.now())) {
                throw new BadRequestException("La prochaine échéance doit être dans le futur");
            }
            task.setNextRunAt(request.nextRunAt());
        }
        if (request.frequency() != null) {
            task.setFrequency(request.frequency());
        }
        if (request.dayOfMonth() != null) {
            task.setDayOfMonth(request.dayOfMonth());
        }

        // Leaving a monthly schedule without a chosen day would let it drift from whatever run it
        // happens to be sitting on, which is the bug the stored day exists to prevent.
        if (task.getFrequency() == ScheduleFrequency.MONTHLY && task.getDayOfMonth() == null) {
            task.setDayOfMonth(LocalDateTime.ofInstant(task.getNextRunAt(), ZoneOffset.UTC)
                    .getDayOfMonth());
        }
        if (task.getFrequency() != ScheduleFrequency.MONTHLY) {
            task.setDayOfMonth(null);
        }
    }

    /** A limit already reached would end the schedule the moment it is saved. */
    private void requireNotAlreadyPast(ScheduledTask task, int maxOccurrences) {
        if (maxOccurrences <= task.getOccurrencesCompleted()) {
            throw new BadRequestException("Cette planification compte déjà "
                    + task.getOccurrencesCompleted() + " exécutions : le maximum doit être supérieur.");
        }
    }

    /**
     * Resolves the vault the schedule will spend from.
     *
     * <p>Mandatory for everything but vault and Bankivi deposits, whose source is the current
     * account by nature. Money otherwise leaving the everyday balance on a date chosen weeks
     * earlier is the surprise a wallet must never spring; naming a vault makes the money
     * deliberately set aside, and visibly short when it is not.
     *
     * <p>Checked at creation rather than only at midnight: telling someone their rent failed is a
     * far worse moment to discover the vault is in the wrong currency.
     */
    private UUID fundingVaultFor(UUID ownerId, ScheduledTaskRequest request) {
        if (request.type() == ScheduledTaskType.VAULT_DEPOSIT
                || request.type() == ScheduledTaskType.SAVINGS_DEPOSIT) {
            return null;
        }
        if (request.fundingVaultId() == null) {
            throw new BadRequestException(
                    "Choisissez le coffre qui financera cette planification");
        }

        return resolveVault(ownerId, request.fundingVaultId(), request.currency());
    }

    /** Shared by creation and edition, so switching vault is checked exactly as choosing one. */
    private UUID resolveVault(UUID ownerId, UUID vaultId, Currency currency) {
        Vault vault = vaultService.getVault(ownerId, vaultId);
        if (vault.getStatus() != VaultStatus.ACTIVE) {
            throw new BadRequestException("Ce coffre est clôturé");
        }
        if (vault.getCurrency() != currency) {
            throw new BadRequestException("Le coffre « " + vault.getName() + " » est en "
                    + vault.getCurrency() + ", la planification en " + currency);
        }
        return vault.getId();
    }

    /**
     * Validates the beneficiary against what the schedule is for.
     *
     * <p>A bill is not addressed like a transfer: Canal+ wants the 14-digit card number under the
     * decoder, Cash Power wants a meter number, CEET a customer reference. The catalogue knows
     * which, and normalises the separators people copy off a paper bill.
     */
    private String beneficiaryFor(ScheduledTaskRequest request) {
        if (request.type() != ScheduledTaskType.BILL_PAYMENT) {
            return request.beneficiaryReference();
        }
        if (request.biller() == null) {
            throw new BadRequestException("Choisissez le service à payer");
        }
        // Only bills whose amount is the same every month can carry a fixed scheduled sum:
        // scheduling one against a consumption bill would silently underpay or overpay for ever.
        billerCatalog.requireSchedulable(request.biller());
        return billerCatalog.normaliseIdentifier(request.biller(), request.beneficiaryReference());
    }

    /** The chosen day, or the one implied by the first run. Only monthly schedules have one. */
    private Integer dayOfMonthFor(ScheduledTaskRequest request) {
        if (request.frequency() != ScheduleFrequency.MONTHLY) {
            return null;
        }
        return request.dayOfMonth() != null
                ? request.dayOfMonth()
                : LocalDateTime.ofInstant(request.firstRunAt(), ZoneOffset.UTC).getDayOfMonth();
    }

    @Override
    public ScheduledTaskResponse pause(UUID ownerId, UUID taskId) {
        ScheduledTask task = requireOwner(ownerId, findActiveOrPaused(taskId));
        task.setStatus(ScheduledTaskStatus.PAUSED);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse resume(UUID ownerId, UUID taskId) {
        ScheduledTask task = requireOwner(ownerId, getOrThrow(taskId));
        if (task.getStatus() != ScheduledTaskStatus.PAUSED) {
            throw new ConflictException("Seule une tache en pause peut etre reprise.");
        }
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse cancel(UUID ownerId, UUID taskId) {
        ScheduledTask task = requireOwner(ownerId, findActiveOrPaused(taskId));
        task.setStatus(ScheduledTaskStatus.CANCELLED);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    /**
     * Someone else's task answers exactly like one that does not exist. Saying "forbidden" would
     * confirm the id is real, which is itself information the caller has no business having.
     */
    private ScheduledTask requireOwner(UUID ownerId, ScheduledTask task) {
        if (!task.getUserId().equals(ownerId)) {
            throw new ResourceNotFoundException("Tache programmee introuvable : " + task.getId());
        }
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskResponse> listByUser(UUID userId) {
        return repository.findByUserIdOrderByNextRunAtAsc(userId).stream()
                .map(ScheduledTaskMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskResponse> listAll() {
        return repository.findAll().stream()
                .map(ScheduledTaskMapper::toResponse)
                .toList();
    }

    private ScheduledTask findActiveOrPaused(UUID taskId) {
        ScheduledTask task = getOrThrow(taskId);
        if (task.getStatus() != ScheduledTaskStatus.ACTIVE && task.getStatus() != ScheduledTaskStatus.PAUSED) {
            throw new ConflictException("Cette tache ne peut plus etre modifiee (statut: " + task.getStatus() + ").");
        }
        return task;
    }

    private ScheduledTask getOrThrow(UUID taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tache programmee introuvable: " + taskId));
    }
}
