import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/amount_input_field.dart';
import '../../core/widgets/primary_button.dart';
import '../../models/scheduled_transfer.dart';
import '../../models/vault.dart';
import '../../providers/scheduled_transfer_provider.dart';
import '../../providers/vault_provider.dart';

const _joursSemaine = {
  1: 'Lundi',
  2: 'Mardi',
  3: 'Mercredi',
  4: 'Jeudi',
  5: 'Vendredi',
  6: 'Samedi',
  7: 'Dimanche',
};

/// Formulaire de création d'un virement programmé.
class CreateScheduledTransferScreen extends StatefulWidget {
  final int walletId;

  const CreateScheduledTransferScreen({super.key, required this.walletId});

  @override
  State<CreateScheduledTransferScreen> createState() =>
      _CreateScheduledTransferScreenState();
}

class _CreateScheduledTransferScreenState
    extends State<CreateScheduledTransferScreen> {
  final _amountController = TextEditingController();
  final _descriptionController = TextEditingController();

  ScheduleFrequency _frequency = ScheduleFrequency.monthly;
  int _executionDay = 1;
  int? _targetVaultId;

  @override
  void initState() {
    super.initState();
    // Les coffres actifs alimentables sont proposés comme destination.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<VaultProvider>().loadVaults();
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  /// Le domaine du jour dépend de la fréquence : 1-31 mensuel, 1-7 hebdo.
  int get _maxDay => _frequency == ScheduleFrequency.monthly ? 31 : 7;

  Future<void> _submit() async {
    final amount = AmountInputField.parse(_amountController.text);
    if (amount <= 0) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Montant invalide')));
      return;
    }

    final provider = context.read<ScheduledTransferProvider>();
    final success = await provider.create(
      walletId: widget.walletId,
      targetVaultId: _targetVaultId,
      frequency: _frequency,
      executionDay: _executionDay,
      amount: amount,
      description: _descriptionController.text.trim().isEmpty
          ? null
          : _descriptionController.text.trim(),
    );

    if (!mounted) return;
    if (success) {
      Navigator.of(context).pop();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            provider.errorMessage ?? 'Impossible de créer le virement',
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isSubmitting = context
        .watch<ScheduledTransferProvider>()
        .isSubmitting;
    final activeVaults = context
        .watch<VaultProvider>()
        .vaults
        .where((v) => v.status == VaultStatus.active)
        .toList();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Nouveau virement', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AmountInputField(
                controller: _amountController,
                label: 'Montant à virer',
              ),
              const SizedBox(height: AppSpacing.md),

              Text('Fréquence', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              SegmentedButton<ScheduleFrequency>(
                segments: const [
                  ButtonSegment(
                    value: ScheduleFrequency.monthly,
                    label: Text('Mensuel'),
                  ),
                  ButtonSegment(
                    value: ScheduleFrequency.weekly,
                    label: Text('Hebdomadaire'),
                  ),
                ],
                selected: {_frequency},
                onSelectionChanged: (values) => setState(() {
                  _frequency = values.first;
                  // Un jour > 7 n'a aucun sens en hebdomadaire : on borne.
                  if (_executionDay > _maxDay) _executionDay = 1;
                }),
              ),
              const SizedBox(height: AppSpacing.md),

              Text(
                _frequency == ScheduleFrequency.monthly
                    ? 'Jour du mois'
                    : 'Jour de la semaine',
                style: AppTypography.bodySm,
              ),
              const SizedBox(height: AppSpacing.xxs),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: _executionDay,
                    isExpanded: true,
                    items: List.generate(_maxDay, (i) => i + 1)
                        .map(
                          (d) => DropdownMenuItem(
                            value: d,
                            child: Text(
                              _frequency == ScheduleFrequency.monthly
                                  ? 'Le $d'
                                  : _joursSemaine[d]!,
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (v) => setState(() => _executionDay = v!),
                  ),
                ),
              ),
              if (_frequency == ScheduleFrequency.monthly &&
                  _executionDay > 28) ...[
                const SizedBox(height: AppSpacing.xxs),
                Text(
                  'Pour les mois plus courts, le virement sera exécuté le dernier jour du mois.',
                  style: AppTypography.labelXs,
                ),
              ],
              const SizedBox(height: AppSpacing.md),

              Text('Destination', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int?>(
                    value: _targetVaultId,
                    isExpanded: true,
                    items: [
                      const DropdownMenuItem<int?>(
                        value: null,
                        child: Text('Débit simple du portefeuille'),
                      ),
                      ...activeVaults.map(
                        (v) => DropdownMenuItem<int?>(
                          value: v.id,
                          child: Text('Coffre « ${v.name} »'),
                        ),
                      ),
                    ],
                    onChanged: (v) => setState(() => _targetVaultId = v),
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),

              Text('Description (optionnelle)', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _descriptionController,
                decoration: InputDecoration(
                  hintText: 'ex : Épargne automatique',
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
                label: 'Programmer',
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
