import 'package:flutter/foundation.dart';
import '../models/transaction.dart';
import '../services/api_client.dart';
import '../services/transaction_service.dart';

enum TxActionStatus { idle, loading, success, error }

/// Provider regroupant l'historique paginé, le détail d'une transaction,
/// et les actions dépôt/retrait/transfert (elles partagent TransactionService).
/// Ne touche jamais WalletProvider directement — c'est à l'écran appelant
/// de rafraîchir WalletProvider après une action réussie.
class TransactionProvider extends ChangeNotifier {
  TransactionProvider({TransactionService? transactionService})
    : _service = transactionService ?? TransactionService();

  final TransactionService _service;

  // --- Historique paginé ---
  final List<Transaction> _history = [];
  bool _isLoadingHistory = false;
  bool _hasMoreHistory = true;
  int _historyPage = 0;
  int? _historyWalletId;
  String? _historyError;

  List<Transaction> get history => _history;
  bool get isLoadingHistory => _isLoadingHistory;
  bool get hasMoreHistory => _hasMoreHistory;
  String? get historyError => _historyError;

  Future<void> loadHistory(int walletId) async {
    _historyWalletId = walletId;
    _history.clear();
    _historyPage = 0;
    _hasMoreHistory = true;
    _historyError = null;
    await _fetchHistoryPage();
  }

  Future<void> loadMoreHistory() async {
    if (_isLoadingHistory || !_hasMoreHistory || _historyWalletId == null) {
      return;
    }
    await _fetchHistoryPage();
  }

  Future<void> _fetchHistoryPage() async {
    _isLoadingHistory = true;
    notifyListeners();

    final result = await _service.getWalletHistory(
      walletId: _historyWalletId!,
      page: _historyPage,
    );

    if (result.success) {
      final page = result.data!;
      _history.addAll(page.content);
      _hasMoreHistory = !page.last;
      _historyPage++;
    } else {
      _historyError =
          result.error?.message ?? "Impossible de charger l'historique";
    }

    _isLoadingHistory = false;
    notifyListeners();
  }

  // --- Détail d'une transaction ---
  Transaction? _detail;
  bool _isLoadingDetail = false;
  String? _detailError;

  Transaction? get detail => _detail;
  bool get isLoadingDetail => _isLoadingDetail;
  String? get detailError => _detailError;

  Future<void> loadDetail(String reference) async {
    _isLoadingDetail = true;
    _detailError = null;
    notifyListeners();

    final result = await _service.getByReference(reference);
    if (result.success) {
      _detail = result.data;
    } else {
      _detailError = result.error?.message ?? 'Transaction introuvable';
    }
    _isLoadingDetail = false;
    notifyListeners();
  }

  // --- Actions : dépôt / retrait / transfert ---
  TxActionStatus _actionStatus = TxActionStatus.idle;
  String? _actionError;

  TxActionStatus get actionStatus => _actionStatus;
  String? get actionError => _actionError;

  void resetActionState() {
    _actionStatus = TxActionStatus.idle;
    _actionError = null;
    notifyListeners();
  }

  /// Les quatre actions ci-dessous exigent une [idempotencyKey] : c'est
  /// l'appelant — l'écran — qui la détient, via [IdempotencyKeyHolder].
  ///
  /// Le provider la générait lui-même, donc chaque appel en produisait une
  /// nouvelle : un re-tap sur « Réessayer » après un timeout repartait avec
  /// une clé neuve et le backend exécutait un SECOND mouvement d'argent, dans
  /// le seul cas où l'app ne sait justement pas si le premier est passé.
  /// Rendre le paramètre obligatoire est délibéré : une valeur par défaut
  /// rouvrirait le trou au premier écran qui oublie de la passer.

  Future<bool> deposit({
    required int walletId,
    required double amount,
    required String idempotencyKey,
  }) {
    return _runAction(
      () => _service.deposit(
        walletId: walletId,
        amount: amount,
        idempotencyKey: idempotencyKey,
      ),
    );
  }

  Future<bool> withdraw({
    required int walletId,
    required double amount,
    required String idempotencyKey,
  }) {
    return _runAction(
      () => _service.withdraw(
        walletId: walletId,
        amount: amount,
        idempotencyKey: idempotencyKey,
      ),
    );
  }

  Future<bool> depositMobileMoney({
    required int walletId,
    required double amount,
    required String mode,
    required String phoneNumber,
    required String idempotencyKey,
  }) {
    return _runAction(
      () => _service.depositMobileMoney(
        walletId: walletId,
        amount: amount,
        mode: mode,
        phoneNumber: phoneNumber,
        idempotencyKey: idempotencyKey,
      ),
    );
  }

  Future<bool> withdrawMobileMoney({
    required int walletId,
    required double amount,
    required String mode,
    required String phoneNumber,
    required String idempotencyKey,
  }) {
    return _runAction(
      () => _service.withdrawMobileMoney(
        walletId: walletId,
        amount: amount,
        mode: mode,
        phoneNumber: phoneNumber,
        idempotencyKey: idempotencyKey,
      ),
    );
  }

  Future<bool> transfer({
    required int sourceWalletId,
    required int beneficiaryId,
    required double amount,
    required String idempotencyKey,
    String? description,
  }) {
    return _runAction(
      () => _service.transfer(
        sourceWalletId: sourceWalletId,
        beneficiaryId: beneficiaryId,
        amount: amount,
        description: description,
        idempotencyKey: idempotencyKey,
      ),
    );
  }

  Future<bool> payMerchant({
    required int sourceWalletId,
    required String merchantCode,
    required double amount,
    required String idempotencyKey,
  }) {
    return _runAction(
      () => _service.payMerchant(
        sourceWalletId: sourceWalletId,
        merchantCode: merchantCode,
        amount: amount,
        idempotencyKey: idempotencyKey,
      ),
    );
  }

  Future<bool> _runAction<T>(Future<ApiResult<T>> Function() action) async {
    _actionStatus = TxActionStatus.loading;
    _actionError = null;
    notifyListeners();

    final result = await action();
    if (result.success) {
      _actionStatus = TxActionStatus.success;
      notifyListeners();
      return true;
    }
    _actionError = result.error?.message ?? "Échec de l'opération";
    _actionStatus = TxActionStatus.error;
    notifyListeners();
    return false;
  }

  void clear() {
    _history.clear();
    _hasMoreHistory = true;
    _historyPage = 0;
    _historyWalletId = null;
    _historyError = null;
    _detail = null;
    _detailError = null;
    _actionStatus = TxActionStatus.idle;
    _actionError = null;
    notifyListeners();
  }
}
