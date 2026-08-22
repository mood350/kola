import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/merchant.dart';
import 'package:mobile/providers/merchant_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/merchant_service.dart';
import 'package:mocktail/mocktail.dart';

class MockMerchantService extends Mock implements MerchantService {}

const _merchant = Merchant(
  id: 1,
  name: 'Boutique Kola',
  category: 'Commerce général',
  merchantCode: 'MERCHANT001',
);

void main() {
  late MockMerchantService service;
  late MerchantProvider provider;

  setUp(() {
    service = MockMerchantService();
    provider = MerchantProvider(merchantService: service);
  });

  test('lookup() transitions to loaded with the fetched merchant', () async {
    when(
      () => service.getByCode('MERCHANT001'),
    ).thenAnswer((_) async => const ApiResult.ok(_merchant));

    await provider.lookup('MERCHANT001');

    expect(provider.status, MerchantLookupStatus.loaded);
    expect(provider.merchant, _merchant);
  });

  test('lookup() transitions to error on failure', () async {
    when(() => service.getByCode('UNKNOWN')).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'ENTITY_NOT_FOUND', message: 'Marchand introuvable'),
      ),
    );

    await provider.lookup('UNKNOWN');

    expect(provider.status, MerchantLookupStatus.error);
    expect(provider.errorMessage, 'Marchand introuvable');
  });

  test('clear() resets the merchant and status', () async {
    when(
      () => service.getByCode('MERCHANT001'),
    ).thenAnswer((_) async => const ApiResult.ok(_merchant));
    await provider.lookup('MERCHANT001');

    provider.clear();

    expect(provider.merchant, isNull);
    expect(provider.status, MerchantLookupStatus.initial);
  });
}
