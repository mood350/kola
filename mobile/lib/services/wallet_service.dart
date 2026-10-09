import '../models/json.dart';
import '../models/wallet.dart';
import 'api_client.dart';

class WalletService {
  WalletService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<List<Wallet>>> list() {
    return _client.get(
      '/api/v1/wallets',
      decode: (json) => asList(json, Wallet.fromJson),
    );
  }

  /// Recharge du compte courant depuis Mobile Money.
  Future<ApiResult<Wallet>> deposit(double amount) {
    return _client.post(
      '/api/v1/wallets/XOF/deposit',
      body: {'amount': amount},
      decode: (json) => Wallet.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  /// Versement du compte courant vers l'épargne Bankivi.
  Future<ApiResult<List<Wallet>>> depositToSavings(double amount) {
    return _client.post(
      '/api/v1/wallets/savings/deposit',
      body: {'currency': 'XOF', 'amount': amount},
      decode: (json) => asList(json, Wallet.fromJson),
    );
  }

  Future<ApiResult<List<Wallet>>> withdrawFromSavings(double amount) {
    return _client.post(
      '/api/v1/wallets/savings/withdraw',
      body: {'currency': 'XOF', 'amount': amount},
      decode: (json) => asList(json, Wallet.fromJson),
    );
  }
}
