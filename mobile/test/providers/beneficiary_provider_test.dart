import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/beneficiary.dart';
import 'package:mobile/providers/beneficiary_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/beneficiary_service.dart';
import 'package:mocktail/mocktail.dart';

class MockBeneficiaryService extends Mock implements BeneficiaryService {}

const _beneficiary = Beneficiary(
  id: 1,
  alias: 'Maman',
  phoneNumber: '+221770000000',
  countryCode: 'SN',
  network: MobileNetwork.orangeMoney,
);

void main() {
  setUpAll(() {
    registerFallbackValue(MobileNetwork.orangeMoney);
  });

  late MockBeneficiaryService service;
  late BeneficiaryProvider provider;

  setUp(() {
    service = MockBeneficiaryService();
    provider = BeneficiaryProvider(beneficiaryService: service);
  });

  test('loadBeneficiaries() populates the list on success', () async {
    when(
      () => service.getMyBeneficiaries(),
    ).thenAnswer((_) async => const ApiResult.ok([_beneficiary]));

    await provider.loadBeneficiaries();

    expect(provider.beneficiaries, [_beneficiary]);
    expect(provider.errorMessage, isNull);
  });

  test(
    'addBeneficiary() returns the created beneficiary and reloads the list',
    () async {
      when(
        () => service.getMyBeneficiaries(),
      ).thenAnswer((_) async => const ApiResult.ok([_beneficiary]));
      when(
        () => service.createBeneficiary(
          alias: any(named: 'alias'),
          phoneNumber: any(named: 'phoneNumber'),
          countryCode: any(named: 'countryCode'),
          network: any(named: 'network'),
        ),
      ).thenAnswer((_) async => const ApiResult.ok(_beneficiary));

      final result = await provider.addBeneficiary(
        alias: 'Maman',
        phoneNumber: '+221770000000',
        countryCode: 'SN',
        network: MobileNetwork.orangeMoney,
      );

      expect(result, _beneficiary);
      expect(provider.beneficiaries, [_beneficiary]);
    },
  );

  test('removeBeneficiary() removes the entry locally on success', () async {
    when(
      () => service.getMyBeneficiaries(),
    ).thenAnswer((_) async => ApiResult.ok(<Beneficiary>[_beneficiary]));
    await provider.loadBeneficiaries();

    when(
      () => service.deleteBeneficiary(1),
    ).thenAnswer((_) async => const ApiResult<void>.ok(null));

    final success = await provider.removeBeneficiary(1);

    expect(success, true);
    expect(provider.beneficiaries, isEmpty);
  });

  test('clear() resets the list', () async {
    when(
      () => service.getMyBeneficiaries(),
    ).thenAnswer((_) async => const ApiResult.ok([_beneficiary]));
    await provider.loadBeneficiaries();

    provider.clear();

    expect(provider.beneficiaries, isEmpty);
  });
}
