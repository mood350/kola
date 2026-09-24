import '../models/json.dart';
import '../models/transaction.dart';
import 'api_client.dart';

class TransactionService {
  TransactionService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  /// Relevé du compte. La réponse est paginée (`Page<TransactionResponse>`) :
  /// seule la page courante nous intéresse ici.
  Future<ApiResult<List<KolaTransaction>>> history({int size = 100}) {
    return _client.get(
      '/api/v1/transactions',
      query: {'size': '$size'},
      decode: (json) {
        final content = (json as Map?)?['content'];
        return asList(content, KolaTransaction.fromJson);
      },
    );
  }

  Future<ApiResult<KolaTransaction>> byReference(String reference) {
    return _client.get(
      '/api/v1/transactions/$reference',
      decode: (json) =>
          KolaTransaction.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  /// Frais applicables avant confirmation, pour les afficher plutôt que de
  /// laisser l'utilisateur les découvrir sur son relevé.
  Future<ApiResult<FeeQuote>> quote({
    required double amount,
    String type = 'P2P_TRANSFER',
  }) {
    return _client.post(
      '/api/v1/transactions/quote',
      body: {'type': type, 'currency': 'XOF', 'amount': amount},
      decode: (json) => FeeQuote.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  /// [idempotencyKey] vient d'un [IdempotencyKeyHolder] tenu par l'écran : il
  /// reste stable tant que l'intention de paiement ne change pas, pour qu'un
  /// re-essai après timeout ne débite pas deux fois.
  Future<ApiResult<KolaTransaction>> transfer({
    required double amount,
    required String recipientPhone,
    required String idempotencyKey,
    String? description,
  }) {
    return _client.post(
      '/api/v1/transactions/transfer',
      idempotencyKey: idempotencyKey,
      body: {
        'amount': amount,
        'currency': 'XOF',
        'recipientPhone': recipientPhone,
        if (description != null && description.isNotEmpty)
          'description': description,
      },
      decode: (json) =>
          KolaTransaction.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<KolaTransaction>> payBill({
    required double amount,
    required String billerReference,
    required String idempotencyKey,
    String? description,
  }) {
    return _client.post(
      '/api/v1/transactions/bill-payment',
      idempotencyKey: idempotencyKey,
      body: {
        'amount': amount,
        'currency': 'XOF',
        'billerReference': billerReference,
        if (description != null && description.isNotEmpty)
          'description': description,
      },
      decode: (json) =>
          KolaTransaction.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<KolaTransaction>> cashOut({
    required double amount,
    required String destination,
    required String idempotencyKey,
  }) {
    return _client.post(
      '/api/v1/transactions/cash-out',
      idempotencyKey: idempotencyKey,
      body: {
        'amount': amount,
        'currency': 'XOF',
        'destination': destination,
      },
      decode: (json) =>
          KolaTransaction.fromJson((json as Map).cast<String, dynamic>()),
    );
  }
}
