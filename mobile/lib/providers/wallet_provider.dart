import 'package:flutter/foundation.dart';
import '../models/transaction.dart';
import '../services/wallet_service.dart';

/// Provider gérant l'état du wallet (solde principal, transactions récentes).
class WalletProvider extends ChangeNotifier {
  final WalletService _walletService = WalletService();

  double _balance = 0;
  List<Transaction> _recentTransactions = [];
  bool _isLoading = false;
  bool _isBalanceVisible = true;

  double get balance => _balance;
  List<Transaction> get recentTransactions => _recentTransactions;
  bool get isLoading => _isLoading;
  bool get isBalanceVisible => _isBalanceVisible;

  void toggleBalanceVisibility() {
    _isBalanceVisible = !_isBalanceVisible;
    notifyListeners();
  }

  Future<void> loadHomeData() async {
    _isLoading = true;
    notifyListeners();

    final balanceResult = await _walletService.getBalance();
    final transactionsResult = await _walletService.getRecentTransactions(limit: 3);

    if (balanceResult.success) {
      _balance = balanceResult.data ?? 0;
    }
    if (transactionsResult.success) {
      _recentTransactions = transactionsResult.data ?? [];
    }

    _isLoading = false;
    notifyListeners();
  }
}