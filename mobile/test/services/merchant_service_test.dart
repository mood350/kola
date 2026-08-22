import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/merchant.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/merchant_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late MerchantService service;

  setUp(() {
    apiClient = MockApiClient();
    service = MerchantService(apiClient: apiClient);
  });

  test(
    'getByCode() calls GET /merchants/{code} and decodes the merchant',
    () async {
      when(
        () => apiClient.get<Merchant>(
          '/merchants/MERCHANT001',
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode] as Merchant Function(dynamic);
        return ApiResult.ok(
          decode({
            'id': 1,
            'name': 'Boutique Kola',
            'category': 'Commerce général',
            'merchantCode': 'MERCHANT001',
          }),
        );
      });

      final result = await service.getByCode('MERCHANT001');

      expect(result.success, true);
      expect(result.data!.name, 'Boutique Kola');
      expect(result.data!.merchantCode, 'MERCHANT001');
    },
  );

  test('propagates a not-found error', () async {
    when(
      () => apiClient.get<Merchant>(
        '/merchants/UNKNOWN',
        decode: any(named: 'decode'),
      ),
    ).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'ENTITY_NOT_FOUND', message: 'Marchand introuvable'),
      ),
    );

    final result = await service.getByCode('UNKNOWN');

    expect(result.success, false);
    expect(result.error?.code, 'ENTITY_NOT_FOUND');
  });
}
