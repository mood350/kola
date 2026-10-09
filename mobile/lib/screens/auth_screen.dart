import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/theme/kola_icons.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/widgets/code_input.dart';
import '../models/user.dart';
import '../services/auth_service.dart';

enum _Mode { welcome, login, signup }

const _steps = ['Identité', 'Téléphone', 'Vérification', 'Code PIN'];

/// Accueil non connecté : présentation, connexion par PIN, et inscription en
/// quatre étapes (identité → numéro → code SMS → PIN).
class AuthScreen extends StatefulWidget {
  const AuthScreen({
    super.key,
    required this.onAuthenticated,
    this.authService,
  });

  final Future<void> Function(AuthSession) onAuthenticated;
  final AuthService? authService;

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  late final AuthService _auth = widget.authService ?? AuthService();

  _Mode _mode = _Mode.welcome;
  int _step = 0;
  bool _loading = false;
  bool _accepted = false;
  bool _secure = true;

  final _name = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _otp = TextEditingController();
  final _pin = TextEditingController();
  final _confirmPin = TextEditingController();
  DateTime? _dateOfBirth;
  String _verificationToken = '';

  @override
  void dispose() {
    for (final controller in [_name, _email, _phone, _otp, _pin, _confirmPin]) {
      controller.dispose();
    }
    super.dispose();
  }

  void _open(_Mode mode) => setState(() {
    _mode = mode;
    _step = 0;
  });

  void _previous() => setState(() {
    if (_step == 0) {
      _mode = _Mode.welcome;
    } else {
      _step--;
    }
  });

