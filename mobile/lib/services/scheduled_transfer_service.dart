import '../models/json.dart';
import '../models/scheduled_transfer.dart';
import 'api_client.dart';

class SchedulingService {
  SchedulingService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<List<ScheduledTask>>> list() {
    return _client.get(
      '/api/v1/scheduling/tasks/me',
      decode: (json) => asList(json, ScheduledTask.fromJson),
    );
  }

  Future<ApiResult<List<Biller>>> billers() {
    return _client.get(
      '/api/v1/scheduling/tasks/billers',
      decode: (json) => asList(json, Biller.fromJson),
    );
  }

  Future<ApiResult<ScheduledTask>> create({
    required String type,
    required String frequency,
    required double amount,
    required String beneficiaryReference,
    required String firstRunAt,
    String? description,
    String? endDate,
    int? maxOccurrences,
    String? fundingVaultId,
    String? biller,
    int? dayOfMonth,
  }) {
    return _client.post(
      '/api/v1/scheduling/tasks',
      body: {
        'type': type,
        'frequency': frequency,
        'amount': amount,
        'currency': 'XOF',
        'beneficiaryReference': beneficiaryReference,
        'firstRunAt': firstRunAt,
        if (description != null && description.isNotEmpty)
          'description': description,
        'endDate': ?endDate,
        'maxOccurrences': ?maxOccurrences,
        'fundingVaultId': ?fundingVaultId,
        'biller': ?biller,
        'dayOfMonth': ?dayOfMonth,
      },
      decode: (json) =>
          ScheduledTask.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<ScheduledTask>> pause(String id) => _patch(id, 'pause');
  Future<ApiResult<ScheduledTask>> resume(String id) => _patch(id, 'resume');
  Future<ApiResult<ScheduledTask>> cancel(String id) => _patch(id, 'cancel');

  Future<ApiResult<ScheduledTask>> _patch(String id, String action) {
    return _client.patch(
      '/api/v1/scheduling/tasks/$id/$action',
      decode: (json) =>
          ScheduledTask.fromJson((json as Map).cast<String, dynamic>()),
    );
  }
}
