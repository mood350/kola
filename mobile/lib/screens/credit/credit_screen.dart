import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_in.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/primary_button.dart';
import '../../core/widgets/skeleton_box.dart';
import '../../core/widgets/skeleton_list_item.dart';
import '../../core/widgets/status_pill.dart';
import '../../models/credit_score.dart';
import '../../models/loan.dart';
import '../../providers/credit_provider.dart';
import '../../providers/wallet_provider.dart';
import 'loan_application_screen.dart';
import 'loan_detail_screen.dart';

/// Onglet Credit : score de crédit (affichage uniquement, jamais recalculé
/// côté mobile — cf. CreditScoringService backend) et liste des prêts.
class CreditScreen extends StatefulWidget {
  const CreditScreen({super.key});

  @override
  State<CreditScreen> createState() => _CreditScreenState();
}

class _CreditScreenState extends State<CreditScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<CreditProvider>().loadScore();
      context.read<CreditProvider>().loadLoans();
    });
  }

  StatusPillTone _toneFor(LoanStatus status) {
    switch (status) {
      case LoanStatus.pending:
        return StatusPillTone.warning;
      case LoanStatus.approved:
      case LoanStatus.disbursed:
        return StatusPillTone.info;
      case LoanStatus.repaid:
        return StatusPillTone.success;
      case LoanStatus.rejected:
      case LoanStatus.defaulted:
        return StatusPillTone.danger;
    }
  }

  @override
  Widget build(BuildContext context) {
    final creditProvider = context.watch<CreditProvider>();
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');
    final score = creditProvider.score;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Crédit', style: AppTypography.headingSm),
        /* Plus de bouton « Recalculer » : le score est recalculé à chaque
           consultation de cet écran (GET /credit/score) et après chaque
           transaction validée. Demander à l'utilisateur d'actionner un
           recalcul, c'était lui faire porter l'existence d'un cache. */
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            await context.read<CreditProvider>().loadScore();
            if (!context.mounted) return;
            await context.read<CreditProvider>().loadLoans();
          },
          child: ListView(
            padding: const EdgeInsets.all(AppSpacing.marginMobile),
            children: [
              if (creditProvider.isLoadingScore && score == null)
                const SkeletonBox(
                  height: 200,
                  borderRadius: BorderRadius.all(Radius.circular(AppRadius.lg)),
                )
              else if (creditProvider.scoreError != null && score == null)
                EmptyState(
                  icon: Icons.error_outline_rounded,
                  title: 'Impossible de charger le score',
                  subtitle: creditProvider.scoreError,
                )
              else if (score != null)
                _ScoreCard(score: score, currencyFormat: currencyFormat),
              const SizedBox(height: AppSpacing.xl),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [Text('Mes prêts', style: AppTypography.headingMd)],
              ),
              const SizedBox(height: AppSpacing.md),
              if (creditProvider.isLoadingLoans && creditProvider.loans.isEmpty)
                const SkeletonList(count: 2)
              else if (creditProvider.loans.isEmpty)
                const EmptyState(
                  icon: Icons.account_balance_outlined,
                  title: 'Aucun prêt en cours',
                )
              else
                ...creditProvider.loans.asMap().entries.map((entry) {
                  final index = entry.key;
                  final loan = entry.value;
                  return FadeSlideIn(
                    delay: Duration(milliseconds: index * 40),
                    child: Padding(
                      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                      child: KolaCard(
                        onTap: () async {
                          await Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => LoanDetailScreen(loanId: loan.id),
                            ),
                          );
                          if (context.mounted) {
                            context.read<CreditProvider>().loadLoans();
                          }
                        },
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${currencyFormat.format(loan.requestedAmount)} XOF',
                                    style: AppTypography.bodyMdBold,
                                  ),
                                  Text(
                                    '${loan.durationMonths} mois'
                                    '${loan.dueDate != null ? ' · échéance ${loan.dueDate!.day.toString().padLeft(2, '0')}/${loan.dueDate!.month.toString().padLeft(2, '0')}/${loan.dueDate!.year}' : ''}',
                                    style: AppTypography.bodySm,
                                  ),
                                ],
                              ),
                            ),
                            StatusPill(
                              label: loan.status.label,
                              tone: _toneFor(loan.status),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                }),
              const SizedBox(height: AppSpacing.lg),
              PrimaryButton(
                label: 'Demander un prêt',
                onPressed: score == null || score.tier == CreditTier.ineligible
                    ? null
                    : () async {
                        final wallet = context
                            .read<WalletProvider>()
                            .primaryWallet;
                        if (wallet == null) return;
                        await Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => LoanApplicationScreen(
                              walletId: wallet.id,
                              score: score,
                            ),
                          ),
                        );
                        if (context.mounted) {
                          context.read<CreditProvider>().loadLoans();
                        }
                      },
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ScoreCard extends StatelessWidget {
  final CreditScoreBreakdown score;
  final NumberFormat currencyFormat;

  const _ScoreCard({required this.score, required this.currencyFormat});

  @override
  Widget build(BuildContext context) {
    return KolaCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Score de crédit', style: AppTypography.bodySm),
              StatusPill(label: score.tier.label, tone: StatusPillTone.info),
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text('${score.totalScore}', style: AppTypography.displayLgMobile),
              Text(
                '/100',
                style: AppTypography.headingSm.copyWith(
                  color: AppColors.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          if (score.tier != CreditTier.ineligible) ...[
            Text(
              'Prêt maximum : ${currencyFormat.format(score.maxLoanAmount)} XOF',
              style: AppTypography.bodyMd,
            ),
            Text(
              'Taux mensuel : ${(score.monthlyRate * 100).toStringAsFixed(1)}%',
              style: AppTypography.bodyMd,
            ),
            const SizedBox(height: AppSpacing.md),
          ],
          /* ═══ LE DÉTAIL DU BARÈME N'EST PLUS AFFICHÉ ═══
             Un panneau dépliable listait chaque règle, ses points et son
             plafond. Publier les poids apprend à les optimiser — cinq
             bénéficiaires enregistrés pour cinq points, un dépôt hebdomadaire
             plutôt que mensuel pour le même argent. Un score que l'on sait
             fabriquer ne mesure plus rien.
             Reste ce qui engage Kola : la note, le palier, le taux.
             ⚠️ Masquage d'interface seulement : `details` arrive toujours dans
             la réponse de GET /credit/score. */
          Text(
            "Votre score est recalculé à partir de votre usage du compte : son "
            "ancienneté, la régularité de vos entrées, votre épargne, vos "
            "remboursements et votre niveau de vérification.",
            style: AppTypography.bodySm,
          ),
        ],
      ),
    );
  }
}
