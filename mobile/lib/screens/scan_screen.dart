import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/utils/idempotency_key.dart';
import '../core/widgets/confirm_operation.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../models/transaction.dart';
import '../models/user.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';
import '../services/transaction_service.dart';
import '../services/user_service.dart';

/// Extrait un numéro togolais d'un QR KOLA.
///
/// Le contenu peut être un JSON (`{"phone": "..."}`), une URL portant le
/// numéro en paramètre, ou le numéro brut : accepter les trois évite qu'un QR
/// généré par une version antérieure devienne illisible.
String? togolesePhone(String payload) {
  var value = payload.trim();

  try {
    final decoded = jsonDecode(value);
    if (decoded is Map) {
      value =
          (decoded['recipientPhone'] ??
                  decoded['phone'] ??
                  decoded['account'] ??
                  '')
              .toString();
    }
  } catch (_) {
    // Charge utile non JSON : on continue avec les autres formes.
  }

  final query = RegExp(
    r'[?&](?:recipientPhone|phone)=([^&]+)',
    caseSensitive: false,
  ).firstMatch(value);
  if (query != null) value = Uri.decodeComponent(query.group(1)!);

  final digits = value.replaceAll(RegExp(r'\D'), '');
  if (digits.length == 8) return '+228$digits';
  if (digits.length == 11 && digits.startsWith('228')) return '+$digits';
  return null;
}

enum _Mode { scan, manual }

/// Envoi d'argent : scan d'un QR KOLA ou saisie du numéro, puis récapitulatif
/// des frais avant confirmation.
class ScanScreen extends StatefulWidget {
  const ScanScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> {
  final _users = UserService();
  final _transactions = TransactionService();
  final _idempotency = IdempotencyKeyHolder();
  final _manualPhone = TextEditingController();
  final _amount = TextEditingController();
  final _description = TextEditingController();
  final _scanner = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
  );

  _Mode _mode = _Mode.scan;
  String _phone = '';
  RecipientLookup? _recipient;
  bool _lookingUp = false;
  String _lookupError = '';
  bool _scanned = false;

  FeeQuote? _quote;
  bool _sending = false;
  KolaTransaction? _receipt;

  @override
  void dispose() {
    _manualPhone.dispose();
    _amount.dispose();
    _description.dispose();
    _scanner.dispose();
    super.dispose();
  }

  void _reset() {
    setState(() {
      _scanned = false;
      _phone = '';
      _recipient = null;
      _lookupError = '';
      _quote = null;
      _receipt = null;
      _manualPhone.clear();
      _amount.clear();
      _description.clear();
    });
    _idempotency.reset();
  }

  Future<void> _lookup(String value) async {
    setState(() {
      _lookingUp = true;
      _lookupError = '';
      _recipient = null;
    });

    final result = await _users.lookupRecipient(value);
    if (!mounted) return;

    setState(() {
      _lookingUp = false;
      if (result.success && result.data != null) {
        _recipient = result.data;
        _phone = value;
      } else {
        _phone = '';
        _lookupError = result.error?.message ?? 'Compte KOLA introuvable.';
        // Le scan est reprogrammable : sans ça, un QR refusé bloquerait la
        // caméra jusqu'au redémarrage de l'écran.
        if (_mode == _Mode.scan) _scanned = false;
      }
    });
  }

  void _onDetect(BarcodeCapture capture) {
    if (_scanned) return;
    final raw = capture.barcodes.firstOrNull?.rawValue;
    if (raw == null) return;

    setState(() => _scanned = true);
    final phone = togolesePhone(raw);
    if (phone == null) {
      _alert(
        'QR code non reconnu',
        'Ce QR ne contient pas un numéro KOLA togolais valide.',
      );
      setState(() => _scanned = false);
      return;
    }
    _lookup(phone);
  }

  Future<void> _prepare() async {
    final value = double.tryParse(_amount.text.replaceAll(RegExp(r'\D'), ''));
    if (_phone.isEmpty || _recipient == null) {
      _alert(
        'Bénéficiaire requis',
        'Saisissez ou scannez un numéro KOLA valide.',
      );
      return;
    }
    if (value == null || value <= 0) {
      _alert('Montant invalide', 'Saisissez un montant supérieur à zéro.');
      return;
    }

    final result = await _transactions.quote(amount: value);
    if (!mounted) return;
    if (!result.success) {
      _alert('Calcul impossible', result.error?.message ?? 'Erreur serveur.');
      return;
    }
    setState(() => _quote = result.data);
  }

