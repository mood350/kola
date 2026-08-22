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
import '../../models/scheduled_transfer.dart';
import '../../providers/scheduled_transfer_provider.dart';
import '../../providers/wallet_provider.dart';
import 'create_scheduled_transfer_screen.dart';

/// Liste des virements programmés (récurrents) de l'utilisateur.
class ScheduledTransfersScreen extends StatefulWidget {
  const ScheduledTransfersScreen({super.key});

  @override
  State<ScheduledTransfersScreen> createState() =>
      _ScheduledTransfersScreenState();
}

class _ScheduledTransfersScreenState extends State<ScheduledTransfersScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<ScheduledTransferProvider>().loadTransfers();
    });
  }

  StatusPillTone _toneFor(ScheduleStatus status) {
    switch (status) {
      case ScheduleStatus.active:
        return StatusPillTone.success;
      case ScheduleStatus.paused:
        return StatusPillTone.warning;
      case ScheduleStatus.failedPermanently:
        return StatusPillTone.danger;
    }
  }

  Future<void> _confirmDelete(ScheduledTransfer st) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Supprimer ce virement ?'),
        content: const Text(
          'La programmation sera supprimée. Les virements déjà exécutés '
          'restent visibles dans votre historique.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    await context.read<ScheduledTransferProvider>().delete(st.id);
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ScheduledTransferProvider>();
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Virements programmés', style: AppTypography.headingSm),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded, color: AppColors.primary),
            onPressed: () async {
              final wallet = context.read<WalletProvider>().primaryWallet;
              if (wallet == null) return;
              await Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) =>
                      CreateScheduledTransferScreen(walletId: wallet.id),
                ),
              );
              if (context.mounted) {
                context.read<ScheduledTransferProvider>().loadTransfers();
              }
            },
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => provider.loadTransfers(),
          child: Builder(
            builder: (context) {
              if (provider.isLoading && provider.transfers.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.all(AppSpacing.marginMobile),
                  child: SkeletonList(),
                );
              }
              if (provider.errorMessage != null && provider.transfers.isEmpty) {
                return ListView(
                  children: [
                    EmptyState(
                      icon: Icons.error_outline_rounded,
                      title: 'Impossible de charger les virements',
                      subtitle: provider.errorMessage,
                    ),
                  ],
                );
              }
              if (provider.transfers.isEmpty) {
                return ListView(
                  children: const [
                    EmptyState(
                      icon: Icons.schedule_rounded,
                      title: 'Aucun virement programmé',
                      subtitle:
                          'Automatisez votre épargne en programmant un virement récurrent.',
                    ),
                  ],
                );
              }

              return ListView.separated(
                padding: const EdgeInsets.all(AppSpacing.marginMobile),
                itemCount: provider.transfers.length,
                separatorBuilder: (_, _) =>
                    const SizedBox(height: AppSpacing.sm),
                itemBuilder: (context, index) {
                  final st = provider.transfers[index];
                  return FadeSlideIn(
                    delay: Duration(milliseconds: index * 40),
                    child: KolaCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  '${currencyFormat.format(st.amount)} ${st.currency}',
                                  style: AppTypography.bodyMdBold,
                                ),
                              ),
                              StatusPill(
                                label: st.status.label,
                                tone: _toneFor(st.status),
                              ),
                            ],
                          ),
                          const SizedBox(height: AppSpacing.xxs),
                          Text(st.scheduleLabel, style: AppTypography.bodySm),
                          if (st.targetVaultName != null)
                            Text(
                              'Vers le coffre « ${st.targetVaultName} »',
                              style: AppTypography.bodySm,
                            ),
                          if (st.description != null &&
                              st.description!.isNotEmpty)
                            Text(st.description!, style: AppTypography.bodySm),
                          if (st.nextExecutionDate != null)
                            Text(
                              'Prochaine exécution le '
                              '${st.nextExecutionDate!.day.toString().padLeft(2, '0')}/'
                              '${st.nextExecutionDate!.month.toString().padLeft(2, '0')}/'
                              '${st.nextExecutionDate!.year}',
                              style: AppTypography.labelXs,
                            ),
                          const SizedBox(height: AppSpacing.sm),
                          Row(
                            children: [
                              TextButton.icon(
                                onPressed: provider.isSubmitting
                                    ? null
                                    : () => st.status == ScheduleStatus.active
                                          ? context
                                                .read<
                                                  ScheduledTransferProvider
                                                >()
                                                .pause(st.id)
                                          : context
                                                .read<
                                                  ScheduledTransferProvider
                                                >()
                                                .resume(st.id),
                                icon: Icon(
                                  st.status == ScheduleStatus.active
                                      ? Icons.pause_rounded
                                      : Icons.play_arrow_rounded,
                                  size: 18,
                                ),
                                label: Text(
                                  st.status == ScheduleStatus.active
                                      ? 'Mettre en pause'
                                      : 'Reprendre',
                                ),
                              ),
                              const Spacer(),
                              TextButton.icon(
                                onPressed: provider.isSubmitting
                                    ? null
                                    : () => _confirmDelete(st),
                                icon: const Icon(
                                  Icons.delete_outline_rounded,
                                  size: 18,
                                  color: AppColors.danger,
                                ),
                                label: Text(
                                  'Supprimer',
                                  style: AppTypography.bodySm.copyWith(
                                    color: AppColors.danger,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
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
