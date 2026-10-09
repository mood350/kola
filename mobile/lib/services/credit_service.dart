import '../models/json.dart';
import '../models/loan.dart';
import 'api_client.dart';

class CreditService {
  CreditService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<CreditEligibility>> eligibility() {
    return _client.get(
      '/api/v1/credit/eligibility',
      query: {'currency': 'XOF'},
      decode: (json) =>
          CreditEligibility.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<List<Loan>>> loans() {
    return _client.get(
      '/api/v1/credit/loans',
      decode: (json) => asList(json, Loan.fromJson),
    );
  }

  Future<ApiResult<Loan>> requestLoan({
    required double amount,
    required String idempotencyKey,
  }) {
    return _client.post(
      '/api/v1/credit/loans',
      idempotencyKey: idempotencyKey,
      body: {'currency': 'XOF', 'amount': amount},
      decode: (json) => Loan.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  /// Remboursement anticipé, total ou partiel.
  Future<ApiResult<Loan>> repay({
    required String loanId,
    required double amount,
    required String idempotencyKey,
  }) {
    return _client.post(
      '/api/v1/credit/loans/$loanId/repay',
      idempotencyKey: idempotencyKey,
      body: {'amount': amount},
      decode: (json) => Loan.fromJson((json as Map).cast<String, dynamic>()),
    );
  }
}
