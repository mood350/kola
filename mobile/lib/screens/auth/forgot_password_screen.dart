import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/primary_button.dart';

/// Écran de demande de réinitialisation de mot de passe.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifierController = TextEditingController();
  bool _isLoading = false;
  bool _linkSent = false;

  @override
  void dispose() {
    _identifierController.dispose();
    super.dispose();
  }

  Future<void> _onSendLink() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);
    // TODO: appeler le backend pour envoyer le lien de réinitialisation.
    await Future.delayed(const Duration(milliseconds: 800));
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      _linkSent = true;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.marginMobile, vertical: AppSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // --- Bouton retour ---
              CircleAvatar(
                backgroundColor: AppColors.surfaceCard,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back_rounded, color: AppColors.onSurface),
                  onPressed: () => Navigator.pop(context),
                ),
              ),
              const SizedBox(height: AppSpacing.sectionV),

              // --- Icône ---
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: AppColors.primaryContainer,
                  borderRadius: BorderRadius.circular(AppRadius.lg),
                ),
                child: const Icon(Icons.lock_reset_rounded, color: Colors.white, size: 28),
              ),
              const SizedBox(height: AppSpacing.lg),

              Text('Mot de passe oublié', style: AppTypography.displayLgMobile),
              const SizedBox(height: AppSpacing.sm),
              Text(
                'Entrez votre email ou numéro de téléphone pour recevoir un lien de réinitialisation.',
                style: AppTypography.bodyMd,
              ),
              const SizedBox(height: AppSpacing.xl),

              // --- Card formulaire ---
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(AppDimens.cardPadding),
                decoration: BoxDecoration(
                  color: AppColors.surfaceCard,
                  borderRadius: BorderRadius.circular(AppRadius.lg),
                  border: Border.all(color: AppColors.hairlineLight),
                ),
                child: _linkSent
                    ? _SuccessMessage(identifier: _identifierController.text.trim())
                    : Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        'Email ou Numéro de téléphone',
                        style: AppTypography.bodySm.copyWith(fontWeight: FontWeight.w600, color: AppColors.onSurface),
                      ),
                      const SizedBox(height: AppSpacing.xs),
                      TextFormField(
                        controller: _identifierController,
                        keyboardType: TextInputType.emailAddress,
                        decoration: const InputDecoration(
                          hintText: 'nom@exemple.com ou +237...',
                          prefixIcon: Icon(Icons.mail_outline_rounded),
                        ),
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) return 'Ce champ est requis';
                          return null;
                        },
                      ),
                      const SizedBox(height: AppSpacing.lg),
                      PrimaryButton(
                        label: 'Envoyer le lien',
                        icon: Icons.arrow_forward_rounded,
                        isLoading: _isLoading,
                        onPressed: _onSendLink,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.lg),

              Center(
                child: GestureDetector(
                  onTap: () => Navigator.pop(context),
                  child: Text(
                    'Je me souviens de mon mot de passe',
                    style: AppTypography.bodyMdBold.copyWith(color: AppColors.primary),
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

class _SuccessMessage extends StatelessWidget {
  final String identifier;
  const _SuccessMessage({required this.identifier});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const Icon(Icons.mark_email_read_outlined, color: AppColors.success, size: 40),
        const SizedBox(height: AppSpacing.md),
        Text(
          'Lien envoyé !',
          style: AppTypography.headingSm,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: AppSpacing.xs),
        Text(
          'Vérifiez "$identifier" pour réinitialiser votre mot de passe.',
          style: AppTypography.bodyMd,
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}