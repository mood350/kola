import '../models/merchant.dart';
import 'api_client.dart';

/// Service gérant les appels API liés aux marchands (cf. MerchantController).
class MerchantService {
  MerchantService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<Merchant>> getByCode(String code) {
    return _api.get<Merchant>(
      '/merchants/$code',
      decode: (json) => Merchant.fromJson(json as Map<String, dynamic>),
    );
  }
}
