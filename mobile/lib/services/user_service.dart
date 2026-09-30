import '../models/kyc.dart';
import '../models/user.dart';
import 'api_client.dart';

class UserService {
  UserService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<KolaUser>> me() {
    return _client.get(
      '/api/v1/users/me',
      decode: (json) =>
          KolaUser.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  /// Résout un numéro en destinataire affichable avant un envoi. Échoue si le
  /// numéro n'a pas de compte KOLA — c'est ce qui sert de garde-fou à l'écran
  /// d'envoi.
  Future<ApiResult<RecipientLookup>> lookupRecipient(String phone) {
    return _client.get(
      '/api/v1/users/recipients/${Uri.encodeComponent(phone)}',
      decode: (json) =>
          RecipientLookup.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  /// Met à jour le profil (prénom, nom, e-mail).
  Future<ApiResult<KolaUser>> updateProfile(Map<String, dynamic> changes) {
    return _client.patch(
      '/api/v1/users/me',
      body: changes,
      decode: (json) =>
          KolaUser.fromJson((json as Map).cast<String, dynamic>()),
    );
  }
}

/// État de la vérification d'identité et dépôt de pièces justificatives.
class KycService {
  KycService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<KycStatus>> status() {
    return _client.get(
      '/api/v1/kyc/status',
      decode: (json) =>
          KycStatus.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<KycDocument>> uploadDocument({
    required String type,
    required String filePath,
  }) {
    return _client.upload(
      '/api/v1/kyc/documents',
      field: 'file',
      filePath: filePath,
      fields: {'type': type},
      decode: (json) =>
          KycDocument.fromJson((json as Map).cast<String, dynamic>()),
    );
  }
}
