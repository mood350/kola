import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/amount_input_field.dart';
import '../../core/widgets/primary_button.dart';
import '../../models/credit_score.dart';
import '../../models/loan.dart';
import '../../models/loan_capacity.dart';
import '../../providers/credit_provider.dart';

/// Formulaire de demande de prêt.
///
/// ═══ LE PLAFOND VIENT DE LA CAPACITÉ, PAS DU SCORE ═══
///
/// L'écran affichait « jusqu'à X XOF selon votre palier » : c'était le plafond
/// du PALIER, que la capacité réelle rend presque toujours inatteignable. Le
/// backend refuse désormais tout montant supérieur à la capacité calculée sur
/// les flux des 90 derniers jours, et cet écran lit donc `/credit/capacity`.
/// Le maximum change avec la durée : le prêt se rembourse en une fois à
/// l'échéance, une durée plus longue laisse davantage de temps pour accumuler.
///
/// ═══ AU-DELÀ D'UN SEUIL, LA DEMANDE EST EXAMINÉE ═══
///
/// Elle n'est alors ni accordée ni versée sur-le-champ : elle revient en
/// attente. L'écran le dit AVANT l'envoi, et la confirmation qui suit
/// l'annonce sans promettre d'argent qui n'arrivera pas tout de suite.
class LoanApplicationScreen extends StatefulWidget {
  final int walletId;
  final CreditScoreBreakdown? score;

  const LoanApplicationScreen({super.key, required this.walletId, this.score});

  @override
  State<LoanApplicationScreen> createState() => _LoanApplicationScreenState();
}

class _LoanApplicationScreenState extends State<LoanApplicationScreen> {
  final _amountController = TextEditingController();
  final _purposeController = TextEditingController();
  int _durationMonths = 3;

