import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/idempotency_key.dart';
import '../../core/widgets/amount_input_field.dart';
import '../../core/widgets/primary_button.dart';
import '../../models/beneficiary.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/wallet_provider.dart';

/// Dernière étape du transfert : montant + description, envoi vers
/// POST /api/transactions/transfer.
class TransferAmountScreen extends StatefulWidget {
  final int walletId;
  final Beneficiary beneficiary;

  const TransferAmountScreen({
    super.key,
    required this.walletId,
    required this.beneficiary,
  });

  @override
  State<TransferAmountScreen> createState() => _TransferAmountScreenState();
}

class _TransferAmountScreenState extends State<TransferAmountScreen> {
  final _amountController = TextEditingController();
  final _descriptionController = TextEditingController();

  /// Vit aussi longtemps que l'écran, donc aussi longtemps que l'intention de
  /// virement : c'est ce qui rend « Réessayer » sûr après un timeout.
  final _idempotencyKey = IdempotencyKeyHolder();

  @override
  void dispose() {
    _amountController.dispose();
    _descriptionController.dispose();
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

    final description = _descriptionController.text.trim();
    final txProvider = context.read<TransactionProvider>();
    final success = await txProvider.transfer(
      sourceWalletId: widget.walletId,
      beneficiaryId: widget.beneficiary.id,
      amount: amount,
      description: description.isEmpty ? null : description,
      // Signature = les champs envoyés. Identiques : le backend rejoue le
      // virement d'origine. Montant corrigé : clé neuve, donc pas de rejeu
      // silencieux de l'ancien montant.
      idempotencyKey: _idempotencyKey.forIntent(
        '${widget.walletId}:${widget.beneficiary.id}:$amount:$description',
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
        SnackBar(content: Text(txProvider.actionError ?? "Échec de l'envoi")),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isSubmitting =
        context.watch<TransactionProvider>().actionStatus ==
        TxActionStatus.loading;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text(
          'Envoyer à ${widget.beneficiary.alias}',
          style: AppTypography.headingSm,
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${widget.beneficiary.phoneNumber} · ${widget.beneficiary.network.label}',
                style: AppTypography.bodySm,
              ),
              const SizedBox(height: AppSpacing.md),
              AmountInputField(controller: _amountController, label: 'Montant'),
              const SizedBox(height: AppSpacing.md),
              Text('Description (optionnelle)', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _descriptionController,
                decoration: InputDecoration(
                  hintText: 'ex : Aide familiale',
                  filled: true,
                  fillColor: AppColors.surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: 'Envoyer',
                isLoading: isSubmitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
