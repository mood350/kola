import 'dart:async';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/kyc.dart';
import '../routes/kola_route.dart';
import '../services/user_service.dart';

class _Requirement {
  const _Requirement(this.type, this.title, this.subtitle, this.icon);
  final String type;
  final String title;
  final String subtitle;
  final IconData icon;
}

const _requirements = <_Requirement>[
  _Requirement(
    'NATIONAL_ID',
    'Pièce d’identité',
    'CNI togolaise lisible, passeport ou permis',
    KolaIcons.idCardOutline,
  ),
  _Requirement(
    'SELFIE',
    'Photo du titulaire',
    'Selfie récent, visage net et bien éclairé',
    KolaIcons.personCircleOutline,
  ),
  _Requirement(
    'PROOF_OF_ADDRESS',
    'Justificatif de domicile',
    'Facture récente de moins de 3 mois',
    KolaIcons.homeOutline,
  ),
];

/// Taille maximale acceptée par le backend pour une pièce justificative.
const _maxFileBytes = 10 * 1024 * 1024;

class _PickedFile {
  const _PickedFile(this.name, this.path, this.size);
  final String name;
  final String path;
  final int size;
}

/// Synthèse de l'état du dossier, qui pilote l'écran entier : tant que rien
/// n'est envoyé ou qu'un document est refusé, on montre le formulaire ; sinon
/// on montre le suivi.
class _StatusInfo {
  const _StatusInfo({
    required this.label,
    required this.title,
    required this.text,
    required this.icon,
    required this.color,
    required this.background,
    required this.showForm,
  });

  final String label;
  final String title;
  final String text;
  final IconData icon;
  final Color color;
  final Color background;
  final bool showForm;
}

class KycScreen extends StatefulWidget {
  const KycScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<KycScreen> createState() => _KycScreenState();
}

class _KycScreenState extends State<KycScreen> {
  final _kyc = KycService();
  final _picked = <String, _PickedFile>{};

  KycStatus? _status;
  bool _loading = true;
  bool _consent = false;
  bool _submitting = false;
  Timer? _poller;

  @override
  void initState() {
    super.initState();
    _load();
    // Un dossier peut être validé pendant que l'écran est ouvert : le
    // rafraîchir évite d'obliger l'utilisateur à revenir en arrière.
    _poller = Timer.periodic(const Duration(seconds: 15), (_) => _load());
  }

  @override
  void dispose() {
    _poller?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    final result = await _kyc.status();
    if (!mounted) return;
    setState(() {
      _loading = false;
      if (result.success) _status = result.data;
    });
  }

  /// Le backend garde l'historique complet ; seul le dernier dépôt par type
  /// décrit l'état courant du dossier.
  List<KycDocument> get _latestDocuments {
    final seen = <String>{};
    final latest = <KycDocument>[];
    for (final doc in _status?.documents ?? const <KycDocument>[]) {
      if (seen.add(doc.type)) latest.add(doc);
    }
    return latest;
  }

  _StatusInfo get _info {
    final docs = _latestDocuments;
    final pending = docs.any((d) => d.status == 'PENDING');
    final rejected = docs.any((d) => d.isRejected);
    final approved = _requirements.every(
      (r) => docs.any((d) => d.type == r.type && d.isApproved),
    );

    if (pending) {
      return const _StatusInfo(
        label: 'EN COURS',
        title: 'Dossier en cours de vérification',
        text: 'Vos documents ont bien été reçus. Le service KOLA les examine.',
        icon: KolaIcons.timeOutline,
        color: AppColors.yellowDark,
        background: Color(0xFFFFF4C6),
        showForm: false,
      );
    }
    if (rejected) {
      return const _StatusInfo(
        label: 'REFUSÉ',
        title: 'Dossier KYC refusé',
        text:
            'Consultez le motif, corrigez les documents puis envoyez-les à nouveau.',
        icon: KolaIcons.closeCircleOutline,
        color: AppColors.red,
        background: AppColors.redPale,
        showForm: true,
      );
    }
    if (approved) {
      return _StatusInfo(
        label: 'ACTIVÉ',
        title: 'Compte KYC validé et activé',
        text:
            'Votre identité est validée. Niveau actuel : ${_status?.tierLabel ?? 'actif'}.',
        icon: KolaIcons.shieldCheckmark,
        color: AppColors.greenDark,
        background: AppColors.greenPale,
        showForm: false,
      );
    }
    return const _StatusInfo(
      label: 'NON ACTIVÉ',
      title: 'KYC non activé',
      text:
          'Envoyez les documents demandés pour faire vérifier votre identité.',
      icon: KolaIcons.shieldOutline,
      color: AppColors.muted,
      background: AppColors.pale,
      showForm: true,
    );
  }

