import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/utils/idempotency_key.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/credit_service.dart';

/// Montant minimum d'un prêt, aligné sur la borne basse du simulateur.
const _minLoan = 20000.0;

/// Simulateur de prêt : le client choisit un montant, voit le coût réel, puis
/// demande le déblocage.
///
/// Toutes les valeurs (plafond, taux, durée, score) viennent de
/// `/api/v1/credit/eligibility` : le simulateur ne doit jamais annoncer une
/// offre que le backend refuserait ensuite.
class LoanScreen extends StatefulWidget {
  const LoanScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<LoanScreen> createState() => _LoanScreenState();
}

class _LoanScreenState extends State<LoanScreen> {
  final _credit = CreditService();
  final _idempotency = IdempotencyKeyHolder();

  double? _amount;
  bool _requesting = false;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final eligibility = data.eligibility;
    final ceiling = eligibility?.maxLoanAmount ?? 0;
    final compact = MediaQuery.of(context).size.width < 370;

    // Tant que le plafond n'est pas connu, le curseur reste sur le minimum
    // plutôt que d'afficher un montant que le backend rejetterait.
    final amount = (_amount ?? (ceiling > 0 ? ceiling / 2 : _minLoan)).clamp(
      0.0,
      ceiling,
    );
    final ratePercent = eligibility?.monthlyRatePercent ?? 0;
    final interest = amount * ratePercent / 100;
    final total = amount + interest;
    final termDays = eligibility?.termDays ?? 30;

    // Le levier dit combien on peut emprunter par franc épargné : la garantie
    // à bloquer est donc le montant emprunté divisé par ce levier.
    final leverage = eligibility?.leverageRatio ?? 0;
    final collateral = leverage > 0 ? amount / leverage : amount;

    final presets = <double>[
      if (ceiling >= _minLoan) _minLoan,
      if (ceiling > 0) ceiling * 0.35,
      if (ceiling > 0) ceiling * 0.7,
      if (ceiling > 0) ceiling,
    ];

