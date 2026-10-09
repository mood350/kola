package com.kola.backend.modules.assistant.service;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.util.PhoneNumbers;
import com.kola.backend.config.AssistantProperties;
import com.kola.backend.modules.credit.dto.CreditEligibilityResponse;
import com.kola.backend.modules.credit.dto.LoanResponse;
import com.kola.backend.modules.credit.service.CreditService;
import com.kola.backend.modules.kyc.dto.KycStatusResponse;
import com.kola.backend.modules.kyc.service.KycService;
import com.kola.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.kola.backend.modules.scheduling.service.ScheduledTaskService;
import com.kola.backend.modules.scoring.dto.CreditScoreResponse;
import com.kola.backend.modules.scoring.service.ScoringService;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import com.kola.backend.modules.vault.entity.Vault;
import com.kola.backend.modules.vault.service.VaultService;
import com.kola.backend.modules.wallet.entity.Wallet;
import com.kola.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The caller's own situation, rendered for the model: KYC level, balances, score, loan, vaults,
 * scheduled operations and recent transactions.
 *
 * <p><b>The user id is a parameter, never a request field.</b> Every read below goes through the
 * owning module's service with that one id, so there is no code path where this class could
 * assemble somebody else's account. That is the whole security story of the module: the assistant
 * has no tools and cannot move money, so the only thing worth protecting is what it is allowed to
 * see.
 *
 * <p>What is deliberately left out: the PIN hash, tokens, the full phone number (masked), and the
 * document files themselves. The assistant needs to know a document was approved, not what is on it.
 *
 * <p>Each section degrades on its own. A user with no savings wallet makes the credit lookup throw;
 * that must cost the answer its credit paragraph, not the whole reply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextCollector {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private final AssistantProperties properties;
    private final UserService userService;
    private final KycService kycService;
    private final WalletService walletService;
    private final VaultService vaultService;
    private final ScoringService scoringService;
    private final CreditService creditService;
    private final ScheduledTaskService scheduledTaskService;
    private final TransactionService transactionService;

    @Transactional(readOnly = true)
    public String snapshot(UUID userId) {
        StringBuilder out = new StringBuilder(2000);
        out.append("# Situation de l'utilisateur qui pose la question\n\n");

        User user = userService.getById(userId);
        out.append("Prénom : ").append(orDash(user.getFirstName())).append('\n');
        out.append("Nom : ").append(orDash(user.getLastName())).append('\n');
        out.append("Téléphone : ").append(PhoneNumbers.mask(user.getPhone())).append('\n');
        out.append("Compte créé le : ").append(DAY.format(user.getCreatedAt())).append('\n');
        out.append("Statut du compte : ").append(user.getStatus()).append('\n');
        out.append("E-mail vérifié : ").append(user.isEmailVerified() ? "oui" : "non")
                .append(" (facultatif)\n");

        section(out, "Vérification d'identité", () -> kyc(userId));
        section(out, "Comptes", () -> wallets(userId));
        section(out, "Coffres", () -> vaults(userId));
        section(out, "Score de crédit", () -> score(userId));
        section(out, "Crédit", () -> credit(userId));
        section(out, "Opérations programmées", () -> scheduled(userId));
        section(out, "Dernières transactions", () -> transactions(userId));

        return out.toString();
    }

    // --- sections ---------------------------------------------------------

    private String kyc(UUID userId) {
        KycStatusResponse status = kycService.getStatus(userId);
        StringBuilder out = new StringBuilder();
        out.append("Niveau actuel : ").append(status.tier()).append('\n');
        out.append("Niveau suivant : ")
                .append(status.nextTier() == null ? "aucun, niveau maximum atteint" : status.nextTier())
                .append('\n');
        out.append("Pièce d'identité approuvée : ")
                .append(status.identityDocumentApproved() ? "oui" : "non").append('\n');

        if (status.requirementsForNextTier().isEmpty()) {
            out.append("Il ne manque rien pour monter de niveau.\n");
        } else {
            out.append("Pour monter de niveau, il manque :\n");
            status.requirementsForNextTier().forEach(r -> out.append("- ").append(r).append('\n'));
        }

        if (status.limits() != null) {
            out.append("Plafonds applicables à ce niveau : ").append(status.limits()).append('\n');
        }
        return out.toString();
    }

    private String wallets(UUID userId) {
        List<Wallet> wallets = walletService.listWallets(userId);
        if (wallets.isEmpty()) {
            return "Aucun compte.\n";
        }
        StringBuilder out = new StringBuilder();
        for (Wallet wallet : wallets) {
            out.append("- compte ").append(wallet.getType())
                    .append(" en ").append(wallet.getCurrency())
                    .append(" : ").append(money(wallet.getAvailableBalance(), wallet.getCurrency()))
                    .append(" disponible, ")
                    .append(money(wallet.getLockedBalance(), wallet.getCurrency()))
                    .append(" bloqué (statut ").append(wallet.getStatus()).append(")\n");
        }
        return out.toString();
    }

    private String vaults(UUID userId) {
        List<Vault> vaults = vaultService.listVaults(userId);
        if (vaults.isEmpty()) {
            return "Aucun coffre ouvert.\n";
        }
        StringBuilder out = new StringBuilder();
        for (Vault vault : vaults) {
            out.append("- « ").append(vault.getName()).append(" » : ")
                    .append(money(vault.getBalance(), vault.getCurrency()));
            if (vault.getTargetAmount() != null) {
                out.append(" sur un objectif de ")
                        .append(money(vault.getTargetAmount(), vault.getCurrency()));
            }
            if (vault.getTargetDate() != null) {
                out.append(", échéance ").append(vault.getTargetDate());
            }
            out.append(" (").append(vault.getStatus()).append(")\n");
        }
        return out.toString();
    }

    private String score(UUID userId) {
        CreditScoreResponse score = scoringService.getLatestScore(userId);
        return """
                Score : %d sur 100 (score brut avant lissage : %d), calculé sur %d jours, le %s.
                Détail par axe : %s
                """.formatted(score.scoreValue(), score.rawScoreValue(), score.windowDays(),
                score.calculatedAt() == null ? "jamais" : DAY.format(score.calculatedAt()),
                score.breakdown());
    }

    private String credit(UUID userId) {
        StringBuilder out = new StringBuilder();
        CreditEligibilityResponse eligibility = creditService.checkEligibility(userId, Currency.XOF);

        out.append("Éligible à un prêt : ").append(eligibility.eligible() ? "oui" : "non")
                .append('\n');
        if (!eligibility.blockers().isEmpty()) {
            out.append("Ce qui bloque aujourd'hui :\n");
            eligibility.blockers().forEach(b -> out.append("- ").append(b).append('\n'));
        }
        out.append("Épargne mobilisable : ")
                .append(money(eligibility.savingsBalance(), eligibility.currency())).append('\n');
        out.append("Montant maximum empruntable : ")
                .append(money(eligibility.maxLoanAmount(), eligibility.currency()))
                .append(" (levier ").append(eligibility.leverageRatio())
                .append(", taux ").append(eligibility.monthlyRatePercent()).append(" % par mois, ")
                .append("à rembourser ")
                .append(money(eligibility.totalRepayable(), eligibility.currency()))
                .append(" sur ").append(eligibility.termDays()).append(" jours)\n");
        out.append("Prêts déjà remboursés : ").append(eligibility.loansRepaid()).append('\n');

        creditService.activeLoan(userId).ifPresentOrElse(
                loan -> out.append(activeLoan(loan)),
                () -> out.append("Aucun prêt en cours.\n"));
        return out.toString();
    }

    private String activeLoan(LoanResponse loan) {
        return """
                Prêt en cours : %s empruntés, %s déjà remboursés, reste %s à payer.
                Statut %s, échéance le %s.
                """.formatted(money(loan.principal(), loan.currency()),
                money(loan.amountRepaid(), loan.currency()),
                money(loan.outstanding(), loan.currency()),
                loan.status(),
                loan.dueAt() == null ? "non fixée" : DAY.format(loan.dueAt()));
    }

    private String scheduled(UUID userId) {
        List<ScheduledTaskResponse> tasks = scheduledTaskService.listByUser(userId);
        if (tasks.isEmpty()) {
            return "Aucune opération programmée.\n";
        }
        StringBuilder out = new StringBuilder();
        tasks.forEach(task -> out.append("- ").append(task).append('\n'));
        return out.toString();
    }

    private String transactions(UUID userId) {
        List<Transaction> recent = transactionService
                .history(userId, PageRequest.of(0, properties.getRecentTransactions()))
                .getContent();
        if (recent.isEmpty()) {
            return "Aucune transaction.\n";
        }
        StringBuilder out = new StringBuilder();
        for (Transaction tx : recent) {
            out.append("- ").append(DAY.format(tx.getCreatedAt()))
                    .append(" | ").append(tx.getType())
                    .append(" | ").append(money(tx.getAmount(), tx.getCurrency()))
                    .append(" | frais ").append(money(tx.getFee(), tx.getCurrency()))
                    .append(" | ").append(tx.getStatus())
                    .append(" | réf ").append(tx.getReference());
            if (tx.getFailureReason() != null) {
                out.append(" | échec : ").append(tx.getFailureReason());
            }
            out.append('\n');
        }
        return out.toString();
    }

    // --- plumbing ---------------------------------------------------------

    /**
     * Appends one section, or a note saying it could not be read.
     *
     * <p>A user with no savings wallet, or whose score has never been computed, makes one of these
     * lookups throw. Losing that paragraph is acceptable; losing the answer is not — and telling the
     * model the section is missing is better than letting it assume the value is zero.
     */
    private void section(StringBuilder out, String title, Supplier<String> body) {
        out.append("\n## ").append(title).append('\n');
        try {
            out.append(body.get());
        } catch (RuntimeException ex) {
            log.warn("Assistant context: section '{}' unavailable: {}", title, ex.getMessage());
            out.append("Information indisponible pour le moment.\n");
        }
    }

    private static String money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            return "non applicable";
        }
        return NumberFormat.getInstance(Locale.FRANCE).format(amount) + " "
                + (currency == null ? "" : currency.name());
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "non renseigné" : value;
    }
}
