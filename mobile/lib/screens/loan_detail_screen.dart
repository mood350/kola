import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/utils/idempotency_key.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/loan.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/credit_service.dart';

/// Détail du prêt en cours : capital restant, coût, échéance, remboursement.
class LoanDetailScreen extends StatefulWidget {
  const LoanDetailScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<LoanDetailScreen> createState() => _LoanDetailScreenState();
}

class _LoanDetailScreenState extends State<LoanDetailScreen> {
  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final loan = data.activeLoan;

    if (loan == null) {
      return KolaScreen(
        route: KolaRoute.loanDetail,
        onNavigate: widget.onNavigate,
        children: [
          BackLink(
            label: 'Mes crédits',
            onTap: () => widget.onNavigate(KolaRoute.credit),
          ),
          const KolaCard(
            padding: EdgeInsets.symmetric(vertical: 35),
            child: EmptyState(
              icon: KolaIcons.cashOutline,
              title: 'Aucun prêt en cours',
              message: 'Votre prochain crédit apparaîtra ici.',
            ),
          ),
        ],
      );
    }

    final overdue = loan.status == 'OVERDUE';
    final days = loan.daysLeft;

    return KolaScreen(
      route: KolaRoute.loanDetail,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Mes crédits',
          onTap: () => widget.onNavigate(KolaRoute.credit),
        ),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'DÉTAIL DU PRÊT',
                    style: AppTypography.badge.copyWith(
                      fontSize: 8,
                      fontWeight: FontWeight.w800,
                      color: AppColors.greenDark,
                    ),
                  ),
                  Text(
                    'Prêt Express KOLA',
                    style: AppTypography.screenTitle.copyWith(fontSize: 20),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    'Référence ${loan.id.length >= 8 ? loan.id.substring(0, 8).toUpperCase() : loan.id.toUpperCase()}',
                    style: AppTypography.caption.copyWith(fontSize: 8),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            Pill(
              overdue ? 'En retard' : 'À jour',
              green: !overdue,
              red: overdue,
            ),
          ],
        ),
        const SizedBox(height: 17),
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'CAPITAL RESTANT',
                style: AppTypography.fieldLabel.copyWith(
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFFADC6FF),
                ),
              ),
              const SizedBox(height: 5),
              RichText(
                text: TextSpan(
                  style: AppTypography.balance.copyWith(fontSize: 28),
                  children: [
                    TextSpan(text: money(loan.outstanding)),
                    TextSpan(
                      text: ' FCFA',
                      style: AppTypography.badge.copyWith(
                        fontSize: 12,
                        color: AppColors.white,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${money(loan.amountRepaid)} FCFA déjà remboursés',
                    style: AppTypography.badge.copyWith(
                      fontSize: 8,
                      fontWeight: FontWeight.w400,
                      color: AppColors.white,
                    ),
                  ),
                  Text(
                    '${loan.repaidPercent.round()}%',
                    style: AppTypography.badge.copyWith(
                      fontSize: 8,
                      fontWeight: FontWeight.w400,
                      color: AppColors.white,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 9),
              KolaProgress(loan.repaidPercent, color: AppColors.yellow),
            ],
          ),
        ),
        const SizedBox(height: 14),
        KolaCard(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
          child: Column(
            children: [
              DetailRow(
                label: 'Montant emprunté',
                value: '${money(loan.principal)} FCFA',
              ),
              DetailRow(
                label: 'Intérêts',
                value:
                    '${loan.monthlyRatePercent.toStringAsFixed(1)} % • ${money(loan.interestAmount)} FCFA',
              ),
              if (loan.penaltyAmount > 0)
                DetailRow(
                  label: 'Pénalités de retard',
                  value: '${money(loan.penaltyAmount)} FCFA',
                ),
              DetailRow(
                label: 'Garantie bloquée',
                value: '${money(loan.collateralAmount)} FCFA',
              ),
              DetailRow(
                label: 'Débloqué le',
                value: loan.disbursedAt == null
                    ? '—'
                    : shortDate(loan.disbursedAt!),
              ),
              DetailRow(
                label: 'Échéance',
                value: loan.dueAt == null
                    ? '—'
                    : '${shortDate(loan.dueAt!)}${days == null ? '' : days >= 0 ? ' (J-$days)' : ' (${-days} j de retard)'}',
              ),
              DetailRow(
                label: 'Total à rembourser',
                value: '${money(loan.totalDue)} FCFA',
                last: true,
              ),
            ],
          ),
        ),
        if (loan.shortfallAmount > 0) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.redPale,
              borderRadius: BorderRadius.circular(11),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(
                  KolaIcons.alertCircleOutline,
                  size: 18,
                  color: AppColors.red,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Il manque ${money(loan.shortfallAmount)} FCFA pour solder ce prêt à l’échéance.',
                    style: AppTypography.caption.copyWith(
                      fontSize: 9,
                      color: AppColors.redDark,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
        const SizedBox(height: 16),
        PrimaryButton(
          label: 'Rembourser ce prêt',
          icon: KolaIcons.cardOutline,
          onPressed: () => _openRepaySheet(context, loan),
        ),
      ],
    );
  }

  void _openRepaySheet(BuildContext context, Loan loan) {
    final data = context.read<KolaDataProvider>();
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _RepaySheet(loan: loan, data: data),
    );
  }
}

