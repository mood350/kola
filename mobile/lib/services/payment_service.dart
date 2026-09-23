import '../models/payment_method.dart';
import 'api_client.dart';

/// Moyens de paiement disponibles (cf. PaymentMethodController backend).
class PaymentService {
  PaymentService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  /// GET /payments/methods — sans `country`, ceux du pays de l'utilisateur.
  Future<ApiResult<PaymentMethods>> getMethods({String? country}) {
    return _api.get<PaymentMethods>(
      '/payments/methods',
      query: country != null ? {'country': country} : null,
      decode: (json) => PaymentMethods.fromJson(json as Map<String, dynamic>),
    );
  }
}
