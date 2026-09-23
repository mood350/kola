import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/payment_method.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/payment_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late PaymentService service;

  setUp(() {
    apiClient = MockApiClient();
    service = PaymentService(apiClient: apiClient);
  });

  test(
    'getMethods() decodes the operators and the provider availability flag',
    () async {
      when(
        () => apiClient.get<PaymentMethods>(
          '/payments/methods',
          query: any(named: 'query'),
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode]
                as PaymentMethods Function(dynamic);
        return ApiResult.ok(
          decode({
            'available': [
              {'code': 'MOOV_TOGO', 'label': 'Moov Togo', 'countryCode': 'TG'},
              {
                'code': 'TOGOCEL',
                'label': 'Togocel T-Money',
                'countryCode': 'TG',
              },
            ],
            'all': [
              {'code': 'MTN_BENIN', 'label': 'MTN Bénin', 'countryCode': 'BJ'},
            ],
            'countryCode': 'TG',
            'providerEnabled': true,
          }),
        );
      });

      final result = await service.getMethods();

      expect(result.success, true);
      expect(result.data!.available.length, 2);
      expect(result.data!.available.first.code, 'MOOV_TOGO');
      expect(
        result.data!.available.first.label,
        'Moov Togo',
        reason:
            'le libellé vient du backend : le mobile ne recopie pas la liste',
      );
      expect(result.data!.providerEnabled, true);
      expect(result.data!.countryCode, 'TG');
    },
  );

  test('a response without providerEnabled is read as unavailable', () {
    final methods = PaymentMethods.fromJson({
      'available': [],
      'all': [],
      'countryCode': 'TG',
    });

    expect(
      methods.providerEnabled,
      false,
      reason:
          'en cas de doute on n\'offre pas le paiement réel : '
          'proposer un opérateur pour refuser ensuite est pire que ne rien proposer',
    );
  });
}
