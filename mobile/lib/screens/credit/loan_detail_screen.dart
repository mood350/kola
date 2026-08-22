import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/primary_button.dart';
import '../../core/widgets/status_pill.dart';
import '../../models/loan.dart';
import '../../providers/credit_provider.dart';

/// Détail d'un prêt : montants, statut, remboursement.
class LoanDetailScreen extends StatefulWidget {
  final int loanId;

  const LoanDetailScreen({super.key, required this.loanId});

  @override
  State<LoanDetailScreen> createState() => _LoanDetailScreenState();
}

class _LoanDetailScreenState extends State<LoanDetailScreen> {
  Loan? _loan;

  @override
  void initState() {
    super.initState();
    _syncFromProvider();
  }

  void _syncFromProvider() {
    final loans = context.read<CreditProvider>().loans;
    _loan = loans.where((l) => l.id == widget.loanId).firstOrNull;
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

  Future<void> _repay() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Rembourser ce prêt ?'),
        content: const Text(
          'Le montant total sera prélevé sur votre wallet en une seule fois.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Rembourser'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    final provider = context.read<CreditProvider>();
    final success = await provider.repay(widget.loanId);
    if (!mounted) return;
    if (success) {
      setState(_syncFromProvider);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.submitError ?? 'Échec du remboursement'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final loan = _loan;
    final isSubmitting = context.watch<CreditProvider>().isSubmitting;
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Détail du prêt', style: AppTypography.headingSm),
      ),
      body: loan == null
          ? const Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            )
          : SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(AppSpacing.marginMobile),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    KolaCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                '${currencyFormat.format(loan.requestedAmount)} XOF',
                                style: AppTypography.headingSm,
                              ),
                              StatusPill(
                                label: loan.status.label,
                                tone: _toneFor(loan.status),
                              ),
                            ],
                          ),
                          const SizedBox(height: AppSpacing.md),
                          _InfoRow(
                            label: 'Durée',
                            value: '${loan.durationMonths} mois',
                          ),
                          _InfoRow(
                            label: 'Taux mensuel',
                            value:
                                '${(loan.monthlyRate * 100).toStringAsFixed(1)}%',
                          ),
                          _InfoRow(
                            label: 'Total à rembourser',
                            value:
                                '${currencyFormat.format(loan.totalRepayment)} XOF',
                          ),
                          if (loan.dueDate != null)
                            _InfoRow(
                              label: 'Échéance',
                              value:
                                  '${loan.dueDate!.day.toString().padLeft(2, '0')}/${loan.dueDate!.month.toString().padLeft(2, '0')}/${loan.dueDate!.year}',
                            ),
                          if (loan.purpose != null)
                            _InfoRow(label: 'Motif', value: loan.purpose!),
                          if (loan.rejectionReason != null)
                            _InfoRow(
                              label: 'Motif du refus',
                              value: loan.rejectionReason!,
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    if (loan.status == LoanStatus.disbursed)
                      PrimaryButton(
                        label: 'Rembourser',
                        isLoading: isSubmitting,
                        onPressed: _repay,
                      ),
                  ],
                ),
              ),
            ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.xs),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: AppTypography.bodySm),
          Text(value, style: AppTypography.bodyMdBold),
        ],
      ),
    );
  }
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
