import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/date_format_utils.dart';
import '../../core/utils/transaction_display_utils.dart';
import '../../core/widgets/transaction_item.dart';
import '../../core/widgets/kola_bottom_nav_bar.dart';
import 'widgets/home_skeleton.dart';
import '../../providers/notification_provider.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/user_provider.dart';
import '../../providers/wallet_provider.dart';
import '../vaults/vaults_screen.dart';
import '../credit/credit_screen.dart';
import '../profile/profile_screen.dart';
import '../notifications/notifications_screen.dart';
import '../transactions/transaction_detail_screen.dart';
import '../transactions/transaction_history_screen.dart';
import '../transactions/widgets/amount_input_sheet.dart';
import '../transfer/beneficiary_picker_screen.dart';
import '../merchant/merchant_scan_screen.dart';

/// Écran principal de l'application : tableau de bord avec carte de solde,
/// actions rapides (Déposer/Envoyer/Payer/Retirer) et transactions récentes.
/// Contient également le shell de navigation à 4 onglets (Home/Vaults/Credit/Profile).
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentTabIndex = 0;

  final List<Widget> _tabs = const [
    _HomeTabContent(),
    VaultsScreen(),
    CreditScreen(),
    ProfileScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: IndexedStack(index: _currentTabIndex, children: _tabs),
      bottomNavigationBar: KolaBottomNavBar(
        currentIndex: _currentTabIndex,
        onTap: (index) => setState(() => _currentTabIndex = index),
      ),
    );
  }
}

/// Contenu de l'onglet Home (extrait pour rester lisible).
class _HomeTabContent extends StatefulWidget {
  const _HomeTabContent();

  @override
  State<_HomeTabContent> createState() => _HomeTabContentState();
}

