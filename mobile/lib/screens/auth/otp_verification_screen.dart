import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../providers/auth_provider.dart';
import '../../routes/app_routes.dart';
import 'reset_password_screen.dart';

/// Ce que le code à 6 chiffres saisi sur cet écran va servir à faire.
///
/// Les deux flux backend envoient un code par email (jamais par SMS) et
/// attendent le même format : `^\d{6}$`. Seul le traitement diffère une fois
/// le code complet.
enum OtpPurpose {
  /// Activation du compte après inscription — POST /api/auth/confirm.
  /// Le code est validé ici même, puis l'utilisateur peut se connecter.
  activation,

  /// Réinitialisation du mot de passe — POST /api/auth/reset-password.
  /// Cet endpoint attend `{ token, newPassword }` en un seul appel : il n'y a
  /// pas de route qui valide le code seul. Le code est donc transporté jusqu'à
  /// ResetPasswordScreen, qui fait l'appel une fois le mot de passe saisi.
  passwordReset,
}

/// Écran de saisie du code à 6 chiffres reçu par email.
/// Numpad custom (pas le clavier natif), avec dots visuels et timer de renvoi.
class OtpVerificationScreen extends StatefulWidget {
  /// Email destinataire du code — sert à l'affichage et au renvoi.
  final String email;
  final OtpPurpose purpose;

  const OtpVerificationScreen({
    super.key,
    required this.email,
    required this.purpose,
  });

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  static const int _pinLength = 6;
  String _pin = '';
  bool _isSubmitting = false;
  String? _errorMessage;
  int _secondsRemaining = 59;
  Timer? _timer;

  /// Seul le flux mot de passe oublié peut renvoyer un code : le backend
  /// expose POST /auth/forgot-password, mais aucune route de renvoi du code
  /// d'activation (cf. AuthController).
  bool get _canResend => widget.purpose == OtpPurpose.passwordReset;

  @override
  void initState() {
    super.initState();
    if (_canResend) _startTimer();
  }

