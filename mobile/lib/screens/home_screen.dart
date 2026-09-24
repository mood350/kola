import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/date_format_utils.dart';
import '../core/utils/formatters.dart';
import '../core/utils/transaction_display.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/transaction.dart';
import '../models/vault.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/wallet_service.dart';

class _QuickAction {
  const _QuickAction(this.icon, this.title, this.subtitle, this.route,
      {this.background});
  final IconData icon;
  final String title;
  final String subtitle;
  final KolaRoute? route;
  final Color? background;
}

const _actions = <_QuickAction>[
  _QuickAction(
    KolaIcons.cashOutline,
    'Virement P2P',
    'Instantané',
    KolaRoute.scan,
  ),
  _QuickAction(
    KolaIcons.storefrontOutline,
    'Marchand QR',
    '0 FCFA frais',
    KolaRoute.scan,
    background: Color(0xFFFFE6A3),
  ),
  _QuickAction(
    KolaIcons.lockClosedOutline,
    'Coffres &\nTontines',
    'Intérêt 5%',
    KolaRoute.vaults,
  ),
  _QuickAction(
    KolaIcons.flashOutline,
    'Microcrédit',
    'En 2 min',
    KolaRoute.loan,
    background: Color(0xFFFFD8D6),
  ),
  _QuickAction(
    KolaIcons.receiptOutline,
    'Factures',
    'CEET, Cash\nPower, TdE',
    KolaRoute.bills,
  ),
  _QuickAction(
    KolaIcons.tvOutline,
    'Abonnements',
    'CANAL+, fibre\n& TV',
    KolaRoute.subscriptions,
    background: AppColors.mint,
  ),
];

class HomeScreen extends StatefulWidget {
  const HomeScreen({
    super.key,
    required this.onNavigate,
    required this.onOpenVault,
  });

  final void Function(KolaRoute) onNavigate;
  final void Function(String vaultId) onOpenVault;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool _hidden = false;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final recent = data.recentTransactions();

    return KolaScreen(
      route: KolaRoute.home,
      onNavigate: widget.onNavigate,
      children: [
        _WelcomeRow(firstName: data.user?.firstName ?? 'Client'),
        const SizedBox(height: AppSpacing.md),
        _WalletCard(
          balance: data.balance,
          hidden: _hidden,
          onToggleHidden: () => setState(() => _hidden = !_hidden),
          onShowQr: _showQrSheet,
          onRecharge: _showRechargeSheet,
          onSend: () => widget.onNavigate(KolaRoute.scan),
        ),
        SectionTitle('Actions Express', action: 'Tout voir (12)'),
        _ActionGrid(onNavigate: widget.onNavigate),
        SectionTitle(
          'Mes Coffres-Forts  ${data.vaults.length}',
          action: 'Gérer  ›',
          onAction: () => widget.onNavigate(KolaRoute.vaults),
        ),
        _VaultStrip(
          vaults: data.vaults,
          onOpenVault: widget.onOpenVault,
          onCreate: () => widget.onNavigate(KolaRoute.vaults),
        ),
        SectionTitle(
          'Activités Récentes',
          action: 'Relevé complet',
          onAction: () => widget.onNavigate(KolaRoute.transactionHistory),
        ),
        KolaCard(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: 4,
          ),
          child: recent.isEmpty
              ? EmptyState(
                  icon: data.loading
                      ? KolaIcons.hourglassOutline
                      : KolaIcons.receiptOutline,
                  title: data.loading
                      ? 'Chargement des activités…'
                      : 'Aucune activité récente',
                  message: 'Vos prochaines transactions apparaîtront ici.',
                )
              : Column(
                  children: [
                    for (var i = 0; i < recent.length; i++)
                      TransactionRow(
                        transaction: recent[i],
                        withDivider: i > 0,
                      ),
                  ],
                ),
        ),
      ],
    );
  }

  void _showQrSheet() {
    final user = context.read<KolaDataProvider>().user;
    showDialog<void>(
      context: context,
      barrierColor: const Color(0x88002353),
      builder: (context) => _QrDialog(
        name: user?.fullName ?? 'Compte KOLA',
        phone: user?.phone ?? '+228',
      ),
    );
  }

  void _showRechargeSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => _RechargeSheet(
        data: context.read<KolaDataProvider>(),
      ),
    );
  }
}

