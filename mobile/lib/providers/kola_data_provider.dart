import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../core/constants/app_constants.dart';
import '../models/loan.dart';
import '../models/scheduled_transfer.dart';
import '../models/transaction.dart';
import '../models/user.dart';
import '../models/vault.dart';
import '../models/wallet.dart';
import '../services/api_client.dart';
import '../services/credit_service.dart';
import '../services/scheduled_transfer_service.dart';
import '../services/storage_service.dart';
import '../services/transaction_service.dart';
import '../services/user_service.dart';
import '../services/vault_service.dart';
import '../services/wallet_service.dart';

/// Notification ponctuelle à présenter par-dessus l'écran courant après une
/// opération (réussie ou échouée).
class TransactionToast {
  const TransactionToast({
    required this.reference,
    required this.type,
    required this.failed,
    required this.amount,
    this.currency = 'XOF',
    this.counterparty,
    this.failureReason,
  });

  final String reference;
  final String type;
  final bool failed;
  final double amount;
  final String currency;
  final String? counterparty;
  final String? failureReason;
}

/// Source unique de vérité pour les écrans : profil, soldes, coffres, relevé,
/// crédit et opérations programmées.
///
/// Un seul store plutôt qu'un provider par domaine : l'accueil affiche les six
/// à la fois, et les recharger séparément ferait clignoter l'écran six fois.
class KolaDataProvider extends ChangeNotifier {
  KolaDataProvider({
    UserService? userService,
    WalletService? walletService,
    VaultService? vaultService,
    TransactionService? transactionService,
    CreditService? creditService,
    SchedulingService? schedulingService,
    StorageService? storageService,
    http.Client? streamClient,
  }) : _users = userService ?? UserService(),
       _wallets = walletService ?? WalletService(),
       _vaults = vaultService ?? VaultService(),
       _transactions = transactionService ?? TransactionService(),
       _credit = creditService ?? CreditService(),
       _scheduling = schedulingService ?? SchedulingService(),
       _storage = storageService ?? StorageService(),
       _streamClient = streamClient ?? http.Client();

  final UserService _users;
  final WalletService _wallets;
  final VaultService _vaults;
  final TransactionService _transactions;
  final CreditService _credit;
  final SchedulingService _scheduling;
  final StorageService _storage;
  final http.Client _streamClient;

  KolaUser? _user;
  List<Wallet> _walletList = const [];
  List<Vault> _vaultList = const [];
  List<KolaTransaction> _transactionList = const [];
  CreditEligibility? _eligibility;
  List<Loan> _loanList = const [];
  List<ScheduledTask> _scheduledList = const [];

  bool _loading = true;
  String? _error;
  TransactionToast? _toast;

  Timer? _poller;
  StreamSubscription<String>? _streamSubscription;
  String? _lastToastKey;
  bool _disposed = false;

  KolaUser? get user => _user;
  List<Wallet> get wallets => _walletList;
  List<Vault> get vaults => _vaultList;
  List<KolaTransaction> get transactions => _transactionList;
  CreditEligibility? get eligibility => _eligibility;
  List<Loan> get loans => _loanList;
  List<ScheduledTask> get scheduled => _scheduledList;
  bool get loading => _loading;
  String? get error => _error;
  TransactionToast? get toast => _toast;

  /// Compte courant en XOF : celui dont le solde s'affiche en haut de
  /// l'accueil.
  Wallet? get currentWallet {
    for (final wallet in _walletList) {
      if (wallet.currency == 'XOF' && wallet.type == 'CURRENT') return wallet;
    }
    for (final wallet in _walletList) {
      if (wallet.currency == 'XOF' && !wallet.isSavings) return wallet;
    }
    return null;
  }

  Wallet? get savingsWallet {
    for (final wallet in _walletList) {
      if (wallet.currency == 'XOF' && wallet.isSavings) return wallet;
    }
    return null;
  }

  double get balance => currentWallet?.availableBalance ?? 0;
  double get savingsBalance => savingsWallet?.availableBalance ?? 0;

  /// Les [count] opérations les plus récentes, du plus récent au plus ancien.
  List<KolaTransaction> recentTransactions([int count = 4]) {
    final sorted = [..._transactionList]
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
    return sorted.take(count).toList();
  }

  Loan? get activeLoan {
    for (final loan in _loanList) {
      if (loan.isActive) return loan;
    }
    return null;
  }

  /// Premier chargement, puis mises à jour périodiques et flux temps réel.
  Future<void> start() async {
    await load(showLoader: true);
    _poller ??= Timer.periodic(
      const Duration(seconds: 15),
      (_) => load(showLoader: false),
    );
    unawaited(_listenToTransactionStream());
  }

  Future<void> refresh() => load(showLoader: true);

