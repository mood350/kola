import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/idempotency_key.dart';
import '../../../core/widgets/amount_input_field.dart';
import '../../../core/widgets/primary_button.dart';
import '../../../models/payment_method.dart';
import '../../../providers/payment_method_provider.dart';

/// Feuille de dépôt ou de retrait Mobile Money.
///
/// ═══ DEUX MODES QUI NE FONT PAS LA MÊME CHOSE ═══
///
/// MOBILE MONEY appelle vraiment l'opérateur : au dépôt, une demande de débit
/// part sur le téléphone du client ; au retrait, un versement part vers son
/// numéro. Dans les deux cas l'écriture naît EN ATTENTE.
///
/// TEST ne parle à personne : le dépôt crédite directement, le retrait débite
/// directement. C'est le comportement historique, conservé pour développer sans
/// compte marchand. Il est signalé comme tel, parce qu'un mode qui fabrique de
/// l'argent ne doit jamais pouvoir être pris pour le mode normal.
///
/// ⚠️ Ces routes de test doivent disparaître — ou passer en réservé
/// administrateur — avant toute mise en production.
class MobileMoneySheet extends StatefulWidget {
  final String title;
  final bool isDeposit;

  /// Opération réelle : montant, opérateur, numéro, clé d'idempotence.
  final Future<bool> Function(
    double amount,
    String mode,
    String phoneNumber,
    String idempotencyKey,
  )
  onRealSubmit;

  /// Opération de test : montant et clé seulement.
  final Future<bool> Function(double amount, String idempotencyKey)
  onTestSubmit;

  const MobileMoneySheet({
    super.key,
    required this.title,
    required this.isDeposit,
    required this.onRealSubmit,
    required this.onTestSubmit,
  });

  static Future<void> show(
    BuildContext context, {
    required String title,
    required bool isDeposit,
    required Future<bool> Function(double, String, String, String) onRealSubmit,
    required Future<bool> Function(double, String) onTestSubmit,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceContainerLowest,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.xl)),
      ),
      builder: (_) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: MobileMoneySheet(
          title: title,
          isDeposit: isDeposit,
          onRealSubmit: onRealSubmit,
          onTestSubmit: onTestSubmit,
        ),
      ),
    );
  }

  @override
  State<MobileMoneySheet> createState() => _MobileMoneySheetState();
}

class _MobileMoneySheetState extends State<MobileMoneySheet> {
  final _amountController = TextEditingController();
  final _phoneController = TextEditingController();

  /// Une clé par intention, pas par envoi : si la réponse se perd et que
  /// l'utilisateur relance, le backend reconnaît la même clé et n'exécute
  /// l'opération qu'une fois.
  final _idempotencyKey = IdempotencyKeyHolder();

  bool _realMode = true;
  String? _operator;
  bool _submitting = false;

  static final _phonePattern = RegExp(r'^\+[1-9]\d{6,14}$');

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<PaymentMethodProvider>().load();
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final amount = AmountInputField.parse(_amountController.text);
    final messenger = ScaffoldMessenger.of(context);

    if (amount < 1) {
      messenger.showSnackBar(
        const SnackBar(content: Text('Saisissez un montant')),
      );
      return;
    }

