/// Échelle d'espacement KOLA (base 4px).
class AppSpacing {
  AppSpacing._();

  static const double xxs = 4;
  static const double xs = 8;
  static const double sm = 12;
  static const double md = 16;
  static const double lg = 24;
  static const double xl = 32;

  /// Marge latérale du contenu de tous les écrans.
  static const double gutter = 20;
}

/// Rayons de bordure.
class AppRadius {
  AppRadius._();

  static const double sm = 6;
  static const double md = 11;
  static const double card = 16;
  static const double lg = 19;
  static const double sheet = 27;
  static const double full = 9999;
}

/// Dimensions fixes communes.
class AppDimens {
  AppDimens._();

  static const double headerHeight = 64;
  static const double buttonHeight = 48;
  static const double inputHeight = 46;

  /// Barre de navigation flottante : hauteur, et marge latérale par rapport
  /// aux bords de l'écran.
  static const double navHeight = 68;
  static const double navInset = 14;

  /// Espace réservé en bas de chaque écran pour que la barre flottante ne
  /// recouvre jamais le dernier élément de la liste.
  static const double navClearance = 112;

  static const double iconCircle = 43;
}
