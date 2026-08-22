import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/idempotency_key.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/amount_input_field.dart';
import '../../core/widgets/primary_button.dart';
import '../../providers/merchant_provider.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/wallet_provider.dart';

/// Étape finale du paiement marchand : confirmation du marchand + montant.
class MerchantPayScreen extends StatefulWidget {
  final int walletId;
  final String merchantCode;

  const MerchantPayScreen({
    super.key,
    required this.walletId,
    required this.merchantCode,
  });

  @override
  State<MerchantPayScreen> createState() => _MerchantPayScreenState();
}

class _MerchantPayScreenState extends State<MerchantPayScreen> {
  final _amountController = TextEditingController();

  /// Cf. TransferAmountScreen : la clé est portée par l'écran pour qu'un
  /// second tap sur « Payer » ne débite pas le marchand deux fois.
  final _idempotencyKey = IdempotencyKeyHolder();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<MerchantProvider>().lookup(widget.merchantCode);
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final amount = AmountInputField.parse(_amountController.text);
    if (amount <= 0) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Montant invalide')));
      return;
    }

    final txProvider = context.read<TransactionProvider>();
    final success = await txProvider.payMerchant(
      sourceWalletId: widget.walletId,
      merchantCode: widget.merchantCode,
      amount: amount,
      idempotencyKey: _idempotencyKey.forIntent(
        '${widget.walletId}:${widget.merchantCode}:$amount',
      ),
    );

    if (!mounted) return;
    if (success) {
      _idempotencyKey.reset();
      await context.read<WalletProvider>().loadHomeData();
      if (!mounted) return;
      Navigator.of(context).popUntil((route) => route.isFirst);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(txProvider.actionError ?? 'Échec du paiement')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final merchantProvider = context.watch<MerchantProvider>();
    final merchant = merchantProvider.merchant;
    final isSubmitting =
        context.watch<TransactionProvider>().actionStatus ==
        TxActionStatus.loading;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Payer', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            if (merchantProvider.status == MerchantLookupStatus.loading) {
              return const Center(
                child: CircularProgressIndicator(color: AppColors.primary),
              );
            }
            if (merchantProvider.status == MerchantLookupStatus.error ||
                merchant == null) {
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.marginMobile),
                  child: Text(
                    merchantProvider.errorMessage ?? 'Marchand introuvable',
                    style: AppTypography.bodyMd,
                    textAlign: TextAlign.center,
                  ),
                ),
              );
            }

            return SingleChildScrollView(
              padding: const EdgeInsets.all(AppSpacing.marginMobile),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  KolaCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const CircleAvatar(
                              backgroundColor: AppColors.surfaceVariant,
                              child: Icon(
                                Icons.storefront_rounded,
                                color: AppColors.onSurfaceVariant,
                              ),
                            ),
                            const SizedBox(width: AppSpacing.sm),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    merchant.name,
                                    style: AppTypography.bodyMdBold,
                                  ),
                                  if (merchant.category != null)
                                    Text(
                                      merchant.category!,
                                      style: AppTypography.bodySm,
                                    ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  AmountInputField(
                    controller: _amountController,
                    label: 'Montant à payer',
                  ),
                  const SizedBox(height: AppSpacing.xl),
                  PrimaryButton(
                    label: 'Payer',
                    isLoading: isSubmitting,
                    onPressed: _submit,
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
