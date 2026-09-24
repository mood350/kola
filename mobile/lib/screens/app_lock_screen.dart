import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';
import 'package:local_auth/local_auth.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_typography.dart';
import '../core/widgets/code_input.dart';
import '../models/user.dart';
import '../services/auth_service.dart';

/// Écran de déverrouillage affiché au retour d'arrière-plan, ou au démarrage
/// quand une session est déjà enregistrée.
///
/// Le PIN est revalidé auprès du backend plutôt que comparé localement : rien
/// de comparable n'est stocké sur l'appareil, et la session repart avec des
/// jetons frais.
class AppLockScreen extends StatefulWidget {
  const AppLockScreen({
    super.key,
    required this.user,
    required this.onUnlock,
    required this.onLogout,
  });

  final KolaUser user;
  final Future<void> Function(AuthSession? session) onUnlock;
  final Future<void> Function() onLogout;

  @override
  State<AppLockScreen> createState() => _AppLockScreenState();
}

class _AppLockScreenState extends State<AppLockScreen> {
  final _pin = TextEditingController();
  final _auth = AuthService();
  final _localAuth = LocalAuthentication();

  bool _loading = false;
  bool _biometricAvailable = false;
  String _biometricLabel = 'Biométrie';

  @override
  void initState() {
    super.initState();
    _detectBiometrics();
  }

  @override
  void dispose() {
    _pin.dispose();
    super.dispose();
  }

  Future<void> _detectBiometrics() async {
    try {
      final supported = await _localAuth.isDeviceSupported();
      final enrolled = supported && await _localAuth.canCheckBiometrics;
      if (!enrolled || !mounted) return;

      final types = await _localAuth.getAvailableBiometrics();
      setState(() {
        _biometricAvailable = true;
        _biometricLabel = types.contains(BiometricType.face)
            ? 'Face ID'
            : types.contains(BiometricType.fingerprint)
            ? 'Empreinte digitale'
            : 'Biométrie';
      });
    } catch (_) {
      if (mounted) setState(() => _biometricAvailable = false);
    }
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

  Future<void> _unlockWithPin() async {
    // La saisie de la dernière case et le bouton appellent tous deux cette
    // méthode : sans ce garde-fou, un appui pendant la vérification enverrait
    // une seconde tentative de connexion.
    if (_loading) return;
    if (_pin.text.length != 4) {
      _alert('PIN incomplet', 'Saisissez votre code PIN à 4 chiffres.');
      return;
    }
    setState(() => _loading = true);
    final result = await _auth.login(widget.user.phone, _pin.text);
    if (!mounted) return;
    setState(() => _loading = false);
    if (result.session == null) {
      _pin.clear();
      _alert('Accès refusé', result.message);
      return;
    }
    await widget.onUnlock(result.session);
  }

  Future<void> _unlockWithBiometrics() async {
    setState(() => _loading = true);
    try {
      final ok = await _localAuth.authenticate(
        localizedReason: 'Déverrouiller KOLA',
        options: const AuthenticationOptions(stickyAuth: true),
      );
      if (!mounted) return;
      if (ok) await widget.onUnlock(null);
    } catch (_) {
      if (mounted) {
        _alert('Biométrie indisponible', 'Utilisez votre code PIN KOLA.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              const SizedBox(height: 52),
              Image.asset('assets/kola-logo-transparent.png', width: 125),
              const SizedBox(height: 35),
              Text(
                'Bonjour ${widget.user.firstName}',
                style: AppTypography.screenTitle.copyWith(fontSize: 27),
              ),
              const SizedBox(height: 7),
              Text(
                'Déverrouillez KOLA pour accéder à votre compte.',
                textAlign: TextAlign.center,
                style: AppTypography.small,
              ),
              const SizedBox(height: 38 + 29),
              Stack(
                clipBehavior: Clip.none,
                alignment: Alignment.topCenter,
                children: [
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.fromLTRB(20, 24, 20, 20),
                    decoration: BoxDecoration(
                      color: AppColors.white,
                      borderRadius: BorderRadius.circular(22),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          'CODE PIN',
                          style: AppTypography.fieldLabel,
                        ),
                        const SizedBox(height: 7),
                        CodeInput(
                          controller: _pin,
                          length: 4,
                          obscure: true,
                          autofocus: true,
                          // Le clavier numérique n'a pas de touche de
                          // validation : la quatrième case saisie déclenche le
                          // déverrouillage, comme le faisait « Entrée ».
                          onCompleted: _unlockWithPin,
                        ),
                        const SizedBox(height: 17),
                        GestureDetector(
                          onTap: _loading ? null : _unlockWithPin,
                          child: Opacity(
                            opacity: _loading ? 0.65 : 1,
                            child: Container(
                              height: 52,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                color: AppColors.yellow,
                                borderRadius: BorderRadius.circular(26),
                              ),
                              child: _loading
                                  ? const SizedBox(
                                      width: 22,
                                      height: 22,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2.4,
                                        color: AppColors.primary,
                                      ),
                                    )
                                  : Text(
                                      'Déverrouiller',
                                      style: AppTypography.button.copyWith(
                                        fontSize: 13,
                                        fontWeight: FontWeight.w900,
                                      ),
                                    ),
                            ),
                          ),
                        ),
                        if (_biometricAvailable) ...[
                          const SizedBox(height: 10),
                          GestureDetector(
                            onTap: _loading ? null : _unlockWithBiometrics,
                            child: Container(
                              height: 50,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                border: Border.all(color: AppColors.primary),
                                borderRadius: BorderRadius.circular(25),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(
                                    KolaIcons.fingerPrint,
                                    size: 25,
                                    color: AppColors.primary,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Utiliser $_biometricLabel',
                                    style: AppTypography.button.copyWith(
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  Positioned(
                    top: -29,
                    child: Container(
                      width: 58,
                      height: 58,
                      alignment: Alignment.center,
                      decoration: const BoxDecoration(
                        color: AppColors.primary,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        KolaIcons.lockClosed,
                        size: 28,
                        color: AppColors.yellow,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextButton(
                onPressed: () => widget.onLogout(),
                child: Text(
                  'Changer de compte',
                  style: AppTypography.smallBold.copyWith(
                    color: AppColors.muted,
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
