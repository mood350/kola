import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/payment_method.dart';
import 'package:mobile/providers/payment_method_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/payment_service.dart';
import 'package:mocktail/mocktail.dart';

class MockPaymentService extends Mock implements PaymentService {}

const _methods = PaymentMethods(
  available: [
    PaymentMethod(code: 'MOOV_TOGO', label: 'Moov Togo', countryCode: 'TG'),
  ],
  all: [
    PaymentMethod(code: 'MOOV_TOGO', label: 'Moov Togo', countryCode: 'TG'),
    PaymentMethod(code: 'MTN_BENIN', label: 'MTN Bénin', countryCode: 'BJ'),
  ],
  countryCode: 'TG',
  providerEnabled: true,
);

void main() {
  late MockPaymentService service;
  late PaymentMethodProvider provider;

  setUp(() {
    service = MockPaymentService();
    provider = PaymentMethodProvider(paymentService: service);
  });

  test('load() exposes the operators of the user country', () async {
    when(
      () => service.getMethods(country: any(named: 'country')),
    ).thenAnswer((_) async => const ApiResult.ok(_methods));

    await provider.load();

    expect(provider.available.length, 1);
    expect(provider.available.first.label, 'Moov Togo');
    expect(provider.providerEnabled, true);
    expect(provider.error, isNull);
  });

  test('load() does not call the API twice for nothing', () async {
    when(
      () => service.getMethods(country: any(named: 'country')),
    ).thenAnswer((_) async => const ApiResult.ok(_methods));

    await provider.load();
    await provider.load();

    verify(() => service.getMethods(country: any(named: 'country'))).called(1);
  });

  test('load(force: true) refetches', () async {
    when(
      () => service.getMethods(country: any(named: 'country')),
    ).thenAnswer((_) async => const ApiResult.ok(_methods));

    await provider.load();
    await provider.load(force: true);

    verify(() => service.getMethods(country: any(named: 'country'))).called(2);
  });

  test(
    'a failure leaves the provider disabled rather than half-loaded',
    () async {
      when(() => service.getMethods(country: any(named: 'country'))).thenAnswer(
        (_) async => const ApiResult.fail(
          ApiException(code: 'UNKNOWN', message: 'Service indisponible'),
        ),
      );

      await provider.load();

      expect(provider.available, isEmpty);
      expect(
        provider.providerEnabled,
        false,
        reason: 'sans liste, aucun paiement réel ne doit être proposé',
      );
      expect(provider.error, 'Service indisponible');
    },
  );
}
