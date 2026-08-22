import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/date_format_utils.dart';
import '../../core/utils/transaction_display_utils.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_in.dart';
import '../../core/widgets/skeleton_list_item.dart';
import '../../core/widgets/transaction_item.dart';
import '../../providers/transaction_provider.dart';
import 'transaction_detail_screen.dart';

/// Historique complet des transactions d'un wallet, avec pagination
/// (GET /api/transactions/wallet/{walletId}).
class TransactionHistoryScreen extends StatefulWidget {
  final int walletId;

  const TransactionHistoryScreen({super.key, required this.walletId});

  @override
  State<TransactionHistoryScreen> createState() =>
      _TransactionHistoryScreenState();
}

class _TransactionHistoryScreenState extends State<TransactionHistoryScreen> {
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<TransactionProvider>().loadHistory(widget.walletId);
    });
    _scrollController.addListener(() {
      if (_scrollController.position.pixels >=
          _scrollController.position.maxScrollExtent - 200) {
        context.read<TransactionProvider>().loadMoreHistory();
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TransactionProvider>();
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Historique', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            if (provider.isLoadingHistory && provider.history.isEmpty) {
              return const Padding(
                padding: EdgeInsets.all(AppSpacing.marginMobile),
                child: SkeletonList(),
              );
            }
            if (provider.historyError != null && provider.history.isEmpty) {
              return EmptyState(
                icon: Icons.error_outline_rounded,
                title: "Impossible de charger l'historique",
                subtitle: provider.historyError,
              );
            }
            if (provider.history.isEmpty) {
              return const EmptyState(
                icon: Icons.receipt_long_outlined,
                title: 'Aucune transaction',
              );
            }
            return ListView.separated(
              controller: _scrollController,
              padding: const EdgeInsets.all(AppSpacing.marginMobile),
              itemCount:
                  provider.history.length + (provider.hasMoreHistory ? 1 : 0),
              separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
              itemBuilder: (context, index) {
                if (index >= provider.history.length) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: AppSpacing.md),
                    child: Center(
                      child: CircularProgressIndicator(
                        color: AppColors.primary,
                      ),
                    ),
                  );
                }
                final tx = provider.history[index];
                return FadeSlideIn(
                  delay: Duration(milliseconds: index * 40),
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
                              TransactionDetailScreen(reference: tx.reference),
                        ),
                      );
                    },
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}
