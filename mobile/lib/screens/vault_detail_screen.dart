import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/widgets/confirm_operation.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/vault.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/vault_service.dart';

class VaultDetailScreen extends StatelessWidget {
  const VaultDetailScreen({
    super.key,
    required this.onNavigate,
    required this.vaultId,
  });

  final void Function(KolaRoute) onNavigate;
  final String? vaultId;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final matches = data.vaults.where((item) => item.id == vaultId);
    final vault = matches.isEmpty ? null : matches.first;

    if (vault == null) {
      return KolaScreen(
        route: KolaRoute.vaultDetail,
        onNavigate: onNavigate,
        children: [
          BackLink(
            label: 'Mes coffres',
            onTap: () => onNavigate(KolaRoute.vaults),
          ),
          const KolaCard(
            padding: EdgeInsets.symmetric(vertical: 35),
            child: EmptyState(
              icon: KolaIcons.lockOpenOutline,
              title: 'Coffre introuvable',
              message: 'Revenez à la liste et sélectionnez un coffre.',
            ),
          ),
        ],
      );
    }

    final remaining = (vault.targetAmount - vault.balance).clamp(
      0.0,
      double.infinity,
    );

    return KolaScreen(
      route: KolaRoute.vaultDetail,
      onNavigate: onNavigate,
      children: [
        BackLink(
          label: 'Mes coffres',
          onTap: () => onNavigate(KolaRoute.vaults),
        ),
        const SizedBox(height: AppSpacing.xs),
        Row(
          children: [
            Container(
              width: 58,
              height: 58,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(18),
              ),
              child: const Icon(
                KolaIcons.lockClosedOutline,
                size: 27,
                color: AppColors.yellow,
              ),
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'COFFRE KOLA',
                    style: AppTypography.badge.copyWith(
                      fontSize: 8,
                      fontWeight: FontWeight.w800,
                      color: AppColors.greenDark,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    vault.name,
                    style: AppTypography.screenTitle.copyWith(fontSize: 23),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    vault.description ?? 'Votre épargne sécurisée.',
                    style: AppTypography.caption,
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),
        KolaCard(
          color: AppColors.primary,
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'SOLDE DU COFFRE',
                style: AppTypography.fieldLabel.copyWith(
                  color: const Color(0xFFADC6FF),
                ),
              ),
              const SizedBox(height: 7),
              RichText(
                text: TextSpan(
                  style: AppTypography.balance.copyWith(fontSize: 30),
                  children: [
                    TextSpan(text: money(vault.balance)),
                    TextSpan(
                      text: ' FCFA',
                      style: AppTypography.body.copyWith(
                        fontSize: 15,
                        color: AppColors.white,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 22),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Progression de l’objectif',
                    style: AppTypography.caption.copyWith(
                      color: AppColors.white,
                    ),
                  ),
                  Text(
                    '${vault.progressPercent.round()}%',
                    style: AppTypography.badge.copyWith(
                      fontSize: 11,
                      fontWeight: FontWeight.w900,
                      color: AppColors.yellow,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 9),
              KolaProgress(vault.progressPercent, color: AppColors.yellow),
              const SizedBox(height: 9),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Objectif : ${vault.targetAmount > 0 ? '${money(vault.targetAmount)} FCFA' : 'Non défini'}',
                    style: AppTypography.caption.copyWith(
                      fontSize: 9,
                      color: const Color(0xFFDCE4FF),
                    ),
                  ),
                  if (vault.targetAmount > 0)
                    Text(
                      'Reste ${money(remaining)} FCFA',
                      style: AppTypography.badge.copyWith(
                        fontSize: 9,
                        color: AppColors.mint,
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 13),
        // stretch sans hauteur bornée (on est dans une ListView) donne une
        // hauteur infinie : IntrinsicHeight aligne les tuiles sur la plus haute.
        IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: _InfoTile(
                  icon: KolaIcons.calendarOutline,
                  label: 'DATE CIBLE',
                  value: vault.targetDate == null
                      ? 'Non définie'
                      : shortDate(vault.targetDate!),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _InfoTile(
                  icon: KolaIcons.shieldCheckmarkOutline,
                  label: 'STATUT',
                  value: vault.status,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 13),
        KolaCard(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
          child: Column(
            children: [
              DetailRow(
                label: 'Créé le',
                value: vault.createdAt == null
                    ? '—'
                    : shortDate(vault.createdAt!),
              ),
              DetailRow(label: 'Devise', value: vault.currency),
              DetailRow(
                label: 'Identifiant',
                value: vault.id.length >= 8
                    ? vault.id.substring(0, 8).toUpperCase()
                    : vault.id.toUpperCase(),
                last: true,
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.md),
        PrimaryButton(
          label: 'Alimenter ce coffre',
          icon: KolaIcons.addCircleOutline,
          onPressed: () => _openDepositSheet(context, vault),
        ),
      ],
    );
  }

  void _openDepositSheet(BuildContext context, Vault vault) {
    final data = context.read<KolaDataProvider>();
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _DepositSheet(vault: vault, data: data),
    );
  }
}

class _InfoTile extends StatelessWidget {
  const _InfoTile({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return KolaCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: AppColors.greenDark),
          const SizedBox(height: 10),
          Text(label, style: AppTypography.fieldLabel.copyWith(fontSize: 8)),
          const SizedBox(height: 4),
          Text(
            value,
            style: AppTypography.smallBold.copyWith(
              fontWeight: FontWeight.w800,
              color: AppColors.ink,
            ),
          ),
        ],
      ),
    );
  }
}

class _DepositSheet extends StatefulWidget {
  const _DepositSheet({required this.vault, required this.data});

  final Vault vault;
  final KolaDataProvider data;

  @override
  State<_DepositSheet> createState() => _DepositSheetState();
}

class _DepositSheetState extends State<_DepositSheet> {
  final _vaults = VaultService();
  final _amount = TextEditingController();
  bool _saving = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _deposit() async {
    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }
    final confirmed = await confirmOperation(
      context,
      title: 'Confirmer le versement',
      amount: '${money(value)} FCFA',
      details: [('Depuis', 'Compte courant'), ('Vers', widget.vault.name)],
      confirmLabel: 'Verser',
    );
    if (!confirmed || !mounted) return;
    setState(() => _saving = true);
    final result = await _vaults.deposit(widget.vault.id, value);
    if (!mounted) return;
    setState(() => _saving = false);

    if (!result.success) {
      _alert(
        'Versement impossible',
        result.error?.message ?? 'Erreur serveur.',
      );
      return;
    }
    await widget.data.refresh();
    if (!mounted) return;
    Navigator.of(context).pop();
  }

  void _alert(String title, String message) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title, style: AppTypography.cardTitle),
        content: Text(message, style: AppTypography.small),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        padding: const EdgeInsets.fromLTRB(22, 22, 22, 35),
        decoration: const BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppRadius.sheet),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SheetHeader(
              title: 'Alimenter ${widget.vault.name}',
              subtitle:
                  'Les fonds seront transférés depuis votre portefeuille.',
            ),
            const FieldLabel('Montant en FCFA'),
            Container(
              height: 52,
              padding: const EdgeInsets.symmetric(horizontal: 13),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _amount,
                      autofocus: true,
                      keyboardType: TextInputType.number,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      style: AppTypography.amount.copyWith(fontSize: 22),
                      decoration: const InputDecoration(
                        hintText: '0',
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        filled: false,
                        isDense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ),
                  Text(
                    'FCFA',
                    style: AppTypography.badge.copyWith(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: AppColors.yellowDark,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            PrimaryButton(
              label: 'Confirmer le versement',
              icon: KolaIcons.checkmarkCircleOutline,
              loading: _saving,
              onPressed: _deposit,
            ),
          ],
        ),
      ),
    );
  }
}