  void _alert(String title, String message) {
    if (!mounted) return;
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

  String get _internationalPhone => '+228${_phone.text}';

  Future<void> _login() async {
    if (_phone.text.length != 8 || _pin.text.length != 4) {
      _alert(
        'Connexion impossible',
        'Saisissez un numéro togolais à 8 chiffres et votre PIN à 4 chiffres.',
      );
      return;
    }
    setState(() => _loading = true);
    final result = await _auth.login(_internationalPhone, _pin.text);
    if (!mounted) return;
    setState(() => _loading = false);
    if (result.session == null) {
      _alert('Erreur', result.message);
      return;
    }
    await widget.onAuthenticated(result.session!);
  }

  Future<void> _next() async {
    if (_step == 0) {
      if (_name.text.trim().isEmpty ||
          !_email.text.contains('@') ||
          _dateOfBirth == null ||
          !_accepted) {
        _alert(
          'Informations incomplètes',
          'Renseignez votre nom, un e-mail valide, votre date de naissance et acceptez les conditions.',
        );
        return;
      }
    }
    if (_step == 1 && _phone.text.length != 8) {
      _alert(
        'Numéro invalide',
        'Saisissez les 8 chiffres de votre numéro togolais.',
      );
      return;
    }
    if (_step == 2 && _otp.text.length != 6) {
      _alert(
        'Code incorrect',
        'Saisissez le code OTP à 6 chiffres reçu par SMS.',
      );
      return;
    }
    if (_step == 3 &&
        (_pin.text.length != 4 || _pin.text != _confirmPin.text)) {
      _alert(
        'PIN invalide',
        'Le code PIN doit contenir 4 chiffres et les deux codes doivent correspondre.',
      );
      return;
    }

    setState(() => _loading = true);

    if (_step == 1) {
      final result = await _auth.requestOtp(_internationalPhone);
      if (!mounted) return;
      setState(() => _loading = false);
      if (!result.success) {
        _alert('Erreur', result.error?.message ?? 'Envoi du code impossible.');
        return;
      }
      setState(() => _step = 2);
      return;
    }

    if (_step == 2) {
      final result = await _auth.verifyOtp(_internationalPhone, _otp.text);
      if (!mounted) return;
      setState(() => _loading = false);
      if (!result.success) {
        _alert('Erreur', result.error?.message ?? 'Code refusé.');
        return;
      }
      setState(() {
        _verificationToken = result.data ?? '';
        _step = 3;
      });
      return;
    }

    if (_step == 3) {
      // Le backend attend un prénom et un nom distincts : le premier mot
      // saisi fait office de prénom, le reste de nom. Un utilisateur qui
      // n'entre qu'un seul mot voit celui-ci repris des deux côtés plutôt que
      // de se voir refuser l'inscription.
      final parts = _name.text.trim().split(RegExp(r'\s+'));
      final firstName = parts.first;
      final lastName = parts.length > 1 ? parts.skip(1).join(' ') : firstName;

      final result = await _auth.register(
        verificationToken: _verificationToken,
        firstName: firstName,
        lastName: lastName,
        email: _email.text.trim(),
        dateOfBirth:
            '${_dateOfBirth!.year.toString().padLeft(4, '0')}-'
            '${_dateOfBirth!.month.toString().padLeft(2, '0')}-'
            '${_dateOfBirth!.day.toString().padLeft(2, '0')}',
        pin: _pin.text,
      );
      if (!mounted) return;
      setState(() => _loading = false);
      if (result.session == null) {
        _alert('Erreur', result.message);
        return;
      }
      await widget.onAuthenticated(result.session!);
      return;
    }

    setState(() {
      _loading = false;
      _step++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      body: SafeArea(
        child: _mode == _Mode.welcome ? _buildWelcome() : _buildForm(),
      ),
    );
  }

  Widget _buildWelcome() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(25, 70, 25, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Image.asset('assets/kola-logo-transparent.png', width: 150),
          const SizedBox(height: 55),
          Text(
            'Votre finance.\nVotre avenir.',
            style: AppTypography.screenTitle.copyWith(
              fontSize: 38,
              height: 1.18,
            ),
          ),
          const SizedBox(height: 14),
          Text(
            'Un portefeuille sécurisé pour payer, utiliser Bankivi et construire votre accès au crédit au Togo.',
            style: AppTypography.small.copyWith(fontSize: 13, height: 1.6),
          ),
          const Spacer(),
          _YellowButton(
            label: 'Créer mon compte',
            onPressed: () => _open(_Mode.signup),
          ),
          const SizedBox(height: 10),
          GestureDetector(
            onTap: () => _open(_Mode.login),
            child: Container(
              height: 52,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                border: Border.all(color: AppColors.primary, width: 1.5),
                borderRadius: BorderRadius.circular(26),
              ),
              child: Text(
                'J’ai déjà un compte',
                style: AppTypography.button.copyWith(fontSize: 13),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 22),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  KolaIcons.shieldCheckmarkOutline,
                  size: 16,
                  color: AppColors.green,
                ),
                const SizedBox(width: 6),
                Text(
                  'Protection des données et sécurité KOLA',
                  style: AppTypography.badge.copyWith(
                    fontSize: 8,
                    fontWeight: FontWeight.w400,
                    color: AppColors.muted,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildForm() {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(22, 14, 22, 35),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              GestureDetector(
                onTap: _mode == _Mode.signup
                    ? _previous
                    : () => setState(() => _mode = _Mode.welcome),
                child: Container(
                  width: 40,
                  height: 40,
                  alignment: Alignment.center,
                  decoration: const BoxDecoration(
                    color: AppColors.pale2,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    KolaIcons.arrowBack,
                    size: 20,
                    color: AppColors.primary,
                  ),
                ),
              ),
              Image.asset('assets/kola-logo-transparent.png', width: 70),
            ],
          ),
          if (_mode == _Mode.signup) ..._buildSignup() else ..._buildLogin(),
        ],
      ),
    );
  }

  List<Widget> _buildLogin() {
    return [
      const SizedBox(height: 24),
      Text('Bon retour parmi nous', style: _titleStyle),
      const SizedBox(height: 5),
      Text(
        'Connectez-vous avec votre numéro togolais.',
        style: AppTypography.small,
      ),
      _PhoneField(controller: _phone),
      const _Label('CODE PIN'),
      _PinField(
        controller: _pin,
        secure: _secure,
        onToggle: () => setState(() => _secure = !_secure),
      ),
      const SizedBox(height: 24),
      _YellowButton(
        label: 'Se connecter',
        loading: _loading,
        onPressed: _login,
      ),
      const SizedBox(height: 14),
      Text(
        'Code PIN oublié ?',
        textAlign: TextAlign.center,
        style: AppTypography.caption,
      ),
      const SizedBox(height: 18),
      GestureDetector(
        onTap: () => _open(_Mode.signup),
        child: Text(
          'Nouveau sur KOLA ? Créer un compte',
          textAlign: TextAlign.center,
          style: AppTypography.smallBold.copyWith(fontSize: 10),
        ),
      ),
    ];
  }

