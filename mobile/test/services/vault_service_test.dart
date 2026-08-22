import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/vault.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/vault_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

Map<String, dynamic> _rawVault({String status = 'ACTIVE'}) => {
  'id': 1,
  'name': 'Vacances',
  'purpose': 'Voyage',
  'targetAmount': 100000,
  'currentAmount': 5000,
  'currency': 'XOF',
  'unlockDate': '2027-01-01',
  'status': status,
  'walletId': 10,
};

void main() {
  late MockApiClient apiClient;
  late VaultService service;

  setUp(() {
    apiClient = MockApiClient();
    service = VaultService(apiClient: apiClient);
  });

  test(
    'getMyVaults() decodes the vault list and maps status correctly',
    () async {
      when(
        () =>
            apiClient.get<List<Vault>>('/vaults', decode: any(named: 'decode')),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode] as List<Vault> Function(dynamic);
        return ApiResult.ok(decode([_rawVault(status: 'UNLOCKED')]));
      });

      final result = await service.getMyVaults();

      expect(result.success, true);
      expect(result.data!.first.status, VaultStatus.unlocked);
      expect(result.data!.first.currentAmount, 5000);
    },
  );

  test('createVault() sends the required fields plus optional ones', () async {
    Map<String, dynamic>? sentBody;
    when(
      () => apiClient.post<Vault>(
        '/vaults',
        body: any(named: 'body'),
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      sentBody = invocation.namedArguments[#body] as Map<String, dynamic>;
      final decode =
          invocation.namedArguments[#decode] as Vault Function(dynamic);
      return ApiResult.ok(decode(_rawVault()));
    });

    final result = await service.createVault(
      walletId: 10,
      name: 'Vacances',
      initialAmount: 5000,
      unlockDate: DateTime(2027, 1, 1),
    );

    expect(result.success, true);
    expect(sentBody!['walletId'], 10);
    expect(sentBody!['name'], 'Vacances');
    expect(sentBody!['initialAmount'], 5000);
    expect(sentBody!['unlockDate'], '2027-01-01');
    expect(sentBody!.containsKey('purpose'), false);
  });

  test('unlock() posts to /vaults/{id}/unlock', () async {
    when(
      () => apiClient.post<Vault>(
        '/vaults/1/unlock',
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      final decode =
          invocation.namedArguments[#decode] as Vault Function(dynamic);
      return ApiResult.ok(decode(_rawVault(status: 'UNLOCKED')));
    });

    final result = await service.unlock(1);

    expect(result.success, true);
    expect(result.data!.status, VaultStatus.unlocked);
  });
}
