import '../models/wallet.dart';
import 'api_client.dart';

/// Service gérant les appels API liés aux wallets.
/// Endpoints réels (cf. WalletController backend) : GET /wallets,
/// GET /wallets/{id}, POST /wallets. L'utilisateur courant est toujours
/// déduit du JWT côté backend — jamais de walletId "propriétaire" à fournir.
class WalletService {
  WalletService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<List<Wallet>>> getMyWallets() {
    return _api.get<List<Wallet>>(
      '/wallets',
      decode: (json) => (json as List<dynamic>)
          .map((e) => Wallet.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<ApiResult<Wallet>> getWallet(int id) {
    return _api.get<Wallet>(
      '/wallets/$id',
      decode: (json) => Wallet.fromJson(json as Map<String, dynamic>),
    );
  }

  /// Crée un wallet (devise fixée à XOF pour ce marché, pas de sélecteur).
  Future<ApiResult<Wallet>> createWallet({String currency = 'XOF'}) {
    return _api.post<Wallet>(
      '/wallets',
      body: {'currency': currency},
      decode: (json) => Wallet.fromJson(json as Map<String, dynamic>),
    );
  }
}
