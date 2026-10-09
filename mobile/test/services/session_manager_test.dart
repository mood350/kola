import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/services/auth_service.dart';
import 'package:mobile/services/session_manager.dart';
import 'package:mobile/services/storage_service.dart';
import 'package:mocktail/mocktail.dart';

class MockAuthService extends Mock implements AuthService {}

class MockStorageService extends Mock implements StorageService {}

/// Fabrique un JWT non signe dont seul le claim `exp` compte : c'est tout ce
/// que lit isJwtExpired cote app.
String _jwt(Duration validitePendant) {
  final exp =
      DateTime.now().toUtc().add(validitePendant).millisecondsSinceEpoch ~/
      1000;
  String segment(Map<String, dynamic> claims) =>
      base64Url.encode(utf8.encode(jsonEncode(claims))).replaceAll('=', '');
  return '${segment({'alg': 'HS256'})}.${segment({'sub': 'u', 'exp': exp})}.sig';
}

void main() {
  late MockAuthService auth;
  late MockStorageService storage;
  late SessionManager session;

  final refreshValide = _jwt(const Duration(days: 7));
  final accessValide = _jwt(const Duration(hours: 12));
  final accessExpire = _jwt(const Duration(hours: -1));

  setUp(() {
    auth = MockAuthService();
    storage = MockStorageService();
    session = SessionManager(authService: auth, storageService: storage);

    when(() => storage.saveToken(any())).thenAnswer((_) async {});
    when(() => storage.saveRefreshToken(any())).thenAnswer((_) async {});
    when(() => storage.clearAll()).thenAnswer((_) async {});
  });

  group('refreshAccessToken', () {
    test('stocke le nouvel access token ET le refresh token rendu', () async {
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => refreshValide);
      when(() => auth.refreshToken(any())).thenAnswer(
        (_) async => AuthResult.ok(
          accessToken: 'access-neuf',
          refreshToken: 'refresh-neuf',
        ),
      );

      final outcome = await session.refreshAccessToken();

      expect(outcome, RefreshOutcome.renewed);
      verify(() => storage.saveToken('access-neuf')).called(1);
      // Le backend fait tourner le refresh token : ne pas stocker le nouveau
      // condamnerait le renouvellement suivant.
      verify(() => storage.saveRefreshToken('refresh-neuf')).called(1);
    });

    test('un refus du backend purge la session et previent l app', () async {
      var previenue = false;
      session.onSessionExpired = () => previenue = true;
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => refreshValide);
      when(
        () => auth.refreshToken(any()),
      ).thenAnswer((_) async => AuthResult.fail('Session expiree'));

      final outcome = await session.refreshAccessToken();

      expect(outcome, RefreshOutcome.expired);
      verify(() => storage.clearAll()).called(1);
      expect(previenue, true);
    });

    test('une coupure reseau ne detruit RIEN', () async {
      var previenue = false;
      session.onSessionExpired = () => previenue = true;
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => refreshValide);
      when(
        () => auth.refreshToken(any()),
      ).thenAnswer((_) async => AuthResult.networkFailure());

      final outcome = await session.refreshAccessToken();

      // Confondre « serveur injoignable » et « session invalide »
      // deconnecterait un utilisateur qui passe sous un tunnel, tokens
      // parfaitement valides en poche.
      expect(outcome, RefreshOutcome.unreachable);
      verifyNever(() => storage.clearAll());
      expect(previenue, false);
    });

    test('un refresh token expire evite l aller-retour reseau', () async {
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => _jwt(const Duration(days: -1)));

      final outcome = await session.refreshAccessToken();

      expect(outcome, RefreshOutcome.expired);
      verifyNever(() => auth.refreshToken(any()));
      verify(() => storage.clearAll()).called(1);
    });

    test('des appels concurrents ne declenchent qu UNE requete', () async {
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => refreshValide);
      when(() => auth.refreshToken(any())).thenAnswer((_) async {
        await Future<void>.delayed(const Duration(milliseconds: 20));
        return AuthResult.ok(
          accessToken: 'access-neuf',
          refreshToken: 'refresh-neuf',
        );
      });

      // L'accueil tire cinq endpoints en parallele : cinq 401 simultanes.
      // Cinq POST /auth/refresh-token concurrents feraient echouer les quatre
      // derniers, le backend ayant deja fait tourner le refresh token.
      final resultats = await Future.wait(
        List.generate(5, (_) => session.refreshAccessToken()),
      );

      expect(resultats, everyElement(RefreshOutcome.renewed));
      verify(() => auth.refreshToken(any())).called(1);
    });

    test('le verrou est relache apres un echec', () async {
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => refreshValide);
      when(
        () => auth.refreshToken(any()),
      ).thenAnswer((_) async => AuthResult.networkFailure());
      await session.refreshAccessToken();

      when(
        () => auth.refreshToken(any()),
      ).thenAnswer((_) async => AuthResult.ok(accessToken: 'access-neuf'));

      // Un verrou qui resterait pris apres un echec gelerait tout
      // renouvellement pour le reste de la session.
      expect(await session.refreshAccessToken(), RefreshOutcome.renewed);
    });
  });

  group('restoreSession', () {
    test('un access token valide suffit, sans appel reseau', () async {
      when(() => storage.getToken()).thenAnswer((_) async => accessValide);

      expect(await session.restoreSession(), true);
      verifyNever(() => auth.refreshToken(any()));
    });

    test('un access token expire est renouvele silencieusement', () async {
      when(() => storage.getToken()).thenAnswer((_) async => accessExpire);
      when(
        () => storage.getRefreshToken(),
      ).thenAnswer((_) async => refreshValide);
      when(
        () => auth.refreshToken(any()),
      ).thenAnswer((_) async => AuthResult.ok(accessToken: 'access-neuf'));

      // Avant, le splash renvoyait sur l'onboarding des que l'access token
      // avait plus de 24 h — alors que le refresh token en vaut 7 jours.
      expect(await session.restoreSession(), true);
    });

    test('sans aucun token, retourne false sans notifier', () async {
      var previenue = false;
      session.onSessionExpired = () => previenue = true;
      when(() => storage.getToken()).thenAnswer((_) async => null);
      when(() => storage.getRefreshToken()).thenAnswer((_) async => null);

      expect(await session.restoreSession(), false);
      // Au splash, personne n'ecoute encore : notifier ferait naviguer vers
      // /login par-dessus la redirection que le splash s'apprete a faire.
      expect(previenue, false);
    });
  });
}
