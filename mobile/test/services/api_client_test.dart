import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/session_manager.dart';
import 'package:mobile/services/storage_service.dart';
import 'package:mocktail/mocktail.dart';

class MockStorageService extends Mock implements StorageService {}

class MockSessionManager extends Mock implements SessionManager {}

void main() {
  late MockStorageService storage;

  setUp(() {
    storage = MockStorageService();
    when(() => storage.getToken()).thenAnswer((_) async => 'test-token');
  });

  ApiClient buildClient(http.Client mockHttp) {
    return ApiClient(httpClient: mockHttp, storageService: storage);
  }

  test(
    'get() injects the bearer token and decodes a success response',
    () async {
      final mockHttp = MockClient((request) async {
        expect(request.headers['Authorization'], 'Bearer test-token');
        return http.Response(jsonEncode({'value': 42}), 200);
      });

      final client = buildClient(mockHttp);
      final result = await client.get<int>(
        '/foo',
        decode: (json) => (json as Map<String, dynamic>)['value'] as int,
      );

      expect(result.success, true);
      expect(result.data, 42);
    },
  );

  test('get() parses the backend ErrorResponse shape on failure', () async {
    final mockHttp = MockClient((request) async {
      return http.Response(
        jsonEncode({
          'code': 'VALIDATION_ERROR',
          'message': 'Données invalides',
          'details': {'name': 'obligatoire'},
          'path': '/foo',
          'timestamp': '2026-01-01T00:00:00',
        }),
        400,
      );
    });

    final client = buildClient(mockHttp);
    final result = await client.get<int>('/foo', decode: (_) => 0);

    expect(result.success, false);
    expect(result.error?.code, 'VALIDATION_ERROR');
    expect(result.error?.message, 'Données invalides');
    expect(result.error?.details['name'], 'obligatoire');
    expect(result.error?.statusCode, 400);
  });

  test('post() sends a JSON body with the Content-Type header', () async {
    late Map<String, dynamic> sentBody;
    final mockHttp = MockClient((request) async {
      sentBody = jsonDecode(request.body) as Map<String, dynamic>;
      expect(request.headers['Content-Type'], 'application/json');
      return http.Response('', 201);
    });

    final client = buildClient(mockHttp);
    final result = await client.post<void>(
      '/foo',
      body: {'a': 1},
      decode: (_) {},
    );

    expect(result.success, true);
    expect(sentBody['a'], 1);
  });

  test('network failures are mapped to a NETWORK_ERROR ApiException', () async {
    final mockHttp = MockClient((request) async => throw Exception('boom'));

    final client = buildClient(mockHttp);
    final result = await client.get<int>('/foo', decode: (_) => 0);

    expect(result.success, false);
    expect(result.error?.code, 'NETWORK_ERROR');
  });

  test('empty-body success responses (204) decode to null', () async {
    final mockHttp = MockClient((request) async => http.Response('', 204));

    final client = buildClient(mockHttp);
    final result = await client.deleteEmpty('/foo');

    expect(result.success, true);
  });

  test('an unparsable error body still yields a usable ApiException', () async {
    final mockHttp = MockClient(
      (request) async => http.Response('not json', 500),
    );

    final client = buildClient(mockHttp);
    final result = await client.get<int>('/foo', decode: (_) => 0);

    expect(result.success, false);
    expect(result.error?.code, 'UNKNOWN_ERROR');
    expect(result.error?.statusCode, 500);
  });

  group('renouvellement du token sur 401', () {
    late MockSessionManager session;

    setUp(() {
      session = MockSessionManager();
    });

    ApiClient buildClientAvecSession(http.Client mockHttp) {
      return ApiClient(
        httpClient: mockHttp,
        storageService: storage,
        sessionManager: session,
      );
    }

    test(
      'rejoue la requete avec le nouveau token apres renouvellement',
      () async {
        // Le stockage rend l'ancien token, puis le neuf : c'est exactement ce
        // que fait SessionManager en ecrivant le token renouvele.
        final tokens = ['token-expire', 'token-neuf'];
        when(
          () => storage.getToken(),
        ).thenAnswer((_) async => tokens.removeAt(0));
        when(
          () => session.refreshAccessToken(),
        ).thenAnswer((_) async => RefreshOutcome.renewed);

        final tokensVus = <String?>[];
        final mockHttp = MockClient((request) async {
          tokensVus.add(request.headers['Authorization']);
          return tokensVus.length == 1
              ? http.Response('', 401)
              : http.Response(jsonEncode({'value': 7}), 200);
        });

        final result = await buildClientAvecSession(mockHttp).get<int>(
          '/wallets',
          decode: (json) => (json as Map<String, dynamic>)['value'] as int,
        );

        expect(result.success, true);
        expect(result.data, 7);
        expect(tokensVus, ['Bearer token-expire', 'Bearer token-neuf']);
      },
    );

    test(
      'le rejeu renvoie un corps identique, cle idempotence comprise',
      () async {
        when(() => storage.getToken()).thenAnswer((_) async => 'token');
        when(
          () => session.refreshAccessToken(),
        ).thenAnswer((_) async => RefreshOutcome.renewed);

        final corpsEnvoyes = <String>[];
        final mockHttp = MockClient((request) async {
          corpsEnvoyes.add(request.body);
          return corpsEnvoyes.length == 1
              ? http.Response('', 401)
              : http.Response(jsonEncode({'reference': 'TX-1'}), 200);
        });

        await buildClientAvecSession(mockHttp).post<void>(
          '/transactions/transfer',
          body: {'amount': 5000, 'idempotencyKey': 'cle-stable'},
          decode: (_) {},
        );

        // Un rejeu qui reencoderait le corps pourrait changer la cle : le
        // backend ne reconnaitrait plus l'intention et ferait un second
        // virement.
        expect(corpsEnvoyes, hasLength(2));
        expect(corpsEnvoyes.first, corpsEnvoyes.last);
        expect(corpsEnvoyes.last, contains('cle-stable'));
      },
    );

    test('session morte : le 401 d origine remonte a l appelant', () async {
      when(() => storage.getToken()).thenAnswer((_) async => 'token-mort');
      when(
        () => session.refreshAccessToken(),
      ).thenAnswer((_) async => RefreshOutcome.expired);

      var appels = 0;
      final mockHttp = MockClient((request) async {
        appels++;
        return http.Response(
          jsonEncode({'code': 'UNAUTHORIZED', 'message': 'Token invalide'}),
          401,
        );
      });

      final result = await buildClientAvecSession(
        mockHttp,
      ).get<int>('/wallets', decode: (_) => 0);

      expect(result.success, false);
      expect(result.error?.statusCode, 401);
      // Pas de rejeu : sans token valide il echouerait a l'identique.
      expect(appels, 1);
    });

    test('serveur de refresh injoignable : erreur reseau, pas 401', () async {
      when(() => storage.getToken()).thenAnswer((_) async => 'token');
      when(
        () => session.refreshAccessToken(),
      ).thenAnswer((_) async => RefreshOutcome.unreachable);

      final mockHttp = MockClient((request) async => http.Response('', 401));

      final result = await buildClientAvecSession(
        mockHttp,
      ).get<int>('/wallets', decode: (_) => 0);

      // Annoncer une deconnexion alors que le reseau a juste saute enverrait
      // l'utilisateur ressaisir son mot de passe pour rien.
      expect(result.error?.code, 'NETWORK_ERROR');
    });

    test('un 401 persistant n est rejoue qu une fois', () async {
      when(() => storage.getToken()).thenAnswer((_) async => 'token');
      when(
        () => session.refreshAccessToken(),
      ).thenAnswer((_) async => RefreshOutcome.renewed);

      var appels = 0;
      final mockHttp = MockClient((request) async {
        appels++;
        return http.Response('', 401);
      });

      final result = await buildClientAvecSession(
        mockHttp,
      ).get<int>('/wallets', decode: (_) => 0);

      expect(result.success, false);
      // Garde-fou anti-boucle : deux tentatives, pas une de plus.
      expect(appels, 2);
      verify(() => session.refreshAccessToken()).called(1);
    });
  });
}
