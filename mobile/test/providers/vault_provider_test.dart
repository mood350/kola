import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/vault.dart';
import 'package:mobile/providers/vault_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/vault_service.dart';
import 'package:mocktail/mocktail.dart';

class MockVaultService extends Mock implements VaultService {}

const _vault = Vault(
  id: 1,
  name: 'Vacances',
  currentAmount: 5000,
  currency: 'XOF',
  status: VaultStatus.active,
  walletId: 10,
);

void main() {
  late MockVaultService service;
  late VaultProvider provider;

  setUp(() {
    service = MockVaultService();
    provider = VaultProvider(vaultService: service);
  });

  test('loadVaults() populates the vault list on success', () async {
    when(
      () => service.getMyVaults(),
    ).thenAnswer((_) async => const ApiResult.ok([_vault]));

    await provider.loadVaults();

    expect(provider.vaults, [_vault]);
    expect(provider.isLoading, false);
    expect(provider.errorMessage, isNull);
  });

  test('createVault() reloads the list on success', () async {
    when(
      () => service.getMyVaults(),
    ).thenAnswer((_) async => const ApiResult.ok([_vault]));
    when(
      () => service.createVault(
        walletId: any(named: 'walletId'),
        name: any(named: 'name'),
        purpose: any(named: 'purpose'),
        targetAmount: any(named: 'targetAmount'),
        initialAmount: any(named: 'initialAmount'),
        unlockDate: any(named: 'unlockDate'),
      ),
    ).thenAnswer((_) async => const ApiResult.ok(_vault));

    final success = await provider.createVault(
      walletId: 10,
      name: 'Vacances',
      initialAmount: 5000,
    );

    expect(success, true);
    expect(provider.vaults, [_vault]);
    expect(provider.isSubmitting, false);
  });

  test('addFunds() reports failure and keeps the error message', () async {
    when(
      () => service.addFunds(
        vaultId: any(named: 'vaultId'),
        amount: any(named: 'amount'),
      ),
    ).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'VAULT_LOCKED', message: 'Coffre verrouillé'),
      ),
    );

    final success = await provider.addFunds(vaultId: 1, amount: 100);

    expect(success, false);
    expect(provider.errorMessage, 'Coffre verrouillé');
  });

  test('clear() resets the vault list', () async {
    when(
      () => service.getMyVaults(),
    ).thenAnswer((_) async => const ApiResult.ok([_vault]));
    await provider.loadVaults();

    provider.clear();

    expect(provider.vaults, isEmpty);
  });
}
