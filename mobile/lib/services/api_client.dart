import 'dart:convert';
import 'package:http/http.dart' as http;
import '../core/constants/app_constants.dart';
import 'session_manager.dart';
import 'storage_service.dart';

/// Exception représentant la forme unique des erreurs backend
/// (cf. GlobalExceptionHandler : message + fieldErrors).
class ApiException implements Exception {
  final String code;
  final String message;

  /// Erreurs de validation champ par champ, à rattacher aux saisies du
  /// formulaire plutôt qu'à afficher en bloc.
  final Map<String, String> details;
  final int? statusCode;

  const ApiException({
    required this.code,
    required this.message,
    this.details = const {},
    this.statusCode,
  });

  bool get isNetworkFailure => code == 'NETWORK_ERROR';

  @override
  String toString() => message;
}

/// Résultat générique d'un appel API.
class ApiResult<T> {
  final bool success;
  final T? data;
  final ApiException? error;

  const ApiResult.ok(this.data) : success = true, error = null;
  const ApiResult.fail(this.error) : success = false, data = null;
}

/// Client HTTP centralisé : injecte le token Bearer, applique le timeout,
/// et parse UNE SEULE FOIS la forme ErrorResponse du backend.
/// Tout nouveau service réseau doit passer par ce client plutôt que
/// dupliquer le parsing d'erreurs dans chaque écran (cf. mobile/CLAUDE.md).
///
/// NOTE : AuthService n'utilise pas encore ce client (il précède sa création
/// et son format d'erreur legacy dégrade proprement) — migration à prévoir
/// dans une passe dédiée, cf. TODO dans auth_service.dart.
class ApiClient {
  ApiClient({
    http.Client? httpClient,
    StorageService? storageService,
    SessionManager? sessionManager,
  }) : _http = httpClient ?? http.Client(),
       _storage = storageService ?? StorageService(),
       _sessionOverride = sessionManager;

  final http.Client _http;
  final StorageService _storage;
  final SessionManager? _sessionOverride;

  /// Résolu à l'usage et non à la construction : les services sont instanciés
  /// très tôt, un test qui remplace `SessionManager.instance` après coup doit
  /// quand même être vu.
  SessionManager get _session => _sessionOverride ?? SessionManager.instance;

