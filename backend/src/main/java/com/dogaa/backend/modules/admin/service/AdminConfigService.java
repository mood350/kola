package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ForbiddenException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.admin.dto.FeeConfigResponse;
import com.dogaa.backend.modules.admin.dto.MerchantResponse;
import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.admin.entity.Merchant;
import com.dogaa.backend.modules.admin.entity.MerchantStatus;
import com.dogaa.backend.modules.admin.repository.MerchantRepository;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.transaction.entity.FeeScheduleEntry;
import com.dogaa.backend.modules.transaction.service.FeeCalculator;
import com.dogaa.backend.modules.transaction.service.FeeScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fees and partner merchants (BACKEND.md 10).
 *
 * <p>The fee grid is written through {@link FeeScheduleService}, which {@link FeeCalculator} reads
 * on every quote — a saved rate is charged, not merely displayed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private static final String MODULE = "config";

    private final FeeCalculator feeCalculator;
    private final FeeScheduleService feeScheduleService;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    // --- fees -------------------------------------------------------------

    /** One row per KYC tier, in ladder order, showing what that tier pays today. */
    @Transactional(readOnly = true)
    public List<FeeConfigResponse> fees() {
        List<FeeConfigResponse> rows = new ArrayList<>(KycTier.values().length);
        for (KycTier tier : KycTier.values()) {
            rows.add(new FeeConfigResponse(
                    tier.name(),
                    AdminFormat.percent(feeCalculator.effectivePercent(TransactionType.P2P_TRANSFER, tier)),
                    AdminFormat.percent(feeCalculator.effectivePercent(TransactionType.MERCHANT_PAYMENT, tier)),
                    AdminFormat.percent(feeCalculator.effectivePercent(TransactionType.CASH_OUT, tier))));
        }
        return rows;
    }

    @Transactional
    public List<FeeConfigResponse> updateFees(CurrentAdmin admin, List<FeeConfigResponse> fees) {
        if (admin.role() != AdminRole.SUPER_ADMIN) {
            throw new ForbiddenException("Seul un super-admin peut modifier la grille de frais");
        }
        if (fees.size() != KycTier.values().length) {
            throw new BadRequestException("La grille compte " + KycTier.values().length
                    + " paliers ; " + fees.size() + " ont été envoyés");
        }

        List<FeeConfigResponse> before = fees();
        List<FeeScheduleEntry> entries = fees.stream()
                .map(AdminConfigService::toEntry)
                .toList();

        feeScheduleService.replace(entries, admin.displayName());

        auditService.record(admin, MODULE, "Modification de la grille de frais",
                AuditService.diff(describe(before), describe(fees())), "FeeSchedule", null);

        log.info("Admin {} updated the fee grid", admin.email());
        return fees();
    }

    // --- merchants --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MerchantResponse> merchants() {
        return merchantRepository.findAllByOrderByNameAsc().stream()
                .map(AdminConfigService::toResponse)
                .toList();
    }

    /**
     * Applies the status the console computed, after checking the move is one the product allows.
     * The client picks the target; the server decides whether it is reachable from where we are.
     */
    @Transactional
    public MerchantResponse updateMerchantStatus(CurrentAdmin admin, UUID merchantId, String targetValue) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Marchand introuvable"));

        MerchantStatus target = MerchantStatus.fromWireValue(targetValue);
        MerchantStatus current = merchant.getStatus();

        if (current == target) {
            throw new ConflictException("Ce marchand est déjà « " + target.wireValue() + " »");
        }
        if (!current.canMoveTo(target)) {
            throw new ConflictException("Transition refusée : " + current.wireValue()
                    + " → " + target.wireValue());
        }

        merchant.setStatus(target);
        merchantRepository.save(merchant);

        auditService.record(admin, MODULE, "Changement de statut du marchand " + merchant.getName(),
                AuditService.diff(current.wireValue(), target.wireValue()),
                "Merchant", merchantId.toString());

        log.info("Admin {} moved merchant {} from {} to {}",
                admin.email(), merchantId, current, target);
        return toResponse(merchant);
    }

    // --- mapping ----------------------------------------------------------

    private static MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId().toString(),
                merchant.getName(),
                merchant.getCategory(),
                merchant.getStatus().wireValue());
    }

    private static FeeScheduleEntry toEntry(FeeConfigResponse row) {
        return FeeScheduleEntry.builder()
                .tier(parseTier(row.tier()))
                .p2pPercent(parsePercent(row.p2p()))
                .merchantPercent(parsePercent(row.merchant()))
                .cashOutPercent(parsePercent(row.cashout()))
                .build();
    }

    private static KycTier parseTier(String value) {
        try {
            return KycTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Palier KYC inconnu : « " + value + " »");
        }
    }

    /** "1,50 %", "1.5", "1,5 %" → 1.50. */
    private static BigDecimal parsePercent(String value) {
        String number = value.replace(',', '.').replaceAll("[^0-9.]", "");
        if (number.isEmpty() || number.equals(".")) {
            throw new BadRequestException("Taux illisible : « " + value + " »");
        }
        BigDecimal percent = new BigDecimal(number).setScale(2, RoundingMode.HALF_UP);
        if (percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Taux hors bornes : « " + value + " »");
        }
        return percent;
    }

    private static String describe(List<FeeConfigResponse> fees) {
        return fees.stream()
                .map(f -> f.tier() + " " + f.p2p() + "/" + f.merchant() + "/" + f.cashout())
                .reduce((a, b) -> a + ", " + b)
                .orElse("—");
    }
}