class _HomeTabContentState extends State<_HomeTabContent> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<WalletProvider>().loadHomeData();
      context.read<UserProvider>().loadMe();
      context.read<NotificationProvider>().loadUnreadCount();
    });
  }

  Future<void> _openAmountSheet({required bool isDeposit}) async {
    final wallet = context.read<WalletProvider>().primaryWallet;
    if (wallet == null) return;

    await AmountInputSheet.show(
      context,
      title: isDeposit ? 'Déposer' : 'Retirer',
      onSubmit: (amount, idempotencyKey) async {
        final txProvider = context.read<TransactionProvider>();
        final success = isDeposit
            ? await txProvider.deposit(
                walletId: wallet.id,
                amount: amount,
                idempotencyKey: idempotencyKey,
              )
            : await txProvider.withdraw(
                walletId: wallet.id,
                amount: amount,
                idempotencyKey: idempotencyKey,
              );
        if (success && mounted) {
          await context.read<WalletProvider>().loadHomeData();
        }
        return success;
      },
    );
  }

  Future<void> _openTransfer() async {
    final wallet = context.read<WalletProvider>().primaryWallet;
    if (wallet == null) return;

    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => BeneficiaryPickerScreen(walletId: wallet.id),
      ),
    );
  }

  Future<void> _openMerchantScan() async {
    final wallet = context.read<WalletProvider>().primaryWallet;
    if (wallet == null) return;

    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => MerchantScanScreen(walletId: wallet.id),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final walletProvider = context.watch<WalletProvider>();
    final displayName = userProvider.user?.fullName ?? 'Utilisateur';
    final kycLevel = userProvider.user?.kycLevel ?? 'TIER_1';

    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: () => walletProvider.loadHomeData(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // --- Header : avatar + salutation + notifications ---
              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.marginMobile,
                  vertical: AppSpacing.md,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        CircleAvatar(
                          radius: 20,
                          backgroundColor: AppColors.surfaceVariant,
                          child: Icon(
                            Icons.person_rounded,
                            color: AppColors.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Bonjour,', style: AppTypography.bodySm),
                            Text(
                              displayName,
                              style: AppTypography.headingSm.copyWith(
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    Stack(
                      children: [
                        IconButton(
                          icon: const Icon(
                            Icons.notifications_outlined,
                            color: AppColors.primary,
                            size: 28,
                          ),
                          onPressed: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const NotificationsScreen(),
                            ),
                          ),
                        ),
                        if (context.watch<NotificationProvider>().unreadCount >
                            0)
                          Positioned(
                            top: 8,
                            right: 8,
                            child: Container(
                              width: 10,
                              height: 10,
                              decoration: BoxDecoration(
                                color: AppColors.danger,
                                shape: BoxShape.circle,
                                border: Border.all(
                                  color: AppColors.surface,
                                  width: 1.5,
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ),

              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.marginMobile,
                ),
                child:
                    walletProvider.isLoading &&
                        walletProvider.primaryWallet == null
                    ? const HomeSkeleton()
                    : Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SizedBox(height: AppSpacing.sm),

                          // --- Balance Card ---
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(
                              AppDimens.cardPadding,
                            ),
                            decoration: BoxDecoration(
                              color: AppColors.surfaceCard,
                              borderRadius: BorderRadius.circular(AppRadius.xl),
                              border: Border.all(
                                color: AppColors.secondaryContainer,
                              ),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Badge KYC + toggle visibilité
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: AppSpacing.xs,
                                        vertical: 4,
                                      ),
                                      decoration: BoxDecoration(
                                        color: AppColors.surfaceContainerLow,
                                        borderRadius: BorderRadius.circular(
                                          AppRadius.full,
                                        ),
                                        border: Border.all(
                                          color: AppColors.surfaceVariant,
                                        ),
                                      ),
                                      child: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Container(
                                            width: 8,
                                            height: 8,
                                            decoration: const BoxDecoration(
                                              color: AppColors.success,
                                              shape: BoxShape.circle,
                                            ),
                                          ),
                                          const SizedBox(width: AppSpacing.xxs),
                                          Text(
                                            _kycTierLabel(kycLevel),
                                            style: AppTypography.labelXs,
                                          ),
                                        ],
                                      ),
                                    ),
                                    IconButton(
                                      icon: Icon(
                                        walletProvider.isBalanceVisible
                                            ? Icons.visibility_outlined
                                            : Icons.visibility_off_outlined,
                                        color: AppColors.onSurfaceVariant,
                                      ),
                                      onPressed: () => context
                                          .read<WalletProvider>()
                                          .toggleBalanceVisibility(),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: AppSpacing.sm),

                                Text(
                                  'Solde principal',
                                  style: AppTypography.bodyMd,
                                ),
                                const SizedBox(height: AppSpacing.xxs),
                                Row(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.baseline,
                                  textBaseline: TextBaseline.alphabetic,
                                  children: [
                                    AnimatedSwitcher(
                                      duration: const Duration(
                                        milliseconds: 200,
                                      ),
                                      child: Text(
                                        walletProvider.isBalanceVisible
                                            ? currencyFormat.format(
                                                walletProvider.balance,
                                              )
                                            : '•••••••',
                                        key: ValueKey(
                                          walletProvider.isBalanceVisible,
                                        ),
                                        style: AppTypography.displayLgMobile,
                                      ),
                                    ),
                                    const SizedBox(width: AppSpacing.xs),
                                    Text(
                                      'XOF',
                                      style: AppTypography.headingSm.copyWith(
                                        color: AppColors.onSurfaceVariant,
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: AppSpacing.md),

                                // Actions rapides
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    _QuickAction(
                                      icon: Icons.add_rounded,
                                      label: 'Déposer',
                                      isPrimary: true,
                                      onTap: () =>
                                          _openAmountSheet(isDeposit: true),
                                    ),
                                    _QuickAction(
                                      icon: Icons.send_rounded,
                                      label: 'Envoyer',
                                      onTap: _openTransfer,
                                    ),
                                    _QuickAction(
                                      icon: Icons.qr_code_scanner_rounded,
                                      label: 'Payer',
                                      onTap: _openMerchantScan,
                                    ),
                                    _QuickAction(
                                      icon: Icons.arrow_downward_rounded,
                                      label: 'Retirer',
                                      onTap: () =>
                                          _openAmountSheet(isDeposit: false),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),

                          const SizedBox(height: AppSpacing.xl),

                          // --- Section Récent ---
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('Récent', style: AppTypography.headingMd),
                              GestureDetector(
                                onTap: () {
                                  final wallet = walletProvider.primaryWallet;
                                  if (wallet == null) return;
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (_) => TransactionHistoryScreen(
                                        walletId: wallet.id,
                                      ),
                                    ),
                                  );
                                },
                                child: Text(
                                  'Voir tout',
                                  style: AppTypography.bodySm.copyWith(
                                    color: AppColors.primary,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: AppSpacing.md),

                          if (walletProvider.errorMessage != null &&
                              walletProvider.primaryWallet == null)
                            Padding(
                              padding: const EdgeInsets.symmetric(
                                vertical: AppSpacing.xl,
                              ),
                              child: Center(
                                child: Column(
                                  children: [
                                    Text(
                                      walletProvider.errorMessage!,
                                      style: AppTypography.bodyMd,
                                      textAlign: TextAlign.center,
                                    ),
                                    const SizedBox(height: AppSpacing.sm),
                                    TextButton(
                                      onPressed: () =>
                                          walletProvider.loadHomeData(),
                                      child: const Text('Réessayer'),
                                    ),
                                  ],
                                ),
                              ),
                            )
                          else if (walletProvider.recentTransactions.isEmpty)
                            Padding(
                              padding: const EdgeInsets.symmetric(
                                vertical: AppSpacing.xl,
                              ),
                              child: Center(
                                child: Text(
                                  'Aucune transaction récente',
                                  style: AppTypography.bodyMd,
                                ),
                              ),
                            )
                          else
                            Column(
                              children: walletProvider.recentTransactions.map((
                                tx,
                              ) {
                                return Padding(
                                  padding: const EdgeInsets.only(
                                    bottom: AppSpacing.sm,
                                  ),
                                  child: TransactionItem(
                                    icon: tx.icon,
                                    iconColor: colorForTransactionType(tx.type),
                                    title: tx.displayTitle,
                                    subtitle: tx.displaySubtitle,
                                    amount:
                                        '${tx.isCredit ? '+' : '-'}${currencyFormat.format(tx.amount)} XOF',
                                    dateLabel: formatRelativeDate(tx.createdAt),
                                    isPositive: tx.isCredit,
                                    onTap: () {
                                      Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (_) =>
                                              TransactionDetailScreen(
                                                reference: tx.reference,
                                              ),
                                        ),
                                      );
                                    },
                                  ),
                                );
                              }).toList(),
                            ),

                          const SizedBox(height: AppSpacing.xl),
                        ],
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _kycTierLabel(String tier) {
    switch (tier) {
      case 'TIER_0':
        return 'NIVEAU 0';
      case 'TIER_1':
        return 'NIVEAU 1';
      case 'TIER_2':
        return 'NIVEAU 2';
      case 'TIER_3':
        return 'NIVEAU 3';
      default:
        return 'NIVEAU 1';
    }
  }
}

/// Bouton d'action rapide circulaire (Déposer, Envoyer, Payer, Retirer).
class _QuickAction extends StatefulWidget {
  final IconData icon;
  final String label;
  final bool isPrimary;
  final VoidCallback onTap;

  const _QuickAction({
    required this.icon,
    required this.label,
    required this.onTap,
    this.isPrimary = false,
  });

  @override
  State<_QuickAction> createState() => _QuickActionState();
}

class _QuickActionState extends State<_QuickAction> {
  bool _pressed = false;

  void _setPressed(bool value) => setState(() => _pressed = value);

  @override
  Widget build(BuildContext context) {
    final icon = widget.icon;
    final label = widget.label;
    final isPrimary = widget.isPrimary;

    return GestureDetector(
      onTapDown: (_) => _setPressed(true),
      onTapUp: (_) => _setPressed(false),
      onTapCancel: () => _setPressed(false),
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _pressed ? 0.9 : 1,
        duration: const Duration(milliseconds: 100),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: AppDimens.iconActionSize,
              height: AppDimens.iconActionSize,
              decoration: BoxDecoration(
                color: isPrimary
                    ? AppColors.primaryContainer
                    : AppColors.surfaceContainerHigh,
                shape: BoxShape.circle,
                border: isPrimary
                    ? null
                    : Border.all(color: AppColors.surfaceVariant),
              ),
              child: Icon(
                icon,
                color: isPrimary ? Colors.white : AppColors.onSurface,
                size: 22,
              ),
            ),
            const SizedBox(height: AppSpacing.xxs),
            Text(
              label,
              style: AppTypography.bodySm.copyWith(
                color: AppColors.onSurface,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