  List<Widget> _buildSignup() {
    return [
      const SizedBox(height: 25),
      Row(
        children: [
          for (var i = 0; i < _steps.length; i++)
            Expanded(
              child: Column(
                children: [
                  Container(
                    width: 28,
                    height: 28,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: i <= _step ? AppColors.primary : AppColors.pale2,
                      shape: BoxShape.circle,
                    ),
                    child: Text(
                      '${i + 1}',
                      style: AppTypography.badge.copyWith(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        color: i <= _step ? AppColors.white : AppColors.muted,
                      ),
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    _steps[i],
                    style: AppTypography.badge.copyWith(
                      fontSize: 7,
                      fontWeight: i == _step
                          ? FontWeight.w800
                          : FontWeight.w400,
                      color: i == _step ? AppColors.primary : AppColors.muted,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
      ..._buildSignupStep(),
      const SizedBox(height: 24),
      Row(
        children: [
          if (_step > 0) ...[
            GestureDetector(
              onTap: _loading ? null : _previous,
              child: Container(
                height: 54,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  border: Border.all(color: AppColors.primary),
                  borderRadius: BorderRadius.circular(27),
                ),
                child: Text(
                  'Précédent',
                  style: AppTypography.button.copyWith(fontSize: 12),
                ),
              ),
            ),
            const SizedBox(width: 10),
          ],
          Expanded(
            child: _YellowButton(
              label: _step == 3 ? 'Créer mon compte' : 'Suivant',
              loading: _loading,
              onPressed: _next,
            ),
          ),
        ],
      ),
    ];
  }

  List<Widget> _buildSignupStep() {
    switch (_step) {
      case 0:
        return [
          const SizedBox(height: 24),
          Text('Faisons connaissance', style: _titleStyle),
          const SizedBox(height: 5),
          Text(
            'Renseignez les informations du titulaire du compte.',
            style: AppTypography.small,
          ),
          const _Label('NOM COMPLET'),
          _IconField(
            controller: _name,
            icon: KolaIcons.personOutline,
            hint: 'Votre nom et prénom',
            capitalization: TextCapitalization.words,
          ),
          const _Label('ADRESSE E-MAIL'),
          _IconField(
            controller: _email,
            icon: KolaIcons.mailOutline,
            hint: 'nom@exemple.com',
            keyboardType: TextInputType.emailAddress,
          ),
          const _Label('DATE DE NAISSANCE'),
          GestureDetector(
            onTap: _pickBirthDate,
            child: Container(
              height: 50,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: _fieldDecoration,
              child: Row(
                children: [
                  const Icon(
                    KolaIcons.calendarOutline,
                    size: 18,
                    color: AppColors.muted,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    _dateOfBirth == null
                        ? 'Choisir une date'
                        : shortDate(_dateOfBirth!),
                    style: AppTypography.body.copyWith(
                      fontSize: 13,
                      color: _dateOfBirth == null
                          ? AppColors.muted
                          : AppColors.ink,
                    ),
                  ),
                ],
              ),
            ),
          ),
          GestureDetector(
            onTap: () => setState(() => _accepted = !_accepted),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 18),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    _accepted ? KolaIcons.checkbox : KolaIcons.squareOutline,
                    size: 22,
                    color: _accepted ? AppColors.greenDark : AppColors.muted,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'J’accepte les conditions d’utilisation et la politique de confidentialité.',
                      style: AppTypography.caption.copyWith(fontSize: 9),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ];
      case 1:
        return [
          const SizedBox(height: 24),
          Text('Votre numéro mobile', style: _titleStyle),
          const SizedBox(height: 5),
          Text(
            'Un SMS de vérification sera envoyé à ce numéro.',
            style: AppTypography.small,
          ),
          _PhoneField(controller: _phone),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.pale2,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                const Icon(
                  KolaIcons.informationCircleOutline,
                  size: 18,
                  color: AppColors.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Format Togo : +228 suivi des 8 chiffres de votre numéro.',
                    style: AppTypography.caption.copyWith(
                      color: AppColors.primary,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ];
      case 2:
        return [
          const SizedBox(height: 24),
          Text('Vérifiez votre numéro', style: _titleStyle),
          const SizedBox(height: 5),
          Text(
            'Entrez le code envoyé au $_internationalPhone.',
            style: AppTypography.small,
          ),
          const _Label('CODE OTP À 6 CHIFFRES'),
          CodeInput(controller: _otp, length: 6, autofocus: true),
          const SizedBox(height: 18),
          GestureDetector(
            onTap: () async {
              final result = await _auth.requestOtp(_internationalPhone);
              if (!mounted) return;
              _alert(
                result.success ? 'Code renvoyé' : 'Erreur',
                result.success
                    ? 'Un nouveau code OTP vous a été envoyé.'
                    : result.error?.message ??
                          'Impossible de renvoyer le code.',
              );
            },
            child: Text(
              'Renvoyer le code',
              textAlign: TextAlign.center,
              style: AppTypography.smallBold,
            ),
          ),
        ];
      default:
        return [
          const SizedBox(height: 24),
          Text('Créez votre code PIN', style: _titleStyle),
          const SizedBox(height: 5),
          Text(
            'Ce code à 4 chiffres protégera l’accès à votre portefeuille.',
            style: AppTypography.small,
          ),
          const _Label('CODE PIN'),
          _PinField(
            controller: _pin,
            secure: _secure,
            onToggle: () => setState(() => _secure = !_secure),
          ),
          const _Label('CONFIRMER LE CODE PIN'),
          _PinField(
            controller: _confirmPin,
            secure: _secure,
            onToggle: () => setState(() => _secure = !_secure),
          ),
        ];
    }
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _dateOfBirth ?? DateTime(now.year - 25),
      firstDate: DateTime(now.year - 100),
      lastDate: now,
      helpText: 'Date de naissance',
      locale: const Locale('fr', 'FR'),
    );
    if (picked != null) setState(() => _dateOfBirth = picked);
  }

  TextStyle get _titleStyle => AppTypography.screenTitle.copyWith(fontSize: 27);
}

BoxDecoration get _fieldDecoration => BoxDecoration(
  color: AppColors.white,
  borderRadius: BorderRadius.circular(12),
  border: Border.all(color: AppColors.border),
);

class _Label extends StatelessWidget {
  const _Label(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 14, bottom: 6),
      child: Text(text, style: AppTypography.fieldLabel.copyWith(fontSize: 8)),
    );
  }
}

class _IconField extends StatelessWidget {
  const _IconField({
    required this.controller,
    required this.icon,
    required this.hint,
    this.keyboardType,
    this.capitalization = TextCapitalization.none,
  });

  final TextEditingController controller;
  final IconData icon;
  final String hint;
  final TextInputType? keyboardType;
  final TextCapitalization capitalization;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: _fieldDecoration,
      child: Row(
        children: [
          Icon(icon, size: 18, color: AppColors.muted),
          const SizedBox(width: 8),
          Expanded(
            child: TextField(
              controller: controller,
              keyboardType: keyboardType,
              textCapitalization: capitalization,
              style: AppTypography.body.copyWith(fontSize: 13),
              decoration: InputDecoration(
                hintText: hint,
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                filled: false,
                isDense: true,
                contentPadding: EdgeInsets.zero,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PhoneField extends StatelessWidget {
  const _PhoneField({required this.controller});

  final TextEditingController controller;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const _Label('NUMÉRO DE TÉLÉPHONE'),
        Container(
          height: 50,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: _fieldDecoration,
          child: Row(
            children: [
              const Text('🇹🇬', style: TextStyle(fontSize: 22)),
              const SizedBox(width: 8),
              Text(
                '+228',
                style: AppTypography.bodyBold.copyWith(fontSize: 13),
              ),
              const SizedBox(width: 8),
              Container(width: 1, height: 24, color: AppColors.border),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: controller,
                  keyboardType: TextInputType.phone,
                  maxLength: 8,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: AppTypography.body.copyWith(fontSize: 13),
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
            ],
          ),
        ),
      ],
    );
  }
}

class _PinField extends StatelessWidget {
  const _PinField({
    required this.controller,
    required this.secure,
    required this.onToggle,
  });

  final TextEditingController controller;
  final bool secure;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: CodeInput(controller: controller, length: 4, obscure: secure),
        ),
        const SizedBox(width: 10),
        GestureDetector(
          onTap: onToggle,
          child: Container(
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
              color: AppColors.pale2,
              shape: BoxShape.circle,
            ),
            child: Icon(
              secure ? KolaIcons.eyeOutline : KolaIcons.eyeOffOutline,
              size: 20,
              color: AppColors.primary,
            ),
          ),
        ),
      ],
    );
  }
}

class _YellowButton extends StatelessWidget {
  const _YellowButton({
    required this.label,
    required this.onPressed,
    this.loading = false,
  });

  final String label;
  final VoidCallback onPressed;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: loading ? null : onPressed,
      child: Opacity(
        opacity: loading ? 0.7 : 1,
        child: Container(
          height: 54,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: AppColors.yellow,
            borderRadius: BorderRadius.circular(27),
          ),
          child: loading
              ? const SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.4,
                    color: AppColors.primary,
                  ),
                )
              : Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      label,
                      style: AppTypography.button.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(width: 9),
                    const Icon(
                      KolaIcons.arrowForward,
                      size: 19,
                      color: AppColors.primary,
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