  @override
  void initState() {
    super.initState();
    // Après le premier rendu : `context.read` n'est pas utilisable pendant
    // initState, et la capacité n'est de toute façon pas nécessaire pour
    // afficher le squelette de l'écran.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<CreditProvider>().loadCapacity();
    });

    /* Le bandeau de capacité et l'avertissement d'examen dépendent du montant
       saisi : sans cette écoute, ils ne se mettraient à jour qu'au changement
       de durée, et l'avertissement apparaîtrait trop tard. */
    _amountController.addListener(_onAmountChanged);
  }

  void _onAmountChanged() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _amountController.removeListener(_onAmountChanged);
    _amountController.dispose();
    _purposeController.dispose();
    super.dispose();
  }

  String? _friendlyErrorFor(String? code, String? fallback) {
    switch (code) {
      case 'INSUFFICIENT_CREDIT_SCORE':
        return 'Votre score de crédit actuel est insuffisant pour ce montant.';
      case 'ACTIVE_LOAN_EXISTS':
        return 'Vous avez déjà un prêt en cours. Remboursez-le avant d\'en demander un nouveau.';
      case 'LOAN_NOT_ELIGIBLE':
        // Le backend nomme toujours la condition manquante (ancienneté du
        // compte, niveau de vérification) : son message est plus utile que
        // n'importe quelle reformulation générique.
        return fallback ?? 'Votre compte ne remplit pas encore les conditions.';
      default:
        return fallback ?? 'Impossible de soumettre la demande';
    }
  }

  Future<void> _submit() async {
    final amount = AmountInputField.parse(_amountController.text);
    if (amount < 1000) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le montant minimum est de 1 000 XOF')),
      );
      return;
    }

    final provider = context.read<CreditProvider>();
    final maximum = provider.capacity?.maxAmountFor(_durationMonths);
    if (maximum != null && amount > maximum) {
      final format = NumberFormat.decimalPattern('fr_FR');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Maximum ${format.format(maximum)} XOF sur $_durationMonths mois '
            'au vu de vos entrées et sorties.',
          ),
        ),
      );
      return;
    }

    final success = await provider.applyForLoan(
      walletId: widget.walletId,
      requestedAmount: amount,
      durationMonths: _durationMonths,
      purpose: _purposeController.text.trim().isEmpty
          ? null
          : _purposeController.text.trim(),
    );

    if (!mounted) return;
    if (success) {
      // Deux issues distinctes : versé, ou en attente d'examen. Les confondre
      // ferait attendre un virement qui ne viendra pas avant la décision.
      final pending = provider.lastSubmittedLoan?.status == LoanStatus.pending;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            pending
                ? 'Demande enregistrée. Elle est en cours d\'examen : vous serez notifié de la décision.'
                : 'Prêt accordé et versé sur votre compte.',
          ),
        ),
      );
      Navigator.of(context).pop();
    } else {
      final message = _friendlyErrorFor(
        provider.submitErrorCode,
        provider.submitError,
      );
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(message!)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CreditProvider>();
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');
    final capacity = provider.capacity;
    final score = widget.score;

    final maximum = capacity?.maxAmountFor(_durationMonths);
    final amount = AmountInputField.parse(_amountController.text);
    final needsReview =
        capacity != null && amount > capacity.manualReviewThreshold;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Demander un prêt', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (provider.isLoadingCapacity)
                Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.md),
                  child: Text(
                    'Calcul de votre capacité de remboursement…',
                    style: AppTypography.bodySm,
                  ),
                )
              else if (capacity != null && maximum != null)
                _CapacityBanner(
                  maximum: maximum,
                  durationMonths: _durationMonths,
                  capacity: capacity,
                  monthlyRate: score?.monthlyRate ?? 0,
                  currencyFormat: currencyFormat,
                ),
              AmountInputField(
                controller: _amountController,
                label: 'Montant demandé',
              ),
              const SizedBox(height: AppSpacing.md),
              Text('Durée (mois)', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              Row(
                children: [
                  IconButton(
                    icon: const Icon(
                      Icons.remove_circle_outline,
                      color: AppColors.primary,
                    ),
                    onPressed: _durationMonths > 1
                        ? () => setState(() => _durationMonths--)
                        : null,
                  ),
                  Text('$_durationMonths', style: AppTypography.headingSm),
                  IconButton(
                    icon: const Icon(
                      Icons.add_circle_outline,
                      color: AppColors.primary,
                    ),
                    onPressed: _durationMonths < 12
                        ? () => setState(() => _durationMonths++)
                        : null,
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.md),
              Text('Motif (optionnel)', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _purposeController,
                decoration: InputDecoration(
                  hintText: 'ex : Achat de matériel',
                  filled: true,
                  fillColor: AppColors.surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              if (needsReview) ...[
                const SizedBox(height: AppSpacing.md),
                _ReviewNotice(
                  threshold: capacity.manualReviewThreshold,
                  currencyFormat: currencyFormat,
                ),
              ],
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: needsReview
                    ? 'Soumettre à examen'
                    : 'Envoyer la demande',
                isLoading: provider.isSubmitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Ce que l'emprunteur peut réellement obtenir, et pourquoi.
///
/// Les chiffres du calcul sont montrés : l'utilisateur reconnaît ses propres
/// entrées et sorties, et voit sur quoi agir. Un plafond sans explication se
/// vit comme un refus arbitraire.
class _CapacityBanner extends StatelessWidget {
  final double maximum;
  final int durationMonths;
  final LoanCapacity capacity;
  final double monthlyRate;
  final NumberFormat currencyFormat;

  const _CapacityBanner({
    required this.maximum,
    required this.durationMonths,
    required this.capacity,
    required this.monthlyRate,
    required this.currencyFormat,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.primaryFixed,
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Jusqu\'à ${currencyFormat.format(maximum)} XOF sur $durationMonths mois'
            '${monthlyRate > 0 ? ' à ${(monthlyRate * 100).toStringAsFixed(1)}%/mois' : ''}',
            style: AppTypography.bodyMdBold,
          ),
          const SizedBox(height: AppSpacing.xxs),
          Text(capacity.limitingFactorLabel, style: AppTypography.bodySm),
          const SizedBox(height: AppSpacing.xs),
          Text(
            'Entrées ${currencyFormat.format(capacity.monthlyInflow)} · '
            'Sorties ${currencyFormat.format(capacity.monthlyOutflow)} · '
            'Disponible ${currencyFormat.format(capacity.monthlyDisposable)} par mois',
            style: AppTypography.bodySm,
          ),
        ],
      ),
    );
  }
}

/// Avertissement d'examen manuel, affiché dès que le montant saisi dépasse le
/// seuil — donc avant l'envoi, pas après.
class _ReviewNotice extends StatelessWidget {
  final double threshold;
  final NumberFormat currencyFormat;

  const _ReviewNotice({required this.threshold, required this.currencyFormat});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.warning.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.schedule_rounded,
            color: AppColors.warning,
            size: 20,
          ),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              'Au-delà de ${currencyFormat.format(threshold)} XOF, votre demande '
              'est examinée par notre équipe avant versement. Aucun montant '
              'n\'est versé d\'ici là.',
              style: AppTypography.bodySm,
            ),
          ),
        ],
      ),
    );
  }
}
