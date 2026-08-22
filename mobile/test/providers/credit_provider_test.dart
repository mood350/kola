import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/credit_score.dart';
import 'package:mobile/models/loan.dart';
import 'package:mobile/providers/credit_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/credit_service.dart';
import 'package:mocktail/mocktail.dart';

class MockCreditService extends Mock implements CreditService {}

const _score = CreditScoreBreakdown(
  totalScore: 62,
  tier: CreditTier.standard,
  maxLoanAmount: 100000,
  monthlyRate: 0.02,
  details: [],
);

const _loan = Loan(
  id: 1,
  requestedAmount: 20000,
  totalRepayment: 20600,
  monthlyRate: 0.03,
  durationMonths: 1,
  status: LoanStatus.disbursed,
  creditScoreAtRequest: 62,
  tierAtRequest: CreditTier.standard,
);

void main() {
  late MockCreditService service;
  late CreditProvider provider;

  setUp(() {
    service = MockCreditService();
    provider = CreditProvider(creditService: service);
  });

  test('loadScore() populates the score breakdown', () async {
    when(
      () => service.getScore(),
    ).thenAnswer((_) async => const ApiResult.ok(_score));

    await provider.loadScore();

    expect(provider.score, _score);
    expect(provider.scoreError, isNull);
  });

  test(
    'applyForLoan() surfaces both the message and the error code on failure',
    () async {
      when(
        () => service.applyForLoan(
          walletId: any(named: 'walletId'),
          requestedAmount: any(named: 'requestedAmount'),
          durationMonths: any(named: 'durationMonths'),
          purpose: any(named: 'purpose'),
        ),
      ).thenAnswer(
        (_) async => const ApiResult.fail(
          ApiException(code: 'ACTIVE_LOAN_EXISTS', message: 'Prêt déjà actif'),
        ),
      );

      final success = await provider.applyForLoan(
        walletId: 10,
        requestedAmount: 20000,
        durationMonths: 3,
      );

      expect(success, false);
      expect(provider.submitError, 'Prêt déjà actif');
      expect(provider.submitErrorCode, 'ACTIVE_LOAN_EXISTS');
    },
  );

  test('repay() reloads the loan list on success', () async {
    when(
      () => service.getMyLoans(),
    ).thenAnswer((_) async => const ApiResult.ok([_loan]));
    when(
      () => service.repay(1),
    ).thenAnswer((_) async => const ApiResult.ok(_loan));

    final success = await provider.repay(1);

    expect(success, true);
    expect(provider.loans, [_loan]);
  });

  test('clear() resets score, loans and errors', () async {
    when(
      () => service.getScore(),
    ).thenAnswer((_) async => const ApiResult.ok(_score));
    await provider.loadScore();

    provider.clear();

    expect(provider.score, isNull);
    expect(provider.loans, isEmpty);
  });
}
