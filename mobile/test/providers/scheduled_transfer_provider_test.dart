import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/scheduled_transfer.dart';
import 'package:mobile/providers/scheduled_transfer_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/scheduled_transfer_service.dart';
import 'package:mocktail/mocktail.dart';

class MockScheduledTransferService extends Mock
    implements ScheduledTransferService {}

ScheduledTransfer _transfer({
  int id = 1,
  ScheduleStatus status = ScheduleStatus.active,
}) => ScheduledTransfer(
  id: id,
  frequency: ScheduleFrequency.monthly,
  executionDay: 15,
  amount: 5000,
  currency: 'XOF',
  status: status,
);

void main() {
  setUpAll(() {
    registerFallbackValue(ScheduleFrequency.monthly);
  });

  late MockScheduledTransferService service;
  late ScheduledTransferProvider provider;

  setUp(() {
    service = MockScheduledTransferService();
    provider = ScheduledTransferProvider(service: service);
  });

  test('loadTransfers() alimente la liste en cas de succès', () async {
    when(
      () => service.getMine(),
    ).thenAnswer((_) async => ApiResult.ok([_transfer()]));

    await provider.loadTransfers();

    expect(provider.transfers, hasLength(1));
    expect(provider.isLoading, false);
    expect(provider.errorMessage, isNull);
  });

  test('loadTransfers() expose le message d\'erreur en cas d\'échec', () async {
    when(() => service.getMine()).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'NETWORK_ERROR', message: 'Erreur réseau'),
      ),
    );

    await provider.loadTransfers();

    expect(provider.transfers, isEmpty);
    expect(provider.errorMessage, 'Erreur réseau');
  });

  test('create() recharge la liste après succès', () async {
    when(
      () => service.getMine(),
    ).thenAnswer((_) async => ApiResult.ok([_transfer()]));
    when(
      () => service.create(
        walletId: any(named: 'walletId'),
        targetVaultId: any(named: 'targetVaultId'),
        frequency: any(named: 'frequency'),
        executionDay: any(named: 'executionDay'),
        amount: any(named: 'amount'),
        description: any(named: 'description'),
      ),
    ).thenAnswer((_) async => ApiResult.ok(_transfer()));

    final ok = await provider.create(
      walletId: 1,
      frequency: ScheduleFrequency.monthly,
      executionDay: 15,
      amount: 5000,
    );

    expect(ok, true);
    expect(provider.transfers, hasLength(1));
    expect(provider.isSubmitting, false);
  });

  test('pause() remonte l\'erreur backend sans planter', () async {
    when(() => service.pause(1)).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'ENTITY_NOT_FOUND', message: 'Introuvable'),
      ),
    );

    final ok = await provider.pause(1);

    expect(ok, false);
    expect(provider.errorMessage, 'Introuvable');
  });

  test('clear() vide la liste', () async {
    when(
      () => service.getMine(),
    ).thenAnswer((_) async => ApiResult.ok([_transfer()]));
    await provider.loadTransfers();

    provider.clear();

    expect(provider.transfers, isEmpty);
  });
}
