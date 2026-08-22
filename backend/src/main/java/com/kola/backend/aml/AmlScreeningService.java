package com.kola.backend.aml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Moteur de surveillance LAB-FT (lutte anti-blanchiment / financement du
 * terrorisme). Analyse chaque transaction au regard de l'historique du client
 * et produit une alerte explicable si le score de risque dépasse le seuil.
 *
 * ⚠️ INTERDICTION DE DIVULGATION : le résultat n'est JAMAIS renvoyé au client
 * ni exposé via l'API mobile. Prévenir une personne qu'elle est surveillée
 * constitue le délit de « tipping off ». Les alertes ne sont lisibles que par
 * un analyste conformité via /api/admin/aml.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmlScreeningService {

    private final AmlAlertRepository alertRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** Seuil déclaratif unitaire (XOF). En production : paramètre réglementaire. */
    private static final BigDecimal DECLARATION_THRESHOLD = new BigDecimal("1000000");

    /** À partir de ce score cumulé, on matérialise une alerte. */
    private static final int ALERT_THRESHOLD = 25;

    /**
     * Juridictions à risque élevé. Illustratif ici : en production cette liste
     * provient de la liste GAFI/FATF et se met à jour sans redéploiement.
     */
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of("KP", "IR", "MM");

    private static final Set<TransactionType> OUTGOING = Set.of(
            TransactionType.WITHDRAWAL,
            TransactionType.TRANSFER_OUT,
            TransactionType.MERCHANT_PAYMENT
    );

    /**
     * Déclenché APRÈS le commit du paiement (AFTER_COMMIT), dans une
     * transaction neuve. Deux raisons :
     *  1. La ligne `transactions` doit être committée avant que l'alerte ne la
     *     référence, sinon la clé étrangère échoue.
     *  2. La surveillance ne doit jamais pouvoir annuler une opération
     *     financière déjà validée — elle est hors du chemin critique.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        try {
            User user = userRepository.findById(event.userId()).orElse(null);
            Transaction tx = transactionRepository.findById(event.transactionId()).orElse(null);
            if (user == null || tx == null) return;
            screen(user, tx);
        } catch (Exception e) {
            log.error("[LAB-FT] Échec de l'analyse post-commit (transaction {})",
                    event.transactionId(), e);
        }
    }

    /** Analyse une transaction validée et matérialise une alerte si besoin. */
    public void screen(User user, Transaction tx) {
        try {
            List<Transaction> history = transactionRepository
                    .findBySenderIdOrderByCreatedAtDesc(user.getId())
                    .stream()
                    .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                    .filter(t -> t.getCreatedAt() != null)
                    .filter(t -> !t.getId().equals(tx.getId()))
                    .toList();

            List<TriggeredRule> triggered = new ArrayList<>();

            checkThresholdBreach(tx, triggered);
            checkAmountAnomaly(tx, history, triggered);
            checkStructuring(tx, history, triggered);
            checkVelocitySpike(history, triggered);
            checkRapidPassthrough(tx, history, triggered);
            checkUnusualHour(tx, history, triggered);
            checkHighRiskCountry(tx, triggered);
            checkNewBeneficiaryBurst(tx, history, triggered);
            checkDormantReactivation(tx, history, triggered);

            if (triggered.isEmpty()) return;

            int score = Math.min(
                    triggered.stream().mapToInt(TriggeredRule::weight).sum(), 100);

            if (score < ALERT_THRESHOLD) return;

            AmlAlert alert = AmlAlert.builder()
                    .user(user)
                    .transaction(tx)
                    .riskScore(score)
                    .riskLevel(AmlRiskLevel.fromScore(score))
                    .status(AmlAlertStatus.OPEN)
                    .triggeredRulesJson(serialize(triggered))
                    .build();

            alertRepository.save(alert);
            log.warn("[LAB-FT] Alerte {} (score {}) sur user {} / transaction {}",
                    alert.getRiskLevel(), score, user.getId(), tx.getReference());

        } catch (Exception e) {
            // Jamais de propagation : la surveillance ne bloque pas le paiement.
            log.error("[LAB-FT] Échec de l'analyse de la transaction {}",
                    tx != null ? tx.getReference() : "?", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RÈGLES
    // ═══════════════════════════════════════════════════════════════

    /** Opération unitaire au-dessus du seuil déclaratif. */
    private void checkThresholdBreach(Transaction tx, List<TriggeredRule> out) {
        if (tx.getAmount().compareTo(DECLARATION_THRESHOLD) >= 0) {
            add(out, AmlRule.THRESHOLD_BREACH,
                    "Montant de " + tx.getAmount() + " XOF au-dessus du seuil de "
                            + DECLARATION_THRESHOLD + " XOF.");
        }
    }

    /** Montant très au-dessus de la moyenne habituelle du client (> moyenne + 3σ). */
    private void checkAmountAnomaly(Transaction tx, List<Transaction> history, List<TriggeredRule> out) {
        List<BigDecimal> amounts = history.stream().map(Transaction::getAmount).toList();
        if (amounts.size() < 5) return; // pas assez d'historique pour juger

        double mean = amounts.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = amounts.stream()
                .mapToDouble(a -> Math.pow(a.doubleValue() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return;

        double value = tx.getAmount().doubleValue();
        double zScore = (value - mean) / stdDev;

        if (zScore > 3) {
            add(out, AmlRule.AMOUNT_ANOMALY, String.format(
                    "Montant %.0f XOF, soit %.1f écarts-types au-dessus de la moyenne du client (%.0f XOF).",
                    value, zScore, mean));
        }
    }

    /** Fractionnement : plusieurs opérations juste sous le seuil déclaratif. */
    private void checkStructuring(Transaction tx, List<Transaction> history, List<TriggeredRule> out) {
        BigDecimal floor = DECLARATION_THRESHOLD.multiply(new BigDecimal("0.70"));
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        List<Transaction> nearThreshold = new ArrayList<>(history.stream()
                .filter(t -> t.getCreatedAt().isAfter(since))
                .filter(t -> t.getAmount().compareTo(floor) >= 0
                        && t.getAmount().compareTo(DECLARATION_THRESHOLD) < 0)
                .toList());

        if (tx.getAmount().compareTo(floor) >= 0
                && tx.getAmount().compareTo(DECLARATION_THRESHOLD) < 0) {
            nearThreshold.add(tx);
        }

        BigDecimal total = nearThreshold.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (nearThreshold.size() >= 3 && total.compareTo(DECLARATION_THRESHOLD) >= 0) {
            add(out, AmlRule.STRUCTURING,
                    nearThreshold.size() + " opérations entre " + floor + " et " + DECLARATION_THRESHOLD
                            + " XOF sur 7 jours, cumulant " + total + " XOF.");
        }
    }

    /** Nombre d'opérations sur 24h très au-dessus du rythme habituel. */
    private void checkVelocitySpike(List<Transaction> history, List<TriggeredRule> out) {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        LocalDateTime last30d = LocalDateTime.now().minusDays(30);

        long recent = history.stream().filter(t -> t.getCreatedAt().isAfter(last24h)).count() + 1;
        long monthly = history.stream().filter(t -> t.getCreatedAt().isAfter(last30d)).count();

        double dailyAverage = monthly / 30.0;
        if (recent >= 5 && dailyAverage > 0 && recent > dailyAverage * 3) {
            add(out, AmlRule.VELOCITY_SPIKE, String.format(
                    "%d opérations sur 24h contre une moyenne de %.1f/jour.", recent, dailyAverage));
        }
    }

    /** Dépôt suivi d'une sortie quasi immédiate du même ordre de grandeur. */
    private void checkRapidPassthrough(Transaction tx, List<Transaction> history, List<TriggeredRule> out) {
        if (!OUTGOING.contains(tx.getType())) return;

        BigDecimal floor = tx.getAmount().multiply(new BigDecimal("0.80"));

        boolean matched = history.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .filter(t -> t.getAmount().compareTo(floor) >= 0)
                .anyMatch(t -> {
                    Duration gap = Duration.between(t.getCreatedAt(), tx.getCreatedAt());
                    return !gap.isNegative() && gap.toMinutes() <= 60;
                });

        if (matched) {
            add(out, AmlRule.RAPID_PASSTHROUGH,
                    "Sortie de " + tx.getAmount() + " XOF moins d'une heure après un dépôt équivalent.");
        }
    }

    /** Opération nocturne alors que le client n'opère quasiment jamais la nuit. */
    private void checkUnusualHour(Transaction tx, List<Transaction> history, List<TriggeredRule> out) {
        int hour = tx.getCreatedAt().getHour();
        if (hour >= 5) return; // on ne regarde que 00h-05h
        if (history.size() < 10) return;

        long nightCount = history.stream()
                .filter(t -> t.getCreatedAt().getHour() < 5)
                .count();

        if ((double) nightCount / history.size() < 0.10) {
            add(out, AmlRule.UNUSUAL_HOUR,
                    "Opération à " + hour + "h alors que le client n'opère presque jamais la nuit.");
        }
    }

    /** Destination dans une juridiction à risque élevé. */
    private void checkHighRiskCountry(Transaction tx, List<TriggeredRule> out) {
        String country = tx.getReceiverCountryCode();
        if (country != null && HIGH_RISK_COUNTRIES.contains(country.toUpperCase())) {
            add(out, AmlRule.HIGH_RISK_COUNTRY,
                    "Transfert vers une juridiction à risque élevé (" + country + ").");
        }
    }

    /** Plusieurs destinataires inédits contactés en 24h. */
    private void checkNewBeneficiaryBurst(Transaction tx, List<Transaction> history, List<TriggeredRule> out) {
        if (tx.getReceiverPhoneNumber() == null) return;

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        Set<String> known = history.stream()
                .filter(t -> t.getCreatedAt().isBefore(last24h))
                .map(Transaction::getReceiverPhoneNumber)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> recent = new java.util.HashSet<>(history.stream()
                .filter(t -> t.getCreatedAt().isAfter(last24h))
                .map(Transaction::getReceiverPhoneNumber)
                .filter(java.util.Objects::nonNull)
                .toList());
        recent.add(tx.getReceiverPhoneNumber());
        recent.removeAll(known);

        if (recent.size() >= 3) {
            add(out, AmlRule.NEW_BENEFICIARY_BURST,
                    recent.size() + " destinataires jamais contactés auparavant, en moins de 24h.");
        }
    }

    /** Compte inactif depuis longtemps qui redémarre sur un montant élevé. */
    private void checkDormantReactivation(Transaction tx, List<Transaction> history, List<TriggeredRule> out) {
        if (history.isEmpty()) return;

        LocalDateTime lastActivity = history.stream()
                .map(Transaction::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (lastActivity == null) return;

        long daysInactive = Duration.between(lastActivity, tx.getCreatedAt()).toDays();

        if (daysInactive >= 90 && tx.getAmount().compareTo(new BigDecimal("100000")) >= 0) {
            add(out, AmlRule.DORMANT_REACTIVATION,
                    "Compte inactif depuis " + daysInactive + " jours, réactivé sur "
                            + tx.getAmount() + " XOF.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void add(List<TriggeredRule> out, AmlRule rule, String detail) {
        out.add(new TriggeredRule(rule.name(), rule.getLabel(), rule.getRiskWeight(), detail));
    }

    private String serialize(List<TriggeredRule> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            log.error("[LAB-FT] Erreur de sérialisation des règles déclenchées", e);
            return "[]";
        }
    }

    /** Détail d'une règle déclenchée, sérialisé dans l'alerte. */
    public record TriggeredRule(String rule, String label, int weight, String detail) {
    }
}
