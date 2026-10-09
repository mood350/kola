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
import '../models/scheduled_transfer.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/scheduled_transfer_service.dart';

/// Facturiers de service public, connus de l'app : contrairement aux
/// abonnements, ils ne sont pas servis par le backend.
const _utilities = <Biller>[
  Biller(
    code: 'ceet',
    displayName: 'CEET — électricité',
    identifierLabel: 'Référence client',
    identifierKind: 'ALPHANUMERIC',
    minLength: 4,
    maxLength: 24,
    fixedAmount: false,
  ),
  Biller(
    code: 'cash_power',
    displayName: 'Cash Power',
    identifierLabel: 'Numéro du compteur',
    identifierKind: 'DIGITS',
    minLength: 6,
    maxLength: 20,
    fixedAmount: false,
  ),
  Biller(
    code: 'tde',
    displayName: 'TdE — eau',
    identifierLabel: 'Référence client',
    identifierKind: 'ALPHANUMERIC',
    minLength: 4,
    maxLength: 24,
    fixedAmount: false,
  ),
];

/// Icône déduite du code du facturier, pour que les offres servies par le
/// backend s'affichent correctement sans table de correspondance à maintenir.
IconData billerIcon(String code) {
  if (code.contains('canal') || code.contains('tv')) return KolaIcons.tvOutline;
  if (code.contains('fibre')) return KolaIcons.wifiOutline;
  if (code == 'tde') return KolaIcons.waterOutline;
  return KolaIcons.flashOutline;
}

/// Paiement de factures (ponctuel) ou d'abonnements (mensuel), toujours
/// prélevé sur un coffre choisi par le client.
class ServicePaymentsScreen extends StatefulWidget {
  const ServicePaymentsScreen({
    super.key,
    required this.onNavigate,
    required this.subscriptions,
    this.onOpenSubscription,
  });

  final void Function(KolaRoute) onNavigate;
  final bool subscriptions;
  final void Function(String)? onOpenSubscription;

  @override
  State<ServicePaymentsScreen> createState() => _ServicePaymentsScreenState();
}

class _ServicePaymentsScreenState extends State<ServicePaymentsScreen> {
  final _scheduling = SchedulingService();
  final _reference = TextEditingController();
  final _amount = TextEditingController();

  List<Biller> _remoteBillers = const [];
  String _selectedCode = '';
  String _fundingVaultId = '';
  late DateTime _date;
  late bool _automatic;
  bool _saving = false;

  bool get _isSubscription => widget.subscriptions;

  @override
  void initState() {
    super.initState();
    _automatic = _isSubscription;
    final now = DateTime.now();
    _date = _isSubscription
        ? DateTime(now.year, now.month + 1, now.day)
        : now.add(const Duration(days: 1));

    if (_isSubscription) {
      _loadBillers();
    } else {
      _selectedCode = _utilities.first.code;
    }
  }

  @override
  void dispose() {
    _reference.dispose();
    _amount.dispose();
    super.dispose();
  }

  Future<void> _loadBillers() async {
    final result = await _scheduling.billers();
    if (!mounted) return;
    if (!result.success) {
      _alert(
        'Services indisponibles',
        result.error?.message ?? 'Impossible de charger les abonnements.',
      );
      return;
    }
    setState(() {
      _remoteBillers = result.data ?? const [];
      if (_selectedCode.isEmpty && _remoteBillers.isNotEmpty) {
        _selectedCode = _remoteBillers.first.code;
      }
    });
  }

  List<Biller> get _services => _isSubscription ? _remoteBillers : _utilities;

  Biller? get _selected {
    for (final service in _services) {
      if (service.code == _selectedCode) return service;
    }
    return _services.isEmpty ? null : _services.first;
  }

