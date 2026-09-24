import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/loan.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';

/// Statuts qui clôturent un prêt : il bascule alors dans l'historique.
const _closedStatuses = {'REPAID', 'DEFAULTED'};

/// Espace financement : prêts en cours, offre disponible selon le score, et
/// historique des prêts soldés.
class CreditScreen extends StatefulWidget {
  const CreditScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<CreditScreen> createState() => _CreditScreenState();
}

class _CreditScreenState extends State<CreditScreen> {
  bool _historyOpen = false;

  String _shortId(Loan loan) =>
      loan.id.length >= 8 ? loan.id.substring(0, 8) : loan.id;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final eligibility = data.eligibility;

    final active = data.loans
        .where((loan) => !_closedStatuses.contains(loan.status))
        .toList();
    final history = data.loans
        .where((loan) => _closedStatuses.contains(loan.status))
        .toList();
    final outstanding = active.fold<double>(
      0,
      (sum, loan) => sum + loan.outstanding,
    );
    final eligible = eligibility?.eligible ?? false;

    return KolaScreen(
      route: KolaRoute.credit,
      onNavigate: widget.onNavigate,
      children: [
        const PageHeader(
          overline: 'ESPACE FINANCEMENT',
          title: 'Mes crédits',
          subtitle: 'Suivez vos prêts et découvrez vos offres.',
          icon: KolaIcons.cardOutline,
        ),
        const SizedBox(height: 18),
        Container(
          padding: const EdgeInsets.all(17),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('EN COURS', style: _summaryLabel),
                    const SizedBox(height: 3),
                    Text(
                      '${active.length}',
                      style: AppTypography.balance.copyWith(fontSize: 25),
                    ),
                  ],
                ),
              ),
              Container(
                width: 1,
                height: 42,
                margin: const EdgeInsets.symmetric(horizontal: 15),
                color: AppColors.white.withValues(alpha: 0.13),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('CAPITAL RESTANT', style: _summaryLabel),
                    const SizedBox(height: 3),
                    RichText(
                      text: TextSpan(
                        style: AppTypography.screenTitle.copyWith(
                          fontSize: 20,
                          color: AppColors.white,
                        ),
                        children: [
                          TextSpan(text: money(outstanding)),
                          TextSpan(
                            text: ' FCFA',
                            style: AppTypography.badge.copyWith(
                              fontSize: 9,
                              color: AppColors.yellow,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        _SectionRow(
          title: 'Prêts en cours',
          trailing: Pill(
            active.any((loan) => loan.status == 'OVERDUE')
                ? 'En retard'
                : 'À jour',
            green: !active.any((loan) => loan.status == 'OVERDUE'),
            red: active.any((loan) => loan.status == 'OVERDUE'),
          ),
        ),
        for (final loan in active)
          Padding(
            padding: const EdgeInsets.only(bottom: 11),
            child: _LoanCard(
              loan: loan,
              shortId: _shortId(loan),
              onTap: () => widget.onNavigate(KolaRoute.loanDetail),
            ),
          ),
        if (!data.loading && active.isEmpty)
          const KolaCard(
            padding: EdgeInsets.symmetric(vertical: 25),
            child: EmptyState(
              icon: KolaIcons.documentTextOutline,
              title: 'Aucun prêt en cours',
              message: 'Vos futurs prêts apparaîtront ici.',
            ),
          ),
        _SectionRow(
          title: eligible ? 'Offre disponible' : 'Accès au crédit',
          trailing: Pill('Score ${eligibility?.score ?? 0}/100'),
        ),
        Opacity(
          opacity: eligible ? 1 : 0.72,
          child: GestureDetector(
            onTap: eligible
                ? () => widget.onNavigate(KolaRoute.loan)
                : null,
            child: Container(
              padding: const EdgeInsets.all(17),
              decoration: BoxDecoration(
                color: AppColors.yellow,
                borderRadius: BorderRadius.circular(AppRadius.card),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 43,
                        height: 43,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: AppColors.white.withValues(alpha: 0.6),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          KolaIcons.flashOutline,
                          size: 24,
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              eligible
                                  ? 'Prêt Express pré-approuvé'
                                  : 'Crédit non disponible',
                              style: AppTypography.button.copyWith(
                                fontSize: 14,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            const SizedBox(height: 3),
                            Text(
                              eligible
                                  ? 'Sans caution • Réponse immédiate'
                                  : (eligibility?.blockers.isNotEmpty ?? false)
                                  ? eligibility!.blockers.first
                                  : 'Calcul de l’éligibilité indisponible',
                              style: AppTypography.caption.copyWith(
                                fontSize: 9,
                                color: AppColors.yellowDark,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'JUSQU’À',
                    style: AppTypography.badge.copyWith(
                      fontSize: 8,
                      fontWeight: FontWeight.w800,
                      color: AppColors.yellowDark,
                    ),
                  ),
                  RichText(
                    text: TextSpan(
                      style: AppTypography.amount.copyWith(
                        fontSize: 27,
                        fontWeight: FontWeight.w900,
                      ),
                      children: [
                        TextSpan(
                          text: money(eligibility?.maxLoanAmount ?? 0),
                        ),
                        TextSpan(
                          text: ' FCFA',
                          style: AppTypography.amount.copyWith(fontSize: 13),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 13),
                  Container(
                    height: 42,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: AppColors.white,
                      borderRadius: BorderRadius.circular(21),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          eligible
                              ? 'Simuler mon prêt'
                              : 'Conditions non remplies',
                          style: AppTypography.button.copyWith(fontSize: 12),
                        ),
                        const SizedBox(width: 8),
                        const Icon(
                          KolaIcons.arrowForward,
                          size: 18,
                          color: AppColors.primary,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        GestureDetector(
          onTap: () => setState(() => _historyOpen = !_historyOpen),
          behavior: HitTestBehavior.opaque,
          child: Padding(
            padding: const EdgeInsets.only(top: AppSpacing.lg, bottom: 10),
            child: Row(
              children: [
                const Icon(
                  KolaIcons.timeOutline,
                  size: 19,
                  color: AppColors.primary,
                ),
                const SizedBox(width: 7),
                Expanded(
                  child: Text(
                    'Historique des prêts',
                    style: AppTypography.section.copyWith(fontSize: 17),
                  ),
                ),
                Icon(
                  _historyOpen
                      ? KolaIcons.chevronUp
                      : KolaIcons.chevronDown,
                  size: 19,
                  color: AppColors.primary,
                ),
              ],
            ),
          ),
        ),
        if (_historyOpen)
          KolaCard(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
            child: history.isEmpty
                ? const EmptyState(
                    icon: KolaIcons.timeOutline,
                    title: 'Aucun ancien prêt.',
                  )
                : Column(
                    children: [
                      for (var i = 0; i < history.length; i++)
                        _HistoryRow(
                          loan: history[i],
                          shortId: _shortId(history[i]),
                          withDivider: i > 0,
                          onTap: () =>
                              widget.onNavigate(KolaRoute.loanDetail),
                        ),
                    ],
                  ),
          ),
      ],
    );
  }
}

TextStyle get _summaryLabel => AppTypography.badge.copyWith(
  fontSize: 8,
  fontWeight: FontWeight.w700,
  color: const Color(0xFFADC6FF),
);

class _SectionRow extends StatelessWidget {
  const _SectionRow({required this.title, required this.trailing});

  final String title;
  final Widget trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 23, bottom: 11),
      child: Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: AppTypography.section.copyWith(fontSize: 17),
            ),
          ),
          trailing,
        ],
      ),
    );
  }
}

class _LoanCard extends StatelessWidget {
  const _LoanCard({
    required this.loan,
    required this.shortId,
    required this.onTap,
  });

  final Loan loan;
  final String shortId;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return KolaCard(
      onTap: onTap,
      padding: const EdgeInsets.all(15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const IconCircle(KolaIcons.cashOutline),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Prêt KOLA $shortId',
                      style: AppTypography.bodyBold.copyWith(
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'Montant initial : ${money(loan.principal)} FCFA',
                      style: AppTypography.caption.copyWith(fontSize: 9),
                    ),
                  ],
                ),
              ),
              const Icon(
                KolaIcons.chevronForward,
                size: 20,
                color: AppColors.primary,
              ),
            ],
          ),
          const SizedBox(height: 17),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('RESTANT À PAYER', style: _tinyLabel),
                    const SizedBox(height: 3),
                    RichText(
                      text: TextSpan(
                        style: AppTypography.amount.copyWith(
                          fontSize: 21,
                          fontWeight: FontWeight.w900,
                        ),
                        children: [
                          TextSpan(text: money(loan.outstanding)),
                          TextSpan(
                            text: ' FCFA',
                            style: AppTypography.badge.copyWith(
                              fontSize: 9,
                              color: AppColors.yellowDark,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text('PROGRESSION', style: _tinyLabel),
                  const SizedBox(height: 4),
                  Text(
                    '${loan.repaidPercent.round()}% remboursé',
                    style: AppTypography.caption.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppColors.greenDark,
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 9),
          KolaProgress(loan.repaidPercent),
          const SizedBox(height: 13),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppColors.pale,
              borderRadius: BorderRadius.circular(9),
            ),
            child: Row(
              children: [
                const Icon(
                  KolaIcons.calendarOutline,
                  size: 16,
                  color: AppColors.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Prochaine échéance',
                        style: AppTypography.caption.copyWith(fontSize: 8),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        loan.dueAt == null ? '—' : shortDate(loan.dueAt!),
                        style: AppTypography.caption.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppColors.ink,
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  '${money(loan.outstanding)} FCFA',
                  style: AppTypography.badge.copyWith(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          Text(
            'Touchez la carte pour afficher tous les détails',
            textAlign: TextAlign.center,
            style: AppTypography.caption.copyWith(fontSize: 8),
          ),
        ],
      ),
    );
  }
}

TextStyle get _tinyLabel => AppTypography.badge.copyWith(
  fontSize: 7,
  fontWeight: FontWeight.w700,
  color: AppColors.muted,
);

class _HistoryRow extends StatelessWidget {
  const _HistoryRow({
    required this.loan,
    required this.shortId,
    required this.withDivider,
    required this.onTap,
  });

  final Loan loan;
  final String shortId;
  final bool withDivider;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final repaid = loan.status == 'REPAID';

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        constraints: const BoxConstraints(minHeight: 65),
        decoration: withDivider
            ? const BoxDecoration(
                border: Border(top: BorderSide(color: AppColors.border)),
              )
            : null,
        child: Row(
          children: [
            Icon(
              repaid
                  ? KolaIcons.checkmarkCircleOutline
                  : KolaIcons.alertCircleOutline,
              size: 22,
              color: repaid ? AppColors.green : AppColors.red,
            ),
            const SizedBox(width: 9),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Prêt KOLA $shortId',
                    style: AppTypography.badge.copyWith(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    loan.settledAt != null
                        ? 'Terminé le ${shortDate(loan.settledAt!)}'
                        : repaid
                        ? 'Remboursé'
                        : 'En défaut',
                    style: AppTypography.caption.copyWith(fontSize: 8),
                  ),
                ],
              ),
            ),
            Text(
              '${money(loan.principal)} F',
              style: AppTypography.badge.copyWith(
                fontSize: 11,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(width: 6),
            const Icon(
              KolaIcons.chevronForward,
              size: 16,
              color: AppColors.muted,
            ),
          ],
        ),
      ),
    );
  }
}
