import '../models/beneficiary.dart';
import 'api_client.dart';

/// Service gérant les appels API liés aux bénéficiaires
/// (cf. BeneficiaryController backend).
class BeneficiaryService {
  BeneficiaryService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<List<Beneficiary>>> getMyBeneficiaries() {
    return _api.get<List<Beneficiary>>(
      '/beneficiaries',
      decode: (json) => (json as List<dynamic>)
          .map((e) => Beneficiary.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<ApiResult<Beneficiary>> getBeneficiary(int id) {
    return _api.get<Beneficiary>(
      '/beneficiaries/$id',
      decode: (json) => Beneficiary.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<Beneficiary>> createBeneficiary({
    required String alias,
    required String phoneNumber,
    required String countryCode,
    required MobileNetwork network,
  }) {
    return _api.post<Beneficiary>(
      '/beneficiaries',
      body: {
        'alias': alias,
        'phoneNumber': phoneNumber,
        'countryCode': countryCode,
        'network': network.toBackend(),
      },
      decode: (json) => Beneficiary.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<ApiResult<void>> deleteBeneficiary(int id) {
    return _api.deleteEmpty('/beneficiaries/$id');
  }
}
