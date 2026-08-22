import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';

/// Bottom navigation bar partagée, 4 onglets : Home, Vaults, Credit, Profile.
/// Cf. DESIGN.md - composant partagé "BottomNavBar".
class KolaBottomNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const KolaBottomNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  static const _items = [
    (icon: Icons.home_rounded, outlineIcon: Icons.home_outlined, label: 'Home'),
    (
      icon: Icons.account_balance_wallet_rounded,
      outlineIcon: Icons.account_balance_wallet_outlined,
      label: 'Vaults',
    ),
    (
      icon: Icons.speed_rounded,
      outlineIcon: Icons.speed_outlined,
      label: 'Credit',
    ),
    (
      icon: Icons.person_rounded,
      outlineIcon: Icons.person_outline_rounded,
      label: 'Profile',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      // Sans libellés, la ligne de texte en moins permet de resserrer la barre.
      height: AppDimens.bottomNavHeight,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.marginMobile,
        vertical: AppSpacing.xs,
      ),
      decoration: const BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        border: Border(top: BorderSide(color: AppColors.hairlineLight)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: List.generate(_items.length, (index) {
          final item = _items[index];
          final isActive = index == currentIndex;
          return _NavItem(
            icon: isActive ? item.icon : item.outlineIcon,
            label: item.label,
            isActive: isActive,
            onTap: () => onTap(index),
          );
        }),
      ),
    );
  }
}

/// Icône seule (sans libellé). Le nom de l'onglet reste exposé en tooltip et
/// en label sémantique : le retirer complètement casserait la navigation au
/// lecteur d'écran. L'onglet actif est signalé par la couleur + une pastille,
/// puisqu'il n'y a plus de texte pour porter cette information.
class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool isActive;
  final VoidCallback onTap;

  const _NavItem({
    required this.icon,
    required this.label,
    required this.isActive,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final color = isActive ? AppColors.primary : AppColors.onSurfaceVariant;
    return Semantics(
      label: label,
      selected: isActive,
      button: true,
      child: Tooltip(
        message: label,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppRadius.full),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              vertical: AppSpacing.xs,
              horizontal: AppSpacing.md,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, color: color, size: 26),
                const SizedBox(height: 4),
                AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  width: isActive ? 6 : 0,
                  height: isActive ? 6 : 0,
                  decoration: const BoxDecoration(
                    color: AppColors.primary,
                    shape: BoxShape.circle,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
