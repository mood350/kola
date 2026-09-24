import 'dart:ui';

import 'package:flutter/material.dart';
import '../theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../../providers/kola_data_provider.dart';
import '../../routes/kola_route.dart';
import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';
import '../theme/app_typography.dart';

/// Bandeau supérieur permanent : marque, palier KYC, notifications, profil.
class AppHeader extends StatelessWidget {
  const AppHeader({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  Widget build(BuildContext context) {
    final tier = context.select<KolaDataProvider, String>(
      (data) => data.user?.tierLabel ?? 'TIER 0',
    );

    return Container(
      height: AppDimens.headerHeight,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.gutter),
      color: AppColors.bg,
      child: Row(
        children: [
          const KolaLogo(size: 34),
          const SizedBox(width: AppSpacing.xs),
          Text(
            'KOLA',
            style: AppTypography.cardTitle.copyWith(
              fontSize: 17,
              color: AppColors.primary,
              letterSpacing: 0.4,
            ),
          ),
          const Spacer(),
          Container(
            height: 27,
            padding: const EdgeInsets.symmetric(horizontal: 10),
            decoration: BoxDecoration(
              color: AppColors.pale2,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              children: [
                const Icon(
                  KolaIcons.shieldCheckmarkOutline,
                  size: 13,
                  color: AppColors.green,
                ),
                const SizedBox(width: 4),
                Text(
                  tier,
                  style: AppTypography.badge.copyWith(
                    fontWeight: FontWeight.w800,
                    color: AppColors.ink,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () => onNavigate(KolaRoute.faq),
            icon: const Icon(
              KolaIcons.notificationsOutline,
              size: 22,
              color: AppColors.ink,
            ),
          ),
          GestureDetector(
            onTap: () => onNavigate(KolaRoute.profile),
            child: Container(
              width: 38,
              height: 38,
              decoration: const BoxDecoration(
                color: AppColors.primary,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                KolaIcons.personOutline,
                size: 19,
                color: AppColors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Monogramme KOLA : carré marine, "K" jaune.
class KolaLogo extends StatelessWidget {
  const KolaLogo({super.key, this.size = 38});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppColors.primary,
        borderRadius: BorderRadius.circular(size * 0.28),
      ),
      child: Text(
        'K',
        style: AppTypography.section.copyWith(
          fontSize: size * 0.55,
          fontWeight: FontWeight.w900,
          color: AppColors.yellow,
        ),
      ),
    );
  }
}

class _NavTab {
  const _NavTab(this.route, this.label, this.icon);
  final KolaRoute route;
  final String label;
  final IconData icon;
}

const _tabs = <_NavTab>[
  _NavTab(KolaRoute.home, 'Accueil', KolaIcons.walletOutline),
  _NavTab(KolaRoute.savings, 'Bankivi', KolaIcons.businessOutline),
  _NavTab(KolaRoute.scan, '', KolaIcons.qrCodeOutline),
  _NavTab(KolaRoute.credit, 'Prêts', KolaIcons.cashOutline),
  _NavTab(KolaRoute.scheduled, 'Planifié', KolaIcons.calendarOutline),
];

/// Barre de navigation flottante translucide, avec bouton de scan central.
class BottomNav extends StatelessWidget {
  const BottomNav({super.key, required this.route, required this.onNavigate});

  final KolaRoute route;
  final void Function(KolaRoute) onNavigate;

  /// Un onglet reste allumé sur les écrans qui en dépendent : consulter le
  /// détail d'un prêt ne doit pas éteindre l'onglet « Prêts ».
  bool _isActive(KolaRoute tab) {
    if (route == tab) return true;
    return switch (tab) {
      KolaRoute.credit =>
        route == KolaRoute.loan || route == KolaRoute.loanDetail,
      KolaRoute.savings => route == KolaRoute.vaults ||
          route == KolaRoute.vaultDetail,
      KolaRoute.scheduled =>
        route == KolaRoute.bills ||
            route == KolaRoute.subscriptions ||
            route == KolaRoute.subscriptionDetail,
      _ => false,
    };
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).padding.bottom;

    return Positioned(
      left: AppDimens.navInset,
      right: AppDimens.navInset,
      bottom: bottomInset > 10 ? bottomInset : 10,
      child: Container(
        height: AppDimens.navHeight,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(AppDimens.navHeight / 2),
          boxShadow: const [
            BoxShadow(
              color: Color(0x2E071B52),
              blurRadius: 18,
              offset: Offset(0, 8),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(AppDimens.navHeight / 2),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 5),
              decoration: BoxDecoration(
                color: AppColors.white.withValues(alpha: 0.88),
                border: Border.all(
                  color: AppColors.white.withValues(alpha: 0.72),
                ),
                borderRadius: BorderRadius.circular(AppDimens.navHeight / 2),
              ),
              child: Row(
                children: [
                  for (var i = 0; i < _tabs.length; i++)
                    if (i == 2)
                      _ScanButton(onTap: () => onNavigate(_tabs[i].route))
                    else
                      Expanded(
                        child: _NavItem(
                          tab: _tabs[i],
                          active: _isActive(_tabs[i].route),
                          onTap: () => onNavigate(_tabs[i].route),
                        ),
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

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.tab,
    required this.active,
    required this.onTap,
  });

  final _NavTab tab;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        height: 54,
        decoration: BoxDecoration(
          color: active ? const Color(0x1A0E3375) : Colors.transparent,
          borderRadius: BorderRadius.circular(27),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              tab.icon,
              size: 22,
              color: active ? AppColors.primary : AppColors.ink,
            ),
            const SizedBox(height: 3),
            Text(
              tab.label,
              style: AppTypography.badge.copyWith(
                fontWeight: active ? FontWeight.w800 : FontWeight.w400,
                color: active ? AppColors.primary : AppColors.ink,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScanButton extends StatelessWidget {
  const _ScanButton({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 5),
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: AppColors.yellow,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: AppColors.yellow.withValues(alpha: 0.38),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: const Icon(
            KolaIcons.qrCodeOutline,
            size: 25,
            color: AppColors.primary,
          ),
        ),
      ),
    );
  }
}

/// Gabarit commun à tous les écrans : contenu défilant, tirer-pour-rafraîchir,
/// bandeau d'erreur d'API et barre de navigation flottante.
class KolaScreen extends StatelessWidget {
  const KolaScreen({
    super.key,
    required this.route,
    required this.onNavigate,
    required this.children,
  });

  final KolaRoute route;
  final void Function(KolaRoute) onNavigate;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();

    return Stack(
      children: [
        RefreshIndicator(
          onRefresh: data.refresh,
          color: AppColors.primary,
          backgroundColor: AppColors.white,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.gutter,
              15,
              AppSpacing.gutter,
              AppDimens.navClearance,
            ),
            children: [
              if (data.error != null) _ApiErrorBanner(message: data.error!),
              ...children,
            ],
          ),
        ),
        BottomNav(route: route, onNavigate: onNavigate),
      ],
    );
  }
}

class _ApiErrorBanner extends StatelessWidget {
  const _ApiErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: context.read<KolaDataProvider>().refresh,
      child: Container(
        margin: const EdgeInsets.only(bottom: AppSpacing.sm),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: AppColors.redPale,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          children: [
            const Icon(
              KolaIcons.cloudOfflineOutline,
              size: 16,
              color: AppColors.redDark,
            ),
            const SizedBox(width: 7),
            Expanded(
              child: Text(
                '$message Touchez pour réessayer.',
                style: AppTypography.caption.copyWith(
                  fontSize: 9,
                  color: AppColors.redDark,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