  /// Recharge tout le tableau de bord.
  ///
  /// Les six ressources sont demandées en parallèle et échouent
  /// indépendamment : si seul le module crédit est indisponible, l'accueil
  /// garde ses soldes et son relevé, et ne signale que ce qui manque. Un
  /// `Future.wait` classique aurait vidé l'écran entier pour une seule panne.
  Future<void> load({required bool showLoader}) async {
    if (showLoader) {
      _loading = true;
      _safeNotify();
    }

    final profile = await _users.me();
    if (!profile.success) {
      _error = profile.error?.message ?? 'Impossible de charger votre compte.';
      _loading = false;
      _safeNotify();
      return;
    }
    _user = profile.data;

    final results = await Future.wait([
      _wallets.list(),
      _vaults.list(),
      _transactions.history(),
      _credit.eligibility(),
      _credit.loans(),
      _scheduling.list(),
    ]);

    const labels = [
      'portefeuilles',
      'coffres',
      'transactions',
      'éligibilité crédit',
      'prêts',
      'planifications',
    ];

    _walletList = _keep(results[0], _walletList);
    _vaultList = _keep(results[1], _vaultList);
    _transactionList = _keep(results[2], _transactionList);
    _eligibility = (results[3].data as CreditEligibility?) ?? _eligibility;
    _loanList = _keep(results[4], _loanList);
    _scheduledList = _keep(results[5], _scheduledList);

    final failed = <String>[];
    for (var i = 0; i < results.length; i++) {
      if (!results[i].success) failed.add(labels[i]);
    }
    _error = failed.isEmpty
        ? null
        : 'Modules temporairement indisponibles : ${failed.join(', ')}.';

    _loading = false;
    _safeNotify();
  }

  /// Conserve la valeur précédente quand l'appel a échoué, pour ne pas vider
  /// une liste déjà affichée sur une erreur passagère.
  List<T> _keep<T>(ApiResult<dynamic> result, List<T> previous) {
    final data = result.data;
    return data is List<T> ? data : previous;
  }

  /// Remplace les portefeuilles après une opération qui les renvoie déjà :
  /// évite un aller-retour réseau pour rafraîchir un solde qu'on connaît.
  void replaceWallets(List<Wallet> wallets) {
    _walletList = wallets;
    _safeNotify();
  }

  void notifyTransaction(KolaTransaction transaction) {
    if (!transaction.isCompleted && !transaction.isFailed) return;
    final key = '${transaction.reference}:${transaction.status}';
    if (_lastToastKey == key) return;
    _lastToastKey = key;
    _toast = TransactionToast(
      reference: transaction.reference,
      type: transaction.type,
      failed: transaction.isFailed,
      amount: transaction.amount,
      currency: transaction.currency,
      counterparty: transaction.counterparty,
      failureReason: transaction.failureReason,
    );
    _safeNotify();
  }

  /// Échec survenu côté app (validation, réseau) : il n'y a pas de transaction
  /// à afficher, seulement un message.
  void notifyFailure(String message, [String type = 'Transaction']) {
    final reference = 'local-${DateTime.now().microsecondsSinceEpoch}';
    _lastToastKey = '$reference:FAILED';
    _toast = TransactionToast(
      reference: reference,
      type: type,
      failed: true,
      amount: 0,
      failureReason: message,
    );
    _safeNotify();
  }

  void dismissToast() {
    _toast = null;
    _safeNotify();
  }

  /// Flux `text/event-stream` des transactions : le backend pousse chaque
  /// mouvement dès qu'il est écrit, ce qui évite d'attendre le prochain
  /// sondage pour voir arriver un virement reçu.
  Future<void> _listenToTransactionStream() async {
    final token = await _storage.getToken();
    if (token == null || token.isEmpty || _disposed) return;

    try {
      final request =
          http.Request(
              'GET',
              Uri.parse('${AppConstants.baseUrl}/api/v1/transactions/stream'),
            )
            ..headers.addAll({
              'Authorization': 'Bearer $token',
              'Accept': 'text/event-stream',
            });

      final response = await _streamClient.send(request);
      if (response.statusCode != 200 || _disposed) return;

      _streamSubscription = response.stream
          .transform(utf8.decoder)
          .transform(const LineSplitter())
          .listen(_onStreamLine, onError: (_) {}, cancelOnError: true);
    } catch (_) {
      // Le temps réel est un confort : son absence ne doit jamais empêcher
      // l'app de fonctionner, le sondage de 15 s prend le relais.
    }
  }

  void _onStreamLine(String line) {
    if (!line.startsWith('data:')) return;
    final payload = line.substring(5).trim();
    if (payload.isEmpty) return;
    try {
      final decoded = jsonDecode(payload);
      if (decoded is Map<String, dynamic>) {
        notifyTransaction(KolaTransaction.fromJson(decoded));
      }
    } catch (_) {
      // Ligne de keep-alive ou charge utile inattendue : sans intérêt ici.
    }
    load(showLoader: false);
  }

  void _safeNotify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _poller?.cancel();
    _streamSubscription?.cancel();
    _streamClient.close();
    super.dispose();
  }
}
