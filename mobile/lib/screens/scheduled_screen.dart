import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/scheduled_transfer.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/scheduled_transfer_service.dart';
import '../services/user_service.dart';
import 'service_payments_screen.dart';

enum _Destination { person, vault, bankivi }

const _frequencies = {
  'ONCE': 'Une fois',
  'DAILY': 'Jour',
  'WEEKLY': 'Semaine',
  'MONTHLY': 'Mois',
};

String _frequencySentence(String frequency) => switch (frequency) {
  'DAILY' => 'chaque jour',
  'WEEKLY' => 'chaque semaine',
  'MONTHLY' => 'chaque mois',
  _ => 'une seule fois',
};

/// Opérations programmées : virements vers une personne, versements vers un
/// coffre, ou cotisations Bankivi.
class ScheduledScreen extends StatefulWidget {
  const ScheduledScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<ScheduledScreen> createState() => _ScheduledScreenState();
}

class _ScheduledScreenState extends State<ScheduledScreen> {
  int _tab = 0;

  /// Un ordre de paiement de facture appartient aux écrans Factures et
  /// Abonnements : l'afficher ici aussi le ferait apparaître deux fois.
  bool _isTransfer(ScheduledTask task) => task.type != 'BILL_PAYMENT';

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final tasks = data.scheduled.where(_isTransfer).toList();

    final active = tasks
        .where((t) => t.status == 'ACTIVE' || t.status == 'RETRY_PENDING')
        .toList();
    final paused = tasks.where((t) => t.isPaused).toList();
    final history = tasks
        .where(
          (t) =>
              t.status != 'ACTIVE' &&
              t.status != 'RETRY_PENDING' &&
              !t.isPaused,
        )
        .toList();

    final visible = switch (_tab) {
      0 => active,
      1 => history,
      _ => paused,
    };

    final upcoming = [...active]
      ..sort((a, b) {
        final left = a.nextRunAt;
        final right = b.nextRunAt;
        if (left == null) return 1;
        if (right == null) return -1;
        return left.compareTo(right);
      });
    final next = upcoming.isEmpty ? null : upcoming.first.nextRunAt;
    final reminder = next == null
        ? '—'
        : 'J-${next.difference(DateTime.now()).inDays.clamp(0, 9999)}';

