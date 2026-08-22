import 'package:flutter/foundation.dart';
import '../models/transaction.dart';
import '../models/wallet.dart';
import '../services/transaction_service.dart';
import '../services/wallet_service.dart';

/// Provider gérant l'état du wallet principal (solde, transactions récentes).
///
/// Un nouvel utilisateur n'a aucun wallet tant qu'il n'en crée pas un
/// (aucune création automatique à l'inscription côté backend) : si
/// `GET /wallets` renvoie une liste vide, on en provisionne un
/// silencieusement (devise XOF fixe pour ce marché, pas de sélecteur)
/// plutôt que d'afficher une carte de solde cassée au premier lancement.
class WalletProvider extends ChangeNotifier {
  WalletProvider({
    WalletService? walletService,
    TransactionService? transactionService,
  }) : _walletService = walletService ?? WalletService(),
       _transactionService = transactionService ?? TransactionService();

  final WalletService _walletService;
  final TransactionService _transactionService;

  Wallet? _primaryWallet;
  List<Transaction> _recentTransactions = [];
  bool _isLoading = false;
  bool _isBalanceVisible = true;
  String? _errorMessage;

  Wallet? get primaryWallet => _primaryWallet;
  double get balance => _primaryWallet?.balance ?? 0;
  List<Transaction> get recentTransactions => _recentTransactions;
  bool get isLoading => _isLoading;
  bool get isBalanceVisible => _isBalanceVisible;
  String? get errorMessage => _errorMessage;

  void toggleBalanceVisibility() {
    _isBalanceVisible = !_isBalanceVisible;
    notifyListeners();
  }

  Future<void> loadHomeData() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    final walletsResult = await _walletService.getMyWallets();
    if (!walletsResult.success) {
      _errorMessage =
          walletsResult.error?.message ?? 'Impossible de charger le wallet';
      _isLoading = false;
      notifyListeners();
      return;
    }

    final activeWallets = walletsResult.data!.where((w) => w.active).toList();

    Wallet? wallet;
    if (activeWallets.isEmpty) {
      final createResult = await _walletService.createWallet();
      if (createResult.success) {
        wallet = createResult.data;
      } else {
        _errorMessage =
            createResult.error?.message ?? 'Impossible de créer le wallet';
      }
    } else {
      wallet = activeWallets.first;
    }

    _primaryWallet = wallet;

    if (wallet != null) {
      final txResult = await _transactionService.getRecent(
        walletId: wallet.id,
        limit: 3,
      );
      if (txResult.success) {
        _recentTransactions = txResult.data ?? [];
      }
    }

    _isLoading = false;
    notifyListeners();
  }

  void clear() {
    _primaryWallet = null;
    _recentTransactions = [];
    _errorMessage = null;
    notifyListeners();
  }
}
