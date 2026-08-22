import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/kola_avatar.dart';
import '../../core/widgets/primary_button.dart';
import '../../providers/user_provider.dart';

/// Modification des informations personnelles + choix de l'avatar.
/// L'email n'est pas modifiable : il sert d'identifiant de connexion.
class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  late final TextEditingController _firstNameController;
  late final TextEditingController _lastNameController;
  late final TextEditingController _phoneController;
  String? _avatarId;
  String? _phoneError;

  static final _phonePattern = RegExp(r'^\+[1-9]\d{6,14}$');

  @override
  void initState() {
    super.initState();
    final user = context.read<UserProvider>().user;
    _firstNameController = TextEditingController(text: user?.firstName ?? '');
    _lastNameController = TextEditingController(text: user?.lastName ?? '');
    _phoneController = TextEditingController(text: user?.phoneNumber ?? '');
    _avatarId = user?.avatar;
  }

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _phoneError = null);

    if (_firstNameController.text.trim().isEmpty ||
        _lastNameController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le nom et le prénom sont obligatoires')),
      );
      return;
    }
    if (!_phonePattern.hasMatch(_phoneController.text.trim())) {
      setState(
        () => _phoneError = 'Format international requis, ex: +22890000000',
      );
      return;
    }

    final provider = context.read<UserProvider>();
    final success = await provider.updateProfile(
      firstName: _firstNameController.text.trim(),
      lastName: _lastNameController.text.trim(),
      phoneNumber: _phoneController.text.trim(),
      avatar: _avatarId,
    );

    if (!mounted) return;
    if (success) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Profil mis à jour')));
      Navigator.of(context).pop();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ?? 'Échec de la mise à jour'),
        ),
      );
    }
  }

  InputDecoration _decoration(String hint, {String? errorText}) {
    return InputDecoration(
      hintText: hint,
      errorText: errorText,
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
        title: Text('Modifier mon profil', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(child: KolaAvatar(avatarId: _avatarId, radius: 40)),
              const SizedBox(height: AppSpacing.md),
              Text('Choisir un avatar', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xs),
              Wrap(
                spacing: AppSpacing.sm,
                runSpacing: AppSpacing.sm,
                children: KolaAvatars.ids.map((id) {
                  final selected = id == _avatarId;
                  return GestureDetector(
                    onTap: () => setState(() => _avatarId = id),
                    child: Container(
                      padding: const EdgeInsets.all(3),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: selected
                              ? AppColors.primary
                              : Colors.transparent,
                          width: 2,
                        ),
                      ),
                      child: KolaAvatar(avatarId: id, radius: 24),
                    ),
                  );
                }).toList(),
              ),
              const SizedBox(height: AppSpacing.lg),

              Text('Prénom', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _firstNameController,
                decoration: _decoration('Votre prénom'),
              ),
              const SizedBox(height: AppSpacing.md),

              Text('Nom', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _lastNameController,
                decoration: _decoration('Votre nom'),
              ),
              const SizedBox(height: AppSpacing.md),

              Text('Numéro de téléphone', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: _decoration('+22890000000', errorText: _phoneError),
              ),
              const SizedBox(height: AppSpacing.xl),

              PrimaryButton(
                label: 'Enregistrer',
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
