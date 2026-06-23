import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/primary_button.dart';
import '../../providers/auth_provider.dart';
import '../../routes/app_routes.dart';

/// Écran d'inscription : étape 1/3 (Nom complet, email/téléphone, mot de passe).
/// Les étapes 2 et 3 correspondent au flow de vérification KYC (écrans séparés).
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _fullNameController = TextEditingController();
  final _identifierController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  bool _acceptedTerms = false;

  @override
  void dispose() {
    _fullNameController.dispose();
    _identifierController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _onCreateAccount() async {
    if (!_formKey.currentState!.validate()) return;

    if (!_acceptedTerms) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez accepter les conditions générales pour continuer.'),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.register(
      _fullNameController.text.trim(),
      _identifierController.text.trim(),
      _passwordController.text,
    );

    if (!mounted) return;

    if (success) {
      // Étape suivante : vérification OTP du numéro de téléphone.
      Navigator.pushNamed(
        context,
        AppRoutes.otpVerification,
        arguments: _identifierController.text.trim(),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.errorMessage ?? "Échec de l'inscription"),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final isLoading = authProvider.status == AuthStatus.loading;

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
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.marginMobile, vertical: AppSpacing.lg),
          child: Container(
            padding: const EdgeInsets.all(AppDimens.cardPadding),
            decoration: BoxDecoration(
              color: AppColors.surfaceCard,
              borderRadius: BorderRadius.circular(AppRadius.lg),
              border: Border.all(color: AppColors.hairlineLight),
            ),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('Create Account', style: AppTypography.displayLgMobile),
                  const SizedBox(height: AppSpacing.xs),
                  Text('Step 1 of 3', style: AppTypography.bodySm),
                  const SizedBox(height: AppSpacing.sm),

                  // --- Barre de progression (3 étapes) ---
                  Row(
                    children: [
                      _StepBar(active: true),
                      const SizedBox(width: AppSpacing.xs),
                      _StepBar(active: false),
                      const SizedBox(width: AppSpacing.xs),
                      _StepBar(active: false),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xl),

                  // --- Nom complet ---
                  Text('Full Name', style: AppTypography.bodySm.copyWith(fontWeight: FontWeight.w600, color: AppColors.onSurface)),
                  const SizedBox(height: AppSpacing.xs),
                  TextFormField(
                    controller: _fullNameController,
                    decoration: const InputDecoration(hintText: 'Jane Doe'),
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) return 'Le nom complet est requis';
                      return null;
                    },
                  ),
                  const SizedBox(height: AppSpacing.md),

                  // --- Email ou téléphone ---
                  Text('Email or Phone', style: AppTypography.bodySm.copyWith(fontWeight: FontWeight.w600, color: AppColors.onSurface)),
                  const SizedBox(height: AppSpacing.xs),
                  TextFormField(
                    controller: _identifierController,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(hintText: 'name@example.com'),
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) return 'Ce champ est requis';
                      return null;
                    },
                  ),
                  const SizedBox(height: AppSpacing.md),

                  // --- Mot de passe ---
                  Text('Password', style: AppTypography.bodySm.copyWith(fontWeight: FontWeight.w600, color: AppColors.onSurface)),
                  const SizedBox(height: AppSpacing.xs),
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      hintText: '••••••••',
                      suffixIcon: IconButton(
                        icon: Icon(_obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                        onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.length < 8) return '8 caractères minimum';
                      return null;
                    },
                  ),
                  const SizedBox(height: AppSpacing.md),

                  // --- Checkbox conditions ---
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 24,
                        height: 24,
                        child: Checkbox(
                          value: _acceptedTerms,
                          onChanged: (value) => setState(() => _acceptedTerms = value ?? false),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      Expanded(
                        child: Wrap(
                          children: [
                            Text('I agree to the ', style: AppTypography.bodyMd),
                            Text(
                              'Terms & Conditions',
                              style: AppTypography.bodyMd.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600),
                            ),
                            Text(' and ', style: AppTypography.bodyMd),
                            Text(
                              'Privacy Policy',
                              style: AppTypography.bodyMd.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600),
                            ),
                            Text('.', style: AppTypography.bodyMd),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xl),

                  PrimaryButton(
                    label: 'Create Account',
                    isLoading: isLoading,
                    onPressed: _onCreateAccount,
                  ),
                  const SizedBox(height: AppSpacing.md),

                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Already have an account? ', style: AppTypography.bodyMd),
                      GestureDetector(
                        onTap: () => Navigator.pushReplacementNamed(context, AppRoutes.login),
                        child: Text(
                          'Log In',
                          style: AppTypography.bodyMd.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Petite barre de progression utilisée pour le stepper "Step X of 3".
class _StepBar extends StatelessWidget {
  final bool active;
  const _StepBar({required this.active});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        height: 4,
        decoration: BoxDecoration(
          color: active ? AppColors.primary : AppColors.surfaceContainerHigh,
          borderRadius: BorderRadius.circular(AppRadius.full),
        ),
      ),
    );
  }
}