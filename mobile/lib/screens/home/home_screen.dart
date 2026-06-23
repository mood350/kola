import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/transaction_item.dart';
import '../../core/widgets/kola_bottom_nav_bar.dart';
import '../../providers/auth_provider.dart';
import '../../providers/wallet_provider.dart';
import '../../models/transaction.dart';
import '../vaults/vaults_screen.dart';
import '../credit/credit_screen.dart';
import '../profile/profile_screen.dart';

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
    });
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final walletProvider = context.watch<WalletProvider>();
    final user = authProvider.currentUser;

    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: () => context.read<WalletProvider>().loadHomeData(),
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
                          child: Icon(Icons.person_rounded, color: AppColors.onSurfaceVariant),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Bonjour,', style: AppTypography.bodySm),
                            Text(
                              user?.fullName.split(' ').first ?? 'Utilisateur',
                              style: AppTypography.headingSm.copyWith(color: AppColors.primary),
                            ),
                          ],
                        ),
                      ],
                    ),
                    Stack(
                      children: [
                        IconButton(
                          icon: const Icon(Icons.notifications_outlined, color: AppColors.primary, size: 28),
                          onPressed: () {
                            // TODO: navigation vers l'écran de notifications.
                          },
                        ),
                        Positioned(
                          top: 8,
                          right: 8,
                          child: Container(
                            width: 10,
                            height: 10,
                            decoration: BoxDecoration(
                              color: AppColors.danger,
                              shape: BoxShape.circle,
                              border: Border.all(color: AppColors.surface, width: 1.5),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              Padding(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.marginMobile),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: AppSpacing.sm),

                    // --- Balance Card ---
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(AppDimens.cardPadding),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceCard,
                        borderRadius: BorderRadius.circular(AppRadius.xl),
                        border: Border.all(color: AppColors.secondaryContainer),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Badge KYC + toggle visibilité
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xs, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppColors.surfaceContainerLow,
                                  borderRadius: BorderRadius.circular(AppRadius.full),
                                  border: Border.all(color: AppColors.surfaceVariant),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Container(
                                      width: 8,
                                      height: 8,
                                      decoration: const BoxDecoration(color: AppColors.success, shape: BoxShape.circle),
                                    ),
                                    const SizedBox(width: AppSpacing.xxs),
                                    Text(
                                      _kycTierLabel(user?.kycTier),
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
                                onPressed: () => context.read<WalletProvider>().toggleBalanceVisibility(),
                              ),
                            ],
                          ),
                          const SizedBox(height: AppSpacing.sm),

                          Text('Solde principal', style: AppTypography.bodyMd),
                          const SizedBox(height: AppSpacing.xxs),
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.baseline,
                            textBaseline: TextBaseline.alphabetic,
                            children: [
                              Text(
                                walletProvider.isBalanceVisible
                                    ? currencyFormat.format(walletProvider.balance)
                                    : '•••••••',
                                style: AppTypography.displayLgMobile,
                              ),
                              const SizedBox(width: AppSpacing.xs),
                              Text('XOF', style: AppTypography.headingSm.copyWith(color: AppColors.onSurfaceVariant)),
                            ],
                          ),
                          const SizedBox(height: AppSpacing.md),

                          // Actions rapides
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              _QuickAction(
                                icon: Icons.add_rounded,
                                label: 'Déposer',
                                isPrimary: true,
                                onTap: () {},
                              ),
                              _QuickAction(
                                icon: Icons.send_rounded,
                                label: 'Envoyer',
                                onTap: () {},
                              ),
                              _QuickAction(
                                icon: Icons.qr_code_scanner_rounded,
                                label: 'Payer',
                                onTap: () {},
                              ),
                              _QuickAction(
                                icon: Icons.arrow_downward_rounded,
                                label: 'Retirer',
                                onTap: () {},
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
                            // TODO: navigation vers l'historique complet.
                          },
                          child: Text(
                            'Voir tout',
                            style: AppTypography.bodySm.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.md),

                    if (walletProvider.isLoading)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: AppSpacing.xl),
                        child: Center(child: CircularProgressIndicator(color: AppColors.primary)),
                      )
                    else if (walletProvider.recentTransactions.isEmpty)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl),
                        child: Center(
                          child: Text('Aucune transaction récente', style: AppTypography.bodyMd),
                        ),
                      )
                    else
                      Column(
                        children: walletProvider.recentTransactions.map((tx) {
                          return Padding(
                            padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                            child: TransactionItem(
                              icon: tx.icon,
                              iconColor: _colorForTransaction(tx.type),
                              title: tx.title,
                              subtitle: tx.subtitle,
                              amount: '${tx.isPositive ? '+' : ''}${currencyFormat.format(tx.amount)} XOF',
                              dateLabel: _formatRelativeDate(tx.date),
                              isPositive: tx.isPositive,
                              onTap: () {
                                // TODO: navigation vers le détail de la transaction.
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

  String _kycTierLabel(String? tier) {
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

  Color _colorForTransaction(TransactionType type) {
    switch (type) {
      case TransactionType.merchantPayment:
        return AppColors.orangeMoney;
      case TransactionType.deposit:
        return AppColors.success;
      case TransactionType.transferSent:
      case TransactionType.transferReceived:
        return AppColors.primary;
      case TransactionType.withdrawal:
        return AppColors.warning;
      case TransactionType.billPayment:
        return AppColors.moovBlue;
    }
  }

  String _formatRelativeDate(DateTime date) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final txDate = DateTime(date.year, date.month, date.day);
    final timeStr = DateFormat('HH:mm').format(date);

    if (txDate == today) {
      return "Aujourd'hui, $timeStr";
    } else if (txDate == today.subtract(const Duration(days: 1))) {
      return 'Hier, $timeStr';
    } else {
      return '${DateFormat('d MMM', 'fr_FR').format(date)}, $timeStr';
    }
  }
}

/// Bouton d'action rapide circulaire (Déposer, Envoyer, Payer, Retirer).
class _QuickAction extends StatelessWidget {
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
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppRadius.full),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: AppDimens.iconActionSize,
            height: AppDimens.iconActionSize,
            decoration: BoxDecoration(
              color: isPrimary ? AppColors.primaryContainer : AppColors.surfaceContainerHigh,
              shape: BoxShape.circle,
              border: isPrimary ? null : Border.all(color: AppColors.surfaceVariant),
            ),
            child: Icon(
              icon,
              color: isPrimary ? Colors.white : AppColors.onSurface,
              size: 22,
            ),
          ),
          const SizedBox(height: AppSpacing.xxs),
          Text(label, style: AppTypography.bodySm.copyWith(color: AppColors.onSurface, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}