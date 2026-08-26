import '../models/credit_score.dart';
import '../models/loan.dart';
import '../models/loan_capacity.dart';
import 'api_client.dart';

/// Service gérant les appels API liés au score de crédit et aux prêts
/// (cf. CreditController backend). Affichage uniquement — le score
/// n'est jamais recalculé côté mobile.
class CreditService {
  CreditService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<CreditScoreBreakdown>> getScore() {
    return _api.get<CreditScoreBreakdown>(
      '/credit/score',
      decode: (json) =>
          CreditScoreBreakdown.fromJson(json as Map<String, dynamic>),
    );
  }

  /// Force le recalcul serveur (contourne le cache de 30 jours).
  Future<ApiResult<CreditScoreBreakdown>> refreshScore() {
    return _api.post<CreditScoreBreakdown>(
      '/credit/score/refresh',
      decode: (json) =>
          CreditScoreBreakdown.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<List<CreditScoreBreakdown>>> getScoreHistory() {
    return _api.get<List<CreditScoreBreakdown>>(
      '/credit/score/history',
      decode: (json) => (json as List<dynamic>)
          .map((e) => CreditScoreBreakdown.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// GET /credit/capacity — le MONTANT empruntable, par durée.
  ///
  /// Séparé du score à dessein : le score dit si l'on prête et à quel taux,
  /// cette route dit combien. C'est elle que l'écran de demande interroge.
  Future<ApiResult<LoanCapacity>> getCapacity() {
    return _api.get<LoanCapacity>(
      '/credit/capacity',
      decode: (json) => LoanCapacity.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Loan>> applyForLoan({
    required int walletId,
    required double requestedAmount,
    required int durationMonths,
    String? purpose,
  }) {
    return _api.post<Loan>(
      '/credit/loans',
      body: {
        'walletId': walletId,
        'requestedAmount': requestedAmount,
        'durationMonths': durationMonths,
        'purpose': ?purpose,
      },
      decode: (json) => Loan.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<List<Loan>>> getMyLoans() {
    return _api.get<List<Loan>>(
      '/credit/loans',
      decode: (json) => (json as List<dynamic>)
          .map((e) => Loan.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<ApiResult<Loan>> getLoan(int id) {
    return _api.get<Loan>(
      '/credit/loans/$id',
      decode: (json) => Loan.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Loan>> repay(int loanId) {
    return _api.post<Loan>(
      '/credit/loans/$loanId/repay',
      decode: (json) => Loan.fromJson(json as Map<String, dynamic>),
    );
  }
}
