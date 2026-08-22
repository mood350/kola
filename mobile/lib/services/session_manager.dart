import '../core/utils/jwt_utils.dart';
import 'auth_service.dart';
import 'storage_service.dart';

/// Issue d'une tentative de renouvellement du token d'accès.
enum RefreshOutcome {
  /// Un nouvel access token est en place : la requête d'origine peut être rejouée.
  renewed,

  /// Le backend a refusé le refresh token (expiré, révoqué, absent) : la
  /// session est morte, les tokens locaux ont été purgés.
  expired,

  /// Le serveur n'a pas répondu. La session est peut-être toujours valide —
  /// surtout ne rien effacer, sinon une coupure réseau déconnecte
  /// l'utilisateur alors que ses tokens sont bons.
  unreachable,
}

/// Propriétaire du cycle de vie de la session côté app : renouvellement du
/// token d'accès, et signalement de l'expiration définitive.
///
/// Le refresh token était sauvegardé au login (`AuthProvider.login`) puis plus
/// jamais relu. À l'expiration de l'access token, chaque écran affichait donc
/// une erreur 401 générique jusqu'à ce que l'utilisateur tue l'app. Tout le
/// renouvellement passe désormais par ici, et [ApiClient] s'y branche.
class SessionManager {
  SessionManager({AuthService? authService, StorageService? storageService})
    : _auth = authService ?? AuthService(),
      _storage = storageService ?? StorageService();

  /// Instance partagée par tous les [ApiClient]. Chaque service construit le
  /// sien (`ApiClient()` par défaut dans WalletService, VaultService...), donc
  /// le verrou anti-concurrence ne peut pas vivre dans l'un d'eux : l'écran
  /// d'accueil déclenche cinq requêtes en parallèle, et cinq 401 simultanés
  /// lanceraient cinq POST /auth/refresh-token. Or le backend fait tourner le
  /// refresh token à chaque appel — les quatre derniers échoueraient et
  /// déconnecteraient l'utilisateur au lieu de le reconnecter.
  ///
  /// Réassignable pour les tests.
  static SessionManager instance = SessionManager();

  final AuthService _auth;
  final StorageService _storage;

  Future<RefreshOutcome>? _inFlight;

  /// Appelé quand plus aucun token ne permet de continuer : l'app doit vider
  /// son état et renvoyer l'utilisateur sur l'écran de connexion (branché dans
  /// `app.dart`).
  void Function()? onSessionExpired;

  /// Renouvelle l'access token à partir du refresh token stocké.
  /// Les appels concurrents partagent une seule requête réseau.
  Future<RefreshOutcome> refreshAccessToken({
    bool notifyOnExpiry = true,
  }) async {
    final pending = _inFlight;
    if (pending != null) return pending;

    final started = _performRefresh(notifyOnExpiry: notifyOnExpiry);
    _inFlight = started;
    try {
      return await started;
    } finally {
      // Le verrou tombe dans tous les cas : le garder après un échec gèlerait
      // définitivement le renouvellement pour le reste de la session.
      _inFlight = null;
    }
  }

  Future<RefreshOutcome> _performRefresh({required bool notifyOnExpiry}) async {
    final refreshToken = await _storage.getRefreshToken();
    if (refreshToken == null ||
        refreshToken.isEmpty ||
        isJwtExpired(refreshToken)) {
      // Inutile d'appeler le backend : on sait déjà qu'il refusera.
      await expireSession(notify: notifyOnExpiry);
      return RefreshOutcome.expired;
    }

    final result = await _auth.refreshToken(refreshToken);
    if (result.isNetworkFailure) {
      return RefreshOutcome.unreachable;
    }
    if (!result.success || result.accessToken == null) {
      await expireSession(notify: notifyOnExpiry);
      return RefreshOutcome.expired;
    }

    await _storage.saveToken(result.accessToken!);
    // Le backend renvoie un refresh token neuf à chaque rotation : ne pas le
    // stocker condamnerait le renouvellement suivant.
    if (result.refreshToken != null) {
      await _storage.saveRefreshToken(result.refreshToken!);
    }
    return RefreshOutcome.renewed;
  }

  /// Reprise de session au démarrage : `true` si l'app peut aller directement
  /// à l'accueil.
  ///
  /// Un access token expiré ne suffit plus à renvoyer l'utilisateur sur
  /// l'onboarding — tant que le refresh token vit (7 jours contre 24 h), la
  /// session est restaurée silencieusement.
  Future<bool> restoreSession() async {
    final token = await _storage.getToken();
    if (token != null && token.isNotEmpty && !isJwtExpired(token)) {
      return true;
    }
    // Au splash, personne n'écoute encore la navigation : inutile de notifier.
    final outcome = await refreshAccessToken(notifyOnExpiry: false);
    return outcome == RefreshOutcome.renewed;
  }

  /// Détruit la session locale et, si demandé, prévient l'app.
  Future<void> expireSession({bool notify = true}) async {
    await _storage.clearAll();
    if (notify) onSessionExpired?.call();
  }
}
