import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/credit_score.dart';
import 'package:mobile/models/loan.dart';
import 'package:mobile/models/loan_capacity.dart';
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

  test(
    'getCapacity() decodes the per-duration amounts and the review threshold',
    () async {
      when(
        () => apiClient.get<LoanCapacity>(
          '/credit/capacity',
          decode: any(named: 'decode'),
        ),
      ).thenAnswer((invocation) async {
        final decode =
            invocation.namedArguments[#decode]
                as LoanCapacity Function(dynamic);
        return ApiResult.ok(
          decode({
            'monthlyInflow': 90000,
            'monthlyOutflow': 30000,
            'monthlyDisposable': 60000,
            'activeMonths': 3,
            'stabilityFactor': 1,
            'tierCeiling': 500000,
            'graduationCeiling': 50000,
            // Les clés arrivent en chaînes : c'est la conversion que ce test
            // protège, une régression y rendrait tout plafond nul.
            'maxAmountByDuration': {'1': 22900, '6': 50000, '12': 50000},
            'limitingFactor': 'GRADUATION',
            'limitingFactorLabel': 'Le plafond des premiers prêts',
            'manualReviewThreshold': 200000,
          }),
        );
      });

      final result = await service.getCapacity();

      expect(result.success, true);
      expect(result.data!.maxAmountFor(1), 22900);
      expect(result.data!.maxAmountFor(6), 50000);
      expect(
        result.data!.maxAmountFor(7),
        0,
        reason: 'une durée absente vaut zéro, jamais null',
      );
      expect(result.data!.manualReviewThreshold, 200000);
      expect(result.data!.isEmpty, false);
    },
  );

  test('a capacity with only zeros is reported as empty', () {
    const capacity = LoanCapacity(
      monthlyInflow: 0,
      monthlyOutflow: 0,
      monthlyDisposable: 0,
      activeMonths: 0,
      stabilityFactor: 0,
      tierCeiling: 0,
      graduationCeiling: 50000,
      maxAmountByDuration: {1: 0, 6: 0},
      limitingFactor: 'NO_ACTIVITY',
      limitingFactorLabel: 'Activité insuffisante',
      manualReviewThreshold: 200000,
    );

    expect(capacity.isEmpty, true);
  });
}