  Future<void> _submit() async {
    final selected = _selected;
    if (selected == null) return;

    final clean = selected.digitsOnly
        ? _reference.text.replaceAll(RegExp(r'\D'), '')
        : _reference.text.trim();
    if (clean.length < selected.minLength ||
        clean.length > selected.maxLength) {
      _alert(
        'Référence invalide',
        '${selected.identifierLabel} : entre ${selected.minLength} et ${selected.maxLength} caractères.',
      );
      return;
    }

    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }
    if (_fundingVaultId.isEmpty) {
      _alert(
        'Coffre requis',
        'Choisissez le coffre qui paiera ${_isSubscription ? 'cet abonnement' : 'cette facture'}.',
      );
      return;
    }

    final monthly = _isSubscription && _automatic;
    final vault = context
        .read<KolaDataProvider>()
        .vaults
        .where((item) => item.id == _fundingVaultId)
        .firstOrNull;
    final confirmed = await confirmOperation(
      context,
      title: _isSubscription
          ? 'Confirmer l’abonnement'
          : 'Confirmer la facture',
      amount: '${money(value)} FCFA',
      details: [
        ('Service', selected.displayName),
        (selected.identifierLabel, clean),
        ('Payé depuis', vault?.name ?? 'Coffre'),
        (monthly ? 'Premier paiement' : 'Date de paiement', shortDate(_date)),
        if (monthly) ('Fréquence', 'Chaque mois'),
      ],
      note:
          'Vérifiez la référence : un paiement envoyé sur une mauvaise référence crédite un autre client.',
      confirmLabel: 'Enregistrer',
    );
    if (!confirmed || !mounted) return;
    setState(() => _saving = true);
    final result = await _scheduling.create(
      type: 'BILL_PAYMENT',
      frequency: monthly ? 'MONTHLY' : 'ONCE',
      amount: value,
      beneficiaryReference: clean,
      firstRunAt: _date.toUtc().toIso8601String(),
      fundingVaultId: _fundingVaultId,
      biller: selected.code,
      dayOfMonth: monthly ? _date.day : null,
    );
    if (!mounted) return;
    setState(() => _saving = false);

    if (!result.success) {
      _alert(
        'Enregistrement impossible',
        result.error?.message ?? 'Erreur serveur.',
      );
      return;
    }

    await context.read<KolaDataProvider>().refresh();
    if (!mounted) return;
    _reference.clear();
    _amount.clear();
    _alert(
      _isSubscription ? 'Abonnement enregistré' : 'Facture enregistrée',
      monthly
          ? '${selected.displayName} sera payé automatiquement chaque mois depuis le coffre choisi.'
          : '${selected.displayName} sera payé depuis le coffre choisi à la date indiquée.',
    );
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
    final vaults = data.vaults;
    if (_fundingVaultId.isEmpty && vaults.isNotEmpty) {
      _fundingVaultId = vaults.first.id;
    }

    final saved = data.scheduled.where((task) {
      if (task.type != 'BILL_PAYMENT') return false;
      if (!const {'ACTIVE', 'PAUSED', 'RETRY_PENDING'}.contains(task.status)) {
        return false;
      }
      return _isSubscription
          ? task.frequency == 'MONTHLY'
          : task.frequency == 'ONCE';
    }).toList();

    final selected = _selected;

