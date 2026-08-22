import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/primary_button.dart';
import '../../core/widgets/secondary_button.dart';
import '../../core/widgets/status_pill.dart';
import '../../models/vault.dart';
import '../../providers/vault_provider.dart';
import '../transactions/widgets/amount_input_sheet.dart';

/// Détail d'un coffre-fort : ajout de fonds, déblocage, fermeture anticipée.
/// Un coffre n'est jamais retiré de la liste — seulement transitionné de statut
/// (cf. politique backend "jamais de suppression physique").
class VaultDetailScreen extends StatefulWidget {
  final int vaultId;

  const VaultDetailScreen({super.key, required this.vaultId});

  @override
  State<VaultDetailScreen> createState() => _VaultDetailScreenState();
}

class _VaultDetailScreenState extends State<VaultDetailScreen> {
  Vault? _vault;

  @override
  void initState() {
    super.initState();
    _syncFromProvider();
  }

  void _syncFromProvider() {
    final vaults = context.read<VaultProvider>().vaults;
    _vault = vaults.where((v) => v.id == widget.vaultId).firstOrNull;
  }

  Future<void> _addFunds() async {
    await AmountInputSheet.show(
      context,
      title: 'Ajouter des fonds',
      onSubmit: (amount, idempotencyKey) async {
        // La clé est volontairement ignorée ici : contrairement aux endpoints
        // /transactions/**, POST /vaults/{id}/add-funds ne l'accepte pas
        // (AddFundsRequest ne porte qu'un `amount`). Une alimentation de
        // coffre retentée après un timeout peut donc être comptée deux fois —
        // seul le backend peut fermer ce trou, en ajoutant le champ comme il
        // l'a fait pour les transactions.
        final success = await context.read<VaultProvider>().addFunds(
          vaultId: widget.vaultId,
          amount: amount,
        );
        if (success && mounted) setState(_syncFromProvider);
        return success;
      },
    );
  }

  Future<void> _unlock() async {
    final provider = context.read<VaultProvider>();
    final success = await provider.unlock(widget.vaultId);
    if (!mounted) return;
    if (success) {
      setState(_syncFromProvider);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.errorMessage ?? 'Échec du déblocage')),
      );
    }
  }

  Future<void> _closeEarly() async {
    final hasDeadline = _vault?.unlockDate != null;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Fermer ce coffre ?'),
        content: Text(
          hasDeadline
              ? 'Le coffre sera fermé avant son échéance et les fonds reversés '
                    'sur votre solde disponible. Cette action est définitive.'
              : 'Le coffre sera fermé et les fonds reversés sur votre solde '
                    'disponible. Cette action est définitive.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Fermer'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    final success = await context.read<VaultProvider>().closeEarly(
      widget.vaultId,
    );
    if (success && mounted) setState(_syncFromProvider);
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
    final vault = _vault;
    final isSubmitting = context.watch<VaultProvider>().isSubmitting;
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text(vault?.name ?? 'Coffre', style: AppTypography.headingSm),
      ),
      body: vault == null
          ? const Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            )
          : SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(AppSpacing.marginMobile),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    KolaCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(vault.name, style: AppTypography.headingSm),
                              StatusPill(
                                label: _labelFor(vault.status),
                                tone: _toneFor(vault.status),
                              ),
                            ],
                          ),
                          if (vault.purpose != null) ...[
                            const SizedBox(height: AppSpacing.xxs),
                            Text(vault.purpose!, style: AppTypography.bodySm),
                          ],
                          const SizedBox(height: AppSpacing.md),
                          Text(
                            '${currencyFormat.format(vault.currentAmount)} XOF',
                            style: AppTypography.displayLgMobile,
                          ),
                          if (vault.targetAmount != null)
                            Text(
                              'Objectif : ${currencyFormat.format(vault.targetAmount)} XOF',
                              style: AppTypography.bodySm,
                            ),
                          if (vault.unlockDate != null) ...[
                            const SizedBox(height: AppSpacing.xs),
                            Text(
                              'Déblocage prévu le '
                              '${vault.unlockDate!.day.toString().padLeft(2, '0')}/${vault.unlockDate!.month.toString().padLeft(2, '0')}/${vault.unlockDate!.year}',
                              style: AppTypography.bodySm,
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // Les actions proposées suivent strictement ce que le
                    // backend accepte (cf. VaultService) :
                    //  - ACTIVE  : /unlock est TOUJOURS rejeté (échéance non
                    //              atteinte, ou coffre sans date qui ne passera
                    //              jamais UNLOCKED tout seul) → seul /close
                    //              permet de récupérer les fonds.
                    //  - UNLOCKED: l'échéance est atteinte mais les fonds sont
                    //              encore bloqués sur le wallet — c'est ici
                    //              que /unlock doit être proposé.
                    if (vault.status == VaultStatus.active) ...[
                      PrimaryButton(
                        label: 'Ajouter des fonds',
                        isLoading: isSubmitting,
                        onPressed: _addFunds,
                      ),
                      const SizedBox(height: AppSpacing.sm),
                      SecondaryButton(
                        label: vault.unlockDate != null
                            ? 'Fermer avant échéance'
                            : 'Fermer et récupérer les fonds',
                        onPressed: isSubmitting ? null : _closeEarly,
                      ),
                      if (vault.unlockDate != null) ...[
                        const SizedBox(height: AppSpacing.sm),
                        Text(
                          'Vos fonds seront disponibles à partir du '
                          '${vault.unlockDate!.day.toString().padLeft(2, '0')}/${vault.unlockDate!.month.toString().padLeft(2, '0')}/${vault.unlockDate!.year}.',
                          style: AppTypography.bodySm,
                        ),
                      ],
                    ] else if (vault.status == VaultStatus.unlocked &&
                        vault.currentAmount > 0) ...[
                      PrimaryButton(
                        label: 'Récupérer mes fonds',
                        isLoading: isSubmitting,
                        onPressed: _unlock,
                      ),
                      const SizedBox(height: AppSpacing.sm),
                      Text(
                        'Échéance atteinte : vous pouvez transférer '
                        '${currencyFormat.format(vault.currentAmount)} XOF vers votre solde disponible.',
                        style: AppTypography.bodySm,
                      ),
                    ] else
                      Text(
                        vault.status == VaultStatus.closed
                            ? 'Ce coffre est fermé. Les fonds ont été reversés sur votre solde disponible.'
                            : 'Les fonds de ce coffre ont déjà été récupérés.',
                        style: AppTypography.bodySm,
                      ),
                  ],
                ),
              ),
            ),
    );
  }
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
