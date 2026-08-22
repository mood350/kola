import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/amount_input_field.dart';
import '../../core/widgets/primary_button.dart';
import '../../providers/vault_provider.dart';

/// Formulaire de création d'un coffre-fort.
/// walletId = wallet principal de l'utilisateur (pas de sélecteur de wallet
/// pour l'instant — un seul wallet actif par utilisateur dans cette version).
class CreateVaultScreen extends StatefulWidget {
  final int walletId;

  const CreateVaultScreen({super.key, required this.walletId});

  @override
  State<CreateVaultScreen> createState() => _CreateVaultScreenState();
}

class _CreateVaultScreenState extends State<CreateVaultScreen> {
  final _nameController = TextEditingController();
  final _purposeController = TextEditingController();
  final _targetController = TextEditingController();
  final _initialController = TextEditingController();
  DateTime? _unlockDate;

  Map<String, String> _fieldErrors = {};

  @override
  void dispose() {
    _nameController.dispose();
    _purposeController.dispose();
    _targetController.dispose();
    _initialController.dispose();
    super.dispose();
  }

  Future<void> _pickUnlockDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: now.add(const Duration(days: 30)),
      firstDate: now.add(const Duration(days: 1)),
      lastDate: now.add(const Duration(days: 365 * 5)),
    );
    if (picked != null) {
      setState(() => _unlockDate = picked);
    }
  }

  Future<void> _submit() async {
    setState(() => _fieldErrors = {});

    if (_nameController.text.trim().isEmpty) {
      setState(() => _fieldErrors = {'name': 'Le nom est obligatoire'});
      return;
    }

    final provider = context.read<VaultProvider>();
    final targetAmount = _targetController.text.isEmpty
        ? null
        : AmountInputField.parse(_targetController.text);

    final success = await provider.createVault(
      walletId: widget.walletId,
      name: _nameController.text.trim(),
      purpose: _purposeController.text.trim().isEmpty
          ? null
          : _purposeController.text.trim(),
      targetAmount: targetAmount,
      initialAmount: AmountInputField.parse(_initialController.text),
      unlockDate: _unlockDate,
    );

    if (!mounted) return;
    if (success) {
      Navigator.of(context).pop();
    } else {
      final error = provider.errorMessage;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error ?? 'Impossible de créer le coffre')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isSubmitting = context.watch<VaultProvider>().isSubmitting;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Nouveau coffre', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Nom du coffre', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _nameController,
                decoration: InputDecoration(
                  hintText: 'ex : Vacances 2027',
                  errorText: _fieldErrors['name'],
                  filled: true,
                  fillColor: AppColors.surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              Text('Objectif (optionnel)', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _purposeController,
                decoration: InputDecoration(
                  hintText: 'ex : Voyage en famille',
                  filled: true,
                  fillColor: AppColors.surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              AmountInputField(
                controller: _targetController,
                label: 'Montant cible (optionnel)',
              ),
              const SizedBox(height: AppSpacing.md),
              AmountInputField(
                controller: _initialController,
                label: 'Dépôt initial',
              ),
              const SizedBox(height: AppSpacing.md),
              Text(
                'Date de déblocage (optionnelle)',
                style: AppTypography.bodySm,
              ),
              const SizedBox(height: AppSpacing.xxs),
              InkWell(
                onTap: _pickUnlockDate,
                borderRadius: BorderRadius.circular(AppRadius.md),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.md,
                    vertical: AppSpacing.sm,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(AppRadius.md),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        _unlockDate != null
                            ? '${_unlockDate!.day.toString().padLeft(2, '0')}/${_unlockDate!.month.toString().padLeft(2, '0')}/${_unlockDate!.year}'
                            : 'Aucune date choisie',
                        style: AppTypography.bodyMd,
                      ),
                      const Icon(
                        Icons.calendar_today_outlined,
                        color: AppColors.onSurfaceVariant,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: 'Créer le coffre',
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
