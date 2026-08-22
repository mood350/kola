import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/primary_button.dart';
import '../../providers/auth_provider.dart';
import '../../routes/app_routes.dart';
import 'otp_verification_screen.dart';

/// Écran d'inscription : étape 1/3 (infos de base).
///
/// Champs exactement alignés sur RegistrationRequest (backend) :
/// firstname, lastname, email, phoneNumber (format international,
/// ex: +22890000000), countryCode (ISO 3166-1 alpha-2, ex: "TG"), password
/// (8+ caractères, au moins 1 majuscule, 1 minuscule, 1 chiffre).
///
/// ⚠️ Le backend renvoie 202 Accepted SANS tokens : un code à 6 chiffres est
/// envoyé par email, et l'utilisateur doit activer son compte avant de se
/// connecter. Donc après inscription, on redirige vers l'écran de saisie du
/// code (OtpVerificationScreen), pas vers Home.
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

/// Liste réduite de pays UEMOA/cible pour le sélecteur de code pays.
const _countryOptions = [
  (code: 'TG', dialCode: '+228', label: 'Togo (+228)'),
  (code: 'SN', dialCode: '+221', label: 'Sénégal (+221)'),
  (code: 'CI', dialCode: '+225', label: "Côte d'Ivoire (+225)"),
  (code: 'GH', dialCode: '+233', label: 'Ghana (+233)'),
  (code: 'NG', dialCode: '+234', label: 'Nigeria (+234)'),
];

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _firstnameController = TextEditingController();
  final _lastnameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController(); // sans le code pays
  final _passwordController = TextEditingController();

  bool _obscurePassword = true;
  bool _acceptedTerms = false;
  String _selectedCountryCode = _countryOptions.first.code;

  String get _selectedDialCode => _countryOptions
      .firstWhere((c) => c.code == _selectedCountryCode)
      .dialCode;

  @override
  void dispose() {
    _firstnameController.dispose();
    _lastnameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _onCreateAccount() async {
    if (!_formKey.currentState!.validate()) return;

    if (!_acceptedTerms) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Veuillez accepter les conditions générales pour continuer.',
          ),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    // Construit le numéro au format international attendu par le backend :
    // ^\+[1-9]\d{6,14}$  (ex: +22890000000)
    final rawPhone = _phoneController.text.trim().replaceAll(RegExp(r'\D'), '');
    final fullPhoneNumber = '$_selectedDialCode$rawPhone';

    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.register(
      firstname: _firstnameController.text.trim(),
      lastname: _lastnameController.text.trim(),
      email: _emailController.text.trim(),
      phoneNumber: fullPhoneNumber,
      countryCode: _selectedCountryCode,
      password: _passwordController.text,
    );

    if (!mounted) return;

    if (success) {
      // Pas de tokens à ce stade : le compte doit être activé par code email.
      _showConfirmationDialog();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.errorMessage ?? "Échec de l'inscription"),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  void _showConfirmationDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
        ),
        icon: const Icon(
          Icons.mark_email_read_outlined,
          color: AppColors.success,
          size: 40,
        ),
        title: Text(
          'Vérifiez votre boîte mail',
          style: AppTypography.headingSm,
          textAlign: TextAlign.center,
        ),
        content: Text(
          'Un code à 6 chiffres a été envoyé à ${_emailController.text.trim()}. '
          'Saisissez-le pour activer votre compte : il est valable 15 minutes.',
          style: AppTypography.bodyMd,
          textAlign: TextAlign.center,
        ),
        actions: [
          SizedBox(
            width: double.infinity,
            child: PrimaryButton(
              label: 'Saisir le code',
              onPressed: () {
                Navigator.pop(context); // ferme le dialog
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(
                    builder: (_) => OtpVerificationScreen(
                      email: _emailController.text.trim(),
                      purpose: OtpPurpose.activation,
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
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.marginMobile,
            vertical: AppSpacing.lg,
          ),
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

                  Row(
                    children: const [
                      _StepBar(active: true),
                      SizedBox(width: AppSpacing.xs),
                      _StepBar(active: false),
                      SizedBox(width: AppSpacing.xs),
                      _StepBar(active: false),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xl),

                  // --- Prénom + Nom ---
                  Row(
                    children: [
                      Expanded(
                        child: _LabeledField(
                          label: 'First Name',
                          controller: _firstnameController,
                          hint: 'Jane',
                          validator: (v) =>
                              (v == null || v.trim().isEmpty) ? 'Requis' : null,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: _LabeledField(
                          label: 'Last Name',
                          controller: _lastnameController,
                          hint: 'Doe',
                          validator: (v) =>
                              (v == null || v.trim().isEmpty) ? 'Requis' : null,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),

                  // --- Email ---
                  _LabeledField(
                    label: 'Email',
                    controller: _emailController,
                    hint: 'name@example.com',
                    keyboardType: TextInputType.emailAddress,
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) {
                        return "L'email est obligatoire";
                      }
                      if (!v.contains('@')) return 'Email invalide';
                      return null;
                    },
                  ),
                  const SizedBox(height: AppSpacing.md),

                  // --- Pays + Téléphone ---
                  Text(
                    'Phone Number',
                    style: AppTypography.bodySm.copyWith(
                      fontWeight: FontWeight.w600,
                      color: AppColors.onSurface,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: AppSpacing.sm,
                        ),
                        height: AppDimens.inputHeight,
                        decoration: BoxDecoration(
                          border: Border.all(color: AppColors.hairlineLight),
                          borderRadius: BorderRadius.circular(AppRadius.md),
                        ),
                        child: DropdownButtonHideUnderline(
                          child: DropdownButton<String>(
                            value: _selectedCountryCode,
                            items: _countryOptions
                                .map(
                                  (c) => DropdownMenuItem(
                                    value: c.code,
                                    child: Text(c.dialCode),
                                  ),
                                )
                                .toList(),
                            onChanged: (value) {
                              if (value != null) {
                                setState(() => _selectedCountryCode = value);
                              }
                            },
                          ),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      Expanded(
                        child: TextFormField(
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          decoration: const InputDecoration(
                            hintText: '90 00 00 00',
                          ),
                          validator: (v) {
                            if (v == null || v.trim().isEmpty) return 'Requis';
                            final digits = v.replaceAll(RegExp(r'\D'), '');
                            if (digits.length < 6) return 'Numéro trop court';
                            return null;
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),

                  // --- Mot de passe ---
                  Text(
                    'Password',
                    style: AppTypography.bodySm.copyWith(
                      fontWeight: FontWeight.w600,
                      color: AppColors.onSurface,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      hintText: '••••••••',
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                        onPressed: () => setState(
                          () => _obscurePassword = !_obscurePassword,
                        ),
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.length < 8) {
                        return '8 caractères minimum';
                      }
                      final hasUpper = RegExp(r'[A-Z]').hasMatch(value);
                      final hasLower = RegExp(r'[a-z]').hasMatch(value);
                      final hasDigit = RegExp(r'\d').hasMatch(value);
                      if (!hasUpper || !hasLower || !hasDigit) {
                        return '1 majuscule, 1 minuscule, 1 chiffre requis';
                      }
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
                          onChanged: (value) =>
                              setState(() => _acceptedTerms = value ?? false),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      Expanded(
                        child: Wrap(
                          children: [
                            Text(
                              'I agree to the ',
                              style: AppTypography.bodyMd,
                            ),
                            Text(
                              'Terms & Conditions',
                              style: AppTypography.bodyMd.copyWith(
                                color: AppColors.primary,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            Text(' and ', style: AppTypography.bodyMd),
                            Text(
                              'Privacy Policy',
                              style: AppTypography.bodyMd.copyWith(
                                color: AppColors.primary,
                                fontWeight: FontWeight.w600,
                              ),
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
                      Text(
                        'Already have an account? ',
                        style: AppTypography.bodyMd,
                      ),
                      GestureDetector(
                        onTap: () => Navigator.pushReplacementNamed(
                          context,
                          AppRoutes.login,
                        ),
                        child: Text(
                          'Log In',
                          style: AppTypography.bodyMd.copyWith(
                            color: AppColors.primary,
                            fontWeight: FontWeight.w600,
                          ),
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

/// Champ texte avec label externe, factorisé pour éviter la répétition.
class _LabeledField extends StatelessWidget {
  final String label;
  final TextEditingController controller;
  final String hint;
  final TextInputType? keyboardType;
  final String? Function(String?)? validator;

  const _LabeledField({
    required this.label,
    required this.controller,
    required this.hint,
    this.keyboardType,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTypography.bodySm.copyWith(
            fontWeight: FontWeight.w600,
            color: AppColors.onSurface,
          ),
        ),
        const SizedBox(height: AppSpacing.xs),
        TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          decoration: InputDecoration(hintText: hint),
          validator: validator,
        ),
      ],
    );
  }
}

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
