import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/page_response.dart';
import 'package:mobile/models/transaction.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/transaction_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

Map<String, dynamic> _rawTx({
  String type = 'DEPOSIT',
  String status = 'SUCCESS',
}) => {
  'id': 1,
  'reference': 'TX-001',
  'type': type,
  'status': status,
  'amount': 5000,
  'fee': 0,
  'currency': 'XOF',
  'receiverCurrency': null,
  'exchangeRate': null,
  'walletId': 10,
  'receiverPhoneNumber': null,
  'receiverCountryCode': null,
  'description': null,
  'idempotencyKey': null,
  'createdAt': '2026-01-01T10:00:00',
};

void main() {
  late MockApiClient apiClient;
  late TransactionService service;

  setUp(() {
    apiClient = MockApiClient();
    service = TransactionService(apiClient: apiClient);
  });

  test(
    'deposit() posts walletId and amount, decodes the transaction',
    () async {
      Map<String, dynamic>? sentBody;
      when(
        () => apiClient.post<Transaction>(
          '/transactions/deposit',
          body: any(named: 'body'),
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        sentBody = invocation.namedArguments[#body] as Map<String, dynamic>;
        final decode =
            invocation.namedArguments[#decode] as Transaction Function(dynamic);
        return ApiResult.ok(decode(_rawTx()));
      });

      final result = await service.deposit(walletId: 10, amount: 5000);

      expect(result.success, true);
      expect(sentBody!['walletId'], 10);
      expect(sentBody!['amount'], 5000);
      expect(result.data!.type, TransactionType.deposit);
      expect(result.data!.isCredit, true);
    },
  );

  test('getWalletHistory() decodes a paginated response', () async {
    when(
      () => apiClient.get<PageResponse<Transaction>>(
        '/transactions/wallet/10',
        query: {'page': '0', 'size': '20'},
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      final decode =
          invocation.namedArguments[#decode]
              as PageResponse<Transaction> Function(dynamic);
      return ApiResult.ok(
        decode({
          'content': [_rawTx(), _rawTx(type: 'WITHDRAWAL', status: 'PENDING')],
          'totalElements': 2,
          'totalPages': 1,
          'number': 0,
          'size': 20,
          'last': true,
        }),
      );
    });

    final result = await service.getWalletHistory(walletId: 10);

    expect(result.success, true);
    expect(result.data!.content, hasLength(2));
    expect(result.data!.last, true);
    expect(result.data!.content[1].status, TransactionStatus.pending);
  });

  test(
    'getRecent() reuses getWalletHistory with the given limit as size',
    () async {
      when(
        () => apiClient.get<PageResponse<Transaction>>(
          '/transactions/wallet/10',
          query: {'page': '0', 'size': '3'},
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode]
                as PageResponse<Transaction> Function(dynamic);
        return ApiResult.ok(
          decode({
            'content': [_rawTx()],
            'totalElements': 1,
            'totalPages': 1,
            'number': 0,
            'size': 3,
            'last': true,
          }),
        );
      });

      final result = await service.getRecent(walletId: 10, limit: 3);

      expect(result.success, true);
      expect(result.data, hasLength(1));
    },
  );
}
