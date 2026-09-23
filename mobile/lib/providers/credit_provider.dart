import 'package:flutter/foundation.dart';
import '../models/credit_score.dart';
import '../models/loan.dart';
import '../models/loan_capacity.dart';
import '../services/api_client.dart';
import '../services/credit_service.dart';

/// Provider gérant le score de crédit (affichage uniquement, jamais
/// recalculé côté mobile) et les prêts de l'utilisateur.
class CreditProvider extends ChangeNotifier {
  CreditProvider({CreditService? creditService})
    : _service = creditService ?? CreditService();

  final CreditService _service;

  CreditScoreBreakdown? _score;
  bool _isLoadingScore = false;
  String? _scoreError;

  CreditScoreBreakdown? get score => _score;
  bool get isLoadingScore => _isLoadingScore;
  String? get scoreError => _scoreError;

  List<Loan> _loans = [];
  bool _isLoadingLoans = false;
  String? _loansError;

  List<Loan> get loans => _loans;
  bool get isLoadingLoans => _isLoadingLoans;
  String? get loansError => _loansError;

  LoanCapacity? _capacity;
  bool _isLoadingCapacity = false;
  String? _capacityError;

  LoanCapacity? get capacity => _capacity;
  bool get isLoadingCapacity => _isLoadingCapacity;
  String? get capacityError => _capacityError;

  /// Prêt issu de la dernière demande aboutie.
  ///
  /// Exposé parce que la SUITE en dépend : au-delà du seuil d'examen le prêt
  /// revient `pending` et rien n'a été versé, sinon il est déjà décaissé. Sans
  /// cette information, l'écran annoncerait un versement dans les deux cas.
  Loan? _lastSubmittedLoan;
  Loan? get lastSubmittedLoan => _lastSubmittedLoan;

  bool _isSubmitting = false;
  String? _submitError;
  String? _submitErrorCode;

  bool get isSubmitting => _isSubmitting;
  String? get submitError => _submitError;
  String? get submitErrorCode => _submitErrorCode;

  Future<void> loadScore() async {
    _isLoadingScore = true;
    _scoreError = null;
    notifyListeners();

    final result = await _service.getScore();
    if (result.success) {
      _score = result.data;
    } else {
      _scoreError = result.error?.message ?? 'Impossible de charger le score';
    }
    _isLoadingScore = false;
    notifyListeners();
  }

  /// Recalcul forcé côté serveur : le score est mis en cache 30 jours,
  /// donc un simple loadScore() renverrait la même valeur figée.

  Future<void> loadLoans() async {
    _isLoadingLoans = true;
    _loansError = null;
    notifyListeners();

    final result = await _service.getMyLoans();
    if (result.success) {
      _loans = result.data ?? [];
    } else {
      _loansError = result.error?.message ?? 'Impossible de charger les prêts';
    }
    _isLoadingLoans = false;
    notifyListeners();
  }

  Future<void> loadCapacity() async {
    _isLoadingCapacity = true;
    _capacityError = null;
    notifyListeners();

    final result = await _service.getCapacity();
    if (result.success) {
      _capacity = result.data;
    } else {
      _capacityError = result.error?.message ?? 'Capacité indisponible';
    }
    _isLoadingCapacity = false;
    notifyListeners();
  }

  Future<bool> applyForLoan({
    required int walletId,
    required double requestedAmount,
    required int durationMonths,
    String? purpose,
  }) async {
    _isSubmitting = true;
    _submitError = null;
    _submitErrorCode = null;
    _lastSubmittedLoan = null;
    notifyListeners();

    final result = await _service.applyForLoan(
      walletId: walletId,
      requestedAmount: requestedAmount,
      durationMonths: durationMonths,
      purpose: purpose,
    );

    if (result.success) {
      _lastSubmittedLoan = result.data;
      await loadLoans();
      // La capacité a changé : un prêt en cours consomme le droit à emprunter.
      await loadCapacity();
      _isSubmitting = false;
      notifyListeners();
      return true;
    }
    _submitError =
        result.error?.message ?? 'Impossible de soumettre la demande';
    _submitErrorCode = result.error?.code;
    _isSubmitting = false;
    notifyListeners();
    return false;
  }

  Future<bool> repay(int loanId) async {
    return _mutateLoans(() => _service.repay(loanId));
  }

  Future<bool> _mutateLoans<T>(Future<ApiResult<T>> Function() action) async {
    _isSubmitting = true;
    _submitError = null;
    _submitErrorCode = null;
    notifyListeners();

    final result = await action();
    if (result.success) {
      await loadLoans();
      _isSubmitting = false;
      notifyListeners();
      return true;
    }
    _submitError = result.error?.message ?? "Échec de l'opération";
    _submitErrorCode = result.error?.code;
    _isSubmitting = false;
    notifyListeners();
    return false;
  }

  void clear() {
    _score = null;
    _loans = [];
    _scoreError = null;
    _loansError = null;
    notifyListeners();
  }
}
