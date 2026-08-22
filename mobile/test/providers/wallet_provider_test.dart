import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/wallet.dart';
import 'package:mobile/providers/wallet_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/transaction_service.dart';
import 'package:mobile/services/wallet_service.dart';
import 'package:mocktail/mocktail.dart';

class MockWalletService extends Mock implements WalletService {}

class MockTransactionService extends Mock implements TransactionService {}

const _activeWallet = Wallet(
  id: 1,
  currency: 'XOF',
  balance: 5000,
  lockedBalance: 0,
  availableBalance: 5000,
  active: true,
);

const _newWallet = Wallet(
  id: 2,
  currency: 'XOF',
  balance: 0,
  lockedBalance: 0,
  availableBalance: 0,
  active: true,
);

void main() {
  late MockWalletService walletService;
  late MockTransactionService transactionService;
  late WalletProvider provider;

  setUp(() {
    walletService = MockWalletService();
    transactionService = MockTransactionService();
    provider = WalletProvider(
      walletService: walletService,
      transactionService: transactionService,
    );
  });

  test(
    'loadHomeData() picks the first active wallet and loads its recent transactions',
    () async {
      when(
        () => walletService.getMyWallets(),
      ).thenAnswer((_) async => const ApiResult.ok([_activeWallet]));
      when(
        () => transactionService.getRecent(walletId: 1, limit: 3),
      ).thenAnswer((_) async => const ApiResult.ok([]));

      await provider.loadHomeData();

      expect(provider.primaryWallet, _activeWallet);
      expect(provider.balance, 5000);
      expect(provider.isLoading, false);
      expect(provider.errorMessage, isNull);
      verifyNever(() => walletService.createWallet());
    },
  );

  test('loadHomeData() auto-provisions a wallet when none exists', () async {
    when(
      () => walletService.getMyWallets(),
    ).thenAnswer((_) async => const ApiResult.ok([]));
    when(
      () => walletService.createWallet(),
    ).thenAnswer((_) async => const ApiResult.ok(_newWallet));
    when(
      () => transactionService.getRecent(walletId: 2, limit: 3),
    ).thenAnswer((_) async => const ApiResult.ok([]));

    await provider.loadHomeData();

    expect(provider.primaryWallet, _newWallet);
    verify(() => walletService.createWallet()).called(1);
  });

  test(
    'loadHomeData() surfaces an error message when GET /wallets fails',
    () async {
      when(() => walletService.getMyWallets()).thenAnswer(
        (_) async => const ApiResult.fail(
          ApiException(code: 'NETWORK_ERROR', message: 'Erreur réseau'),
        ),
      );

      await provider.loadHomeData();

      expect(provider.primaryWallet, isNull);
      expect(provider.errorMessage, 'Erreur réseau');
      expect(provider.isLoading, false);
    },
  );

  test('toggleBalanceVisibility() flips the visibility flag', () {
    expect(provider.isBalanceVisible, true);
    provider.toggleBalanceVisibility();
    expect(provider.isBalanceVisible, false);
  });
}