  void _startTimer() {
    _timer?.cancel();
    setState(() => _secondsRemaining = 59);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining == 0) {
        timer.cancel();
      } else {
        setState(() => _secondsRemaining--);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String get _maskedEmail {
    // Masque l'email façon "pr•••@gmail.com"
    final raw = widget.email;
    final at = raw.indexOf('@');
    if (at <= 2) return raw;
    return '${raw.substring(0, 2)}•••${raw.substring(at)}';
  }

  void _onDigitPressed(String digit) {
    if (_isSubmitting || _pin.length >= _pinLength) return;
    setState(() {
      _pin += digit;
      _errorMessage = null;
    });
    if (_pin.length == _pinLength) {
      _onSubmit();
    }
  }

  void _onBackspace() {
    if (_isSubmitting || _pin.isEmpty) return;
    setState(() {
      _pin = _pin.substring(0, _pin.length - 1);
      _errorMessage = null;
    });
  }

  Future<void> _onSubmit() async {
    if (_isSubmitting) return;
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    if (widget.purpose == OtpPurpose.activation) {
      await _confirmAccount();
    } else {
      await _goToNewPassword();
    }
  }

  Future<void> _confirmAccount() async {
    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.confirmAccount(_pin);
    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Compte activé ! Vous pouvez maintenant vous connecter.',
          ),
          backgroundColor: AppColors.success,
        ),
      );
      Navigator.pushNamedAndRemoveUntil(
        context,
        AppRoutes.login,
        (route) => false,
      );
      return;
    }

    setState(() {
      _isSubmitting = false;
      _pin = '';
      _errorMessage = authProvider.errorMessage ?? 'Code invalide ou expiré';
    });
  }

  /// Le code n'est pas vérifiable seul : on l'emmène sur l'écran du nouveau
  /// mot de passe, qui l'enverra avec celui-ci. Si le code est faux, c'est là
  /// que le backend le dira, et l'utilisateur revient ici en arrière.
  Future<void> _goToNewPassword() async {
    await Navigator.push<void>(
      context,
      MaterialPageRoute(
        builder: (_) => ResetPasswordScreen(email: widget.email, code: _pin),
      ),
    );
    // En cas de succès, ResetPasswordScreen vide la pile jusqu'au login :
    // cet écran est alors démonté et `mounted` vaut false.
    if (!mounted) return;
    setState(() {
      _isSubmitting = false;
      _pin = '';
    });
  }

  Future<void> _onResend() async {
    if (_secondsRemaining > 0 || _isSubmitting) return;

    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.forgotPassword(widget.email);
    if (!mounted) return;

    if (success) {
      _startTimer();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Un nouveau code vous a été envoyé.'),
          backgroundColor: AppColors.success,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.errorMessage ?? "Échec de l'envoi"),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  String _formatTimer(int seconds) {
    final m = (seconds ~/ 60).toString().padLeft(2, '0');
    final s = (seconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  String get _title => widget.purpose == OtpPurpose.activation
      ? 'Activez votre compte'
      : 'Vérifiez votre email';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text('Kola', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.marginMobile,
          ),
          child: Column(
            children: [
              const SizedBox(height: AppSpacing.xl),
              Text(
                _title,
                textAlign: TextAlign.center,
                style: AppTypography.displayLgMobile,
              ),
              const SizedBox(height: AppSpacing.md),
              Text(
                'Saisissez le code à 6 chiffres envoyé à',
                textAlign: TextAlign.center,
                style: AppTypography.bodyMd,
              ),
              Text(
                _maskedEmail,
                textAlign: TextAlign.center,
                style: AppTypography.bodyMdBold,
              ),

              const Spacer(),

              // --- Dots PIN ---
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(_pinLength, (index) {
                  final filled = index < _pin.length;
                  return Container(
                    margin: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.sm,
                    ),
                    width: 14,
                    height: 14,
                    decoration: BoxDecoration(
                      color: _errorMessage != null
                          ? AppColors.error
                          : filled
                          ? AppColors.primary
                          : AppColors.surfaceVariant,
                      shape: BoxShape.circle,
                    ),
                  );
                }),
              ),
              const SizedBox(height: AppSpacing.lg),

              // --- Feedback : chargement, erreur, ou timer de renvoi ---
              SizedBox(height: 72, child: Center(child: _buildFeedback())),

              const Spacer(),

              // --- Numpad custom ---
              _Keypad(
                onDigit: _onDigitPressed,
                onBackspace: _onBackspace,
                enabled: !_isSubmitting,
              ),
              const SizedBox(height: AppSpacing.lg),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFeedback() {
    if (_isSubmitting) {
      return const SizedBox(
        width: 24,
        height: 24,
        child: CircularProgressIndicator(strokeWidth: 2),
      );
    }

    if (_errorMessage != null) {
      return Text(
        _errorMessage!,
        textAlign: TextAlign.center,
        style: AppTypography.bodySm.copyWith(color: AppColors.error),
      );
    }

    if (!_canResend) {
      // Pas de route de renvoi pour l'activation : on annonce au moins la
      // durée de validité du code (Token.expiresAt = now + 15 min).
      return Text(
        'Le code est valable 15 minutes.',
        textAlign: TextAlign.center,
        style: AppTypography.bodySm,
      );
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text('Code non reçu ?', style: AppTypography.bodySm),
        const SizedBox(height: AppSpacing.xxs),
        GestureDetector(
          onTap: _onResend,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Renvoyer le code',
                style: AppTypography.bodyMdBold.copyWith(
                  color: _secondsRemaining == 0
                      ? AppColors.primary
                      : AppColors.outline,
                ),
              ),
              const SizedBox(width: AppSpacing.xs),
              Text(
                '(${_formatTimer(_secondsRemaining)})',
                style: AppTypography.bodySm,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Keypad extends StatelessWidget {
  final ValueChanged<String> onDigit;
  final VoidCallback onBackspace;
  final bool enabled;

  const _Keypad({
    required this.onDigit,
    required this.onBackspace,
    this.enabled = true,
  });

  @override
  Widget build(BuildContext context) {
    const rows = [
      ['1', '2', '3'],
      ['4', '5', '6'],
      ['7', '8', '9'],
    ];

    return Opacity(
      opacity: enabled ? 1 : 0.4,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final row in rows)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: row
                    .map(
                      (digit) => _KeypadButton(
                        label: digit,
                        onTap: enabled ? () => onDigit(digit) : null,
                      ),
                    )
                    .toList(),
              ),
            ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                const SizedBox(width: 64, height: 64),
                _KeypadButton(
                  label: '0',
                  onTap: enabled ? () => onDigit('0') : null,
                ),
                _KeypadButton(
                  icon: Icons.backspace_outlined,
                  onTap: enabled ? onBackspace : null,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _KeypadButton extends StatelessWidget {
  final String? label;
  final IconData? icon;
  final VoidCallback? onTap;

  const _KeypadButton({this.label, this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 64,
      height: 64,
      child: Material(
        color: Colors.transparent,
        shape: const CircleBorder(),
        child: InkWell(
          onTap: onTap,
          customBorder: const CircleBorder(),
          child: Center(
            child: icon != null
                ? Icon(icon, color: AppColors.onSurfaceVariant, size: 24)
                : Text(label!, style: AppTypography.headingMd),
          ),
        ),
      ),
    );
  }
}