  Future<Map<String, String>> _headers({bool withBody = false}) async {
    final token = await _storage.getToken();
    return {
      if (withBody) 'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Uri _uri(String path, [Map<String, String>? query]) {
    final base = Uri.parse('${AppConstants.baseUrl}$path');
    if (query == null || query.isEmpty) return base;
    return base.replace(queryParameters: {...base.queryParameters, ...query});
  }

  /// Envoie la requête, et la rejoue UNE fois si le backend répond 401 et que
  /// le token a pu être renouvelé.
  ///
  /// Rejouer un POST est sûr ici : un 401 est prononcé par `JwtAuthFilter`
  /// avant que la requête n'atteigne le moindre service métier, donc rien n'a
  /// bougé côté ledger. Le corps est encodé une seule fois, en dehors de la
  /// closure, pour que la seconde tentative soit octet pour octet la première
  /// — clé d'idempotence comprise.
  Future<ApiResult<T>> _send<T>(
    Future<http.Response> Function(Map<String, String> headers) send, {
    required T Function(dynamic json) decode,
    bool withBody = false,
  }) async {
    try {
      var response = await send(
        await _headers(withBody: withBody),
      ).timeout(AppConstants.apiTimeout);

      if (response.statusCode == 401) {
        final outcome = await _session.refreshAccessToken();
        if (outcome == RefreshOutcome.unreachable) {
          // Le serveur de refresh n'a pas répondu : c'est un problème de
          // réseau, pas une session invalide. Le dire tel quel plutôt que de
          // renvoyer un 401 qui ferait croire à une déconnexion.
          return ApiResult.fail(_networkError());
        }
        if (outcome == RefreshOutcome.renewed) {
          response = await send(
            await _headers(withBody: withBody),
          ).timeout(AppConstants.apiTimeout);
        }
        // outcome == expired : SessionManager a déjà purgé les tokens et
        // prévenu l'app ; on laisse le 401 d'origine remonter à l'appelant.
      }

      return _handle(response, decode);
    } catch (_) {
      return ApiResult.fail(_networkError());
    }
  }

  Future<ApiResult<T>> get<T>(
    String path, {
    Map<String, String>? query,
    required T Function(dynamic json) decode,
  }) {
    return _send(
      (headers) => _http.get(_uri(path, query), headers: headers),
      decode: decode,
    );
  }

  /// [idempotencyKey] est repris tel quel par la seconde tentative après un
  /// 401 : c'est ce qui garantit que le rejeu ne débite pas deux fois.
  Future<ApiResult<T>> post<T>(
    String path, {
    Object? body,
    String? idempotencyKey,
    required T Function(dynamic json) decode,
  }) {
    final encoded = body != null ? jsonEncode(body) : null;
    return _send(
      (headers) => _http.post(
        _uri(path),
        headers: {...headers, 'Idempotency-Key': ?idempotencyKey},
        body: encoded,
      ),
      decode: decode,
      withBody: true,
    );
  }

  Future<ApiResult<T>> patch<T>(
    String path, {
    Object? body,
    required T Function(dynamic json) decode,
  }) {
    final encoded = body != null ? jsonEncode(body) : null;
    return _send(
      (headers) => _http.patch(_uri(path), headers: headers, body: encoded),
      decode: decode,
      withBody: true,
    );
  }

  /// Envoi d'un fichier en `multipart/form-data` (pièces justificatives KYC).
  ///
  /// Ne passe pas par [_send] : un corps multipart est un flux à usage unique,
  /// il ne survivrait pas au rejeu après 401. Le token est donc rafraîchi
  /// avant l'envoi plutôt qu'après le refus.
  Future<ApiResult<T>> upload<T>(
    String path, {
    required String field,
    required String filePath,
    Map<String, String> fields = const {},
    required T Function(dynamic json) decode,
  }) async {
    try {
      final request = http.MultipartRequest('POST', _uri(path))
        ..headers.addAll(await _headers())
        ..fields.addAll(fields)
        ..files.add(await http.MultipartFile.fromPath(field, filePath));

      final streamed = await _http
          .send(request)
          .timeout(AppConstants.apiTimeout);
      return _handle(await http.Response.fromStream(streamed), decode);
    } catch (_) {
      return ApiResult.fail(_networkError());
    }
  }

  Future<ApiResult<T>> put<T>(
    String path, {
    Object? body,
    required T Function(dynamic json) decode,
  }) {
    final encoded = body != null ? jsonEncode(body) : null;
    return _send(
      (headers) => _http.put(_uri(path), headers: headers, body: encoded),
      decode: decode,
      withBody: true,
    );
  }

  /// Pour les endpoints qui ne renvoient rien d'utile (204/202 sans body).
  Future<ApiResult<void>> postEmpty(String path, {Object? body}) {
    return post<void>(path, body: body, decode: (_) {});
  }

  Future<ApiResult<T>> delete<T>(
    String path, {
    required T Function(dynamic json) decode,
  }) {
    return _send(
      (headers) => _http.delete(_uri(path), headers: headers),
      decode: decode,
    );
  }

  Future<ApiResult<void>> deleteEmpty(String path) {
    return delete<void>(path, decode: (_) {});
  }

  ApiResult<T> _handle<T>(
    http.Response response,
    T Function(dynamic json) decode,
  ) {
    final status = response.statusCode;
    if (status >= 200 && status < 300) {
      if (response.body.isEmpty) {
        return ApiResult.ok(decode(null));
      }
      return ApiResult.ok(decode(_unwrap(jsonDecode(response.body))));
    }
    return ApiResult.fail(_parseError(response));
  }

  /// Les contrôleurs répondent le plus souvent dans une enveloppe
  /// `{success, message, data}`, mais quelques-uns renvoient l'objet nu. La
  /// présence de la clé `success` est le seul marqueur fiable : s'en remettre
  /// au type du contrôleur appelant obligerait chaque service à savoir lequel
  /// des deux il interroge.
  dynamic _unwrap(dynamic body) {
    if (body is Map<String, dynamic> && body.containsKey('success')) {
      return body['data'];
    }
    return body;
  }

  ApiException _parseError(http.Response response) {
    try {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      final fieldErrors = body['fieldErrors'] ?? body['details'];
      return ApiException(
        code: body['code'] as String? ?? 'UNKNOWN_ERROR',
        message: body['message'] as String? ?? 'Une erreur est survenue',
        details: fieldErrors is Map
            ? fieldErrors.map((k, v) => MapEntry(k.toString(), v.toString()))
            : const {},
        statusCode: response.statusCode,
      );
    } catch (_) {
      return ApiException(
        code: 'UNKNOWN_ERROR',
        message: 'Une erreur est survenue (${response.statusCode})',
        statusCode: response.statusCode,
      );
    }
  }

  ApiException _networkError() {
    return const ApiException(
      code: 'NETWORK_ERROR',
      message: 'Erreur réseau : impossible de contacter le serveur',
    );
  }
}
