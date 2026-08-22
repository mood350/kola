import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/beneficiary.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/beneficiary_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late BeneficiaryService service;

  setUp(() {
    apiClient = MockApiClient();
    service = BeneficiaryService(apiClient: apiClient);
  });

  test(
    'getMyBeneficiaries() decodes the list and maps the network enum',
    () async {
      when(
        () => apiClient.get<List<Beneficiary>>(
          '/beneficiaries',
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode]
                as List<Beneficiary> Function(dynamic);
        return ApiResult.ok(
          decode([
            {
              'id': 1,
              'alias': 'Maman',
              'phoneNumber': '+221770000000',
              'countryCode': 'SN',
              'network': 'ORANGE_MONEY',
            },
          ]),
        );
      });

      final result = await service.getMyBeneficiaries();

      expect(result.success, true);
      expect(result.data!.first.network, MobileNetwork.orangeMoney);
    },
  );

  test('createBeneficiary() sends the network in backend format', () async {
    Map<String, dynamic>? sentBody;
    when(
      () => apiClient.post<Beneficiary>(
        '/beneficiaries',
        body: any(named: 'body'),
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      sentBody = invocation.namedArguments[#body] as Map<String, dynamic>;
      final decode =
          invocation.namedArguments[#decode] as Beneficiary Function(dynamic);
      return ApiResult.ok(
        decode({
          'id': 1,
          'alias': 'Maman',
          'phoneNumber': '+221770000000',
          'countryCode': 'SN',
          'network': 'WAVE',
        }),
      );
    });

    final result = await service.createBeneficiary(
      alias: 'Maman',
      phoneNumber: '+221770000000',
      countryCode: 'SN',
      network: MobileNetwork.wave,
    );

    expect(result.success, true);
    expect(sentBody!['network'], 'WAVE');
  });

  test(
    'deleteBeneficiary() calls deleteEmpty on /beneficiaries/{id}',
    () async {
      when(
        () => apiClient.deleteEmpty('/beneficiaries/1'),
      ).thenAnswer((_) async => const ApiResult<void>.ok(null));

      final result = await service.deleteBeneficiary(1);

      expect(result.success, true);
    },
  );
}
