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

/// Liste des coffres-forts d'épargne, avec création d'un nouvel objectif.
class VaultsScreen extends StatelessWidget {
  const VaultsScreen({
    super.key,
    required this.onNavigate,
    required this.onOpenVault,
  });

  final void Function(KolaRoute) onNavigate;
  final void Function(String) onOpenVault;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final vaults = data.vaults;
    final total = vaults.fold<double>(0, (sum, v) => sum + v.balance);

    return KolaScreen(
      route: KolaRoute.vaults,
      onNavigate: onNavigate,
      children: [
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(15),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    KolaIcons.lockClosedOutline,
                    size: 18,
                    color: AppColors.yellow,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'ÉPARGNE TOTALE\nSÉCURISÉE',
                      style: AppTypography.small.copyWith(
                        fontWeight: FontWeight.w800,
                        height: 1.45,
                        color: const Color(0xFFADC6FF),
                      ),
                    ),
                  ),
                  Pill(
                    '${vaults.length} coffre${vaults.length > 1 ? 's' : ''}',
                    green: true,
                  ),
                ],
              ),
              const SizedBox(height: 14),
              RichText(
                text: TextSpan(
                  style: AppTypography.balance.copyWith(fontSize: 28),
                  children: [
                    TextSpan(text: money(total)),
                    TextSpan(
                      text: ' FCFA',
                      style: AppTypography.body.copyWith(
                        fontSize: 16,
                        color: AppColors.white,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Fonds protégés pour vos projets prioritaires et votre solvabilité.',
                style: AppTypography.small.copyWith(
                  height: 1.55,
                  color: const Color(0xFFE3E7F5),
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
          child: Container(
            padding: const EdgeInsets.all(19),
            decoration: BoxDecoration(
              color: AppColors.pale,
              borderRadius: BorderRadius.circular(15),
            ),
            child: Row(
              children: [
                const IconCircle(
                  KolaIcons.sparklesOutline,
                  background: Color(0xFFFFF4C6),
                  color: AppColors.yellowDark,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Discipline d’épargne',
                        style: AppTypography.cardTitle.copyWith(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Chaque versement régulier renforce votre profil financier KOLA.',
                        style: AppTypography.small.copyWith(height: 1.55),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        PrimaryButton(
          label: 'Créer un nouveau coffre',
          icon: KolaIcons.addCircleOutline,
          onPressed: () => _openCreateSheet(context),
        ),
        SectionTitle('Vos Coffres (${vaults.length})', action: 'TOGO'),
        if (!data.loading && vaults.isEmpty)
          const KolaCard(
            padding: EdgeInsets.symmetric(vertical: 25),
            child: EmptyState(
              icon: KolaIcons.lockOpenOutline,
              title: 'Aucun coffre',
              message: 'Créez votre premier objectif d’épargne.',
            ),
          ),
        for (final vault in vaults)
          Padding(
            padding: const EdgeInsets.only(bottom: 14),
            child: _VaultCard(vault: vault, onTap: () => onOpenVault(vault.id)),
          ),
        Container(
          margin: const EdgeInsets.only(top: 10),
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.pale,
            borderRadius: BorderRadius.circular(15),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(
                    KolaIcons.settingsOutline,
                    size: 19,
                    color: AppColors.primary,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Règles et impact',
                    style: AppTypography.cardTitle.copyWith(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              const _Rule(
                KolaIcons.lockClosedOutline,
                'Verrouillage strict',
                'Les fonds restent protégés dans le coffre.',
              ),
              const SizedBox(height: 12),
              const _Rule(
                KolaIcons.trophyOutline,
                'Discipline récompensée',
                'L’épargne régulière améliore votre profil financier.',
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _openCreateSheet(BuildContext context) {
    final data = context.read<KolaDataProvider>();
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _CreateVaultSheet(data: data),
    );
  }
}

class _VaultCard extends StatelessWidget {
  const _VaultCard({required this.vault, required this.onTap});

  final Vault vault;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return KolaCard(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const IconCircle(KolaIcons.lockClosedOutline),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      vault.name,
                      style: AppTypography.cardTitle.copyWith(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      vault.description ?? 'Coffre KOLA',
                      style: AppTypography.caption.copyWith(
                        fontWeight: FontWeight.w600,
                        color: AppColors.ink,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: AppSpacing.xs),
              Pill(vault.status),
              const Icon(
                KolaIcons.chevronForward,
                size: 18,
                color: AppColors.muted,
              ),
            ],
          ),
          const SizedBox(height: 17),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              RichText(
                text: TextSpan(
                  style: AppTypography.small,
                  children: [
                    const TextSpan(text: 'Cumul : '),
                    TextSpan(
                      text: '${money(vault.balance)} FCFA',
                      style: AppTypography.smallBold.copyWith(
                        fontWeight: FontWeight.w800,
                        color: AppColors.ink,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                '${vault.progressPercent.round()}%',
                style: AppTypography.smallBold.copyWith(fontSize: 12),
              ),
            ],
          ),
          const SizedBox(height: 9),
          KolaProgress(vault.progressPercent),
          const SizedBox(height: 7),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '0 FCFA',
                style: AppTypography.caption.copyWith(fontSize: 9),
              ),
              Text(
                'Objectif ${money(vault.targetAmount)} FCFA',
                style: AppTypography.caption.copyWith(fontSize: 9),
              ),
            ],
          ),
          if (vault.targetDate != null) ...[
            const SizedBox(height: 13),
            Container(
              height: 39,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: AppColors.pale,
                borderRadius: BorderRadius.circular(9),
              ),
              child: Row(
                children: [
                  const Icon(
                    KolaIcons.calendarOutline,
                    size: 16,
                    color: AppColors.green,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Objectif au ${shortDate(vault.targetDate!)}',
                    style: AppTypography.caption,
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _Rule extends StatelessWidget {
  const _Rule(this.icon, this.title, this.text);

  final IconData icon;
  final String title;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: AppColors.greenDark),
        const SizedBox(width: 9),
        Expanded(
          child: RichText(
            text: TextSpan(
              style: AppTypography.small.copyWith(height: 1.55),
              children: [
                TextSpan(
                  text: '$title : ',
                  style: AppTypography.smallBold.copyWith(
                    fontWeight: FontWeight.w800,
                    color: AppColors.ink,
                  ),
                ),
                TextSpan(text: text),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _CreateVaultSheet extends StatefulWidget {
  const _CreateVaultSheet({required this.data});

  final KolaDataProvider data;

  @override
  State<_CreateVaultSheet> createState() => _CreateVaultSheetState();
}

class _CreateVaultSheetState extends State<_CreateVaultSheet> {
  final _vaults = VaultService();
  final _name = TextEditingController();
  final _description = TextEditingController();
  final _target = TextEditingController();
  DateTime? _date;
  bool _saving = false;

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    _target.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    if (_name.text.trim().length < 2) {
      _alert('Nom requis', 'Donnez un nom à votre coffre.');
      return;
    }
    final target = double.tryParse(_target.text.replaceAll(RegExp(r'\D'), ''));
    if (_target.text.isNotEmpty && (target == null || target <= 0)) {
      _alert('Objectif invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }

    final date = _date;
    final confirmed = await confirmOperation(
      context,
      title: 'Confirmer la création',
      amount: _name.text.trim(),
      details: [
        ('Objectif', target == null ? 'Non défini' : '${money(target)} FCFA'),
        ('Date cible', date == null ? 'Non définie' : shortDate(date)),
      ],
      note:
          'Le coffre est créé vide : vous l’alimenterez ensuite depuis votre compte courant.',
      confirmLabel: 'Créer le coffre',
    );
    if (!confirmed || !mounted) return;

    setState(() => _saving = true);
    final result = await _vaults.create(
      name: _name.text.trim(),
      description: _description.text.trim(),
      targetAmount: target,
      targetDate: _date == null
          ? null
          : '${_date!.year.toString().padLeft(4, '0')}-'
                '${_date!.month.toString().padLeft(2, '0')}-'
                '${_date!.day.toString().padLeft(2, '0')}',
    );
    if (!mounted) return;
    setState(() => _saving = false);

    if (!result.success) {
      _alert('Création impossible', result.error?.message ?? 'Erreur serveur.');
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
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.88,
        ),
        padding: const EdgeInsets.fromLTRB(22, 22, 22, 35),
        decoration: const BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppRadius.sheet),
          ),
        ),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SheetHeader(
                title: 'Nouveau coffre',
                subtitle: 'Définissez votre objectif d’épargne.',
              ),
              const FieldLabel('Nom du coffre'),
              _SheetField(
                controller: _name,
                hint: 'Ex. Études, urgence, projet',
              ),
              const FieldLabel('Description (facultative)'),
              _SheetField(
                controller: _description,
                hint: 'À quoi servira cette épargne ?',
              ),
              const FieldLabel('Objectif en FCFA (facultatif)'),
              _SheetField(
                controller: _target,
                hint: '500000',
                digitsOnly: true,
              ),
              const FieldLabel('Date cible (facultative)'),
              GestureDetector(
                onTap: _pickDate,
                child: Container(
                  height: 48,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  alignment: Alignment.centerLeft,
                  decoration: BoxDecoration(
                    color: AppColors.white,
                    borderRadius: BorderRadius.circular(11),
                    border: Border.all(color: AppColors.border),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        KolaIcons.calendarOutline,
                        size: 18,
                        color: AppColors.muted,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _date == null ? 'Choisir une date' : shortDate(_date!),
                        style: AppTypography.body.copyWith(
                          color: _date == null
                              ? AppColors.muted
                              : AppColors.ink,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),
              PrimaryButton(
                label: 'Créer le coffre',
                icon: KolaIcons.checkmarkCircleOutline,
                loading: _saving,
                onPressed: _create,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date ?? now.add(const Duration(days: 90)),
      firstDate: now,
      lastDate: DateTime(now.year + 20),
      helpText: 'Date cible',
      locale: const Locale('fr', 'FR'),
    );
    if (picked != null) setState(() => _date = picked);
  }
}

class _SheetField extends StatelessWidget {
  const _SheetField({
    required this.controller,
    required this.hint,
    this.digitsOnly = false,
  });

  final TextEditingController controller;
  final String hint;
  final bool digitsOnly;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      child: TextField(
        controller: controller,
        keyboardType: digitsOnly ? TextInputType.number : TextInputType.text,
        inputFormatters: digitsOnly
            ? [FilteringTextInputFormatter.digitsOnly]
            : null,
        style: AppTypography.body,
        decoration: InputDecoration(hintText: hint),
      ),
    );
  }
}