    return KolaScreen(
      route: _isSubscription ? KolaRoute.subscriptions : KolaRoute.bills,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Accueil',
          onTap: () => widget.onNavigate(KolaRoute.home),
        ),
        PageHeader(
          overline: _isSubscription
              ? 'SERVICES RÉCURRENTS'
              : 'SERVICES ESSENTIELS',
          title: _isSubscription
              ? 'Mes abonnements'
              : 'Enregistrer une facture',
          subtitle: _isSubscription
              ? 'TV, fibre et assurance au Togo.'
              : 'Choisissez le coffre qui paiera la facture.',
          icon: _isSubscription
              ? KolaIcons.tvOutline
              : KolaIcons.receiptOutline,
        ),
        const _Label('CHOISIR UN SERVICE'),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final service in _services)
              SizedBox(
                width: (MediaQuery.of(context).size.width - 48) / 2,
                child: _ServiceTile(
                  biller: service,
                  active: selected?.code == service.code,
                  onTap: () => setState(() {
                    _selectedCode = service.code;
                    _reference.clear();
                    _amount.clear();
                  }),
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
                    decoration: InputDecoration(
                      hintText: selected.identifierLabel,
                      counterText: '',
                    ),
                  ),
                ),
                const _Label('MONTANT'),
                _AmountField(controller: _amount),
                if (_isSubscription) ...[
                  const SizedBox(height: 16),
                  GestureDetector(
                    onTap: () => setState(() => _automatic = !_automatic),
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.pale,
                        borderRadius: BorderRadius.circular(11),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            _automatic
                                ? KolaIcons.checkbox
                                : KolaIcons.squareOutline,
                            size: 22,
                            color: _automatic
                                ? AppColors.greenDark
                                : AppColors.muted,
                          ),
                          const SizedBox(width: 9),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Paiement mensuel automatique',
                                  style: AppTypography.smallBold.copyWith(
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.ink,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  _automatic
                                      ? 'Répété chaque mois'
                                      : 'Un seul paiement programmé',
                                  style: AppTypography.caption.copyWith(
                                    fontSize: 8,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
                _Label(
                  _isSubscription && _automatic
                      ? 'PREMIÈRE DATE DE PAIEMENT'
                      : 'DATE DE PAIEMENT',
                ),
                _DateField(date: _date, onPick: _pickDate),
                const _Label('COFFRE QUI EFFECTUERA LE PAIEMENT'),
                if (vaults.isEmpty)
                  Container(
                    padding: const EdgeInsets.all(11),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF4C6),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          KolaIcons.alertCircleOutline,
                          size: 18,
                          color: AppColors.yellowDark,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Créez d’abord un coffre KOLA.',
                            style: AppTypography.caption.copyWith(
                              fontSize: 9,
                              color: AppColors.yellowDark,
                            ),
                          ),
                        ),
                      ],
                    ),
                  )
                else
                  for (final vault in vaults)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: VaultChoice(
                        name: vault.name,
                        balance: vault.balance,
                        selected: _fundingVaultId == vault.id,
                        onTap: () => setState(() => _fundingVaultId = vault.id),
                      ),
                    ),
                if (!_isSubscription) ...[
                  const SizedBox(height: 15),
                  Container(
                    padding: const EdgeInsets.all(11),
                    decoration: BoxDecoration(
                      color: AppColors.pale,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(
                          KolaIcons.informationCircleOutline,
                          size: 18,
                          color: AppColors.primary,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'La facture sera payée une seule fois, à la date choisie, avec le solde de ce coffre.',
                            style: AppTypography.caption.copyWith(fontSize: 9),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                PrimaryButton(
                  label: _isSubscription
                      ? 'Enregistrer l’abonnement'
                      : 'Enregistrer le paiement',
                  icon: KolaIcons.calendarOutline,
                  loading: _saving,
                  onPressed: vaults.isEmpty ? null : _submit,
                ),
              ],
            ),
          ),
        ],
        _Label(
          '${_isSubscription ? 'ABONNEMENTS ENREGISTRÉS' : 'FACTURES ENREGISTRÉES'} (${saved.length})',
        ),
        if (saved.isEmpty)
          KolaCard(
            padding: const EdgeInsets.symmetric(vertical: 23),
            child: EmptyState(
              icon: KolaIcons.calendarOutline,
              title: _isSubscription
                  ? 'Aucun abonnement enregistré.'
                  : 'Aucune facture enregistrée.',
            ),
          )
        else
          for (final item in saved)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.xs),
              child: _SavedRow(
                task: item,
                services: _services,
                subscription: _isSubscription,
                onTap: _isSubscription
                    ? () => widget.onOpenSubscription?.call(item.id)
                    : null,
              ),
            ),
      ],
    );
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: now,
      lastDate: DateTime(now.year + 5),
      helpText: 'Date de paiement',
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

