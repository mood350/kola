import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/kola_avatar.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/kyc_tier_badge.dart';
import '../../core/widgets/secondary_button.dart';
import '../../core/widgets/skeleton_box.dart';
import '../../providers/auth_provider.dart';
import '../../providers/beneficiary_provider.dart';
import '../../providers/credit_provider.dart';
import '../../providers/merchant_provider.dart';
import '../../providers/notification_provider.dart';
import '../../providers/scheduled_transfer_provider.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/user_provider.dart';
import '../../providers/vault_provider.dart';
import '../../providers/wallet_provider.dart';
import '../../routes/app_routes.dart';
import '../scheduled/scheduled_transfers_screen.dart';
import 'change_password_screen.dart';
import 'edit_profile_screen.dart';
import 'manage_beneficiaries_screen.dart';

/// Onglet Profile : identité, avatar, réglages du compte, sécurité, déconnexion.
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<UserProvider>().loadMe();
    });
  }

  String _kycLabel(String tier) {
    switch (tier) {
      case 'TIER_0':
        return 'NIVEAU 0';
      case 'TIER_1':
        return 'NIVEAU 1';
      case 'TIER_2':
        return 'NIVEAU 2';
      case 'TIER_3':
        return 'NIVEAU 3';
      default:
        return 'NIVEAU 0';
    }
  }

  KycBadgeStatus _kycStatus(String tier) {
    return tier == 'TIER_0' ? KycBadgeStatus.pending : KycBadgeStatus.verified;
  }

  /// Plafond journalier associé au niveau KYC (cf. TransactionPolicy backend).
  String _kycLimit(String tier) {
    switch (tier) {
      case 'TIER_0':
        return '50 000 XOF / jour';
      case 'TIER_1':
        return '200 000 XOF / jour';
      case 'TIER_2':
        return '1 000 000 XOF / jour';
      case 'TIER_3':
        return 'Plafond étendu';
      default:
        return '—';
    }
  }

  Future<void> _confirmLogout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Se déconnecter ?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Déconnexion'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    await context.read<AuthProvider>().logout();
    if (!mounted) return;

    // Fan-out : AuthProvider reste pur "auth", chaque provider nettoie
    // lui-même son état pour ne pas garder les données du user précédent.
    context.read<UserProvider>().clear();
    context.read<WalletProvider>().clear();
    context.read<BeneficiaryProvider>().clear();
    context.read<VaultProvider>().clear();
    context.read<CreditProvider>().clear();
    context.read<TransactionProvider>().clear();
    context.read<NotificationProvider>().clear();
    context.read<MerchantProvider>().clear();
    context.read<ScheduledTransferProvider>().clear();

    if (!mounted) return;
    Navigator.of(
      context,
    ).pushNamedAndRemoveUntil(AppRoutes.login, (route) => false);
  }

  void _open(Widget screen) {
    Navigator.push(context, MaterialPageRoute(builder: (_) => screen));
  }

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final user = userProvider.user;
    final kycLevel = user?.kycLevel ?? 'TIER_0';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Profil', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => context.read<UserProvider>().loadMe(),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(AppSpacing.marginMobile),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Le bouton de déconnexion (plus bas) doit rester accessible
                // même si le profil n'a pas pu être chargé (erreur réseau,
                // session invalide, etc.) — ne jamais bloquer l'utilisateur.
                if (userProvider.status == UserLoadStatus.loading &&
                    user == null)
                  _buildSkeleton()
                else if (userProvider.status == UserLoadStatus.error &&
                    user == null)
                  Padding(
                    padding: const EdgeInsets.symmetric(
                      vertical: AppSpacing.xl,
                    ),
                    child: Column(
                      children: [
                        Text(
                          userProvider.errorMessage ??
                              'Impossible de charger le profil',
                          style: AppTypography.bodyMd,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        TextButton(
                          onPressed: () =>
                              context.read<UserProvider>().loadMe(),
                          child: const Text('Réessayer'),
                        ),
                      ],
                    ),
                  )
                else ...[
                  // --- Carte identité ---
                  KolaCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            KolaAvatar(
                              avatarId: user?.avatar,
                              radius: 28,
                              onTap: () => _open(const EditProfileScreen()),
                            ),
                            const SizedBox(width: AppSpacing.md),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    user?.fullName ?? '—',
                                    style: AppTypography.headingSm,
                                  ),
                                  const SizedBox(height: AppSpacing.xxs),
                                  KycTierBadge(
                                    label: _kycLabel(kycLevel),
                                    status: _kycStatus(kycLevel),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: AppSpacing.lg),
                        _InfoRow(
                          icon: Icons.email_outlined,
                          label: user?.email ?? '—',
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        _InfoRow(
                          icon: Icons.phone_outlined,
                          label: user?.phoneNumber ?? '—',
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // --- Mon compte ---
                  Text('Mon compte', style: AppTypography.headingSm),
                  const SizedBox(height: AppSpacing.sm),
                  _MenuTile(
                    icon: Icons.person_outline_rounded,
                    title: 'Modifier mes informations',
                    subtitle: 'Nom, téléphone, avatar',
                    onTap: () => _open(const EditProfileScreen()),
                  ),
                  _MenuTile(
                    icon: Icons.lock_outline_rounded,
                    title: 'Changer mon mot de passe',
                    onTap: () => _open(const ChangePasswordScreen()),
                  ),
                  _MenuTile(
                    icon: Icons.contacts_outlined,
                    title: 'Mes bénéficiaires',
                    subtitle: 'Ajouter ou supprimer un destinataire',
                    onTap: () => _open(const ManageBeneficiariesScreen()),
                  ),
                  _MenuTile(
                    icon: Icons.schedule_rounded,
                    title: 'Virements programmés',
                    subtitle: 'Automatiser mon épargne',
                    onTap: () => _open(const ScheduledTransfersScreen()),
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // --- Sécurité ---
                  Text('Sécurité', style: AppTypography.headingSm),
                  const SizedBox(height: AppSpacing.sm),
                  KolaCard(
                    padding: const EdgeInsets.all(AppSpacing.md),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _SecurityRow(
                          label: 'Niveau de vérification',
                          value: _kycLabel(kycLevel),
                        ),
                        _SecurityRow(
                          label: 'Plafond journalier',
                          value: _kycLimit(kycLevel),
                        ),
                        _SecurityRow(
                          label: 'Dernière adresse IP',
                          value: user?.lastKnownIp ?? 'Inconnue',
                        ),
                        _SecurityRow(
                          label: 'Dernier appareil',
                          value: user?.lastKnownUserAgent ?? 'Inconnu',
                        ),
                        if (user?.createdAt != null)
                          _SecurityRow(
                            label: 'Membre depuis',
                            value:
                                '${user!.createdAt!.day.toString().padLeft(2, '0')}/'
                                '${user.createdAt!.month.toString().padLeft(2, '0')}/'
                                '${user.createdAt!.year}',
                          ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: AppSpacing.xl),
                SecondaryButton(
                  label: 'Déconnexion',
                  icon: Icons.logout_rounded,
                  onPressed: _confirmLogout,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSkeleton() {
    return KolaCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const SkeletonBox(
                width: 56,
                height: 56,
                borderRadius: BorderRadius.all(Radius.circular(999)),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SkeletonBox(height: 18, width: 140),
                    const SizedBox(height: AppSpacing.xxs),
                    SkeletonBox(
                      height: 20,
                      width: 90,
                      borderRadius: BorderRadius.circular(AppRadius.full),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.lg),
          const SkeletonBox(height: 16, width: 180),
          const SizedBox(height: AppSpacing.sm),
          const SkeletonBox(height: 16, width: 140),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;

  const _InfoRow({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 18, color: AppColors.onSurfaceVariant),
        const SizedBox(width: AppSpacing.xs),
        Expanded(child: Text(label, style: AppTypography.bodyMd)),
      ],
    );
  }
}

/// Ligne de menu cliquable du profil.
class _MenuTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final VoidCallback onTap;

  const _MenuTile({
    required this.icon,
    required this.title,
    this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: KolaCard(
        padding: const EdgeInsets.all(AppSpacing.md),
        onTap: onTap,
        child: Row(
          children: [
            Icon(icon, color: AppColors.primary),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: AppTypography.bodyMdBold),
                  if (subtitle != null)
                    Text(subtitle!, style: AppTypography.bodySm),
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right_rounded,
              color: AppColors.onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }
}

class _SecurityRow extends StatelessWidget {
  final String label;
  final String value;

  const _SecurityRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(flex: 2, child: Text(label, style: AppTypography.bodySm)),
          Expanded(
            flex: 3,
            child: Text(
              value,
              style: AppTypography.bodySm.copyWith(
                color: AppColors.onSurface,
                fontWeight: FontWeight.w600,
              ),
              textAlign: TextAlign.end,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
