import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/scheduled_transfer.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/scheduled_transfer_service.dart';
import 'service_payments_screen.dart';

/// Modification d'un abonnement existant.
///
/// Le backend ne sait pas changer l'offre d'une planification : on en crée une
/// nouvelle puis on annule l'ancienne. Si l'annulation échoue, la nouvelle est
/// annulée à son tour — sans quoi le client se retrouverait prélevé deux fois.
class SubscriptionDetailScreen extends StatefulWidget {
  const SubscriptionDetailScreen({
    super.key,
    required this.onNavigate,
    required this.subscriptionId,
  });

  final void Function(KolaRoute) onNavigate;
  final String? subscriptionId;

  @override
  State<SubscriptionDetailScreen> createState() =>
      _SubscriptionDetailScreenState();
}

class _SubscriptionDetailScreenState extends State<SubscriptionDetailScreen> {
  final _scheduling = SchedulingService();
  final _reference = TextEditingController();
  final _amount = TextEditingController();

  List<Biller> _offers = const [];
  String _offerCode = '';
  String _vaultId = '';
  DateTime? _date;
  bool _saving = false;
  bool _prefilled = false;

  @override
  void initState() {
    super.initState();
    _loadOffers();
  }

  @override
  void dispose() {
    _reference.dispose();
    _amount.dispose();
    super.dispose();
  }

  Future<void> _loadOffers() async {
    final result = await _scheduling.billers();
    if (!mounted) return;
    if (!result.success) {
      _alert(
        'Offres indisponibles',
        result.error?.message ?? 'Impossible de charger les offres.',
      );
      return;
    }
    setState(() => _offers = result.data ?? const []);
  }

  void _prefill(ScheduledTask task) {
    _prefilled = true;
    _offerCode = task.biller ?? '';
    _reference.text = task.beneficiaryReference;
    _amount.text = task.amount.round().toString();
    _date = task.nextRunAt;
    _vaultId = task.fundingVaultId ?? '';
  }

  Biller? get _selected {
    for (final offer in _offers) {
      if (offer.code == _offerCode) return offer;
    }
    return null;
  }

  Future<void> _save(ScheduledTask task) async {
    final selected = _selected;
    if (selected == null) return;

    // Capturé avant les appels réseau : le widget peut être démonté entre-temps
    // et `context` ne serait alors plus utilisable.
    final data = context.read<KolaDataProvider>();

    final clean = selected.digitsOnly
        ? _reference.text.replaceAll(RegExp(r'\D'), '')
        : _reference.text.trim();
    if (clean.length < selected.minLength ||
        clean.length > selected.maxLength) {
      _alert(
        'Référence invalide',
        '${selected.identifierLabel} doit contenir entre ${selected.minLength} et ${selected.maxLength} caractères.',
      );
      return;
    }

    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }
    if (_vaultId.isEmpty) {
      _alert('Coffre requis', 'Choisissez le coffre qui paiera cet abonnement.');
      return;
    }
    final date = _date;
    if (date == null) {
      _alert('Date requise', 'Choisissez la prochaine date de paiement.');
      return;
    }

    setState(() => _saving = true);

    final replacement = await _scheduling.create(
      type: 'BILL_PAYMENT',
      frequency: 'MONTHLY',
      amount: value,
      beneficiaryReference: clean,
      firstRunAt: date.toUtc().toIso8601String(),
      fundingVaultId: _vaultId,
      biller: selected.code,
      dayOfMonth: date.day,
    );

    if (!replacement.success) {
      if (!mounted) return;
      setState(() => _saving = false);
      _alert(
        'Modification impossible',
        replacement.error?.message ?? 'Erreur serveur.',
      );
      return;
    }

    final cancelled = await _scheduling.cancel(task.id);
    if (!cancelled.success) {
      // L'ancien abonnement vit toujours : retirer le nouveau, sinon les deux
      // prélèveraient le coffre chaque mois.
      final newId = replacement.data?.id;
      if (newId != null) await _scheduling.cancel(newId);
      if (!mounted) return;
      setState(() => _saving = false);
      _alert(
        'Modification impossible',
        cancelled.error?.message ??
            'L’ancien abonnement n’a pas pu être annulé.',
      );
      return;
    }

    await data.refresh();
    if (!mounted) return;
    setState(() => _saving = false);
    widget.onNavigate(KolaRoute.subscriptions);
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
    final data = context.watch<KolaDataProvider>();
    final matches = data.scheduled.where(
      (item) => item.id == widget.subscriptionId,
    );
    final subscription = matches.isEmpty ? null : matches.first;

