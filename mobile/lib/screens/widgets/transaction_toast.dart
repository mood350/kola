import 'dart:math';

import 'package:flutter/material.dart';
import '../../core/theme/kola_icons.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/formatters.dart';
import '../../providers/kola_data_provider.dart';

const _typeLabels = <String, String>{
  'P2P_TRANSFER': 'Transfert P2P',
  'CASH_IN': 'Recharge',
  'CASH_OUT': 'Retrait',
  'MERCHANT_PAYMENT': 'Paiement marchand',
  'VAULT_DEPOSIT': 'Versement au coffre',
  'VAULT_WITHDRAWAL': 'Retrait du coffre',
  'SAVINGS_DEPOSIT': 'Versement sur Bankivi',
  'SAVINGS_WITHDRAWAL': 'Retrait de Bankivi',
  'BILL_PAYMENT': 'Paiement de facture',
  'LOAN_REPAYMENT': 'Remboursement',
};

/// Position et couleur de chaque confetti, figées pour que l'animation soit
/// identique à chaque succès plutôt que de scintiller au hasard.
const _confetti = <(double, double, Color, double)>[
  (24, 62, AppColors.yellow, 15),
  (55, 32, AppColors.blue, -18),
  (92, 73, AppColors.mint, 28),
  (133, 38, Color(0xFFFF6B6B), 12),
  (178, 62, AppColors.yellow, -22),
  (224, 31, AppColors.green, 18),
  (265, 72, Color(0xFF9B72FF), -12),
  (291, 42, AppColors.yellow, 25),
  (39, 126, Color(0xFF9B72FF), -22),
  (278, 130, AppColors.blue, 15),
  (70, 161, AppColors.green, 30),
  (246, 169, Color(0xFFFF6B6B), -20),
];

/// Confirmation plein écran affichée après chaque opération.
class TransactionToastOverlay extends StatelessWidget {
  const TransactionToastOverlay({
    super.key,
    required this.toast,
    required this.onClose,
  });

  final TransactionToast toast;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: ColoredBox(
        color: const Color(0x55131B2E),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(22),
            child: _ToastCard(toast: toast, onClose: onClose),
          ),
        ),
      ),
    );
  }
}

class _ToastCard extends StatefulWidget {
  const _ToastCard({required this.toast, required this.onClose});

  final TransactionToast toast;
  final VoidCallback onClose;

  @override
  State<_ToastCard> createState() => _ToastCardState();
}

class _ToastCardState extends State<_ToastCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 260),
  )..forward();

  @override
  void initState() {
    super.initState();
    // Se referme seul : l'utilisateur vient d'agir, il ne doit pas avoir à
    // fermer une confirmation pour continuer.
    Future.delayed(const Duration(milliseconds: 5200), () {
      if (mounted) widget.onClose();
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final toast = widget.toast;
    final failed = toast.failed;

    return FadeTransition(
      opacity: _controller,
      child: ScaleTransition(
        scale: Tween<double>(begin: 0.82, end: 1).animate(
          CurvedAnimation(parent: _controller, curve: Curves.easeOutBack),
        ),
        child: Container(
          width: double.infinity,
          constraints: BoxConstraints(
            maxWidth: 360,
            minHeight: failed ? 330 : 425,
          ),
          padding: const EdgeInsets.all(22),
          clipBehavior: Clip.antiAlias,
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(22),
            border: Border.all(
              color: failed ? const Color(0xFFFFB4AB) : const Color(0xFFE8EBF2),
            ),
            boxShadow: AppColors.softShadow,
          ),
          child: Stack(
            children: [
              if (!failed)
                for (final (left, top, color, angle) in _confetti)
                  Positioned(
                    left: left,
                    top: top,
                    child: Transform.rotate(
                      angle: angle * pi / 180,
                      child: Container(
                        width: 8,
                        height: 15,
                        decoration: BoxDecoration(
                          color: color,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                    ),
                  ),
              Positioned(
                right: 0,
                top: 0,
                child: GestureDetector(
                  onTap: widget.onClose,
                  child: Container(
                    width: 34,
                    height: 34,
                    alignment: Alignment.center,
                    decoration: const BoxDecoration(
                      color: AppColors.pale,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      KolaIcons.close,
                      size: 20,
                      color: AppColors.muted,
                    ),
                  ),
                ),
              ),
              Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'KOLA',
                    textAlign: TextAlign.center,
                    style: AppTypography.badge.copyWith(
                      fontSize: 10,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 1.5,
                    ),
                  ),
                  const SizedBox(height: 35),
                  Center(
                    child: Container(
                      width: 82,
                      height: 82,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: failed ? AppColors.red : AppColors.green,
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: failed
                              ? AppColors.redPale
                              : AppColors.greenPale,
                          width: 7,
                        ),
                      ),
                      child: Icon(
                        failed ? KolaIcons.close : KolaIcons.checkmark,
                        size: 43,
                        color: AppColors.white,
                      ),
                    ),
                  ),
                  const SizedBox(height: 15),
                  Text(
                    failed ? 'Transaction échouée' : 'Transaction réussie !',
                    textAlign: TextAlign.center,
                    style: AppTypography.screenTitle.copyWith(
                      fontSize: 21,
                      color: failed ? AppColors.redDark : AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    failed
                        ? (toast.failureReason ??
                              'L’opération n’a pas pu être effectuée.')
                        : '${_typeLabels[toast.type] ?? 'Votre opération'} a été effectuée avec succès.',
                    textAlign: TextAlign.center,
                    style: AppTypography.caption.copyWith(fontSize: 10),
                  ),
                  if (!failed) ...[
                    const SizedBox(height: 13),
                    RichText(
                      textAlign: TextAlign.center,
                      text: TextSpan(
                        style: AppTypography.amount.copyWith(
                          fontSize: 27,
                          fontWeight: FontWeight.w900,
                        ),
                        children: [
                          TextSpan(text: money(toast.amount)),
                          TextSpan(
                            text: ' ${toast.currency}',
                            style: AppTypography.badge.copyWith(
                              fontSize: 12,
                              color: AppColors.yellowDark,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (toast.counterparty != null) ...[
                      const SizedBox(height: 3),
                      Text(
                        'Avec ${toast.counterparty}',
                        textAlign: TextAlign.center,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.caption.copyWith(fontSize: 9),
                      ),
                    ],
                    const SizedBox(height: 18),
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppColors.pale,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: AppColors.border),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 38,
                            height: 38,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: AppColors.white,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Icon(
                              KolaIcons.receiptOutline,
                              size: 20,
                              color: AppColors.primary,
                            ),
                          ),
                          const SizedBox(width: 9),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Reçu KOLA',
                                  style: AppTypography.smallBold.copyWith(
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.ink,
                                  ),
                                ),
                                const SizedBox(height: 3),
                                Text(
                                  'Réf. ${toast.reference}',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: AppTypography.caption.copyWith(
                                    fontSize: 8,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Icon(
                            KolaIcons.checkmarkCircle,
                            size: 21,
                            color: AppColors.green,
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: AppSpacing.sm),
                  GestureDetector(
                    onTap: widget.onClose,
                    child: Container(
                      height: 45,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: failed ? AppColors.redPale : AppColors.yellow,
                        borderRadius: BorderRadius.circular(23),
                      ),
                      child: Text(
                        'Fermer',
                        style: AppTypography.button.copyWith(
                          fontSize: 12,
                          fontWeight: FontWeight.w900,
                          color: failed ? AppColors.redDark : AppColors.primary,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
