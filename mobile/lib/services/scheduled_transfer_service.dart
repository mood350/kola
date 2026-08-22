import '../models/scheduled_transfer.dart';
import 'api_client.dart';

/// Service des virements programmés (cf. ScheduledTransferController backend).
class ScheduledTransferService {
  ScheduledTransferService({ApiClient? apiClient})
    : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<List<ScheduledTransfer>>> getMine() {
    return _api.get<List<ScheduledTransfer>>(
      '/scheduled-transfers',
      decode: (json) => (json as List<dynamic>)
          .map((e) => ScheduledTransfer.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<ApiResult<ScheduledTransfer>> create({
    required int walletId,
    int? targetVaultId,
    required ScheduleFrequency frequency,
    required int executionDay,
    required double amount,
    String? description,
  }) {
    return _api.post<ScheduledTransfer>(
      '/scheduled-transfers',
      body: {
        'walletId': walletId,
        'targetVaultId': ?targetVaultId,
        'frequency': frequency.toBackend(),
        'executionDay': executionDay,
        'amount': amount,
        'description': ?description,
      },
      decode: (json) =>
          ScheduledTransfer.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<ScheduledTransfer>> pause(int id) {
    return _api.post<ScheduledTransfer>(
      '/scheduled-transfers/$id/pause',
      decode: (json) =>
          ScheduledTransfer.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<ScheduledTransfer>> resume(int id) {
    return _api.post<ScheduledTransfer>(
      '/scheduled-transfers/$id/resume',
      decode: (json) =>
          ScheduledTransfer.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<void>> delete(int id) {
    return _api.deleteEmpty('/scheduled-transfers/$id');
  }
}