class _RepaySheet extends StatefulWidget {
  const _RepaySheet({required this.loan, required this.data});

  final Loan loan;
  final KolaDataProvider data;

  @override
  State<_RepaySheet> createState() => _RepaySheetState();
}

class _RepaySheetState extends State<_RepaySheet> {
  final _credit = CreditService();
  final _idempotency = IdempotencyKeyHolder();
  late final TextEditingController _amount = TextEditingController(
    text: widget.loan.outstanding.round().toString(),
  );
  bool _saving = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _repay() async {
    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }
    if (value > widget.loan.outstanding) {
      _alert(
        'Montant trop élevé',
        'Il ne reste que ${money(widget.loan.outstanding)} FCFA à rembourser.',
      );
      return;
    }

    setState(() => _saving = true);
    final result = await _credit.repay(
      loanId: widget.loan.id,
      amount: value,
      idempotencyKey: _idempotency.forIntent(
        '${widget.loan.id}:${value.round()}',
      ),
    );
    if (!mounted) return;
    setState(() => _saving = false);

    if (!result.success) {
      _alert(
        'Remboursement impossible',
        result.error?.message ?? 'Erreur serveur.',
      );
      return;
    }

    _idempotency.reset();
    await widget.data.refresh();
    if (!mounted) return;
    Navigator.of(context).pop();
  }

  void _alert(String title, String message) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title, style: AppTypography.cardTitle),
        content: Text(message, style: AppTypography.small),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        padding: const EdgeInsets.fromLTRB(22, 22, 22, 35),
        decoration: const BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppRadius.sheet),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SheetHeader(
              title: 'Rembourser mon prêt',
              subtitle:
                  'Restant dû : ${money(widget.loan.outstanding)} FCFA',
            ),
            const FieldLabel('Montant du remboursement'),
            Container(
              height: 52,
              padding: const EdgeInsets.symmetric(horizontal: 13),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _amount,
                      keyboardType: TextInputType.number,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      style: AppTypography.amount.copyWith(fontSize: 22),
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        filled: false,
                        isDense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ),
                  Text(
                    'FCFA',
                    style: AppTypography.badge.copyWith(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: AppColors.yellowDark,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            PrimaryButton(
              label: 'Confirmer le remboursement',
              icon: KolaIcons.checkmarkCircleOutline,
              loading: _saving,
              onPressed: _repay,
            ),
          ],
        ),
      ),
    );
  }
}
