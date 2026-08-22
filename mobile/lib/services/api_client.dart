import 'dart:convert';
import 'package:http/http.dart' as http;
import '../core/constants/app_constants.dart';
import 'session_manager.dart';
import 'storage_service.dart';

/// Exception représentant la forme unique des erreurs backend
/// (cf. GlobalExceptionHandler.ErrorResponse : code, message, details, path, timestamp).
class ApiException implements Exception {
  final String code;
  final String message;
  final Map<String, String> details;
  final int? statusCode;

  const ApiException({
    required this.code,
    required this.message,
    this.details = const {},
    this.statusCode,
  });

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

  Future<ApiResult<T>> post<T>(
    String path, {
    Object? body,
    required T Function(dynamic json) decode,
  }) {
    final encoded = body != null ? jsonEncode(body) : null;
    return _send(
      (headers) => _http.post(_uri(path), headers: headers, body: encoded),
      decode: decode,
      withBody: true,
    );
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
      return ApiResult.ok(decode(jsonDecode(response.body)));
    }
    return ApiResult.fail(_parseError(response));
  }

  ApiException _parseError(http.Response response) {
    try {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      final rawDetails = body['details'];
      return ApiException(
        code: body['code'] as String? ?? 'UNKNOWN_ERROR',
        message: body['message'] as String? ?? 'Une erreur est survenue',
        details: rawDetails is Map
            ? rawDetails.map((k, v) => MapEntry(k.toString(), v.toString()))
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
