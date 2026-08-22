import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Catalogue d'avatars prédéfinis.
///
/// On ne stocke qu'un identifiant côté serveur (ex: "avatar_03") : pas
/// d'upload de fichier ni de stockage binaire à gérer. Le rendu est purement
/// local — une icône + une couleur de fond issues du design system.
class KolaAvatars {
  KolaAvatars._();

  static const List<String> ids = [
    'avatar_01',
    'avatar_02',
    'avatar_03',
    'avatar_04',
    'avatar_05',
    'avatar_06',
    'avatar_07',
    'avatar_08',
  ];

  static const Map<String, IconData> _icons = {
    'avatar_01': Icons.person_rounded,
    'avatar_02': Icons.face_rounded,
    'avatar_03': Icons.emoji_emotions_rounded,
    'avatar_04': Icons.rocket_launch_rounded,
    'avatar_05': Icons.pets_rounded,
    'avatar_06': Icons.spa_rounded,
    'avatar_07': Icons.sports_soccer_rounded,
    'avatar_08': Icons.music_note_rounded,
  };

  static const Map<String, Color> _colors = {
    'avatar_01': AppColors.primary,
    'avatar_02': AppColors.primaryContainer,
    'avatar_03': AppColors.success,
    'avatar_04': AppColors.warning,
    'avatar_05': AppColors.orangeMoney,
    'avatar_06': AppColors.moovBlue,
    'avatar_07': AppColors.danger,
    'avatar_08': AppColors.primaryBright,
  };

  static IconData iconFor(String? id) => _icons[id] ?? Icons.person_rounded;

  static Color colorFor(String? id) => _colors[id] ?? AppColors.surfaceVariant;
}

/// Avatar rond de l'utilisateur, rendu à partir de son identifiant d'avatar.
class KolaAvatar extends StatelessWidget {
  final String? avatarId;
  final double radius;
  final VoidCallback? onTap;

  const KolaAvatar({super.key, this.avatarId, this.radius = 24, this.onTap});

  @override
  Widget build(BuildContext context) {
    final color = KolaAvatars.colorFor(avatarId);
    final avatar = CircleAvatar(
      radius: radius,
      backgroundColor: color.withValues(alpha: 0.15),
      child: Icon(
        KolaAvatars.iconFor(avatarId),
        color: avatarId == null ? AppColors.onSurfaceVariant : color,
        size: radius,
      ),
    );

    if (onTap == null) return avatar;
    return InkWell(
      onTap: onTap,
      customBorder: const CircleBorder(),
      child: avatar,
    );
  }
}
