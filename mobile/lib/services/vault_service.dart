import '../models/json.dart';
import '../models/vault.dart';
import 'api_client.dart';

class VaultService {
  VaultService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<List<Vault>>> list() {
    return _client.get(
      '/api/v1/vaults',
      decode: (json) => asList(json, Vault.fromJson),
    );
  }

  Future<ApiResult<Vault>> create({
    required String name,
    double? targetAmount,
    String? targetDate,
    String? description,
  }) {
    return _client.post(
      '/api/v1/vaults',
      body: {
        'name': name,
        'currency': 'XOF',
        'targetAmount': ?targetAmount,
        'targetDate': ?targetDate,
        if (description != null && description.isNotEmpty)
          'description': description,
      },
      decode: (json) => Vault.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<Vault>> deposit(String vaultId, double amount) {
    return _client.post(
      '/api/v1/vaults/$vaultId/deposit',
      body: {'amount': amount},
      decode: (json) => Vault.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<Vault>> withdraw(String vaultId, double amount) {
    return _client.post(
      '/api/v1/vaults/$vaultId/withdraw',
      body: {'amount': amount},
      decode: (json) => Vault.fromJson((json as Map).cast<String, dynamic>()),
    );
  }
}