  Future<void> _send() async {
    final quote = _quote;
    final recipient = _recipient;
    if (quote == null || recipient == null) return;

    final data = context.read<KolaDataProvider>();
    // Un transfert P2P ne se rattrape que par un litige : on fait relire le
    // nom du destinataire une dernière fois.
    final confirmed = await confirmOperation(
      context,
      title: 'Confirmer le transfert',
      amount: '${money(quote.amount)} FCFA',
      details: [
        ('Destinataire', recipient.displayName),
        ('Numéro', recipient.maskedPhone),
        ('Frais KOLA', '${money(quote.fee)} FCFA'),
        ('Total débité', '${money(quote.total)} FCFA'),
      ],
      note: 'Un transfert envoyé ne peut pas être annulé.',
      confirmLabel: 'Envoyer',
    );
    if (!confirmed || !mounted) return;
    setState(() => _sending = true);

    final result = await _transactions.transfer(
      amount: quote.amount,
      recipientPhone: _phone,
      description: _description.text.trim(),
      idempotencyKey: _idempotency.forIntent(
        '$_phone:${quote.amount.round()}:${_description.text.trim()}',
      ),
    );
    if (!mounted) return;
    setState(() => _sending = false);

    if (!result.success) {
      data.notifyFailure(
        result.error?.message ?? 'Erreur serveur.',
        'P2P_TRANSFER',
      );
      return;
    }

    data.notifyTransaction(result.data!);
    _idempotency.reset();
    await data.refresh();
    if (!mounted) return;
    setState(() => _receipt = result.data);
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
    final receipt = _receipt;
    if (receipt != null) {
      return _ReceiptView(
        transaction: receipt,
        phone: _phone,
        description: _description.text.trim(),
        onNavigate: widget.onNavigate,
        onNew: _reset,
      );
    }

    return KolaScreen(
      route: KolaRoute.scan,
      onNavigate: widget.onNavigate,
      children: [
        Text(
          'Envoyer de l’argent',
          style: AppTypography.screenTitle.copyWith(
            fontSize: 24,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          'Scannez un QR KOLA ou saisissez directement le numéro du bénéficiaire.',
          style: AppTypography.small.copyWith(height: 1.55),
        ),
        const SizedBox(height: 14),
        Container(
          height: 46,
          padding: const EdgeInsets.all(4),
          decoration: BoxDecoration(
            color: AppColors.pale2,
            borderRadius: BorderRadius.circular(23),
          ),
          child: Row(
            children: [
              _ModeButton(
                icon: KolaIcons.scanOutline,
                label: 'Scanner',
                active: _mode == _Mode.scan,
                onTap: () {
                  _reset();
                  setState(() => _mode = _Mode.scan);
                },
              ),
              _ModeButton(
                icon: KolaIcons.keypadOutline,
                label: 'Saisir le numéro',
                active: _mode == _Mode.manual,
                onTap: () {
                  _reset();
                  setState(() => _mode = _Mode.manual);
                },
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        if (_mode == _Mode.scan && _phone.isEmpty) _buildCamera(),
        if (_mode == _Mode.manual && _recipient == null) _buildManual(),
        if (_recipient != null) _buildRecipientCard(),
        if (_phone.isNotEmpty) ..._buildAmountForm(),
        Container(
          margin: const EdgeInsets.only(top: 15),
          padding: const EdgeInsets.all(13),
          decoration: BoxDecoration(
            color: AppColors.greenPale,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Row(
            children: [
              const Icon(
                KolaIcons.shieldCheckmarkOutline,
                size: 18,
                color: AppColors.greenDark,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Vérifiez toujours le numéro et le montant avant de confirmer.',
                  style: AppTypography.caption.copyWith(fontSize: 9),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildCamera() {
    return Container(
      height: 410,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: AppColors.primary,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          MobileScanner(controller: _scanner, onDetect: _onDetect),
          const _ScanFrame(),
          Positioned(
            bottom: 24,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
              decoration: BoxDecoration(
                color: AppColors.primary.withValues(alpha: 0.8),
                borderRadius: BorderRadius.circular(15),
              ),
              child: Text(
                'Placez le QR KOLA dans le cadre',
                style: AppTypography.caption.copyWith(
                  fontSize: 10,
                  color: AppColors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildManual() {
    return KolaCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'NUMÉRO DU BÉNÉFICIAIRE',
            style: AppTypography.fieldLabel.copyWith(fontSize: 8),
          ),
          const SizedBox(height: 7),
          Container(
            height: 52,
            padding: const EdgeInsets.symmetric(horizontal: 11),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              children: [
                const Text('🇹🇬', style: TextStyle(fontSize: 19)),
                const SizedBox(width: 8),
                Text(
                  '+228',
                  style: AppTypography.badge.copyWith(
                    fontSize: 12,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(width: 8),
                Container(width: 1, height: 23, color: AppColors.border),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _manualPhone,
                    autofocus: true,
                    keyboardType: TextInputType.phone,
                    maxLength: 8,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    onChanged: (value) {
                      setState(() => _quote = null);
                      if (value.length == 8) _lookup('+228$value');
                    },
                    style: AppTypography.amount.copyWith(
                      fontSize: 18,
                      letterSpacing: 1,
                    ),
                    decoration: const InputDecoration(
                      hintText: '90 00 00 00',
                      counterText: '',
                      border: InputBorder.none,
                      enabledBorder: InputBorder.none,
                      focusedBorder: InputBorder.none,
                      filled: false,
                      isDense: true,
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                ),
                if (_lookingUp)
                  const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
              ],
            ),
          ),
          if (_manualPhone.text.length < 8)
            Padding(
              padding: const EdgeInsets.only(top: 7),
              child: Text(
                'Saisissez les 8 chiffres du numéro togolais.',
                style: AppTypography.caption.copyWith(fontSize: 9),
              ),
            ),
          if (_lookupError.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Row(
                children: [
                  const Icon(
                    KolaIcons.alertCircleOutline,
                    size: 17,
                    color: AppColors.red,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      _lookupError,
                      style: AppTypography.caption.copyWith(
                        fontSize: 9,
                        color: AppColors.red,
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildRecipientCard() {
    final recipient = _recipient!;
    return KolaCard(
      child: Row(
        children: [
          const IconCircle(KolaIcons.personOutline, size: 25, diameter: 48),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'BÉNÉFICIAIRE CONFIRMÉ',
                  style: AppTypography.badge.copyWith(
                    fontSize: 8,
                    fontWeight: FontWeight.w800,
                    color: AppColors.greenDark,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  recipient.displayName,
                  style: AppTypography.cardTitle.copyWith(
                    fontSize: 16,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  recipient.maskedPhone,
                  style: AppTypography.caption.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          GestureDetector(
            onTap: _reset,
            child: Icon(
              _mode == _Mode.scan
                  ? KolaIcons.scanOutline
                  : KolaIcons.createOutline,
              size: 22,
              color: AppColors.primary,
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _buildAmountForm() {
    final quote = _quote;

    return [
      const FieldLabel('Montant à envoyer'),
      Container(
        height: 55,
        padding: const EdgeInsets.symmetric(horizontal: 14),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(13),
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
                onChanged: (_) => setState(() => _quote = null),
                style: AppTypography.amount.copyWith(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                ),
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
                fontWeight: FontWeight.w900,
                color: AppColors.yellowDark,
              ),
            ),
          ],
        ),
      ),
      const FieldLabel('Motif (facultatif)'),
      SizedBox(
        height: 49,
        child: TextField(
          controller: _description,
          maxLength: 140,
          onTap: () => setState(() => _quote = null),
          style: AppTypography.body,
          decoration: const InputDecoration(
            hintText: 'Ex. Remboursement, cadeau…',
            counterText: '',
          ),
        ),
      ),
      if (quote != null) ...[
        const SizedBox(height: 15),
        KolaCard(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
          child: Column(
            children: [
              DetailRow(label: 'Montant', value: '${money(quote.amount)} FCFA'),
              DetailRow(label: 'Frais KOLA', value: '${money(quote.fee)} FCFA'),
              Container(
                height: 48,
                alignment: Alignment.center,
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        'TOTAL À DÉBITER',
                        style: AppTypography.badge.copyWith(
                          fontSize: 9,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    Text(
                      '${money(quote.total)} FCFA',
                      style: AppTypography.amount.copyWith(
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
      const SizedBox(height: 16),
      PrimaryButton(
        label: quote == null
            ? 'Voir le récapitulatif'
            : 'Confirmer le transfert',
        icon: quote == null
            ? KolaIcons.calculatorOutline
            : KolaIcons.sendOutline,
        loading: _sending,
        onPressed: quote == null ? _prepare : _send,
      ),
    ];
  }
}

class _ModeButton extends StatelessWidget {
  const _ModeButton({
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
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 18,
                color: active ? AppColors.white : AppColors.primary,
              ),
              const SizedBox(width: 7),
              Flexible(
                child: Text(
                  label,
                  style: AppTypography.badge.copyWith(
                    fontSize: 10,
                    fontWeight: FontWeight.w800,
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

/// Cadre de visée : quatre coins jaunes, sans masque plein pour laisser voir
/// l'image de la caméra.
class _ScanFrame extends StatelessWidget {
  const _ScanFrame();

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: SizedBox(
        width: 230,
        height: 230,
        child: Stack(
          children: const [
            Positioned(top: 0, left: 0, child: _Corner(top: true, left: true)),
            Positioned(
              top: 0,
              right: 0,
              child: _Corner(top: true, left: false),
            ),
            Positioned(
              bottom: 0,
              left: 0,
              child: _Corner(top: false, left: true),
            ),
            Positioned(
              bottom: 0,
              right: 0,
              child: _Corner(top: false, left: false),
            ),
          ],
        ),
      ),
    );
  }
}

class _Corner extends StatelessWidget {
  const _Corner({required this.top, required this.left});

  final bool top;
  final bool left;

  @override
  Widget build(BuildContext context) {
    const side = BorderSide(color: AppColors.yellow, width: 4);
    return Container(
      width: 42,
      height: 42,
      decoration: BoxDecoration(
        border: Border(
          top: top ? side : BorderSide.none,
          bottom: top ? BorderSide.none : side,
          left: left ? side : BorderSide.none,
          right: left ? BorderSide.none : side,
        ),
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(top && left ? 10 : 0),
          topRight: Radius.circular(top && !left ? 10 : 0),
          bottomLeft: Radius.circular(!top && left ? 10 : 0),
          bottomRight: Radius.circular(!top && !left ? 10 : 0),
        ),
      ),
    );
  }
}

class _ReceiptView extends StatefulWidget {
  const _ReceiptView({
    required this.transaction,
    required this.phone,
    required this.description,
    required this.onNavigate,
    required this.onNew,
  });

  final KolaTransaction transaction;
  final String phone;
  final String description;
  final void Function(KolaRoute) onNavigate;
  final VoidCallback onNew;

  @override
  State<_ReceiptView> createState() => _ReceiptViewState();
}

class _ReceiptViewState extends State<_ReceiptView> {
  bool _downloading = false;

  double get _fee => widget.transaction.fee;
  double get _total => widget.transaction.totalDebited > 0
      ? widget.transaction.totalDebited
      : widget.transaction.amount + _fee;

  Future<void> _download() async {
    setState(() => _downloading = true);
    try {
      final document = pw.Document();
      final rows = <List<String>>[
        ['Bénéficiaire', widget.phone],
        ['Date', receiptDate(widget.transaction.displayedAt)],
        ['Montant', '${money(widget.transaction.amount)} FCFA'],
        ['Frais KOLA', '${money(_fee)} FCFA'],
        ['Total débité', '${money(_total)} FCFA'],
        ['Référence', widget.transaction.reference],
        ['Statut', 'Réussi'],
        if (widget.description.isNotEmpty) ['Motif', widget.description],
      ];

      document.addPage(
        pw.Page(
          pageFormat: PdfPageFormat.a4,
          build: (context) => pw.Padding(
            padding: const pw.EdgeInsets.all(32),
            child: pw.Column(
              crossAxisAlignment: pw.CrossAxisAlignment.start,
              children: [
                pw.Text(
                  'KOLA',
                  style: pw.TextStyle(
                    fontSize: 26,
                    fontWeight: pw.FontWeight.bold,
                    color: PdfColor.fromInt(0xFF002353),
                  ),
                ),
                pw.SizedBox(height: 28),
                pw.Text(
                  'Reçu de transfert',
                  style: pw.TextStyle(
                    fontSize: 20,
                    fontWeight: pw.FontWeight.bold,
                    color: PdfColor.fromInt(0xFF005236),
                  ),
                ),
                pw.SizedBox(height: 8),
                pw.Text('Votre transfert a été effectué avec succès.'),
                pw.SizedBox(height: 24),
                pw.TableHelper.fromTextArray(
                  headers: null,
                  data: rows,
                  border: null,
                  cellAlignments: {
                    0: pw.Alignment.centerLeft,
                    1: pw.Alignment.centerRight,
                  },
                  cellStyle: const pw.TextStyle(fontSize: 11),
                ),
                pw.Spacer(),
                pw.Container(
                  padding: const pw.EdgeInsets.all(12),
                  color: PdfColor.fromInt(0xFFF2F3FF),
                  child: pw.Text(
                    'Document généré par KOLA. Conservez cette référence pour toute demande d’assistance.',
                    style: const pw.TextStyle(fontSize: 10),
                  ),
                ),
              ],
            ),
          ),
        ),
      );

      await Printing.sharePdf(
        bytes: await document.save(),
        filename: 'recu-kola-${widget.transaction.reference}.pdf',
      );
    } catch (_) {
      if (mounted) {
        showDialog<void>(
          context: context,
          builder: (context) => AlertDialog(
            title: Text(
              'Téléchargement impossible',
              style: AppTypography.cardTitle,
            ),
            content: Text(
              'Impossible de créer le reçu.',
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
      }
    } finally {
      if (mounted) setState(() => _downloading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return KolaScreen(
      route: KolaRoute.scan,
      onNavigate: widget.onNavigate,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 17, bottom: 20),
          child: Column(
            children: [
              Container(
                width: 76,
                height: 76,
                alignment: Alignment.center,
                decoration: const BoxDecoration(
                  color: AppColors.green,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  KolaIcons.checkmark,
                  size: 42,
                  color: AppColors.white,
                ),
              ),
              const SizedBox(height: 14),
              Text(
                'Transfert réussi',
                style: AppTypography.screenTitle.copyWith(
                  fontSize: 25,
                  color: AppColors.greenDark,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Votre argent a bien été envoyé',
                style: AppTypography.small,
              ),
              const SizedBox(height: 17),
              RichText(
                text: TextSpan(
                  style: AppTypography.amount.copyWith(
                    fontSize: 34,
                    fontWeight: FontWeight.w900,
                  ),
                  children: [
                    TextSpan(text: money(widget.transaction.amount)),
                    TextSpan(
                      text: ' FCFA',
                      style: AppTypography.badge.copyWith(
                        fontSize: 14,
                        color: AppColors.yellowDark,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        KolaCard(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
          child: Column(
            children: [
              DetailRow(label: 'Bénéficiaire', value: widget.phone),
              DetailRow(
                label: 'Date et heure',
                value: receiptDate(widget.transaction.displayedAt),
              ),
              DetailRow(label: 'Frais KOLA', value: '${money(_fee)} FCFA'),
              DetailRow(label: 'Total débité', value: '${money(_total)} FCFA'),
              if (widget.description.isNotEmpty)
                DetailRow(label: 'Motif', value: widget.description),
              Container(
                width: double.infinity,
                margin: const EdgeInsets.symmetric(vertical: 12),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.pale,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  children: [
                    Text(
                      'RÉFÉRENCE',
                      style: AppTypography.fieldLabel.copyWith(fontSize: 8),
                    ),
                    const SizedBox(height: 4),
                    SelectableText(
                      widget.transaction.reference,
                      style: AppTypography.badge.copyWith(
                        fontSize: 11,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        PrimaryButton(
          label: 'Télécharger le reçu PDF',
          icon: KolaIcons.downloadOutline,
          loading: _downloading,
          onPressed: _download,
        ),
        const SizedBox(height: 9),
        GestureDetector(
          onTap: widget.onNew,
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
                  KolaIcons.scanOutline,
                  size: 19,
                  color: AppColors.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  'Faire un nouvel envoi',
                  style: AppTypography.button.copyWith(fontSize: 12),
                ),
              ],
            ),
          ),
        ),
        TextButton(
          onPressed: () => widget.onNavigate(KolaRoute.home),
          child: Text(
            'Retour à l’accueil',
            style: AppTypography.smallBold.copyWith(color: AppColors.muted),
          ),
        ),
      ],
    );
  }
}
