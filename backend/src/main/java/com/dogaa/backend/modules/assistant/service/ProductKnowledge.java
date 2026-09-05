package com.dogaa.backend.modules.assistant.service;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.config.AuthProperties;
import com.dogaa.backend.config.CreditProperties;
import com.dogaa.backend.config.DisputeProperties;
import com.dogaa.backend.config.FeeProperties;
import com.dogaa.backend.config.KycProperties;
import com.dogaa.backend.config.OtpProperties;
import com.dogaa.backend.config.ScoringProperties;
import com.dogaa.backend.modules.transaction.service.FeeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * What Dogaa is and how it works, written for the model rather than for a screen.
 *
 * <p><b>Every figure is read from the live configuration beans</b> — fees, KYC ceilings, the
 * leverage ladder, the scoring weights, the lockout thresholds. Retyping them as prose would have
 * been shorter and would have started lying the first time someone edited a rate: the assistant
 * would then quote 1.5% to a customer the code charges 2%. A briefing that cannot drift is worth
 * more than one that reads well.
 *
 * <p>The text is rebuilt on demand, not cached: an administrator editing the lending ladder through
 * the back-office must change what the assistant tells the next customer, not what it tells them
 * after the next deployment.
 */
@Component
@RequiredArgsConstructor
public class ProductKnowledge {

    private final FeeProperties fees;
    private final KycProperties kyc;
    private final CreditProperties credit;
    private final ScoringProperties scoring;
    private final OtpProperties otp;
    private final AuthProperties auth;
    private final DisputeProperties disputes;
    private final FeeCalculator feeCalculator;

