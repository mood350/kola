import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/credit_score.dart';
import 'package:mobile/models/loan.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/credit_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late CreditService service;

  setUp(() {
    apiClient = MockApiClient();
    service = CreditService(apiClient: apiClient);
  });

  test(
    'getScore() decodes the score breakdown including rule details',
    () async {
      when(
        () => apiClient.get<CreditScoreBreakdown>(
          '/credit/score',
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode]
                as CreditScoreBreakdown Function(dynamic);
        return ApiResult.ok(
          decode({
            'totalScore': 62,
            'tier': 'STANDARD',
            'maxLoanAmount': 100000,
            'monthlyRate': 0.02,
            'computedAt': '2026-01-01T00:00:00',
            'expiresAt': '2026-01-31T00:00:00',
            'details': [
              {
                'rule': 'KYC_LEVEL',
                'label': 'Niveau KYC',
                'points': 7,
                'maxPoints': 15,
                'explanation': 'Niveau KYC : TIER_1',
              },
            ],
          }),
        );
      });

      final result = await service.getScore();

      expect(result.success, true);
      expect(result.data!.tier, CreditTier.standard);
      expect(result.data!.details, hasLength(1));
      expect(result.data!.details.first.points, 7);
    },
  );

  test('applyForLoan() sends the loan application fields', () async {
    Map<String, dynamic>? sentBody;
    when(
      () => apiClient.post<Loan>(
        '/credit/loans',
        body: any(named: 'body'),
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      sentBody = invocation.namedArguments[#body] as Map<String, dynamic>;
      final decode =
          invocation.namedArguments[#decode] as Loan Function(dynamic);
      return ApiResult.ok(
        decode({
          'id': 1,
          'requestedAmount': 20000,
          'totalRepayment': 20600,
          'monthlyRate': 0.03,
          'durationMonths': 1,
          'status': 'PENDING',
          'purpose': null,
          'dueDate': null,
          'rejectionReason': null,
          'creditScoreAtRequest': 45,
          'tierAtRequest': 'BASIC',
        }),
      );
    });

    final result = await service.applyForLoan(
      walletId: 10,
      requestedAmount: 20000,
      durationMonths: 1,
    );

    expect(result.success, true);
    expect(sentBody!['walletId'], 10);
    expect(sentBody!['requestedAmount'], 20000);
    expect(result.data!.status, LoanStatus.pending);
  });

  test(
    'applyForLoan() surfaces INSUFFICIENT_CREDIT_SCORE via the error code',
    () async {
      when(
        () => apiClient.post<Loan>(
          '/credit/loans',
          body: any(named: 'body'),
          decode: any(named: 'decode'),
        ),
      ).thenAnswer(
        (_) async => const ApiResult.fail(
          ApiException(
            code: 'INSUFFICIENT_CREDIT_SCORE',
            message: 'Score insuffisant',
          ),
        ),
      );

      final result = await service.applyForLoan(
        walletId: 10,
        requestedAmount: 500000,
        durationMonths: 6,
      );

      expect(result.success, false);
      expect(result.error?.code, 'INSUFFICIENT_CREDIT_SCORE');
    },
  );
}