    if (subscription == null) {
      return KolaScreen(
        route: KolaRoute.subscriptionDetail,
        onNavigate: widget.onNavigate,
        children: [
          BackLink(
            label: 'Abonnements',
            onTap: () => widget.onNavigate(KolaRoute.subscriptions),
          ),
          const KolaCard(
            padding: EdgeInsets.symmetric(vertical: 25),
            child: EmptyState(
              icon: KolaIcons.alertCircleOutline,
              title: 'Cet abonnement est introuvable.',
            ),
          ),
        ],
      );
    }

    if (!_prefilled) _prefill(subscription);
    final selected = _selected;

    return KolaScreen(
      route: KolaRoute.subscriptionDetail,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Abonnements',
          onTap: () => widget.onNavigate(KolaRoute.subscriptions),
        ),
        const PageHeader(
          overline: 'DÉTAILS DE L’ABONNEMENT',
          title: 'Modifier mon offre',
          subtitle:
              'Changez l’offre, le coffre ou les informations de paiement.',
          icon: KolaIcons.tvOutline,
        ),
        const _Label('CHOISIR UNE OFFRE'),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final offer in _offers)
              SizedBox(
                width: (MediaQuery.of(context).size.width - 48) / 2,
                child: GestureDetector(
                  onTap: () => setState(() => _offerCode = offer.code),
                  child: Container(
                    constraints: const BoxConstraints(minHeight: 58),
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: _offerCode == offer.code
                          ? AppColors.primary
                          : AppColors.white,
                      borderRadius: BorderRadius.circular(13),
                      border: Border.all(
                        color: _offerCode == offer.code
                            ? AppColors.primary
                            : AppColors.border,
                      ),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          billerIcon(offer.code),
                          size: 20,
                          color: _offerCode == offer.code
                              ? AppColors.yellow
                              : AppColors.primary,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            offer.displayName,
                            style: AppTypography.caption.copyWith(
                              fontWeight: FontWeight.w700,
                              color: _offerCode == offer.code
                                  ? AppColors.white
                                  : AppColors.ink,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
          ],
        ),
        if (selected != null) ...[
          const SizedBox(height: 15),
          KolaCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  selected.displayName,
                  style: AppTypography.amount.copyWith(fontSize: 17),
                ),
                _Label(selected.identifierLabel.toUpperCase()),
                SizedBox(
                  height: 47,
                  child: TextField(
                    controller: _reference,
                    keyboardType: selected.digitsOnly
                        ? TextInputType.number
                        : TextInputType.text,
                    maxLength: selected.maxLength,
                    inputFormatters: selected.digitsOnly
                        ? [FilteringTextInputFormatter.digitsOnly]
                        : null,
                    style: AppTypography.body,
                    decoration: const InputDecoration(counterText: ''),
                  ),
                ),
                const _Label('MONTANT MENSUEL'),
                Container(
                  height: 50,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(11),
                    border: Border.all(color: AppColors.border),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _amount,
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                          ],
                          style: AppTypography.amount.copyWith(fontSize: 21),
                          decoration: const InputDecoration(
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
                          fontSize: 10,
                          fontWeight: FontWeight.w900,
                          color: AppColors.yellowDark,
                        ),
                      ),
                    ],
                  ),
                ),
                const _Label('PROCHAINE DATE DE PAIEMENT'),
                GestureDetector(
                  onTap: _pickDate,
                  child: Container(
                    height: 47,
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
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
                          _date == null
                              ? 'Choisir une date'
                              : shortDate(_date!),
                          style: AppTypography.body,
                        ),
                      ],
                    ),
                  ),
                ),
                const _Label('COFFRE DE PAIEMENT'),
                for (final vault in data.vaults)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 7),
                    child: VaultChoice(
                      name: vault.name,
                      balance: vault.balance,
                      selected: _vaultId == vault.id,
                      onTap: () => setState(() => _vaultId = vault.id),
                    ),
                  ),
                const SizedBox(height: 16),
                PrimaryButton(
                  label: 'Enregistrer les modifications',
                  icon: KolaIcons.saveOutline,
                  loading: _saving,
                  onPressed: data.vaults.isEmpty
                      ? null
                      : () => _save(subscription),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date != null && _date!.isAfter(now) ? _date! : now,
      firstDate: now,
      lastDate: DateTime(now.year + 5),
      helpText: 'Prochaine date de paiement',
      locale: const Locale('fr', 'FR'),
    );
    if (picked != null) setState(() => _date = picked);
  }
}

class _Label extends StatelessWidget {
  const _Label(this.label);
  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 16, bottom: 6),
      child: Text(label, style: AppTypography.fieldLabel.copyWith(fontSize: 8)),
    );
  }
}