  Future<void> _pick(String type) async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const ['jpg', 'jpeg', 'png', 'pdf'],
      );
      final file = result?.files.single;
      if (file == null || file.path == null) return;
      if (file.size > _maxFileBytes) {
        _alert(
          'Fichier trop volumineux',
          'Le document doit faire moins de 10 Mo.',
        );
        return;
      }
      setState(() {
        _picked[type] = _PickedFile(file.name, file.path!, file.size);
      });
    } catch (_) {
      _alert(
        'Import impossible',
        'Le document n’a pas pu être ouvert. Réessayez.',
      );
    }
  }

  Future<void> _submit() async {
    if (_picked.length < _requirements.length) {
      _alert('Dossier incomplet', 'Ajoutez les trois documents demandés.');
      return;
    }
    if (!_consent) {
      _alert(
        'Consentement requis',
        'Confirmez que les documents sont exacts et vous appartiennent.',
      );
      return;
    }

    setState(() => _submitting = true);
    // Envoi séquentiel : au premier échec on s'arrête, mais les documents déjà
    // acceptés restent enregistrés côté serveur — d'où le rechargement du
    // statut dans tous les cas.
    String? failure;
    for (final requirement in _requirements) {
      final file = _picked[requirement.type];
      if (file == null) continue;
      final result = await _kyc.uploadDocument(
        type: requirement.type,
        filePath: file.path,
      );
      if (!result.success) {
        failure = result.error?.message;
        break;
      }
    }

    if (!mounted) return;
    setState(() {
      _submitting = false;
      if (failure == null) {
        _picked.clear();
        _consent = false;
      }
    });
    await _load();
    if (!mounted) return;

    _alert(
      failure == null ? 'Dossier envoyé' : 'Envoi incomplet',
      failure ?? 'Vos documents sont maintenant en cours de vérification.',
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
    final info = _info;
    final rejected = _latestDocuments.where((d) => d.isRejected).toList();

    return KolaScreen(
      route: KolaRoute.kyc,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Mon profil',
          onTap: () => widget.onNavigate(KolaRoute.profile),
        ),
        const PageHeader(
          overline: 'VÉRIFICATION D’IDENTITÉ',
          title: 'Mon statut KYC',
          subtitle: 'Suivez l’activation de votre compte vérifié KOLA.',
          icon: KolaIcons.shieldCheckmarkOutline,
        ),
        const SizedBox(height: 17),
        KolaCard(
          color: info.background,
          child: Row(
            children: [
              Container(
                width: 52,
                height: 52,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: info.color,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  _loading ? KolaIcons.hourglassOutline : info.icon,
                  size: 27,
                  color: AppColors.white,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _loading ? 'CHARGEMENT…' : info.label,
                      style: AppTypography.badge.copyWith(
                        fontSize: 8,
                        fontWeight: FontWeight.w900,
                        color: info.color,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      info.title,
                      style: AppTypography.cardTitle.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      info.text,
                      style: AppTypography.caption.copyWith(fontSize: 9),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        if (rejected.isNotEmpty && info.label == 'REFUSÉ') ...[
          const SizedBox(height: 10),
          KolaCard(
            color: const Color(0xFFFFF7F6),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'MOTIF DU REFUS',
                  style: AppTypography.badge.copyWith(
                    fontSize: 8,
                    fontWeight: FontWeight.w900,
                    color: AppColors.red,
                  ),
                ),
                for (final doc in rejected)
                  Padding(
                    padding: const EdgeInsets.only(top: 11),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(
                          KolaIcons.alertCircleOutline,
                          size: 18,
                          color: AppColors.red,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                doc.typeLabel,
                                style: AppTypography.caption.copyWith(
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.ink,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                doc.rejectionReason ??
                                    'Document non conforme. Veuillez le remplacer.',
                                style: AppTypography.caption.copyWith(
                                  fontSize: 9,
                                  color: AppColors.redDark,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ],
        if (!info.showForm) ..._buildTracking(),
        if (info.showForm) ..._buildForm(),
      ],
    );
  }

  List<Widget> _buildTracking() {
    return [
      Padding(
        padding: const EdgeInsets.only(top: 23, bottom: 10),
        child: Text('Documents transmis', style: AppTypography.section),
      ),
      for (final doc in _latestDocuments)
        Padding(
          padding: const EdgeInsets.only(bottom: AppSpacing.xs),
          child: KolaCard(
            child: Row(
              children: [
                Icon(
                  doc.isApproved
                      ? KolaIcons.checkmarkCircle
                      : doc.isRejected
                      ? KolaIcons.closeCircle
                      : KolaIcons.time,
                  size: 23,
                  color: doc.isApproved
                      ? AppColors.green
                      : doc.isRejected
                      ? AppColors.red
                      : AppColors.yellowDark,
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        doc.typeLabel,
                        style: AppTypography.badge.copyWith(
                          fontSize: 12,
                          fontWeight: FontWeight.w800,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        doc.originalFilename,
                        style: AppTypography.caption.copyWith(fontSize: 9),
                      ),
                    ],
                  ),
                ),
                Text(
                  doc.statusLabel,
                  style: AppTypography.badge.copyWith(
                    fontSize: 9,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
            ),
          ),
        ),
      const SizedBox(height: 10),
      GestureDetector(
        onTap: _load,
        child: Container(
          height: 48,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.primary),
            borderRadius: BorderRadius.circular(24),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                KolaIcons.refreshOutline,
                size: 18,
                color: AppColors.primary,
              ),
              const SizedBox(width: 7),
              Text(
                'Actualiser le statut',
                style: AppTypography.button.copyWith(fontSize: 11),
              ),
            ],
          ),
        ),
      ),
    ];
  }

  List<Widget> _buildForm() {
    final count = _picked.length;

    return [
      const SizedBox(height: 12),
      KolaCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Votre nouveau dossier',
                  style: AppTypography.bodyBold.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Text(
                  '$count/${_requirements.length} documents',
                  style: AppTypography.caption.copyWith(
                    fontWeight: FontWeight.w700,
                    color: AppColors.greenDark,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            KolaProgress(count / _requirements.length * 100),
            const SizedBox(height: 8),
            Text(
              'JPG, PNG ou PDF • 10 Mo maximum',
              style: AppTypography.caption.copyWith(fontSize: 8),
            ),
          ],
        ),
      ),
      const SizedBox(height: 10),
      Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppColors.greenPale,
          borderRadius: BorderRadius.circular(11),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(
              KolaIcons.lockClosedOutline,
              size: 18,
              color: AppColors.greenDark,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                'Vos documents sont transmis de façon sécurisée au service de vérification KOLA.',
                style: AppTypography.caption.copyWith(
                  fontSize: 9,
                  color: AppColors.greenDark,
                ),
              ),
            ),
          ],
        ),
      ),
      Padding(
        padding: const EdgeInsets.only(top: 23, bottom: 10),
        child: Text('Documents personnels', style: AppTypography.section),
      ),
      for (final requirement in _requirements)
        Padding(
          padding: const EdgeInsets.only(bottom: 9),
          child: _DocumentCard(
            requirement: requirement,
            file: _picked[requirement.type],
            onPick: () => _pick(requirement.type),
          ),
        ),
      GestureDetector(
        onTap: () => setState(() => _consent = !_consent),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 13),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                _consent ? KolaIcons.checkbox : KolaIcons.squareOutline,
                size: 23,
                color: _consent ? AppColors.greenDark : AppColors.muted,
              ),
              const SizedBox(width: 9),
              Expanded(
                child: Text(
                  'Je certifie que ces documents sont valides, lisibles et m’appartiennent.',
                  style: AppTypography.caption.copyWith(
                    fontSize: 10,
                    color: AppColors.ink,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
      PrimaryButton(
        label: 'Envoyer mon dossier KYC',
        icon: KolaIcons.shieldCheckmarkOutline,
        loading: _submitting,
        onPressed: count < _requirements.length ? null : _submit,
      ),
    ];
  }
}

class _DocumentCard extends StatelessWidget {
  const _DocumentCard({
    required this.requirement,
    required this.file,
    required this.onPick,
  });

  final _Requirement requirement;
  final _PickedFile? file;
  final VoidCallback onPick;

  @override
  Widget build(BuildContext context) {
    final ready = file != null;

    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppRadius.card),
        border: Border.all(color: ready ? AppColors.green : AppColors.hairline),
        boxShadow: AppColors.softShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                width: 42,
                height: 42,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: ready ? AppColors.green : AppColors.pale2,
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Icon(
                  ready ? KolaIcons.checkmark : requirement.icon,
                  size: 22,
                  color: ready ? AppColors.white : AppColors.primary,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      requirement.title,
                      style: AppTypography.badge.copyWith(
                        fontSize: 12,
                        fontWeight: FontWeight.w800,
                        color: AppColors.ink,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      file?.name ?? requirement.subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: AppTypography.caption.copyWith(fontSize: 9),
                    ),
                    if (file != null) ...[
                      const SizedBox(height: 3),
                      Text(
                        '${(file!.size / 1024 / 1024).toStringAsFixed(1)} Mo',
                        style: AppTypography.caption.copyWith(
                          fontSize: 8,
                          color: AppColors.greenDark,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 11),
          GestureDetector(
            onTap: onPick,
            child: Container(
              height: 39,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: ready ? AppColors.pale2 : AppColors.primary,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    ready
                        ? KolaIcons.refreshOutline
                        : KolaIcons.cloudUploadOutline,
                    size: 18,
                    color: ready ? AppColors.primary : AppColors.white,
                  ),
                  const SizedBox(width: 7),
                  Text(
                    ready ? 'Remplacer' : 'Choisir un fichier',
                    style: AppTypography.badge.copyWith(
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      color: ready ? AppColors.primary : AppColors.white,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