    final phone = _phoneController.text.replaceAll(RegExp(r'[\s.-]'), '');
    if (_realMode) {
      if (_operator == null) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Choisissez un opérateur')),
        );
        return;
      }
      if (!_phonePattern.hasMatch(phone)) {
        messenger.showSnackBar(
          const SnackBar(
            content: Text('Numéro au format international, ex : +22890000000'),
          ),
        );
        return;
      }
    }

    setState(() => _submitting = true);

    final intent = _realMode ? 'real' : 'test';
    final key = _idempotencyKey.forIntent('${widget.title}-$intent-$amount');

    final success = _realMode
        ? await widget.onRealSubmit(amount, _operator!, phone, key)
        : await widget.onTestSubmit(amount, key);

    if (!mounted) return;
    setState(() => _submitting = false);

    if (success) {
      _idempotencyKey.reset();
      Navigator.of(context).pop();
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            _realMode
                ? widget.isDeposit
                      ? "Demande envoyée. Validez-la sur votre téléphone."
                      : "Retrait en cours. Vous serez notifié dès l'envoi."
                : widget.isDeposit
                ? 'Compte crédité (test)'
                : 'Compte débité (test)',
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final payment = context.watch<PaymentMethodProvider>();

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.marginMobile),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(widget.title, style: AppTypography.headingSm),
            const SizedBox(height: AppSpacing.md),

            _ModeSwitch(
              realMode: _realMode,
              isDeposit: widget.isDeposit,
              onChanged: (value) => setState(() => _realMode = value),
            ),
            const SizedBox(height: AppSpacing.md),

            AmountInputField(controller: _amountController, label: 'Montant'),
            const SizedBox(height: AppSpacing.md),

            if (_realMode) ...[
              if (payment.isLoading)
                Text('Chargement des opérateurs…', style: AppTypography.bodySm)
              else ...[
                if (!payment.providerEnabled)
                  _Notice(
                    color: AppColors.warning,
                    text:
                        "Le paiement réel n'est pas configuré sur ce serveur. "
                        "Utilisez le mode test.",
                  ),
                Text('Opérateur', style: AppTypography.bodySm),
                const SizedBox(height: AppSpacing.xs),
                Wrap(
                  spacing: AppSpacing.xs,
                  runSpacing: AppSpacing.xs,
                  children: payment.available
                      .map(
                        (method) => _OperatorChip(
                          method: method,
                          selected: _operator == method.code,
                          enabled: payment.providerEnabled,
                          onTap: () => setState(() => _operator = method.code),
                        ),
                      )
                      .toList(),
                ),
                const SizedBox(height: AppSpacing.md),
                TextField(
                  controller: _phoneController,
                  keyboardType: TextInputType.phone,
                  enabled: payment.providerEnabled,
                  decoration: InputDecoration(
                    labelText: 'Numéro Mobile Money',
                    hintText: '+22890000000',
                    helperText: widget.isDeposit
                        ? 'Le numéro qui sera débité'
                        : "Le numéro qui recevra l'argent",
                    filled: true,
                    fillColor: AppColors.surfaceContainerLow,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.md),
                      borderSide: BorderSide.none,
                    ),
                  ),
                ),
                if (!widget.isDeposit) ...[
                  const SizedBox(height: AppSpacing.sm),
                  _Notice(
                    color: AppColors.primary,
                    text:
                        "Le montant quitte votre compte dès l'envoi. En cas "
                        "d'échec, montant et frais sont recrédités.",
                  ),
                ],
              ],
            ] else
              _Notice(
                color: AppColors.warning,
                text: widget.isDeposit
                    ? "Aucun argent réel : le compte est crédité directement."
                    : "Aucun versement : le compte est simplement débité.",
              ),

            const SizedBox(height: AppSpacing.lg),
            PrimaryButton(
              label: _realMode
                  ? (widget.isDeposit ? 'Envoyer la demande' : 'Retirer')
                  : (widget.isDeposit ? 'Créditer (test)' : 'Débiter (test)'),
              isLoading: _submitting,
              onPressed: _realMode && !payment.providerEnabled ? null : _submit,
            ),
            const SizedBox(height: AppSpacing.md),
          ],
        ),
      ),
    );
  }
}

/// Bascule réel / test, visible en permanence.
///
/// Le mode actif est lisible au-dessus du formulaire, et le test porte un
/// avertissement : les deux ne font pas la même chose, et les confondre revient
/// à croire avoir été payé.
class _ModeSwitch extends StatelessWidget {
  final bool realMode;
  final bool isDeposit;
  final ValueChanged<bool> onChanged;

  const _ModeSwitch({
    required this.realMode,
    required this.isDeposit,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _ModeOption(
            label: 'Mobile Money',
            hint: isDeposit ? 'Débit réel' : 'Versement réel',
            selected: realMode,
            color: AppColors.primary,
            onTap: () => onChanged(true),
          ),
        ),
        const SizedBox(width: AppSpacing.xs),
        Expanded(
          child: _ModeOption(
            label: 'Test',
            hint: isDeposit ? 'Crédit fictif' : 'Débit fictif',
            selected: !realMode,
            color: AppColors.warning,
            onTap: () => onChanged(false),
          ),
        ),
      ],
    );
  }
}

class _ModeOption extends StatelessWidget {
  final String label;
  final String hint;
  final bool selected;
  final Color color;
  final VoidCallback onTap;

  const _ModeOption({
    required this.label,
    required this.hint,
    required this.selected,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppRadius.md),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.sm),
        decoration: BoxDecoration(
          color: selected
              ? color.withValues(alpha: 0.12)
              : AppColors.surfaceContainerLow,
          borderRadius: BorderRadius.circular(AppRadius.md),
          border: Border.all(
            color: selected ? color : AppColors.outlineVariant,
            width: selected ? 2 : 1,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: AppTypography.bodyMdBold),
            Text(hint, style: AppTypography.labelXs),
          ],
        ),
      ),
    );
  }
}

class _OperatorChip extends StatelessWidget {
  final PaymentMethod method;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  const _OperatorChip({
    required this.method,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      label: Text(method.label),
      selected: selected,
      onSelected: enabled ? (_) => onTap() : null,
      selectedColor: AppColors.primaryFixed,
      backgroundColor: AppColors.surfaceContainerLow,
    );
  }
}

class _Notice extends StatelessWidget {
  final Color color;
  final String text;

  const _Notice({required this.color, required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.sm),
      padding: const EdgeInsets.all(AppSpacing.sm),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Text(text, style: AppTypography.bodySm),
    );
  }
}
