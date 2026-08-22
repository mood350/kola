import '../models/vault.dart';
import 'api_client.dart';

/// Service gérant les appels API liés aux coffres-forts d'épargne
/// (cf. VaultController backend).
class VaultService {
  VaultService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<List<Vault>>> getMyVaults() {
    return _api.get<List<Vault>>(
      '/vaults',
      decode: (json) => (json as List<dynamic>)
          .map((e) => Vault.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<ApiResult<Vault>> getVault(int id) {
    return _api.get<Vault>(
      '/vaults/$id',
      decode: (json) => Vault.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Vault>> createVault({
    required int walletId,
    required String name,
    String? purpose,
    double? targetAmount,
    required double initialAmount,
    DateTime? unlockDate,
  }) {
    final unlockDateStr = unlockDate != null
        ? '${unlockDate.year.toString().padLeft(4, '0')}-${unlockDate.month.toString().padLeft(2, '0')}-${unlockDate.day.toString().padLeft(2, '0')}'
        : null;

    return _api.post<Vault>(
      '/vaults',
      body: {
        'walletId': walletId,
        'name': name,
        'purpose': ?purpose,
        'targetAmount': ?targetAmount,
        'initialAmount': initialAmount,
        'unlockDate': ?unlockDateStr,
      },
      decode: (json) => Vault.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Vault>> addFunds({
    required int vaultId,
    required double amount,
  }) {
    return _api.post<Vault>(
      '/vaults/$vaultId/add-funds',
      body: {'amount': amount},
      decode: (json) => Vault.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Vault>> unlock(int vaultId) {
    return _api.post<Vault>(
      '/vaults/$vaultId/unlock',
      decode: (json) => Vault.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Vault>> closeEarly(int vaultId) {
    return _api.post<Vault>(
      '/vaults/$vaultId/close',
      decode: (json) => Vault.fromJson(json as Map<String, dynamic>),
    );
  }
}