class _ServiceTile extends StatelessWidget {
  const _ServiceTile({
    required this.biller,
    required this.active,
    required this.onTap,
  });

  final Biller biller;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minHeight: 61),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: active ? AppColors.primary : AppColors.white,
          borderRadius: BorderRadius.circular(13),
          border: Border.all(
            color: active ? AppColors.primary : AppColors.border,
          ),
        ),
        child: Row(
          children: [
            Icon(
              billerIcon(biller.code),
              size: 21,
              color: active ? AppColors.yellow : AppColors.primary,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                biller.displayName,
                style: AppTypography.caption.copyWith(
                  fontWeight: FontWeight.w700,
                  color: active ? AppColors.white : AppColors.ink,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AmountField extends StatelessWidget {
  const _AmountField({required this.controller});

  final TextEditingController controller;

  @override
  Widget build(BuildContext context) {
    return Container(
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
              controller: controller,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              style: AppTypography.amount.copyWith(fontSize: 21),
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
              fontSize: 10,
              fontWeight: FontWeight.w900,
              color: AppColors.yellowDark,
            ),
          ),
        ],
      ),
    );
  }
}

class _DateField extends StatelessWidget {
  const _DateField({required this.date, required this.onPick});

  final DateTime date;
  final VoidCallback onPick;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPick,
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
            Text(shortDate(date), style: AppTypography.body),
          ],
        ),
      ),
    );
  }
}

/// Sélecteur radio d'un coffre de financement, partagé avec l'écran de détail
/// d'un abonnement.
class VaultChoice extends StatelessWidget {
  const VaultChoice({
    super.key,
    required this.name,
    required this.balance,
    required this.selected,
    required this.onTap,
  });

  final String name;
  final double balance;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 44,
        padding: const EdgeInsets.symmetric(horizontal: 10),
        decoration: BoxDecoration(
          color: AppColors.pale,
          borderRadius: BorderRadius.circular(10),
          border: selected
              ? Border.all(color: AppColors.primary)
              : Border.all(color: Colors.transparent),
        ),
        child: Row(
          children: [
            Icon(
              selected ? KolaIcons.radioButtonOn : KolaIcons.radioButtonOff,
              size: 19,
              color: AppColors.primary,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.caption.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppColors.ink,
                ),
              ),
            ),
            Text(
              '${money(balance)} F',
              style: AppTypography.caption.copyWith(fontSize: 9),
            ),
          ],
        ),
      ),
    );
  }
}

class _SavedRow extends StatelessWidget {
  const _SavedRow({
    required this.task,
    required this.services,
    required this.subscription,
    required this.onTap,
  });

  final ScheduledTask task;
  final List<Biller> services;
  final bool subscription;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    String label = task.biller ?? (subscription ? 'Abonnement' : 'Facture');
    for (final service in services) {
      if (service.code == task.biller) label = service.displayName;
    }

    return KolaCard(
      onTap: onTap,
      child: Row(
        children: [
          IconCircle(
            subscription ? KolaIcons.repeatOutline : KolaIcons.receiptOutline,
            background: AppColors.greenPale,
            color: AppColors.greenDark,
            size: 20,
            diameter: 39,
          ),
          const SizedBox(width: 9),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: AppTypography.smallBold.copyWith(
                    fontWeight: FontWeight.w800,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '${task.beneficiaryReference} · '
                  '${task.frequency == 'MONTHLY'
                      ? 'chaque mois'
                      : task.nextRunAt == null
                      ? '—'
                      : shortDate(task.nextRunAt!)}',
                  style: AppTypography.caption.copyWith(fontSize: 8),
                ),
              ],
            ),
          ),
          Text(
            '${money(task.amount)} F',
            style: AppTypography.badge.copyWith(
              fontSize: 11,
              fontWeight: FontWeight.w900,
            ),
          ),
          if (subscription)
            const Icon(
              KolaIcons.chevronForward,
              size: 18,
              color: AppColors.muted,
            ),
        ],
      ),
    );
  }
}
