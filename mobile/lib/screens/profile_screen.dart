import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';

class _Option {
  const _Option(this.key, this.icon, this.title, this.subtitle, {this.route});
  final String key;
  final IconData icon;
  final String title;
  final String subtitle;
  final KolaRoute? route;
}

const _options = <_Option>[
  _Option(
    'kyc',
    KolaIcons.shieldCheckmarkOutline,
    'Passer le KYC',
    'Vérifiez votre identité et passez au TIER 3',
    route: KolaRoute.kyc,
  ),
  _Option(
    'history',
    KolaIcons.timeOutline,
    'Historique du compte',
    'Connexions, activités et récompenses',
    route: KolaRoute.transactionHistory,
  ),
  _Option(
    'documents',
    KolaIcons.documentTextOutline,
    'Documents personnels',
    'Pièce d’identité et justificatifs',
  ),
  _Option(
    'faq',
    KolaIcons.helpCircleOutline,
    'FAQ et assistance',
    'Réponses aux questions fréquentes',
    route: KolaRoute.faq,
  ),
];

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.onNavigate,
    required this.onLogout,
  });

  final void Function(KolaRoute) onNavigate;
  final Future<void> Function() onLogout;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  String? _expanded;

  @override
  Widget build(BuildContext context) {
    final user = context.watch<KolaDataProvider>().user;

    return KolaScreen(
      route: KolaRoute.profile,
      onNavigate: widget.onNavigate,
      children: [
        Row(
          children: [
            Container(
              width: 64,
              height: 64,
              alignment: Alignment.center,
              decoration: const BoxDecoration(
                color: AppColors.primary,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                KolaIcons.person,
                size: 34,
                color: AppColors.white,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    user?.fullName ?? 'Chargement…',
                    style: AppTypography.screenTitle.copyWith(
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '🇹🇬 ${user?.phone ?? '+228'}',
                    style: AppTypography.caption,
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Pill(
                        user?.phoneVerified == true
                            ? 'Compte vérifié'
                            : 'À vérifier',
                        green: user?.phoneVerified == true,
                      ),
                      const SizedBox(width: 5),
                      Pill(user?.tierLabel ?? 'TIER 0'),
                    ],
                  ),
                ],
              ),
            ),
            Container(
              width: 38,
              height: 38,
              alignment: Alignment.center,
              decoration: const BoxDecoration(
                color: AppColors.pale2,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                KolaIcons.createOutline,
                size: 19,
                color: AppColors.primary,
              ),
            ),
          ],
        ),
        Padding(
          padding: const EdgeInsets.only(top: AppSpacing.lg, bottom: 11),
          child: Text(
            'Mon profil',
            style: AppTypography.section.copyWith(fontSize: 19),
          ),
        ),
        for (final option in _options)
          Padding(
            padding: const EdgeInsets.only(bottom: AppSpacing.xs),
            child: _OptionTile(
              option: option,
              expanded: _expanded == option.key,
              tier: user?.kycTier ?? 'TIER_0',
              onTap: () {
                final route = option.route;
                if (route != null) {
                  widget.onNavigate(route);
                  return;
                }
                setState(
                  () => _expanded = _expanded == option.key ? null : option.key,
                );
              },
            ),
          ),
        const SizedBox(height: 18),
        GestureDetector(
          onTap: () => widget.onNavigate(KolaRoute.credit),
          child: Container(
            constraints: const BoxConstraints(minHeight: 54),
            padding: const EdgeInsets.symmetric(horizontal: 17),
            decoration: BoxDecoration(
              color: AppColors.primary,
              borderRadius: BorderRadius.circular(27),
            ),
            child: Row(
              children: [
                const Icon(
                  KolaIcons.cardOutline,
                  size: 20,
                  color: AppColors.yellow,
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: Text(
                    'Voir mes crédits et mes prêts',
                    style: AppTypography.button.copyWith(
                      fontSize: 12,
                      color: AppColors.white,
                    ),
                  ),
                ),
                const Icon(
                  KolaIcons.arrowForward,
                  size: 18,
                  color: AppColors.white,
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        GestureDetector(
          onTap: _confirmLogout,
          child: Container(
            height: 52,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: const Color(0xFFFFF8F8),
              borderRadius: BorderRadius.circular(26),
              border: Border.all(color: const Color(0xFFF0B8B8)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  KolaIcons.logOutOutline,
                  size: 20,
                  color: AppColors.red,
                ),
                const SizedBox(width: 8),
                Text(
                  'Se déconnecter',
                  style: AppTypography.button.copyWith(
                    fontSize: 12,
                    color: AppColors.red,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _confirmLogout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Déconnexion', style: AppTypography.cardTitle),
        content: Text(
          'Voulez-vous vraiment vous déconnecter ?',
          style: AppTypography.small,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(
              'Se déconnecter',
              style: AppTypography.button.copyWith(color: AppColors.red),
            ),
          ),
        ],
      ),
    );
    if (confirmed == true) await widget.onLogout();
  }
}

class _OptionTile extends StatelessWidget {
  const _OptionTile({
    required this.option,
    required this.expanded,
    required this.tier,
    required this.onTap,
  });

  final _Option option;
  final bool expanded;
  final String tier;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          children: [
            Row(
              children: [
                IconCircle(option.icon),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        option.title,
                        style: AppTypography.badge.copyWith(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        option.subtitle,
                        style: AppTypography.caption.copyWith(fontSize: 9),
                      ),
                    ],
                  ),
                ),
                Icon(
                  expanded ? KolaIcons.chevronUp : KolaIcons.chevronForward,
                  size: 18,
                  color: AppColors.muted,
                ),
              ],
            ),
            if (expanded)
              Padding(
                padding: const EdgeInsets.only(left: 53, top: 9),
                child: Container(
                  width: double.infinity,
                  padding: const EdgeInsets.only(top: 9),
                  decoration: const BoxDecoration(
                    border: Border(top: BorderSide(color: AppColors.border)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: _details(),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  List<Widget> _details() {
    // Seuls les documents ont un volet dépliable : les autres entrées
    // conduisent à un écran dédié.
    if (option.key != 'documents') return const [];

    final tierNumber = int.tryParse(tier.split('_').last) ?? 0;
    return [
      const _DetailLine(
        KolaIcons.checkmarkCircleOutline,
        'Identité et téléphone vérifiés',
      ),
      const SizedBox(height: 8),
      _DetailLine(
        tierNumber >= 3
            ? KolaIcons.checkmarkCircleOutline
            : KolaIcons.addCircleOutline,
        tierNumber >= 3
            ? 'Justificatif de domicile validé'
            : 'Justificatif de domicile requis pour le TIER 3',
      ),
    ];
  }
}

class _DetailLine extends StatelessWidget {
  const _DetailLine(this.icon, this.text);

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 16, color: AppColors.greenDark),
        const SizedBox(width: 7),
        Expanded(
          child: Text(text, style: AppTypography.caption.copyWith(fontSize: 9)),
        ),
      ],
    );
  }
}
