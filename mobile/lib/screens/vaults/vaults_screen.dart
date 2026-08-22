import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_in.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/skeleton_list_item.dart';
import '../../core/widgets/status_pill.dart';
import '../../models/vault.dart';
import '../../providers/vault_provider.dart';
import '../../providers/wallet_provider.dart';
import 'create_vault_screen.dart';
import 'vault_detail_screen.dart';

/// Onglet Vaults : liste des coffres-forts d'épargne de l'utilisateur.
class VaultsScreen extends StatefulWidget {
  const VaultsScreen({super.key});

  @override
  State<VaultsScreen> createState() => _VaultsScreenState();
}

class _VaultsScreenState extends State<VaultsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<VaultProvider>().loadVaults();
    });
  }

  StatusPillTone _toneFor(VaultStatus status) {
    switch (status) {
      case VaultStatus.active:
        return StatusPillTone.info;
      case VaultStatus.unlocked:
        return StatusPillTone.success;
      case VaultStatus.closed:
        return StatusPillTone.neutral;
    }
  }

  String _labelFor(VaultStatus status) {
    switch (status) {
      case VaultStatus.active:
        return 'En cours';
      case VaultStatus.unlocked:
        return 'Débloqué';
      case VaultStatus.closed:
        return 'Fermé';
    }
  }

  @override
  Widget build(BuildContext context) {
    final vaultProvider = context.watch<VaultProvider>();
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Coffres-forts', style: AppTypography.headingSm),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded, color: AppColors.primary),
            onPressed: () async {
              final wallet = context.read<WalletProvider>().primaryWallet;
              if (wallet == null) return;
              await Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => CreateVaultScreen(walletId: wallet.id),
                ),
              );
              if (context.mounted) {
                context.read<VaultProvider>().loadVaults();
              }
            },
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => vaultProvider.loadVaults(),
          child: Builder(
            builder: (context) {
              if (vaultProvider.isLoading && vaultProvider.vaults.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.all(AppSpacing.marginMobile),
                  child: SkeletonList(),
                );
              }
              if (vaultProvider.errorMessage != null &&
                  vaultProvider.vaults.isEmpty) {
                return ListView(
                  children: [
                    EmptyState(
                      icon: Icons.error_outline_rounded,
                      title: 'Impossible de charger les coffres',
                      subtitle: vaultProvider.errorMessage,
                    ),
                  ],
                );
              }
              if (vaultProvider.vaults.isEmpty) {
                return ListView(
                  children: [
                    EmptyState(
                      icon: Icons.savings_outlined,
                      title: 'Aucun coffre pour le moment',
                      subtitle:
                          'Créez un coffre pour épargner vers un objectif précis.',
                    ),
                  ],
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.all(AppSpacing.marginMobile),
                itemCount: vaultProvider.vaults.length,
                separatorBuilder: (_, _) =>
                    const SizedBox(height: AppSpacing.sm),
                itemBuilder: (context, index) {
                  final vault = vaultProvider.vaults[index];
                  final progress =
                      vault.targetAmount != null && vault.targetAmount! > 0
                      ? (vault.currentAmount / vault.targetAmount!).clamp(
                          0.0,
                          1.0,
                        )
                      : null;

                  return FadeSlideIn(
                    delay: Duration(milliseconds: index * 40),
                    child: KolaCard(
                      onTap: () async {
                        await Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) =>
                                VaultDetailScreen(vaultId: vault.id),
                          ),
                        );
                        if (context.mounted) {
                          context.read<VaultProvider>().loadVaults();
                        }
                      },
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  vault.name,
                                  style: AppTypography.bodyMdBold,
                                ),
                              ),
                              StatusPill(
                                label: _labelFor(vault.status),
                                tone: _toneFor(vault.status),
                              ),
                            ],
                          ),
                          const SizedBox(height: AppSpacing.xs),
                          Text(
                            '${currencyFormat.format(vault.currentAmount)} XOF'
                            '${vault.targetAmount != null ? ' / ${currencyFormat.format(vault.targetAmount)} XOF' : ''}',
                            style: AppTypography.bodySm,
                          ),
                          if (progress != null) ...[
                            const SizedBox(height: AppSpacing.xs),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(
                                AppRadius.full,
                              ),
                              child: LinearProgressIndicator(
                                value: progress,
                                minHeight: 6,
                                backgroundColor: AppColors.surfaceContainerHigh,
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ),
    );
  }
}
