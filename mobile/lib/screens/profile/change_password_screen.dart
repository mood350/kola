import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/primary_button.dart';
import '../../providers/user_provider.dart';

/// Changement de mot de passe depuis l'application (utilisateur connecté).
/// Distinct du flux « mot de passe oublié », qui passe par un OTP email.
class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _currentController = TextEditingController();
  final _newController = TextEditingController();
  final _confirmController = TextEditingController();

  bool _obscureCurrent = true;
  bool _obscureNew = true;
  String? _newError;
  String? _confirmError;

  /// Même règle que l'inscription côté backend (RegistrationRequest).
  static final _passwordPattern = RegExp(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$');

  @override
  void dispose() {
    _currentController.dispose();
    _newController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _newError = null;
      _confirmError = null;
    });

    final newPassword = _newController.text;

    if (newPassword.length < 8) {
      setState(() => _newError = 'Au moins 8 caractères');
      return;
    }
    if (!_passwordPattern.hasMatch(newPassword)) {
      setState(
        () => _newError =
            'Doit contenir une majuscule, une minuscule et un chiffre',
      );
      return;
    }
    if (newPassword != _confirmController.text) {
      setState(() => _confirmError = 'Les mots de passe ne correspondent pas');
      return;
    }

    final provider = context.read<UserProvider>();
    final success = await provider.changePassword(
      currentPassword: _currentController.text,
      newPassword: newPassword,
    );

    if (!mounted) return;
    if (success) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Mot de passe modifié')));
      Navigator.of(context).pop();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.errorMessage ?? 'Échec du changement')),
      );
    }
  }

  InputDecoration _decoration({
    required String hint,
    String? errorText,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      hintText: hint,
      errorText: errorText,
      suffixIcon: suffixIcon,
      filled: true,
      fillColor: AppColors.surfaceContainerLow,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppRadius.md),
        borderSide: BorderSide.none,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isSubmitting = context.watch<UserProvider>().isSubmitting;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Changer le mot de passe', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Mot de passe actuel', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _currentController,
                obscureText: _obscureCurrent,
                decoration: _decoration(
                  hint: 'Votre mot de passe actuel',
                  suffixIcon: IconButton(
                    icon: Icon(
                      _obscureCurrent
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                      color: AppColors.onSurfaceVariant,
                    ),
                    onPressed: () =>
                        setState(() => _obscureCurrent = !_obscureCurrent),
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),

              Text('Nouveau mot de passe', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _newController,
                obscureText: _obscureNew,
                decoration: _decoration(
                  hint: '8 caractères minimum',
                  errorText: _newError,
                  suffixIcon: IconButton(
                    icon: Icon(
                      _obscureNew
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                      color: AppColors.onSurfaceVariant,
                    ),
                    onPressed: () => setState(() => _obscureNew = !_obscureNew),
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),

              Text(
                'Confirmer le nouveau mot de passe',
                style: AppTypography.bodySm,
              ),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _confirmController,
                obscureText: _obscureNew,
                decoration: _decoration(
                  hint: 'Ressaisissez le nouveau mot de passe',
                  errorText: _confirmError,
                ),
              ),
              const SizedBox(height: AppSpacing.xl),

              PrimaryButton(
                label: 'Modifier',
                isLoading: isSubmitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
