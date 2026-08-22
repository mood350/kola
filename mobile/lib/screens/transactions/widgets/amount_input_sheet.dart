import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/utils/idempotency_key.dart';
import '../../../core/widgets/amount_input_field.dart';
import '../../../core/widgets/primary_button.dart';

/// Bottom sheet de saisie de montant, utilisé par les actions rapides
/// Déposer/Retirer de Home (paramétré par [title]/[onSubmit]).
///
/// La sheet fournit elle-même la clé d'idempotence à [onSubmit] : elle survit
/// aux tentatives ratées (elle ne se ferme qu'en cas de succès), c'est donc
/// elle qui porte l'intention de dépôt/retrait, pas l'appelant.
class AmountInputSheet extends StatefulWidget {
  final String title;
  final Future<bool> Function(double amount, String idempotencyKey) onSubmit;

  const AmountInputSheet({
    super.key,
    required this.title,
    required this.onSubmit,
  });

  static Future<void> show(
    BuildContext context, {
    required String title,
    required Future<bool> Function(double amount, String idempotencyKey)
    onSubmit,
  }) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceContainerLowest,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.xl)),
      ),
      builder: (_) => AmountInputSheet(title: title, onSubmit: onSubmit),
    );
  }

  @override
  State<AmountInputSheet> createState() => _AmountInputSheetState();
}

class _AmountInputSheetState extends State<AmountInputSheet> {
  final _controller = TextEditingController();
  final _idempotencyKey = IdempotencyKeyHolder();
  bool _isSubmitting = false;
  String? _errorText;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final amount = AmountInputField.parse(_controller.text);
    if (amount <= 0) {
      setState(() => _errorText = 'Montant invalide');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorText = null;
    });

    // Même montant retenté = même clé : le backend rejoue le dépôt d'origine
    // au lieu d'en créer un second. Montant modifié = clé neuve.
    final success = await widget.onSubmit(
      amount,
      _idempotencyKey.forIntent('$amount'),
    );

    if (!mounted) return;
    if (success) {
      _idempotencyKey.reset();
      Navigator.of(context).pop();
    } else {
      setState(() {
        _isSubmitting = false;
        _errorText = "Échec de l'opération";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: AppSpacing.marginMobile,
        right: AppSpacing.marginMobile,
        top: AppSpacing.lg,
        bottom: MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(widget.title, style: AppTypography.headingMd),
          const SizedBox(height: AppSpacing.md),
          AmountInputField(
            controller: _controller,
            label: 'Montant',
            errorText: _errorText,
          ),
          const SizedBox(height: AppSpacing.lg),
          PrimaryButton(
            label: 'Confirmer',
            isLoading: _isSubmitting,
            onPressed: _submit,
          ),
        ],
      ),
    );
  }
}
