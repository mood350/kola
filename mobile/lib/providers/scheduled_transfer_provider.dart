import 'package:flutter/foundation.dart';
import '../models/scheduled_transfer.dart';
import '../services/api_client.dart';
import '../services/scheduled_transfer_service.dart';

/// Provider des virements programmés (liste, création, pause/reprise, suppression).
class ScheduledTransferProvider extends ChangeNotifier {
  ScheduledTransferProvider({ScheduledTransferService? service})
    : _service = service ?? ScheduledTransferService();

  final ScheduledTransferService _service;

  List<ScheduledTransfer> _transfers = [];
  bool _isLoading = false;
  bool _isSubmitting = false;
  String? _errorMessage;

  List<ScheduledTransfer> get transfers => _transfers;
  bool get isLoading => _isLoading;
  bool get isSubmitting => _isSubmitting;
  String? get errorMessage => _errorMessage;

  Future<void> loadTransfers() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.getMine();
    if (result.success) {
      _transfers = result.data ?? [];
    } else {
      _errorMessage =
          result.error?.message ??
          'Impossible de charger les virements programmés';
    }
    _isLoading = false;
    notifyListeners();
  }

  Future<bool> create({
    required int walletId,
    int? targetVaultId,
    required ScheduleFrequency frequency,
    required int executionDay,
    required double amount,
    String? description,
  }) async {
    return _mutate(
      () => _service.create(
        walletId: walletId,
        targetVaultId: targetVaultId,
        frequency: frequency,
        executionDay: executionDay,
        amount: amount,
        description: description,
      ),
    );
  }

  Future<bool> pause(int id) => _mutate(() => _service.pause(id));

  Future<bool> resume(int id) => _mutate(() => _service.resume(id));

  Future<bool> delete(int id) => _mutate(() => _service.delete(id));

  Future<bool> _mutate<T>(Future<ApiResult<T>> Function() action) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await action();
    if (result.success) {
      await loadTransfers();
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
    _transfers = [];
    _errorMessage = null;
    notifyListeners();
  }
}
