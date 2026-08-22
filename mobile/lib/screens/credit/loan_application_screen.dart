import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/amount_input_field.dart';
import '../../core/widgets/primary_button.dart';
import '../../models/credit_score.dart';
import '../../providers/credit_provider.dart';

/// Formulaire de demande de prêt. `score` (déjà chargé par CreditScreen) sert
/// uniquement à afficher un indice (max/taux) — le backend reste seul juge.
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
  void dispose() {
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
    final isSubmitting = context.watch<CreditProvider>().isSubmitting;
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');
    final score = widget.score;

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
              if (score != null && score.tier != CreditTier.ineligible)
                Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.md),
                  child: Text(
                    'Jusqu\'à ${currencyFormat.format(score.maxLoanAmount)} XOF '
                    'à ${(score.monthlyRate * 100).toStringAsFixed(1)}%/mois selon votre palier actuel.',
                    style: AppTypography.bodySm,
                  ),
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
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: 'Envoyer la demande',
                isLoading: isSubmitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