class _WelcomeRow extends StatelessWidget {
  const _WelcomeRow({required this.firstName});

  final String firstName;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Bonjour, $firstName ✌️',
                style: AppTypography.body.copyWith(fontSize: 19),
              ),
              const SizedBox(height: 3),
              Row(
                children: [
                  Text('● ', style: AppTypography.caption.copyWith(
                    color: AppColors.green,
                  )),
                  Text(
                    'Compte Particulier Actif',
                    style: AppTypography.caption,
                  ),
                ],
              ),
            ],
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.pale2, width: 2),
          ),
          child: Text('XOF⌄', style: AppTypography.smallBold),
        ),
      ],
    );
  }
}

class _WalletCard extends StatelessWidget {
  const _WalletCard({
    required this.balance,
    required this.hidden,
    required this.onToggleHidden,
    required this.onShowQr,
    required this.onRecharge,
    required this.onSend,
  });

  final double balance;
  final bool hidden;
  final VoidCallback onToggleHidden;
  final VoidCallback onShowQr;
  final VoidCallback onRecharge;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 250,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.lg),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.primary2, AppColors.primary],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 38,
                height: 38,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: AppColors.yellow,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  'K',
                  style: AppTypography.screenTitle.copyWith(
                    fontSize: 22,
                    color: AppColors.primary,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'COMPTE COURANT',
                    style: AppTypography.badge.copyWith(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: AppColors.white,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    'KOLA • XOF',
                    style: AppTypography.badge.copyWith(
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFF29E0C0),
                    ),
                  ),
                ],
              ),
              const Spacer(),
              GestureDetector(
                onTap: onShowQr,
                child: Container(
                  height: 34,
                  padding: const EdgeInsets.symmetric(horizontal: 13),
                  decoration: BoxDecoration(
                    color: AppColors.white.withValues(alpha: 0.09),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        KolaIcons.qrCode,
                        size: 15,
                        color: AppColors.yellow,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        'Mon QR',
                        style: AppTypography.small.copyWith(
                          color: AppColors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 25),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Solde disponible garanti',
                style: AppTypography.body.copyWith(
                  fontSize: 13,
                  color: const Color(0xFFE3E7F5),
                ),
              ),
              GestureDetector(
                onTap: onToggleHidden,
                child: Icon(
                  hidden ? KolaIcons.eyeOffOutline : KolaIcons.eyeOutline,
                  size: 21,
                  color: AppColors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 7),
          RichText(
            text: TextSpan(
              style: AppTypography.balance,
              children: [
                TextSpan(text: hidden ? '••••••' : money(balance)),
                TextSpan(
                  text: ' FCFA',
                  style: AppTypography.balance.copyWith(fontSize: 24),
                ),
              ],
            ),
          ),
          const Spacer(),
          Row(
            children: [
              Expanded(
                child: _WalletButton(
                  label: 'Recharger 0%',
                  icon: KolaIcons.addCircleOutline,
                  background: AppColors.yellow,
                  foreground: AppColors.primary,
                  onTap: onRecharge,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _WalletButton(
                  label: 'Envoyer P2P',
                  icon: KolaIcons.sendOutline,
                  background: AppColors.white.withValues(alpha: 0.13),
                  foreground: AppColors.white,
                  onTap: onSend,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _WalletButton extends StatelessWidget {
  const _WalletButton({
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
          borderRadius: BorderRadius.circular(24),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 18, color: foreground),
            const SizedBox(width: 8),
            Text(
              label,
              style: AppTypography.button.copyWith(
                fontSize: 13,
                color: foreground,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ActionGrid extends StatelessWidget {
  const _ActionGrid({required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: _actions.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 10,
        crossAxisSpacing: 10,
        mainAxisExtent: 140,
      ),
      itemBuilder: (context, i) {
        final action = _actions[i];
        return GestureDetector(
          onTap: action.route == null ? null : () => onNavigate(action.route!),
          child: Container(
            decoration: BoxDecoration(
              color: AppColors.white,
              borderRadius: BorderRadius.circular(15),
              border: Border.all(color: AppColors.hairline),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconCircle(
                  action.icon,
                  background: action.background ?? AppColors.pale2,
                ),
                const SizedBox(height: 10),
                Text(
                  action.title,
                  textAlign: TextAlign.center,
                  style: AppTypography.bodyBold,
                ),
                const SizedBox(height: 3),
                Text(
                  action.subtitle,
                  textAlign: TextAlign.center,
                  style: AppTypography.caption.copyWith(color: AppColors.ink),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _VaultStrip extends StatelessWidget {
  const _VaultStrip({
    required this.vaults,
    required this.onOpenVault,
    required this.onCreate,
  });

  final List<Vault> vaults;
  final void Function(String) onOpenVault;
  final VoidCallback onCreate;

  @override
  Widget build(BuildContext context) {
    if (vaults.isEmpty) {
      return KolaCard(
        onTap: onCreate,
        padding: const EdgeInsets.symmetric(vertical: 22),
        child: Column(
          children: [
            const Icon(
              KolaIcons.addCircleOutline,
              size: 28,
              color: AppColors.primary,
            ),
            const SizedBox(height: 7),
            Text('Créer mon premier coffre', style: AppTypography.cardTitle),
            const SizedBox(height: 3),
            Text(
              'Organisez une épargne pour chacun de vos projets.',
              style: AppTypography.caption.copyWith(fontSize: 9),
            ),
          ],
        ),
      );
    }

    return SizedBox(
      height: 150,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: vaults.length > 4 ? 4 : vaults.length,
        separatorBuilder: (_, _) => const SizedBox(width: 10),
        itemBuilder: (context, i) {
          final vault = vaults[i];
          return KolaCard(
            width: 300,
            onTap: () => onOpenVault(vault.id),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const IconCircle(KolaIcons.lockClosedOutline),
                    const SizedBox(width: 9),
                    Expanded(
                      child: Text(
                        vault.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.cardTitle,
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.all(5),
                      decoration: BoxDecoration(
                        color: AppColors.mint,
                        borderRadius: BorderRadius.circular(5),
                      ),
                      child: Text(
                        '${vault.progressPercent.round()}%',
                        style: AppTypography.badge.copyWith(fontSize: 10),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Padding(
                  padding: const EdgeInsets.only(left: 52),
                  child: Text(
                    vault.targetDate != null
                        ? 'Objectif au ${shortDate(vault.targetDate!)}'
                        : 'Coffre disponible',
                    style: AppTypography.caption,
                  ),
                ),
                const Spacer(),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(money(vault.balance), style: AppTypography.amount),
                    Text(
                      ' FCFA',
                      style: AppTypography.small.copyWith(
                        color: AppColors.primary,
                      ),
                    ),
                    const Spacer(),
                    Text(
                      '/ ${money(vault.targetAmount)}',
                      style: AppTypography.caption.copyWith(
                        color: AppColors.ink,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.xs),
                KolaProgress(vault.progressPercent, color: AppColors.primary),
              ],
            ),
          );
        },
      ),
    );
  }
}

/// Ligne du relevé : pastille colorée, intitulé + horodatage, montant signé.
class TransactionRow extends StatelessWidget {
  const TransactionRow({
    super.key,
    required this.transaction,
    this.withDivider = false,
    this.onTap,
  });

  final KolaTransaction transaction;
  final bool withDivider;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final meta = metaFor(transaction.type);
    final incoming = meta.incoming;
    final failed = transaction.isFailed;

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        constraints: const BoxConstraints(minHeight: 76),
        decoration: withDivider
            ? const BoxDecoration(
                border: Border(top: BorderSide(color: Color(0xFFF0F2F6))),
              )
            : null,
        child: Row(
          children: [
            IconCircle(
              meta.icon,
              background: incoming
                  ? const Color(0xFF83F5C8)
                  : failed
                  ? AppColors.redPale
                  : AppColors.pale2,
              color: failed
                  ? AppColors.red
                  : incoming
                  ? AppColors.greenDark
                  : AppColors.primary,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    transaction.description?.isNotEmpty == true
                        ? transaction.description!
                        : meta.label,
                    style: AppTypography.bodyBold,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    [
                      formatRelativeDate(transaction.displayedAt),
                      statusLabel(transaction),
                      if (transaction.counterparty != null)
                        transaction.counterparty!,
                    ].join(' · '),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.caption,
                  ),
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.xs),
            ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 92),
              child: Text(
                '${incoming ? '+' : '−'}${money(transaction.amount)} F',
                textAlign: TextAlign.right,
                style: AppTypography.bodyBold.copyWith(
                  color: failed
                      ? AppColors.muted
                      : incoming
                      ? AppColors.green
                      : AppColors.ink,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QrDialog extends StatelessWidget {
  const _QrDialog({required this.name, required this.phone});

  final String name;
  final String phone;

  @override
  Widget build(BuildContext context) {
    return Dialog(
      insetPadding: const EdgeInsets.all(22),
      backgroundColor: AppColors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(25),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                GestureDetector(
                  onTap: () => Navigator.of(context).pop(),
                  child: const Icon(KolaIcons.close, size: 24),
                ),
              ],
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const KolaLogo(size: 38),
                const SizedBox(width: 8),
                Text(
                  'KOLA',
                  style: AppTypography.screenTitle.copyWith(
                    fontSize: 18,
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            Text('Recevoir de l’argent', style: AppTypography.screenTitle),
            const SizedBox(height: 6),
            Text(
              'Faites scanner ce QR par la personne qui souhaite vous envoyer de l’argent.',
              textAlign: TextAlign.center,
              style: AppTypography.caption.copyWith(fontSize: 10),
            ),
            const SizedBox(height: 18),
            Container(
              width: 247,
              height: 247,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: AppColors.border),
              ),
              child: QrImageView(
                data: '{"type":"KOLA_P2P","phone":"$phone"}',
                size: 215,
                eyeStyle: const QrEyeStyle(
                  eyeShape: QrEyeShape.square,
                  color: AppColors.primary,
                ),
                dataModuleStyle: const QrDataModuleStyle(
                  dataModuleShape: QrDataModuleShape.square,
                  color: AppColors.primary,
                ),
              ),
            ),
            const SizedBox(height: 14),
            Text(name, style: AppTypography.screenTitle.copyWith(fontSize: 16)),
            const SizedBox(height: 3),
            Text(phone, style: AppTypography.smallBold.copyWith(fontSize: 12)),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.greenPale,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(
                    KolaIcons.shieldCheckmarkOutline,
                    size: 18,
                    color: AppColors.greenDark,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Le QR contient uniquement votre numéro KOLA. Vérifiez le montant avant chaque transfert.',
                      style: AppTypography.caption.copyWith(fontSize: 9),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

const _providers = ['Mixx by Yas', 'Moov Money', 'Carte bancaire'];

class _RechargeSheet extends StatefulWidget {
  const _RechargeSheet({required this.data});

  final KolaDataProvider data;

  @override
  State<_RechargeSheet> createState() => _RechargeSheetState();
}

class _RechargeSheetState extends State<_RechargeSheet> {
  final _wallets = WalletService();
  final _phone = TextEditingController();
  final _amount = TextEditingController();
  String _provider = _providers.first;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _phone.text = widget.data.user?.localPhone ?? '';
  }

  @override
  void dispose() {
    _phone.dispose();
    _amount.dispose();
    super.dispose();
  }

  Future<void> _confirm() async {
    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    if (_phone.text.length < 8) {
      _alert('Numéro invalide', 'Saisissez un numéro Mobile Money valide.');
      return;
    }
    if (value == null || value < 500) {
      _alert('Montant invalide', 'Le montant minimum est de 500 FCFA.');
      return;
    }
    if (value > 1000000) {
      _alert(
        'Plafond dépassé',
        'Le montant maximum par recharge est de 1 000 000 FCFA.',
      );
      return;
    }

    setState(() => _sending = true);
    final result = await _wallets.deposit(value);
    if (!mounted) return;
    setState(() => _sending = false);

    if (!result.success) {
      widget.data.notifyFailure(
        result.error?.message ?? 'Erreur serveur.',
        'CASH_IN',
      );
      Navigator.of(context).pop();
      return;
    }

    await widget.data.refresh();
    if (!mounted) return;
    Navigator.of(context).pop();
    _alert(
      'Recharge réussie',
      '${money(value)} FCFA ont été ajoutés à votre portefeuille.',
    );
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
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.88,
        ),
        padding: const EdgeInsets.fromLTRB(22, 22, 22, 34),
        decoration: const BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppRadius.sheet),
          ),
        ),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SheetHeader(
                title: 'Recharger mon compte',
                subtitle: 'Recharge Mobile Money sans frais',
              ),
              const FieldLabel('Moyen de paiement'),
              for (final name in _providers)
                Padding(
                  padding: const EdgeInsets.only(bottom: 7),
                  child: GestureDetector(
                    onTap: () => setState(() => _provider = name),
                    child: Container(
                      height: 43,
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      decoration: BoxDecoration(
                        color: _provider == name
                            ? AppColors.primary
                            : AppColors.pale,
                        borderRadius: BorderRadius.circular(11),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            _provider == name
                                ? KolaIcons.radioButtonOn
                                : KolaIcons.radioButtonOff,
                            size: 18,
                            color: _provider == name
                                ? AppColors.white
                                : AppColors.primary,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            name,
                            style: AppTypography.smallBold.copyWith(
                              color: _provider == name
                                  ? AppColors.white
                                  : AppColors.primary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              const FieldLabel('Numéro du compte à recharger'),
              Container(
                height: AppDimens.inputHeight,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(11),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  children: [
                    const Text('🇹🇬', style: TextStyle(fontSize: 20)),
                    const SizedBox(width: 8),
                    Text(
                      '+228',
                      style: AppTypography.badge.copyWith(
                        fontSize: 12,
                        fontWeight: FontWeight.w800,
                        color: AppColors.ink,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(width: 1, height: 22, color: AppColors.border),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _phone,
                        keyboardType: TextInputType.phone,
                        maxLength: 8,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: const InputDecoration(
                          hintText: '71 60 80 97',
                          counterText: '',
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                          filled: false,
                          isDense: true,
                          contentPadding: EdgeInsets.zero,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const FieldLabel('Montant à ajouter'),
              Container(
                height: 50,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(11),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _amount,
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        style: AppTypography.amount.copyWith(fontSize: 21),
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
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        color: AppColors.yellowDark,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.xs),
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
                              color: AppColors.pale2,
                              borderRadius: BorderRadius.circular(17),
                            ),
                            child: Text(
                              '+${money(value)}',
                              style: AppTypography.badge.copyWith(fontSize: 10),
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: AppSpacing.md),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.greenPale,
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Row(
                  children: [
                    const Icon(
                      KolaIcons.shieldCheckmarkOutline,
                      size: 19,
                      color: AppColors.green,
                    ),
                    const SizedBox(width: 9),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Frais KOLA : 0 FCFA',
                            style: AppTypography.smallBold.copyWith(
                              fontWeight: FontWeight.w800,
                              color: AppColors.greenDark,
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            'Vous validerez ensuite l’opération chez $_provider.',
                            style: AppTypography.caption.copyWith(fontSize: 9),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              PrimaryButton(
                label: 'Continuer avec $_provider',
                loading: _sending,
                onPressed: _confirm,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
