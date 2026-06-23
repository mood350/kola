import 'dart:convert';
import 'package:http/http.dart' as http;
import '../core/constants/app_constants.dart';
import '../models/transaction.dart';
import 'storage_service.dart';

/// Résultat générique des appels wallet.
class WalletResult<T> {
  final bool success;
  final T? data;
  final String? errorMessage;

  WalletResult.ok(this.data) : success = true, errorMessage = null;
  WalletResult.fail(this.errorMessage) : success = false, data = null;
}

/// Service gérant les appels API liés au wallet (solde, transactions).
class WalletService {
  final StorageService _storageService = StorageService();

  Future<Map<String, String>> _authHeaders() async {
    final token = await _storageService.getToken();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<WalletResult<double>> getBalance() async {
    try {
      final response = await http
          .get(
        Uri.parse('${AppConstants.baseUrl}/wallet/balance'),
        headers: await _authHeaders(),
      )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return WalletResult.ok((data['balance'] as num).toDouble());
      }
      return WalletResult.fail('Impossible de récupérer le solde');
    } catch (e) {
      return WalletResult.fail('Erreur réseau');
    }
  }

  Future<WalletResult<List<Transaction>>> getRecentTransactions({int limit = 3}) async {
    try {
      final response = await http
          .get(
        Uri.parse('${AppConstants.baseUrl}/wallet/transactions?limit=$limit'),
        headers: await _authHeaders(),
      )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as List<dynamic>;
        final transactions = data
            .map((json) => Transaction.fromJson(json as Map<String, dynamic>))
            .toList();
        return WalletResult.ok(transactions);
      }
      return WalletResult.fail('Impossible de récupérer les transactions');
    } catch (e) {
      return WalletResult.fail('Erreur réseau');
    }
  }
}