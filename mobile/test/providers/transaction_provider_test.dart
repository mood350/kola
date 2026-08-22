import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/page_response.dart';
import 'package:mobile/models/transaction.dart';
import 'package:mobile/providers/transaction_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/transaction_service.dart';
import 'package:mocktail/mocktail.dart';

class MockTransactionService extends Mock implements TransactionService {}

Transaction _tx(int id, {String reference = 'TX-1'}) => Transaction(
  id: id,
  reference: reference,
  type: TransactionType.deposit,
  status: TransactionStatus.success,
  amount: 1000,
  fee: 0,
  currency: 'XOF',
  createdAt: DateTime(2026, 1, 1),
);

void main() {
  late MockTransactionService service;
  late TransactionProvider provider;

  setUp(() {
    service = MockTransactionService();
    provider = TransactionProvider(transactionService: service);
  });

  test(
    'loadHistory() replaces the list and tracks hasMoreHistory from the page',
    () async {
      when(() => service.getWalletHistory(walletId: 10, page: 0)).thenAnswer(
        (_) async => ApiResult.ok(
          PageResponse<Transaction>(
            content: [_tx(1)],
            totalElements: 2,
            totalPages: 2,
            number: 0,
            size: 1,
            last: false,
          ),
        ),
      );

      await provider.loadHistory(10);

      expect(provider.history, hasLength(1));
      expect(provider.hasMoreHistory, true);
    },
  );

  test(
    'loadMoreHistory() appends the next page and stops when last is true',
    () async {
      when(() => service.getWalletHistory(walletId: 10, page: 0)).thenAnswer(
        (_) async => ApiResult.ok(
          PageResponse<Transaction>(
            content: [_tx(1)],
            totalElements: 2,
            totalPages: 2,
            number: 0,
            size: 1,
            last: false,
          ),
        ),
      );
      await provider.loadHistory(10);

      when(() => service.getWalletHistory(walletId: 10, page: 1)).thenAnswer(
        (_) async => ApiResult.ok(
          PageResponse<Transaction>(
            content: [_tx(2)],
            totalElements: 2,
            totalPages: 2,
            number: 1,
            size: 1,
            last: true,
          ),
        ),
      );
      await provider.loadMoreHistory();

      expect(provider.history, hasLength(2));
      expect(provider.hasMoreHistory, false);
    },
  );

  test('loadDetail() populates the detail transaction', () async {
    when(
      () => service.getByReference('TX-1'),
    ).thenAnswer((_) async => ApiResult.ok(_tx(1)));

    await provider.loadDetail('TX-1');

    expect(provider.detail?.reference, 'TX-1');
    expect(provider.detailError, isNull);
  });

  test("withdraw() transmet telle quelle la cle fournie par l'ecran", () async {
    when(
      () => service.withdraw(
        walletId: any(named: 'walletId'),
        amount: any(named: 'amount'),
        idempotencyKey: any(named: 'idempotencyKey'),
      ),
    ).thenAnswer((_) async => ApiResult.ok(_tx(1)));

    await provider.withdraw(
      walletId: 10,
      amount: 5000,
      idempotencyKey: 'cle-de-l-ecran',
    );

    final capturee = verify(
      () => service.withdraw(
        walletId: any(named: 'walletId'),
        amount: any(named: 'amount'),
        idempotencyKey: captureAny(named: 'idempotencyKey'),
      ),
    ).captured.single;

    // Le provider generait sa propre cle a chaque appel, donc un re-tap sur
    // « Reessayer » repartait avec une cle neuve et le backend executait un
    // SECOND mouvement d'argent. La cle appartient desormais a l'ecran, qui
    // la garde stable entre deux tentatives : le provider ne doit que la
    // relayer, sans jamais en fabriquer une.
    expect(capturee, 'cle-de-l-ecran');
  });

  test('deposit() transitions actionStatus to success', () async {
    when(
      () => service.deposit(
        walletId: any(named: 'walletId'),
        amount: any(named: 'amount'),
        idempotencyKey: any(named: 'idempotencyKey'),
      ),
    ).thenAnswer((_) async => ApiResult.ok(_tx(1)));

    final success = await provider.deposit(
      walletId: 10,
      amount: 5000,
      idempotencyKey: 'cle-depot',
    );

    expect(success, true);
    expect(provider.actionStatus, TxActionStatus.success);
  });

  test(
    'withdraw() transitions actionStatus to error with the message',
    () async {
      when(
        () => service.withdraw(
          walletId: any(named: 'walletId'),
          amount: any(named: 'amount'),
          idempotencyKey: any(named: 'idempotencyKey'),
        ),
      ).thenAnswer(
        (_) async => const ApiResult.fail(
          ApiException(
            code: 'INSUFFICIENT_FUNDS',
            message: 'Solde insuffisant',
          ),
        ),
      );

      final success = await provider.withdraw(
        walletId: 10,
        amount: 999999,
        idempotencyKey: 'cle-retrait',
      );

      expect(success, false);
      expect(provider.actionStatus, TxActionStatus.error);
      expect(provider.actionError, 'Solde insuffisant');
    },
  );
}
