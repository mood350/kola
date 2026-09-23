import '../models/page_response.dart';
import '../models/transaction.dart';
import 'api_client.dart';

/// Service gérant les appels API liés aux transactions
/// (cf. TransactionController backend).
class TransactionService {
  TransactionService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<Transaction>> deposit({
    required int walletId,
    required double amount,
    String? externalReference,
    String? idempotencyKey,
  }) {
    return _api.post<Transaction>(
      '/transactions/deposit',
      body: {
        'walletId': walletId,
        'amount': amount,
        'externalReference': ?externalReference,
        'idempotencyKey': ?idempotencyKey,
      },
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Transaction>> withdraw({
    required int walletId,
    required double amount,
    String? idempotencyKey,
  }) {
    return _api.post<Transaction>(
      '/transactions/withdraw',
      body: {
        'walletId': walletId,
        'amount': amount,
        'idempotencyKey': ?idempotencyKey,
      },
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  /// POST /transactions/deposit/mobile-money — 202, écriture EN ATTENTE.
  ///
  /// Le wallet n'est PAS crédité : la demande part sur le téléphone du client,
  /// qui la valide par son code Mobile Money. C'est le webhook du prestataire
  /// qui créditera.
  Future<ApiResult<Transaction>> depositMobileMoney({
    required int walletId,
    required double amount,
    required String mode,
    required String phoneNumber,
    String? idempotencyKey,
  }) {
    return _api.post<Transaction>(
      '/transactions/deposit/mobile-money',
      body: {
        'walletId': walletId,
        'amount': amount,
        'mode': mode,
        'phoneNumber': phoneNumber,
        'idempotencyKey': ?idempotencyKey,
      },
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  /// POST /transactions/withdraw/mobile-money — 202, écriture EN ATTENTE.
  ///
  /// Le wallet EST débité tout de suite — sinon la somme resterait dépensable
  /// pendant le traitement — mais l'argent n'est pas encore arrivé. Un échec
  /// recrédite le montant et les frais.
  Future<ApiResult<Transaction>> withdrawMobileMoney({
    required int walletId,
    required double amount,
    required String mode,
    required String phoneNumber,
    String? idempotencyKey,
  }) {
    return _api.post<Transaction>(
      '/transactions/withdraw/mobile-money',
      body: {
        'walletId': walletId,
        'amount': amount,
        'mode': mode,
        'phoneNumber': phoneNumber,
        'idempotencyKey': ?idempotencyKey,
      },
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Transaction>> transfer({
    required int sourceWalletId,
    required int beneficiaryId,
    required double amount,
    String? description,
    String? idempotencyKey,
  }) {
    return _api.post<Transaction>(
      '/transactions/transfer',
      body: {
        'sourceWalletId': sourceWalletId,
        'beneficiaryId': beneficiaryId,
        'amount': amount,
        'description': ?description,
        'idempotencyKey': ?idempotencyKey,
      },
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Transaction>> payMerchant({
    required int sourceWalletId,
    required String merchantCode,
    required double amount,
    String? idempotencyKey,
  }) {
    return _api.post<Transaction>(
      '/transactions/pay-merchant',
      body: {
        'sourceWalletId': sourceWalletId,
        'merchantCode': merchantCode,
        'amount': amount,
        'idempotencyKey': ?idempotencyKey,
      },
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<PageResponse<Transaction>>> getWalletHistory({
    required int walletId,
    int page = 0,
    int size = 20,
  }) {
    return _api.get<PageResponse<Transaction>>(
      '/transactions/wallet/$walletId',
      query: {'page': '$page', 'size': '$size'},
      decode: (json) => PageResponse.fromJson(
        json as Map<String, dynamic>,
        Transaction.fromJson,
      ),
    );
  }

  Future<ApiResult<Transaction>> getByReference(String reference) {
    return _api.get<Transaction>(
      '/transactions/$reference',
      decode: (json) => Transaction.fromJson(json as Map<String, dynamic>),
    );
  }

  /// Raccourci pour le tableau de bord Home : dernières transactions du wallet.
  Future<ApiResult<List<Transaction>>> getRecent({
    required int walletId,
    int limit = 3,
  }) async {
    final result = await getWalletHistory(walletId: walletId, size: limit);
    if (result.success) {
      return ApiResult.ok(result.data!.content);
    }
    return ApiResult.fail(result.error);
  }
}