    return KolaScreen(
      route: KolaRoute.scheduled,
      onNavigate: widget.onNavigate,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Envois programmés',
              style: AppTypography.screenTitle.copyWith(
                color: AppColors.primary,
              ),
            ),
            const Pill('● Automatisés', green: true),
          ],
        ),
        Align(
          alignment: Alignment.centerLeft,
          child: Container(
            height: 28,
            margin: const EdgeInsets.only(top: 8),
            padding: const EdgeInsets.symmetric(horizontal: 10),
            decoration: BoxDecoration(
              color: AppColors.pale2,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(
                  KolaIcons.shieldCheckmarkOutline,
                  size: 15,
                  color: AppColors.green,
                ),
                const SizedBox(width: 6),
                Text(
                  'Ordres enregistrés et contrôlables à tout moment',
                  style: AppTypography.badge.copyWith(
                    fontSize: 9,
                    fontWeight: FontWeight.w400,
                    color: AppColors.ink,
                  ),
                ),
              ],
            ),
          ),
        ),
        Container(
          margin: const EdgeInsets.symmetric(vertical: 18),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Row(
            children: [
              _Stat('${active.length}', 'ACTIFS', AppColors.yellow),
              _Stat('${paused.length}', 'EN PAUSE', AppColors.white),
              _Stat(reminder, 'PROCHAIN', AppColors.mint),
            ],
          ),
        ),
        PrimaryButton(
          label: 'Programmer une opération',
          onPressed: _openForm,
        ),
        Container(
          height: 39,
          margin: const EdgeInsets.symmetric(vertical: 18),
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: AppColors.pale2,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            children: [
              for (final entry
                  in {
                    'À venir (${active.length})': 0,
                    'Historique': 1,
                    'En pause (${paused.length})': 2,
                  }.entries)
                Expanded(
                  child: GestureDetector(
                    onTap: () => setState(() => _tab = entry.value),
                    behavior: HitTestBehavior.opaque,
                    child: Container(
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: _tab == entry.value
                            ? AppColors.white
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: Text(
                        entry.key,
                        style: AppTypography.badge.copyWith(
                          fontSize: 9,
                          fontWeight: FontWeight.w400,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
        if (visible.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 40),
            child: EmptyState(
              icon: _tab == 1
                  ? KolaIcons.receiptOutline
                  : KolaIcons.calendarOutline,
              title: switch (_tab) {
                1 => 'Aucun envoi exécuté',
                2 => 'Aucun envoi en pause',
                _ => 'Aucun envoi programmé',
              },
            ),
          )
        else
          for (final task in visible)
            Padding(
              padding: const EdgeInsets.only(bottom: 11),
              child: _ScheduleCard(
                task: task,
                vaults: {for (final v in data.vaults) v.id: v.name},
                onToggle: () => _toggle(task),
              ),
            ),
        Container(
          margin: const EdgeInsets.only(top: 10),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.pale2,
            borderRadius: BorderRadius.circular(13),
          ),
          child: Row(
            children: [
              const IconCircle(
                KolaIcons.notificationsOutline,
                background: AppColors.yellow,
                color: AppColors.primary,
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Rappel avant l’envoi',
                      style: AppTypography.cardTitle.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Vous pourrez suspendre l’ordre avant son exécution. Le transfert final sera sécurisé par KOLA.',
                      style: AppTypography.caption.copyWith(height: 1.6),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _toggle(ScheduledTask task) async {
    final scheduling = SchedulingService();
    final data = context.read<KolaDataProvider>();
    final result = task.isPaused
        ? await scheduling.resume(task.id)
        : await scheduling.pause(task.id);
    if (!mounted) return;
    if (!result.success) {
      showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text('Action impossible', style: AppTypography.cardTitle),
          content: Text(
            result.error?.message ?? 'Erreur serveur.',
            style: AppTypography.small,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('OK'),
            ),
          ],
        ),
      );
      return;
    }
    await data.refresh();
  }

  void _openForm() {
    final data = context.read<KolaDataProvider>();
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _ScheduleForm(
        data: data,
        onNeedVault: () => widget.onNavigate(KolaRoute.vaults),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat(this.value, this.label, this.color);

  final String value;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Text(
            value,
            style: AppTypography.screenTitle.copyWith(
              fontSize: 24,
              color: color,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: AppTypography.badge.copyWith(
              fontSize: 8,
              fontWeight: FontWeight.w800,
              color: AppColors.white,
            ),
          ),
        ],
      ),
    );
  }
}

class _ScheduleCard extends StatelessWidget {
  const _ScheduleCard({
    required this.task,
    required this.vaults,
    required this.onToggle,
  });

  final ScheduledTask task;
  final Map<String, String> vaults;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final destination = switch (task.type) {
      'SAVINGS_DEPOSIT' => _Destination.bankivi,
      'VAULT_DEPOSIT' => _Destination.vault,
      _ => _Destination.person,
    };

    final title = switch (destination) {
      _Destination.bankivi => 'Cotisation Bankivi',
      _Destination.vault =>
        vaults[task.beneficiaryReference] ?? 'Coffre KOLA',
      _Destination.person => task.beneficiaryReference,
    };

    final subtitle = switch (destination) {
      _Destination.bankivi => task.frequencyLabel,
      _Destination.vault => 'Épargne sécurisée',
      _Destination.person =>
        vaults[task.fundingVaultId] ?? 'Coffre de financement',
    };

    final label = switch (destination) {
      _Destination.bankivi => 'Bankivi',
      _Destination.vault => 'Coffre',
      _Destination.person => 'Personne',
    };

    final icon = switch (destination) {
      _Destination.bankivi => KolaIcons.businessOutline,
      _Destination.vault => KolaIcons.lockClosedOutline,
      _Destination.person => KolaIcons.personOutline,
    };

    final closed = !task.isActive && !task.isPaused;

    return KolaCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              IconCircle(icon),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: AppTypography.cardTitle.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Pill(label),
                    const SizedBox(height: 4),
                    Text(subtitle, style: AppTypography.caption),
                  ],
                ),
              ),
              if (!closed)
                GestureDetector(
                  onTap: onToggle,
                  child: Container(
                    width: 36,
                    height: 36,
                    alignment: Alignment.center,
                    decoration: const BoxDecoration(
                      color: AppColors.pale2,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      task.isPaused ? KolaIcons.play : KolaIcons.pause,
                      size: 18,
                      color: AppColors.primary,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 14),
          RichText(
            text: TextSpan(
              style: AppTypography.amount.copyWith(fontSize: 25),
              children: [
                TextSpan(text: money(task.amount)),
                TextSpan(
                  text: ' FCFA',
                  style: AppTypography.badge.copyWith(
                    fontSize: 10,
                    color: AppColors.yellowDark,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppColors.pale,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                const Icon(
                  KolaIcons.calendarOutline,
                  size: 15,
                  color: AppColors.green,
                ),
                const SizedBox(width: 7),
                Expanded(
                  child: Text(
                    task.nextRunAt == null
                        ? '—'
                        : receiptDate(task.nextRunAt!),
                    style: AppTypography.caption.copyWith(
                      fontSize: 9,
                      color: AppColors.ink,
                    ),
                  ),
                ),
                Text(
                  task.isActive
                      ? 'Actif'
                      : task.isPaused
                      ? 'En pause'
                      : 'Terminé',
                  style: AppTypography.badge.copyWith(
                    fontSize: 9,
                    color: AppColors.greenDark,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ScheduleForm extends StatefulWidget {
  const _ScheduleForm({required this.data, required this.onNeedVault});

  final KolaDataProvider data;
  final VoidCallback onNeedVault;

  @override
  State<_ScheduleForm> createState() => _ScheduleFormState();
}

class _ScheduleFormState extends State<_ScheduleForm> {
  final _scheduling = SchedulingService();
  final _users = UserService();
  final _phone = TextEditingController();
  final _amount = TextEditingController();
  final _description = TextEditingController();

  _Destination _destination = _Destination.person;
  String _frequency = 'ONCE';
  String _vaultId = '';
  late DateTime _when = DateTime.now().add(const Duration(days: 1));

  String _recipient = '';
  String _lookupMessage = '';
  bool _lookingUp = false;
  bool _saving = false;
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    if (widget.data.vaults.isNotEmpty) _vaultId = widget.data.vaults.first.id;
    _phone.addListener(_onPhoneChanged);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _phone.dispose();
    _amount.dispose();
    _description.dispose();
    super.dispose();
  }

  /// Le numéro est résolu après une pause de frappe : sans ce délai, chaque
  /// chiffre déclencherait un appel réseau.
  void _onPhoneChanged() {
    _debounce?.cancel();
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    if (_destination != _Destination.person || digits.length < 8) {
      setState(() {
        _lookupMessage = '';
        _recipient = '';
        _lookingUp = false;
      });
      return;
    }
    setState(() {
      _lookingUp = true;
      _lookupMessage = '';
    });
    _debounce = Timer(const Duration(milliseconds: 450), () => _lookup(digits));
  }

  Future<void> _lookup(String digits) async {
    final local = digits.substring(digits.length - 8);
    final result = await _users.lookupRecipient('+228$local');
    if (!mounted) return;
    setState(() {
      _lookingUp = false;
      if (result.success && result.data != null) {
        _recipient = result.data!.displayName;
        _lookupMessage = 'Compte KOLA vérifié';
      } else {
        _recipient = '';
        _lookupMessage =
            result.error?.message ?? 'Numéro non inscrit sur KOLA';
      }
    });
  }

  Future<void> _pickContact() async {
    final granted = await FlutterContacts.requestPermission(readonly: true);
    if (!granted) {
      _alert(
        'Accès aux contacts refusé',
        'Vous pouvez toujours saisir le numéro manuellement.',
      );
      return;
    }
    final contacts = await FlutterContacts.getContacts(withProperties: true);
    if (!mounted) return;

    final chosen = await showModalBottomSheet<Contact>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _ContactPicker(contacts: contacts),
    );
    if (chosen == null || chosen.phones.isEmpty) return;
    _phone.text = chosen.phones.first.number.replaceAll(RegExp(r'\D'), '');
  }

  Future<void> _submit() async {
    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');

    if (_destination == _Destination.person) {
      if (digits.length < 8) {
        _alert(
          'Bénéficiaire incomplet',
          'Ajoutez un numéro de téléphone togolais valide.',
        );
        return;
      }
      if (_lookingUp) {
        _alert(
          'Vérification en cours',
          'Attendez la confirmation du bénéficiaire.',
        );
        return;
      }
      if (_vaultId.isEmpty) {
        _alert(
          'Coffre requis',
          'Choisissez le coffre qui financera cet envoi programmé.',
        );
        return;
      }
    }
    if (_destination == _Destination.vault && _vaultId.isEmpty) {
      _alert(
        'Aucun coffre',
        'Créez d’abord un coffre avant de programmer une épargne.',
      );
      return;
    }
    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à 0 FCFA.');
      return;
    }
    if (!_when.isAfter(DateTime.now())) {
      _alert('Date passée', 'Choisissez une date et une heure futures.');
      return;
    }

    final local = digits.length >= 8
        ? digits.substring(digits.length - 8)
        : digits;
    final normalisedPhone = '+228$local';

    final type = switch (_destination) {
      _Destination.bankivi => 'SAVINGS_DEPOSIT',
      _Destination.vault => 'VAULT_DEPOSIT',
      _Destination.person => 'P2P_TRANSFER',
    };
    final beneficiary = switch (_destination) {
      _Destination.bankivi => 'BANKIVI',
      _Destination.vault => _vaultId,
      _Destination.person => normalisedPhone,
    };

    setState(() => _saving = true);
    final result = await _scheduling.create(
      type: type,
      frequency: _frequency,
      amount: value,
      beneficiaryReference: beneficiary,
      description: _description.text.trim(),
      firstRunAt: _when.toUtc().toIso8601String(),
      fundingVaultId: _destination == _Destination.person ? _vaultId : null,
      dayOfMonth: _frequency == 'MONTHLY' ? _when.toUtc().day : null,
    );
    if (!mounted) return;
    setState(() => _saving = false);

    if (!result.success) {
      _alert(
        'Programmation impossible',
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
    final vaults = widget.data.vaults;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.92,
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
                title: 'Programmer un envoi',
                subtitle: 'Choisissez le destinataire et le moment.',
              ),
              const FieldLabel('Destination'),
              Container(
                height: 48,
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(
                  color: AppColors.pale2,
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Row(
                  children: [
                    _Choice(
                      icon: KolaIcons.personOutline,
                      label: 'Personne',
                      active: _destination == _Destination.person,
                      onTap: () => setState(
                        () => _destination = _Destination.person,
                      ),
                    ),
                    _Choice(
                      icon: KolaIcons.lockClosedOutline,
                      label: 'Coffre',
                      active: _destination == _Destination.vault,
                      onTap: () =>
                          setState(() => _destination = _Destination.vault),
                    ),
                    _Choice(
                      icon: KolaIcons.businessOutline,
                      label: 'Bankivi',
                      active: _destination == _Destination.bankivi,
                      onTap: () => setState(
                        () => _destination = _Destination.bankivi,
                      ),
                    ),
                  ],
                ),
              ),
              if (_destination == _Destination.person) ...[
                const FieldLabel('Numéro du bénéficiaire'),
                Row(
                  children: [
                    Expanded(
                      child: SizedBox(
                        height: 45,
                        child: TextField(
                          controller: _phone,
                          keyboardType: TextInputType.phone,
                          maxLength: 8,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                          ],
                          style: AppTypography.body,
                          decoration: const InputDecoration(
                            hintText: '90 00 00 00',
                            counterText: '',
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    GestureDetector(
                      onTap: _pickContact,
                      child: Container(
                        width: 45,
                        height: 45,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: AppColors.yellow,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(
                          KolaIcons.peopleOutline,
                          size: 22,
                          color: AppColors.primary,
                        ),
                      ),
                    ),
                  ],
                ),
                if (_lookingUp || _lookupMessage.isNotEmpty)
                  Container(
                    margin: const EdgeInsets.only(top: 8),
                    constraints: const BoxConstraints(minHeight: 48),
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: _recipient.isNotEmpty
                          ? AppColors.greenPale
                          : AppColors.pale,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      children: [
                        if (_lookingUp)
                          const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        else
                          Icon(
                            _recipient.isNotEmpty
                                ? KolaIcons.checkmarkCircle
                                : KolaIcons.informationCircleOutline,
                            size: 19,
                            color: _recipient.isNotEmpty
                                ? AppColors.greenDark
                                : AppColors.muted,
                          ),
                        const SizedBox(width: 9),
                        Expanded(
                          child: Text(
                            _lookingUp
                                ? 'Vérification…'
                                : _recipient.isNotEmpty
                                ? _recipient
                                : _lookupMessage,
                            style: AppTypography.badge.copyWith(
                              fontSize: 12,
                              fontWeight: FontWeight.w800,
                              color: AppColors.ink,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                const FieldLabel('Coffre de financement'),
                Text(
                  'Le montant et les frais seront prélevés dans ce coffre.',
                  style: AppTypography.caption.copyWith(fontSize: 9),
                ),
                const SizedBox(height: 7),
                if (vaults.isEmpty)
                  PrimaryButton(
                    label: 'Créer un coffre de financement',
                    icon: KolaIcons.addCircleOutline,
                    onPressed: () {
                      Navigator.of(context).pop();
                      widget.onNeedVault();
                    },
                  )
                else
                  for (final vault in vaults)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: VaultChoice(
                        name: vault.name,
                        balance: vault.balance,
                        selected: _vaultId == vault.id,
                        onTap: () => setState(() => _vaultId = vault.id),
                      ),
                    ),
              ] else if (_destination == _Destination.vault) ...[
                const FieldLabel('Coffre à alimenter'),
                for (final vault in vaults)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 6),
                    child: VaultChoice(
                      name: vault.name,
                      balance: vault.balance,
                      selected: _vaultId == vault.id,
                      onTap: () => setState(() => _vaultId = vault.id),
                    ),
                  ),
              ] else ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.pale2,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        KolaIcons.businessOutline,
                        size: 23,
                        color: AppColors.primary,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Cotiser vers Bankivi',
                              style: AppTypography.badge.copyWith(
                                fontSize: 12,
                                fontWeight: FontWeight.w800,
                                color: AppColors.primary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Le montant sera transféré automatiquement de votre compte courant vers Bankivi.',
                              style: AppTypography.caption.copyWith(
                                fontSize: 9,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              const FieldLabel('Motif ou description'),
              TextField(
                controller: _description,
                maxLength: 160,
                maxLines: 3,
                style: AppTypography.body,
                decoration: InputDecoration(
                  counterText: '',
                  hintText: _destination == _Destination.person
                      ? 'Ex. Loyer, aide familiale…'
                      : 'Ex. Objectif du versement…',
                ),
              ),
              const FieldLabel('Montant'),
              Container(
                height: 48,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(10),
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
              const FieldLabel('Fréquence'),
              Container(
                height: 42,
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(
                  color: AppColors.pale2,
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Row(
                  children: [
                    for (final entry in _frequencies.entries)
                      Expanded(
                        child: GestureDetector(
                          onTap: () => setState(() => _frequency = entry.key),
                          behavior: HitTestBehavior.opaque,
                          child: Container(
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: _frequency == entry.key
                                  ? AppColors.primary
                                  : Colors.transparent,
                              borderRadius: BorderRadius.circular(9),
                            ),
                            child: Text(
                              entry.value,
                              style: AppTypography.badge.copyWith(
                                fontSize: 10,
                                color: _frequency == entry.key
                                    ? AppColors.white
                                    : AppColors.primary,
                              ),
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              const FieldLabel('Date et heure'),
              Row(
                children: [
                  Expanded(
                    child: _WhenField(
                      label: shortDate(_when),
                      icon: KolaIcons.calendarOutline,
                      onTap: _pickDate,
                    ),
                  ),
                  const SizedBox(width: 8),
                  SizedBox(
                    width: 125,
                    child: _WhenField(
                      label:
                          '${_when.hour.toString().padLeft(2, '0')}:${_when.minute.toString().padLeft(2, '0')}',
                      icon: KolaIcons.timeOutline,
                      onTap: _pickTime,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.pale2,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Row(
                  children: [
                    const Icon(
                      KolaIcons.informationCircleOutline,
                      size: 18,
                      color: AppColors.primary,
                    ),
                    const SizedBox(width: 7),
                    Expanded(
                      child: Text(
                        'L’ordre pourra être mis en pause avant son exécution — ${_frequencySentence(_frequency)}.',
                        style: AppTypography.caption.copyWith(
                          fontSize: 10,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              PrimaryButton(
                label: 'Confirmer la programmation',
                loading: _saving,
                onPressed: _submit,
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
      initialDate: _when,
      firstDate: now,
      lastDate: DateTime(now.year + 5),
      locale: const Locale('fr', 'FR'),
    );
    if (picked == null) return;
    setState(
      () => _when = DateTime(
        picked.year,
        picked.month,
        picked.day,
        _when.hour,
        _when.minute,
      ),
    );
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_when),
    );
    if (picked == null) return;
    setState(
      () => _when = DateTime(
        _when.year,
        _when.month,
        _when.day,
        picked.hour,
        picked.minute,
      ),
    );
  }
}

class _Choice extends StatelessWidget {
  const _Choice({
    required this.icon,
    required this.label,
    required this.active,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: Container(
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: active ? AppColors.primary : Colors.transparent,
            borderRadius: BorderRadius.circular(11),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 20,
                color: active ? AppColors.white : AppColors.primary,
              ),
              const SizedBox(width: 4),
              Flexible(
                child: Text(
                  label,
                  style: AppTypography.badge.copyWith(
                    fontSize: 10,
                    color: active ? AppColors.white : AppColors.primary,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _WhenField extends StatelessWidget {
  const _WhenField({
    required this.label,
    required this.icon,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 45,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: AppColors.border),
        ),
        child: Row(
          children: [
            Icon(icon, size: 17, color: AppColors.muted),
            const SizedBox(width: 8),
            Text(label, style: AppTypography.body),
          ],
        ),
      ),
    );
  }
}

class _ContactPicker extends StatefulWidget {
  const _ContactPicker({required this.contacts});

  final List<Contact> contacts;

  @override
  State<_ContactPicker> createState() => _ContactPickerState();
}

class _ContactPickerState extends State<_ContactPicker> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final results = widget.contacts.where((contact) {
      if (contact.phones.isEmpty) return false;
      if (_query.isEmpty) return true;
      final haystack =
          '${contact.displayName} ${contact.phones.first.number}'.toLowerCase();
      return haystack.contains(_query.toLowerCase());
    }).toList();

    return Container(
      height: MediaQuery.of(context).size.height * 0.78,
      padding: const EdgeInsets.fromLTRB(22, 22, 22, 12),
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppRadius.sheet),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SheetHeader(title: 'Choisir un contact'),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: TextField(
              onChanged: (value) => setState(() => _query = value),
              style: AppTypography.body,
              decoration: const InputDecoration(
                hintText: 'Rechercher un contact',
              ),
            ),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: results.length,
              itemBuilder: (context, i) {
                final contact = results[i];
                return GestureDetector(
                  onTap: () => Navigator.of(context).pop(contact),
                  behavior: HitTestBehavior.opaque,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 9),
                    child: Row(
                      children: [
                        const IconCircle(KolaIcons.personOutline),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                contact.displayName.isEmpty
                                    ? 'Sans nom'
                                    : contact.displayName,
                                style: AppTypography.bodyBold,
                              ),
                              Text(
                                contact.phones.first.number,
                                style: AppTypography.caption,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
