import 'package:flutter/foundation.dart';
import '../models/vault.dart';
import '../services/api_client.dart';
import '../services/vault_service.dart';

/// Provider gérant la liste des coffres-forts et leurs actions
/// (création, ajout de fonds, déblocage, fermeture anticipée).
class VaultProvider extends ChangeNotifier {
  VaultProvider({VaultService? vaultService})
    : _service = vaultService ?? VaultService();

  final VaultService _service;

  List<Vault> _vaults = [];
  bool _isLoading = false;
  bool _isSubmitting = false;
  String? _errorMessage;

  List<Vault> get vaults => _vaults;
  bool get isLoading => _isLoading;
  bool get isSubmitting => _isSubmitting;
  String? get errorMessage => _errorMessage;

  Future<void> loadVaults() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.getMyVaults();
    if (result.success) {
      _vaults = result.data ?? [];
    } else {
      _errorMessage =
          result.error?.message ?? 'Impossible de charger les coffres';
    }
    _isLoading = false;
    notifyListeners();
  }

  Future<bool> createVault({
    required int walletId,
    required String name,
    String? purpose,
    double? targetAmount,
    required double initialAmount,
    DateTime? unlockDate,
  }) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.createVault(
      walletId: walletId,
      name: name,
      purpose: purpose,
      targetAmount: targetAmount,
      initialAmount: initialAmount,
      unlockDate: unlockDate,
    );

    if (result.success) {
      await loadVaults();
      _isSubmitting = false;
      notifyListeners();
      return true;
    }
    _errorMessage = result.error?.message ?? 'Impossible de créer le coffre';
    _isSubmitting = false;
    notifyListeners();
    return false;
  }

  Future<bool> addFunds({required int vaultId, required double amount}) async {
    return _mutate(() => _service.addFunds(vaultId: vaultId, amount: amount));
  }

  Future<bool> unlock(int vaultId) async {
    return _mutate(() => _service.unlock(vaultId));
  }

  Future<bool> closeEarly(int vaultId) async {
    return _mutate(() => _service.closeEarly(vaultId));
  }

  Future<bool> _mutate<T>(Future<ApiResult<T>> Function() action) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await action();
    if (result.success) {
      await loadVaults();
      _isSubmitting = false;
      notifyListeners();
      return true;
    }
    _errorMessage = result.error?.message ?? "Échec de l'opération";
    _isSubmitting = false;
    notifyListeners();
    return false;
  }

  void clear() {
    _vaults = [];
    _errorMessage = null;
    notifyListeners();
  }
}
