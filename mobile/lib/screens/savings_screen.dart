import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/wallet_service.dart';

/// Compte d'épargne Bankivi : il sert de garantie aux micro-prêts, d'où la
/// distinction entre la partie disponible et la partie bloquée.
class SavingsScreen extends StatelessWidget {
  const SavingsScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final savings = data.savingsWallet;
    final total =
        savings?.totalBalance ?? data.eligibility?.savingsBalance ?? 0;
    final available = savings?.availableBalance ?? total;
    final locked = savings?.lockedBalance ?? 0;

    return KolaScreen(
      route: KolaRoute.savings,
      onNavigate: onNavigate,
      children: [
        const PageHeader(
          overline: 'GARANTIE DE FINANCEMENT',
          title: 'Compte Bankivi',
          subtitle:
              'Alimentez régulièrement Bankivi pour augmenter votre capacité de prêt.',
          icon: KolaIcons.businessOutline,
        ),
        const SizedBox(height: 18),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('SOLDE BANKIVI TOTAL', style: _overlineOnDark),
                        const SizedBox(height: 4),
                        RichText(
                          text: TextSpan(
                            style: AppTypography.balance.copyWith(fontSize: 29),
                            children: [
                              TextSpan(text: money(total)),
                              TextSpan(
                                text: ' FCFA',
                                style: AppTypography.small.copyWith(
                                  fontSize: 12,
                                  color: AppColors.white,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    KolaIcons.shieldCheckmarkOutline,
                    size: 30,
                    color: AppColors.yellow,
                  ),
                ],
              ),
              const SizedBox(height: 15),
              Container(
                padding: const EdgeInsets.only(top: 14),
                decoration: BoxDecoration(
                  border: Border(
                    top: BorderSide(
                      color: AppColors.white.withValues(alpha: 0.13),
                    ),
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _MiniStat(label: 'DISPONIBLE', value: available),
                    _MiniStat(label: 'GARANTIE BLOQUÉE', value: locked),
                  ],
                ),
              ),
              const SizedBox(height: 17),
              Row(
                children: [
                  Expanded(
                    child: _HeroButton(
                      label: 'Alimenter',
                      icon: KolaIcons.add,
                      background: AppColors.yellow,
                      foreground: AppColors.primary,
                      onTap: () => _openSheet(context, deposit: true),
                    ),
                  ),
                  const SizedBox(width: 9),
                  Expanded(
                    child: _HeroButton(
                      label: 'Retirer',
                      icon: KolaIcons.arrowUp,
                      background: AppColors.white.withValues(alpha: 0.13),
                      foreground: AppColors.white,
                      onTap: () => _openSheet(context, deposit: false),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const _SectionLabel('CAPACITÉ DE PRÊT'),
        KolaCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Votre solde Bankivi sert de garantie pour vos prêts.',
                style: AppTypography.caption,
              ),
              const SizedBox(height: 13),
              Container(
                padding: const EdgeInsets.all(11),
                decoration: BoxDecoration(
                  color: AppColors.pale,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Montant empruntable actuellement',
                      style: AppTypography.badge.copyWith(
                        fontSize: 8,
                        fontWeight: FontWeight.w400,
                        color: AppColors.muted,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '${money(data.eligibility?.maxLoanAmount ?? 0)} FCFA',
                      style: AppTypography.amount.copyWith(fontSize: 17),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.sm),
              PrimaryButton(
                label: 'Voir mes prêts et mes offres',
                icon: KolaIcons.arrowForward,
                onPressed: () => onNavigate(KolaRoute.credit),
              ),
            ],
          ),
        ),
        const _SectionLabel('COMMENT ÇA MARCHE ?'),
        KolaCard(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
          child: Column(
            children: const [
              _Step(
                KolaIcons.walletOutline,
                '1. Alimentez Bankivi',
                'Transférez gratuitement depuis votre compte courant.',
              ),
              _Step(
                KolaIcons.trendingUpOutline,
                '2. Améliorez votre capacité',
                'Des versements réguliers sur Bankivi améliorent votre profil KOLA.',
              ),
              _Step(
                KolaIcons.shieldCheckmarkOutline,
                '3. Garantissez votre prêt',
                'La garantie nécessaire est bloquée pendant le prêt.',
                last: true,
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _openSheet(BuildContext context, {required bool deposit}) {
    final data = context.read<KolaDataProvider>();
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _SavingsSheet(data: data, deposit: deposit),
    );
  }
}

TextStyle get _overlineOnDark => AppTypography.badge.copyWith(
  fontSize: 8,
  fontWeight: FontWeight.w800,
  color: const Color(0xFFADC6FF),
);

class _MiniStat extends StatelessWidget {
  const _MiniStat({required this.label, required this.value});

  final String label;
  final double value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: _overlineOnDark.copyWith(fontSize: 7)),
        const SizedBox(height: 3),
        Text(
          '${money(value)} F',
          style: AppTypography.badge.copyWith(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: AppColors.white,
          ),
        ),
      ],
    );
  }
}

class _HeroButton extends StatelessWidget {
  const _HeroButton({
    required this.label,
    required this.icon,
    required this.background,
    required this.foreground,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final Color background;
  final Color foreground;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 45,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(23),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 19, color: foreground),
            const SizedBox(width: 7),
            Text(
              label,
              style: AppTypography.button.copyWith(
                fontSize: 12,
                color: foreground,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.label);
  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 22, bottom: 9),
      child: Text(label, style: AppTypography.fieldLabel.copyWith(fontSize: 8)),
    );
  }
}

class _Step extends StatelessWidget {
  const _Step(this.icon, this.title, this.text, {this.last = false});

  final IconData icon;
  final String title;
  final String text;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 66),
      decoration: last
          ? null
          : const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppColors.border)),
            ),
      child: Row(
        children: [
          IconCircle(icon, background: AppColors.pale, size: 19, diameter: 38),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTypography.smallBold.copyWith(
                    fontWeight: FontWeight.w800,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  text,
                  style: AppTypography.caption.copyWith(fontSize: 9),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SavingsSheet extends StatefulWidget {
  const _SavingsSheet({required this.data, required this.deposit});

  final KolaDataProvider data;
  final bool deposit;

  @override
  State<_SavingsSheet> createState() => _SavingsSheetState();
}

class _SavingsSheetState extends State<_SavingsSheet> {
  final _wallets = WalletService();
  final _amount = TextEditingController();
  bool _saving = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _confirm() async {
    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    final data = widget.data;
    final available =
        data.savingsWallet?.availableBalance ?? data.savingsBalance;

    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }
    if (widget.deposit && value > data.balance) {
      data.notifyFailure(
        'Le compte courant ne dispose pas de ce montant.',
        'SAVINGS_DEPOSIT',
      );
      Navigator.of(context).pop();
      return;
    }
    if (!widget.deposit && value > available) {
      data.notifyFailure(
        'Le montant dépasse la partie disponible de votre compte Bankivi.',
        'SAVINGS_WITHDRAWAL',
      );
      Navigator.of(context).pop();
      return;
    }

    setState(() => _saving = true);
    final result = widget.deposit
        ? await _wallets.depositToSavings(value)
        : await _wallets.withdrawFromSavings(value);
    if (!mounted) return;
    setState(() => _saving = false);

    // Le backend renvoie les DEUX portefeuilles mis à jour. S'il en manque un,
    // l'opération n'a pas abouti des deux côtés : mieux vaut le dire que
    // d'afficher un solde à moitié rafraîchi.
    final wallets = result.data;
    if (wallets == null ||
        !wallets.any((w) => w.type == 'CURRENT') ||
        !wallets.any((w) => w.isSavings)) {
      data.notifyFailure(
        result.error?.message ??
            'Le serveur n’a pas confirmé les nouveaux soldes.',
        widget.deposit ? 'SAVINGS_DEPOSIT' : 'SAVINGS_WITHDRAWAL',
      );
      Navigator.of(context).pop();
      return;
    }

    data.replaceWallets(wallets);
    Navigator.of(context).pop();
    unawaited(data.refresh());
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
    final available =
        widget.data.savingsWallet?.availableBalance ??
        widget.data.savingsBalance;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        padding: const EdgeInsets.fromLTRB(22, 20, 22, 30),
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
              title: widget.deposit
                  ? 'Alimenter Bankivi'
                  : 'Retirer de Bankivi',
              subtitle: widget.deposit
                  ? 'Compte courant : ${money(widget.data.balance)} F'
                  : 'Disponible : ${money(available)} F',
            ),
            const FieldLabel('Montant'),
            Container(
              height: 53,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _amount,
                      autofocus: true,
                      keyboardType: TextInputType.number,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      style: AppTypography.amount.copyWith(fontSize: 23),
                      decoration: const InputDecoration(
                        hintText: '0',
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
                      fontSize: 10,
                      fontWeight: FontWeight.w900,
                      color: AppColors.yellowDark,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 9),
            Row(
              children: [
                for (final value in [5000, 10000, 25000])
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(right: 7),
                      child: GestureDetector(
                        onTap: () => _amount.text = '$value',
                        child: Container(
                          height: 34,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: AppColors.pale,
                            borderRadius: BorderRadius.circular(17),
                          ),
                          child: Text(
                            money(value),
                            style: AppTypography.badge.copyWith(fontSize: 9),
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 18),
            PrimaryButton(
              label: 'Confirmer le transfert',
              loading: _saving,
              onPressed: _confirm,
            ),
          ],
        ),
      ),
    );
  }
}
