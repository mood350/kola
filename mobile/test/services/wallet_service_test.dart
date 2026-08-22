import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/wallet.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/wallet_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late WalletService service;

  setUp(() {
    apiClient = MockApiClient();
    service = WalletService(apiClient: apiClient);
  });

  test('getMyWallets() calls GET /wallets and decodes the list', () async {
    when(
      () =>
          apiClient.get<List<Wallet>>('/wallets', decode: any(named: 'decode')),
    ).thenAnswer((invocation) async {
      final decode =
          invocation.namedArguments[#decode] as List<Wallet> Function(dynamic);
      return ApiResult.ok(
        decode([
          {
            'id': 1,
            'currency': 'XOF',
            'balance': 1000,
            'lockedBalance': 0,
            'availableBalance': 1000,
            'active': true,
          },
        ]),
      );
    });

    final result = await service.getMyWallets();

    expect(result.success, true);
    expect(result.data, hasLength(1));
    expect(result.data!.first.currency, 'XOF');
    expect(result.data!.first.balance, 1000);
  });

  test(
    'createWallet() posts the currency and decodes the created wallet',
    () async {
      when(
        () => apiClient.post<Wallet>(
          '/wallets',
          body: {'currency': 'XOF'},
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode] as Wallet Function(dynamic);
        return ApiResult.ok(
          decode({
            'id': 2,
            'currency': 'XOF',
            'balance': 0,
            'lockedBalance': 0,
            'availableBalance': 0,
            'active': true,
          }),
        );
      });

      final result = await service.createWallet();

      expect(result.success, true);
      expect(result.data!.id, 2);
    },
  );

  test('propagates a failed ApiResult without throwing', () async {
    when(
      () =>
          apiClient.get<List<Wallet>>('/wallets', decode: any(named: 'decode')),
    ).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'NETWORK_ERROR', message: 'Erreur réseau'),
      ),
    );

    final result = await service.getMyWallets();

    expect(result.success, false);
    expect(result.error?.code, 'NETWORK_ERROR');
  });
}
