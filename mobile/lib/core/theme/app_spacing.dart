/// Échelle d'espacement (base 4px), rayons de bordure et dimensions Kola.
/// Source : design system Stitch (kola_fintech/DESIGN.md)
class AppSpacing {
  AppSpacing._();

  static const double base = 4;
  static const double xxs = 4;
  static const double xs = 8;
  static const double sm = 12;
  static const double md = 16;
  static const double lg = 24;
  static const double xl = 32;
  static const double sectionV = 64;
  static const double gutter = 16;
  static const double marginMobile = 20;
}

/// Rayons de bordure (border-radius).
class AppRadius {
  AppRadius._();

  static const double sm = 4; // 0.25rem — badges (KYC tiers)
  static const double defaultR = 8; // 0.5rem
  static const double md = 12; // 0.75rem — inputs
  static const double lg =
      20; // ~1.25rem (utilisé comme "rounded-lg" pour les cards/features)
  static const double xl = 24; // 1.5rem
  static const double full = 9999; // pilule (boutons, chips)
}

/// Dimensions fixes communes (hauteurs de composants).
class AppDimens {
  AppDimens._();

  static const double inputHeight = 56;
  static const double buttonMinHeight = 48;
  static const double bottomNavHeight = 64;
  static const double cardPadding =
      32; // padding interne "xl" des cards (esprit magazine)
  static const double iconActionSize =
      48; // boutons d'action circulaires (Déposer, Envoyer...)
  static const double iconBadgeSize =
      40; // icônes dans les listes de transactions
}