    public String briefing() {
        StringBuilder out = new StringBuilder(4000);

        out.append("""
                # Dogaa

                Dogaa est un portefeuille mobile money doublé d'une épargne programmée et d'un
                microcrédit adossé à cette épargne. Il vise la zone UEMOA (Togo, Sénégal, Côte
                d'Ivoire, Ghana). Devises : XOF, GHS, NGN, USD.

                ## Comptes

                Chaque utilisateur reçoit deux comptes à l'inscription, automatiquement :
                - le compte COURANT, pour l'argent du quotidien ;
                - le compte ÉPARGNE, qui sert de garantie aux prêts.

                Chaque solde se lit en deux parties : le solde disponible, dépensable, et le solde
                bloqué, qui ne l'est pas. Mettre de l'argent dans un coffre ou garantir un prêt
                déplace de l'argent du disponible vers le bloqué. Rien ne disparaît : c'est le même
                compte, mais la part bloquée est refusée aux retraits.

                On alimente le compte épargne en y virant de l'argent depuis le compte courant.
                C'est gratuit, immédiat, et ça ne compte pas dans les plafonds d'envoi : déplacer
                son propre argent n'est pas une dépense. On peut le récupérer de la même façon —
                sauf si un prêt est en cours, auquel cas l'épargne est bloquée en garantie.

                ## Inscription et connexion

                L'identifiant est le numéro de téléphone, jamais une adresse e-mail. Il n'y a pas de
                mot de passe : l'utilisateur choisit un code PIN de 4 à 6 chiffres.

                L'inscription se fait en trois étapes et le code OTP est obligatoire :
                1. l'utilisateur donne son numéro et reçoit un code par SMS ;
                2. il saisit ce code et obtient un jeton de vérification ;
                3. il crée son code PIN.
                """);

        out.append("""

                Le code fait %d chiffres, il expire au bout de %d minutes, il ne peut être renvoyé
                qu'après %d secondes, et il est annulé après %d saisies fausses. Le jeton de
                vérification est valable %d minutes et ne sert qu'une fois.

                Un code PIN trop simple est refusé : chiffres tous identiques (1111) ou qui se
                suivent (1234). Après %d codes PIN faux, le compte est bloqué %d minutes. Ce n'est
                pas une punition : un PIN à 6 chiffres ne fait qu'un million de combinaisons, sans
                ce blocage il serait devinable en quelques minutes.
                """.formatted(
                otp.getCodeLength(), otp.getTtl().toMinutes(), otp.getResendCooldown().toSeconds(),
                otp.getMaxAttempts(), otp.getVerificationTokenTtl().toMinutes(),
                auth.getMaxPinAttempts(), auth.getLockDuration().toMinutes()));

        out.append("""

                ## Frais

                - Dépôt (cash-in) : %s — c'est gratuit.
                - Transfert entre particuliers (P2P) : %s
                - Paiement marchand : %s
                - Paiement de facture : %s
                - Retrait (cash-out) : %s

                Les frais sont prélevés en plus du montant envoyé, sur le compte de l'émetteur.

                Ces taux sont ceux d'un compte non vérifié. Vérifier son identité les fait baisser :
                """.formatted(
                percent(fees.getCashInPercent()), percent(fees.getP2pPercent()),
                percent(fees.getMerchantPercent()), percent(fees.getBillPaymentPercent()),
                percent(fees.getCashOutPercent())));

        // Derived from FeeCalculator, not restated: the rebate is what a TIER_3 actually pays, and
        // quoting the base rate to them was simply wrong.
        for (KycTier tier : KycTier.values()) {
            BigDecimal multiplier = feeCalculator.tierMultiplier(tier);
            BigDecimal effectiveP2p = fees.getP2pPercent().multiply(multiplier);
            if (multiplier.compareTo(BigDecimal.ONE) == 0) {
                out.append("- %s : tarif plein, soit %s sur un transfert P2P.%n"
                        .formatted(tier, percent(effectiveP2p)));
            } else {
                out.append("- %s : %s des frais, soit %s sur un transfert P2P.%n"
                        .formatted(tier, percent(multiplier.multiply(new BigDecimal("100"))),
                                percent(effectiveP2p)));
            }
        }

        out.append("""

                ## Vérification d'identité (KYC) et plafonds

                Le niveau n'est jamais attribué à la main : il se déduit de ce que l'utilisateur a
                fourni. Fournir la pièce manquante fait monter le niveau tout seul.

                - TIER_0 : numéro de téléphone vérifié — obtenu dès l'inscription.
                - TIER_1 : profil complet (adresse, ville, pays).
                - TIER_2 : une pièce d'identité approuvée (carte nationale, passeport, permis de
                  conduire ou carte d'électeur). C'est le niveau qui ouvre le crédit.
                - TIER_3 : en plus, un selfie et un justificatif de domicile.

                Une pièce d'identité approuvée fait passer directement au TIER_2 même si le profil
                déclaratif est incomplet : une preuve vérifiée vaut mieux qu'une adresse déclarée.

                L'e-mail ne joue aucun rôle dans ces niveaux et reste facultatif. Le vérifier ne
                sert qu'à recevoir reçus et notifications.

                Plafonds par niveau (en XOF ; « illimité » signifie qu'il n'y a pas de plafond) :
                """);

        for (KycTier tier : KycTier.values()) {
            KycProperties.TierLimits limits = kyc.limitsFor(tier);
            out.append("- %s : %s par opération, %s par jour, %s par mois, solde maximum %s, crédit %s.%n"
                    .formatted(tier,
                            amount(limits.getPerTransaction()), amount(limits.getDaily()),
                            amount(limits.getMonthly()), amount(limits.getBalanceCap()),
                            limits.isCreditEligible() ? "accessible" : "fermé"));
        }

        out.append("""

                Ces plafonds s'appliquent partout de la même façon : à une opération lancée depuis
                l'application comme à une opération programmée exécutée la nuit. Une opération
                programmée qui dépasse le plafond échoue et l'utilisateur en est notifié ; elle
                n'est pas exécutée en douce.

                ## Coffres (épargne programmée)

                Un coffre est un objectif d'épargne : un nom, un montant visé, une échéance.
                Déposer dedans déplace l'argent du solde disponible vers le solde bloqué, donc
                l'argent d'un coffre ne peut plus être dépensé par une opération ordinaire. C'est
                le but : se rendre l'argent difficile à toucher.

                ## Opérations programmées

                Un même moteur couvre les dépôts en coffre, les transferts, les paiements marchands
                et les factures. Chaque nuit à minuit, le système regarde ce qui est dû, vérifie les
                fonds et le niveau KYC, exécute, écrit une transaction et planifie l'échéance
                suivante. Un échec est enregistré et notifié, pas ignoré. Une opération peut être
                mise en pause, reprise ou annulée à tout moment.
                """);

        out.append("""

                ## Score de crédit

                Le score va de 0 à 100 et se recalcule chaque nuit sur les %d derniers jours. Cinq
                axes :

                1. Discipline d'épargne (%d points) — la part des entrées qui part vers l'épargne.
                2. Stabilité financière (%d points) — un solde qui tient, sans découvert.
                3. Régularité des entrées (%d points) — de l'argent qui rentre souvent, pas d'un coup.
                4. Intensité d'usage (%d points) — un compte réellement utilisé, avec plusieurs
                   correspondants.
                5. Historique de crédit (%d points) — les prêts déjà remboursés.

                Trois garde-fous rendent le score difficile à mettre en scène, et il faut les
                expliquer à l'utilisateur qui demande comment le faire monter :

                - chaque signal est une proportion, jamais un nombre d'opérations : faire cent
                  virements d'un franc ne vaut rien ;
                - les montants inférieurs à %s sont ignorés, et l'ensemble est pondéré par le volume
                  réellement passé sur le compte (il faut de l'ordre de %s d'entrées sur la période
                  pour que les axes comptent à plein) ;
                - le score publié est lissé dans le temps : une bonne soirée ne le fait pas bondir,
                  une mauvaise ne l'effondre pas.

                La seule façon de monter le score est donc d'utiliser le compte normalement et
                régulièrement pendant plusieurs semaines. Il n'y a pas de raccourci, et il ne faut
                jamais en suggérer un.
                """.formatted(
                scoring.getWindowDays(), scoring.getSavingsDisciplineWeight(),
                scoring.getFinancialStabilityWeight(), scoring.getInflowRegularityWeight(),
                scoring.getUsageIntensityWeight(), scoring.getCreditHistoryWeight(),
                amount(scoring.getMaterialityThreshold()), amount(scoring.getMinimumWindowInflow())));

        out.append("""

                ## Crédit

                Un prêt est adossé à l'épargne : on ne prête qu'à quelqu'un qui a de l'argent sur
                son compte épargne, et cette épargne est bloquée pendant toute la durée du prêt.
                Bloquée veut dire : impossible de retirer, mais toujours possible de déposer.

                Conditions à remplir, toutes ensemble :
                - niveau KYC TIER_2 au minimum ;
                - score d'au moins %d ;
                - une épargne d'au moins %s ;
                - aucun prêt en cours.

                Le montant maximum dépend d'un barème progressif. Le levier — combien on peut
                emprunter par rapport à son épargne — se gagne en remboursant, jamais avec le score
                seul, et le taux baisse à mesure que le levier monte. Chaque palier porte aussi un
                plafond absolu, qui borne l'exposition sur un seul emprunteur.

                Barème actuel :
                """.formatted(credit.getMinimumScore(), amount(credit.getMinimumCollateral())));

        for (CreditProperties.Rung rung : credit.getLadder()) {
            out.append(("- à partir de %d prêt(s) remboursé(s) et d'un score de %d : emprunt jusqu'à "
                    + "%s fois l'épargne, plafonné à %s, au taux de %s par mois.%n")
                    .formatted(rung.getMinLoansRepaid(), rung.getMinScore(), rung.getLeverage(),
                            amount(rung.getMaxAmount()), percent(rung.getMonthlyRatePercent())));
        }

        out.append("""

                Le prêt court sur %d jours. À l'échéance, le remboursement (capital + intérêts) est
                prélevé automatiquement. Après %d jours de retard, une pénalité de %s par jour
                s'ajoute, plafonnée à %s du capital. Au-delà de %d jours, le prêt est considéré en
                défaut et l'épargne bloquée sert à se rembourser.

                ## Litiges

                Un utilisateur peut contester une transaction en donnant sa référence et un motif
                (fraude, double débit, transfert P2P). Un administrateur instruit le dossier. Un
                remboursement (chargeback) déplace de l'argent réel entre deux clients, donc il
                exige la validation de %d administrateurs différents : la même personne ne peut pas
                valider deux fois.
                """.formatted(
                credit.getTermDays(), credit.getGracePeriodDays(),
                percent(credit.getPenaltyPercentPerDay()), percent(credit.getPenaltyCapPercent()),
                credit.getDefaultAfterDays(), disputes.getValidationsRequired()));

        return out.toString();
    }

    // --- formatting -------------------------------------------------------

    /** A null ceiling means unlimited, and must never be shown as zero. */
    private static String amount(BigDecimal value) {
        if (value == null) {
            return "illimité";
        }
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(value) + " XOF";
    }

    private static String percent(BigDecimal value) {
        if (value == null || value.signum() == 0) {
            return "0 %";
        }
        return value.stripTrailingZeros().toPlainString() + " %";
    }
}
