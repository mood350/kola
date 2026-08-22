import 'package:flutter/foundation.dart';
import '../models/merchant.dart';
import '../services/merchant_service.dart';

enum MerchantLookupStatus { initial, loading, loaded, error }

/// Provider gérant la consultation d'un marchand avant paiement.
class MerchantProvider extends ChangeNotifier {
  MerchantProvider({MerchantService? merchantService})
    : _service = merchantService ?? MerchantService();

  final MerchantService _service;

  Merchant? _merchant;
  MerchantLookupStatus _status = MerchantLookupStatus.initial;
  String? _errorMessage;

  Merchant? get merchant => _merchant;
  MerchantLookupStatus get status => _status;
  String? get errorMessage => _errorMessage;

  Future<void> lookup(String code) async {
    _status = MerchantLookupStatus.loading;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.getByCode(code);
    if (result.success) {
      _merchant = result.data;
      _status = MerchantLookupStatus.loaded;
    } else {
      _errorMessage = result.error?.message ?? 'Marchand introuvable';
      _status = MerchantLookupStatus.error;
    }
    notifyListeners();
  }

  void clear() {
    _merchant = null;
    _status = MerchantLookupStatus.initial;
    _errorMessage = null;
    notifyListeners();
  }
}
