import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/transaction_display.dart';
import 'package:mobile/models/transaction.dart';

KolaTransaction _tx(String? description) => KolaTransaction(
  id: '1',
  reference: 'TXN-1',
  type: 'VAULT_DEPOSIT',
  status: 'COMPLETED',
  currency: 'XOF',
  amount: 1000,
  fee: 0,
  totalDebited: 1000,
  createdAt: DateTime(2026, 9, 30),
  description: description,
);

void main() {
  group('transactionNote', () {
    test('masque les libellés anglais écrits par le backend', () {
      for (final label in [
        'Cash-in',
        'Cash-out',
        'Bill payment',
        'Vault deposit',
        'Vault withdrawal',
      ]) {
        expect(transactionNote(_tx(label)), isNull, reason: label);
      }
    });

    test('masque les identifiants techniques', () {
      const uuid = '3f2b8c1e-9d4a-4b7e-8a51-0c6d2e9f1a77';
      expect(transactionNote(_tx('Loan $uuid')), isNull);
      expect(transactionNote(_tx('Scheduled task $uuid')), isNull);
      expect(transactionNote(_tx('task:$uuid:3')), isNull);
    });

    test('traduit un remboursement de litige', () {
      expect(
        transactionNote(_tx('Chargeback TXN-123')),
        'Remboursement suite à un litige',
      );
    });

    test('garde le motif saisi par la personne', () {
      expect(
        transactionNote(_tx('  Loyer de septembre ')),
        'Loyer de septembre',
      );
    });

    test('description absente ou vide', () {
      expect(transactionNote(_tx(null)), isNull);
      expect(transactionNote(_tx('   ')), isNull);
    });
  });
}