    return KolaScreen(
      route: KolaRoute.loan,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Score & niveau',
          onTap: () => widget.onNavigate(KolaRoute.credit),
        ),
        Row(
          children: [
            Container(
              width: 50,
              height: 50,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(15),
              ),
              child: const Icon(
                KolaIcons.cashOutline,
                size: 24,
                color: AppColors.yellow,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'PRÊT PRÉ-APPROUVÉ',
                    style: AppTypography.badge.copyWith(
                      fontSize: 8,
                      fontWeight: FontWeight.w800,
                      color: AppColors.greenDark,
                    ),
                  ),
                  Text(
                    'Prêt Express',
                    style: AppTypography.screenTitle.copyWith(
                      fontSize: compact ? 22 : 25,
                    ),
                  ),
                  Text(
                    'Une offre claire basée sur votre score KOLA.',
                    style: AppTypography.caption,
                  ),
                ],
              ),
            ),
          ],
        ),
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 17),
          child: Container(
            padding: const EdgeInsets.all(13),
            decoration: BoxDecoration(
              color: eligibility?.eligible == true
                  ? AppColors.greenPale
                  : AppColors.pale,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Icon(
                  eligibility?.eligible == true
                      ? KolaIcons.shieldCheckmark
                      : KolaIcons.shieldOutline,
                  size: 20,
                  color: eligibility?.eligible == true
                      ? AppColors.green
                      : AppColors.muted,
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        eligibility?.eligible == true
                            ? 'Vous êtes éligible'
                            : 'Pas encore éligible',
                        style: AppTypography.smallBold.copyWith(
                          fontSize: 12,
                          fontWeight: FontWeight.w800,
                          color: AppColors.greenDark,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        eligibility?.eligible == true
                            ? 'Jusqu’à ${money(ceiling)} FCFA disponibles'
                            : 'Alimentez Bankivi pour ouvrir votre capacité',
                        style: AppTypography.caption.copyWith(fontSize: 9),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.greenDark,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '${eligibility?.score ?? 0}/100',
                    style: AppTypography.badge.copyWith(
                      fontSize: 9,
                      fontWeight: FontWeight.w800,
                      color: AppColors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        KolaCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.pale,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  children: [
                    Text(
                      'MONTANT À EMPRUNTER',
                      style: AppTypography.badge.copyWith(
                        fontSize: 8,
                        color: AppColors.muted,
                      ),
                    ),
                    const SizedBox(height: 5),
                    FittedBox(
                      child: RichText(
                        text: TextSpan(
                          style: AppTypography.amount.copyWith(
                            fontSize: compact ? 23 : 27,
                            fontWeight: FontWeight.w900,
                          ),
                          children: [
                            TextSpan(text: money(amount)),
                            TextSpan(
                              text: ' FCFA',
                              style: AppTypography.badge.copyWith(
                                fontSize: 12,
                                color: AppColors.yellowDark,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'Plafond disponible : ${money(ceiling)} FCFA',
                      style: AppTypography.caption.copyWith(fontSize: 8),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  for (var i = 0; i < presets.length; i++)
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(right: 6),
                        child: GestureDetector(
                          onTap: () =>
                              setState(() => _amount = presets[i].roundToDouble()),
                          child: Container(
                            height: 38,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: (amount - presets[i]).abs() < 1
                                  ? AppColors.primary
                                  : AppColors.pale2,
                              borderRadius: BorderRadius.circular(9),
                            ),
                            child: FittedBox(
                              child: Text(
                                i == presets.length - 1
                                    ? 'Max'
                                    : money(presets[i]),
                                style: AppTypography.badge.copyWith(
                                  fontSize: 9,
                                  color: (amount - presets[i]).abs() < 1
                                      ? AppColors.yellow
                                      : AppColors.primary,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
              if (ceiling >= _minLoan)
                SliderTheme(
                  data: SliderThemeData(
                    trackHeight: 5,
                    activeTrackColor: AppColors.yellow,
                    inactiveTrackColor: AppColors.pale2,
                    thumbColor: AppColors.yellow,
                    overlayColor: AppColors.yellow.withValues(alpha: 0.15),
                    thumbShape: const RoundSliderThumbShape(
                      enabledThumbRadius: 7,
                    ),
                  ),
                  child: Slider(
                    value: amount.clamp(_minLoan, ceiling),
                    min: _minLoan,
                    max: ceiling,
                    onChanged: (value) =>
                        setState(() => _amount = value.roundToDouble()),
                  ),
                ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${money(_minLoan)} FCFA',
                    style: AppTypography.badge.copyWith(
                      fontSize: 7,
                      fontWeight: FontWeight.w400,
                      color: AppColors.ink,
                    ),
                  ),
                  Text(
                    '${money(ceiling)} FCFA',
                    style: AppTypography.badge.copyWith(
                      fontSize: 7,
                      fontWeight: FontWeight.w400,
                      color: AppColors.ink,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 15),
              Container(
                padding: const EdgeInsets.all(11),
                decoration: BoxDecoration(
                  color: AppColors.pale,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  children: [
                    _LoanDetail(
                      KolaIcons.timeOutline,
                      'Durée du prêt',
                      '$termDays jours nets',
                    ),
                    _LoanDetail(
                      KolaIcons.calculatorOutline,
                      'Intérêts (${ratePercent.toStringAsFixed(1)} %)',
                      '${money(interest)} FCFA',
                    ),
                    _LoanDetail(
                      KolaIcons.shieldCheckmarkOutline,
                      'Garantie bloquée',
                      '${money(collateral)} FCFA',
                      last: true,
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 13),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Total à rembourser',
                              style: AppTypography.small.copyWith(
                                fontSize: 12,
                                color: AppColors.ink,
                              ),
                            ),
                          ),
                          Flexible(
                            child: FittedBox(
                              child: Text(
                                '${money(total)} FCFA',
                                style: AppTypography.amount.copyWith(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 13),
                child: Container(
                  padding: const EdgeInsets.all(11),
                  decoration: BoxDecoration(
                    color: AppColors.pale,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(
                        KolaIcons.informationCircleOutline,
                        size: 18,
                        color: AppColors.primary,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Aucun frais de dossier caché. Le récapitulatif final sera affiché avant votre confirmation.',
                          style: AppTypography.caption.copyWith(fontSize: 9),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              PrimaryButton(
                label: 'Demander ${money(amount)} FCFA',
                icon: KolaIcons.flashOutline,
                loading: _requesting,
                onPressed: eligibility?.eligible == true && amount >= _minLoan
                    ? () => _request(amount)
                    : null,
              ),
              const SizedBox(height: 10),
              Text(
                'Après validation, le versement sera effectué sur votre portefeuille KOLA.',
                textAlign: TextAlign.center,
                style: AppTypography.caption.copyWith(fontSize: 8),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _request(double amount) async {
    final data = context.read<KolaDataProvider>();
    setState(() => _requesting = true);

    final result = await _credit.requestLoan(
      amount: amount,
      idempotencyKey: _idempotency.forIntent('loan:${amount.round()}'),
    );
    if (!mounted) return;
    setState(() => _requesting = false);

    if (!result.success) {
      _alert(
        'Demande refusée',
        result.error?.message ?? 'Le prêt n’a pas pu être accordé.',
      );
      return;
    }

    _idempotency.reset();
    await data.refresh();
    if (!mounted) return;
    widget.onNavigate(KolaRoute.credit);
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
}

class _LoanDetail extends StatelessWidget {
  const _LoanDetail(this.icon, this.label, this.value, {this.last = false});

  final IconData icon;
  final String label;
  final String value;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 36),
      padding: const EdgeInsets.symmetric(vertical: 6),
      decoration: last
          ? null
          : const BoxDecoration(
              border: Border(bottom: BorderSide(color: Color(0xFFDDE2F4))),
            ),
      child: Row(
        children: [
          Icon(icon, size: 15, color: AppColors.primary),
          const SizedBox(width: 7),
          Expanded(
            child: Text(
              label,
              style: AppTypography.caption.copyWith(fontSize: 9),
            ),
          ),
          const SizedBox(width: AppSpacing.xs),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: AppTypography.caption.copyWith(
                fontSize: 9,
                fontWeight: FontWeight.w700,
                color: AppColors.ink,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
