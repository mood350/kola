import 'dart:async';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../routes/app_routes.dart';

/// Écran de vérification du numéro par code OTP (6 chiffres).
/// Numpad custom (pas le clavier natif), avec dots visuels et timer de renvoi.
class OtpVerificationScreen extends StatefulWidget {
  final String phoneNumber;

  const OtpVerificationScreen({super.key, required this.phoneNumber});

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  static const int _pinLength = 6;
  String _pin = '';
  int _secondsRemaining = 59;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _startTimer();
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

  String get _maskedPhone {
    // Masque le numéro façon "+221 ••• •• 45"
    final raw = widget.phoneNumber;
    if (raw.length < 4) return raw;
    final visibleEnd = raw.substring(raw.length - 2);
    final prefix = raw.length > 6 ? raw.substring(0, 4) : raw.substring(0, 1);
    return '$prefix ••• •• $visibleEnd';
  }

  void _onDigitPressed(String digit) {
    if (_pin.length >= _pinLength) return;
    setState(() => _pin += digit);
    if (_pin.length == _pinLength) {
      _onSubmit();
    }
  }

  void _onBackspace() {
    if (_pin.isEmpty) return;
    setState(() => _pin = _pin.substring(0, _pin.length - 1));
  }

  Future<void> _onSubmit() async {
    // TODO: appeler le backend pour valider le code OTP saisi (_pin).
    await Future.delayed(const Duration(milliseconds: 400));
    if (!mounted) return;
    Navigator.pushNamedAndRemoveUntil(context, AppRoutes.home, (route) => false);
  }

  void _onResend() {
    if (_secondsRemaining > 0) return;
    // TODO: appeler le backend pour renvoyer le code OTP.
    _startTimer();
  }

  String _formatTimer(int seconds) {
    final m = (seconds ~/ 60).toString().padLeft(2, '0');
    final s = (seconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

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
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.marginMobile),
          child: Column(
            children: [
              const SizedBox(height: AppSpacing.xl),
              Text(
                'Vérifiez votre numéro',
                textAlign: TextAlign.center,
                style: AppTypography.displayLgMobile,
              ),
              const SizedBox(height: AppSpacing.md),
              Text(
                'Saisissez le code à 6 chiffres envoyé au',
                textAlign: TextAlign.center,
                style: AppTypography.bodyMd,
              ),
              Text(
                _maskedPhone,
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
                    margin: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                    width: 14,
                    height: 14,
                    decoration: BoxDecoration(
                      color: filled ? AppColors.primary : AppColors.surfaceVariant,
                      shape: BoxShape.circle,
                    ),
                  );
                }),
              ),
              const SizedBox(height: AppSpacing.xl),

              // --- Timer / renvoi ---
              Column(
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
                            color: _secondsRemaining == 0 ? AppColors.primary : AppColors.outline,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.xs),
                        Text('(${_formatTimer(_secondsRemaining)})', style: AppTypography.bodySm),
                      ],
                    ),
                  ),
                ],
              ),

              const Spacer(),

              // --- Numpad custom ---
              _Keypad(
                onDigit: _onDigitPressed,
                onBackspace: _onBackspace,
              ),
              const SizedBox(height: AppSpacing.lg),
            ],
          ),
        ),
      ),
    );
  }
}

class _Keypad extends StatelessWidget {
  final ValueChanged<String> onDigit;
  final VoidCallback onBackspace;

  const _Keypad({required this.onDigit, required this.onBackspace});

  @override
  Widget build(BuildContext context) {
    const rows = [
      ['1', '2', '3'],
      ['4', '5', '6'],
      ['7', '8', '9'],
    ];

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (final row in rows)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: row.map((digit) => _KeypadButton(label: digit, onTap: () => onDigit(digit))).toList(),
            ),
          ),
        Padding(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              const SizedBox(width: 64, height: 64),
              _KeypadButton(label: '0', onTap: () => onDigit('0')),
              _KeypadButton(
                icon: Icons.backspace_outlined,
                onTap: onBackspace,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _KeypadButton extends StatelessWidget {
  final String? label;
  final IconData? icon;
  final VoidCallback onTap;

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