import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mobile/core/utils/transaction_display.dart';
import 'package:mobile/core/widgets/transaction_tile.dart';
import 'package:mobile/models/transaction.dart';

const _uuid = 'ce3672bd-aae6-4962-b223-3647101c70b1';

KolaTransaction _tx({
  String type = 'VAULT_DEPOSIT',
  String status = 'COMPLETED',
  String? description,
  String? counterparty,
  double amount = 1000,
  double totalDebited = 1000,
}) => KolaTransaction(
  id: '1',
  reference: 'TXN-1',
  type: type,
  status: status,
  currency: 'XOF',
  amount: amount,
  fee: 0,
  totalDebited: totalDebited,
  createdAt: DateTime(2026, 9, 24, 18, 27),
  description: description,
  counterparty: counterparty,
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
        expect(transactionNote(_tx(description: label)), isNull, reason: label);
      }
    });

    test('masque les identifiants techniques', () {
      expect(transactionNote(_tx(description: 'Loan $_uuid')), isNull);
      expect(
        transactionNote(_tx(description: 'Scheduled task $_uuid')),
        isNull,
      );
      expect(transactionNote(_tx(description: 'task:$_uuid:3')), isNull);
    });

    test('traduit un remboursement de litige', () {
      expect(
        transactionNote(_tx(description: 'Chargeback TXN-123')),
        'Remboursement suite à un litige',
      );
    });

    test('garde le motif saisi par la personne', () {
      expect(
        transactionNote(_tx(description: '  Loyer de septembre ')),
        'Loyer de septembre',
      );
      expect(
        transactionNote(_tx(description: 'Loan pour le loyer')),
        'Loan pour le loyer',
      );
    });

    test('description absente ou vide', () {
      expect(transactionNote(_tx()), isNull);
      expect(transactionNote(_tx(description: '   ')), isNull);
    });
  });

  group('titre et contrepartie', () {
    test('un prêt ne montre jamais son identifiant', () {
      final loan = _tx(
        type: 'LOAN_REPAYMENT',
        counterparty: 'Loan $_uuid',
        description: 'Vault withdrawal',
      );
      expect(transactionParty(loan), isNull);
      expect(transactionTitle(loan), 'Remboursement de prêt');
    });

    test('un litige ne montre pas la référence d\'origine', () {
      final back = _tx(type: 'CHARGEBACK', counterparty: 'TXN-0042');
      expect(transactionTitle(back), 'Remboursement suite à un litige');
    });

    test('un coffre garde son nom', () {
      final vault = _tx(counterparty: 'Vacances');
      expect(transactionTitle(vault), 'Vacances');
    });

    test('initiales d\'un bénéficiaire, pas d\'un numéro', () {
      expect(
        transactionInitials(
          _tx(type: 'P2P_TRANSFER', counterparty: 'Prince Alabi'),
        ),
        'PA',
      );
      expect(
        transactionInitials(
          _tx(type: 'P2P_TRANSFER', counterparty: '+22890123456'),
        ),
        isNull,
      );
    });
  });

  group('sens et montant', () {
    test('une sortie coûte le total débité', () {
      final out = _tx(type: 'P2P_TRANSFER', amount: 10000, totalDebited: 10150);
      expect(isIncoming(out), isFalse);
      expect(signedAmount(out), -10150);
    });

    test('un prêt reçu est une entrée', () {
      final loan = _tx(type: 'LOAN_DISBURSEMENT', amount: 60000);
      expect(isIncoming(loan), isTrue);
      expect(signedAmount(loan), 60000);
    });
  });

  group('TransactionTile', () {
    setUpAll(() => initializeDateFormatting('fr_FR'));

    Future<void> pump(WidgetTester tester, KolaTransaction tx) =>
        tester.pumpWidget(
          MaterialApp(
            home: Scaffold(body: TransactionTile(transaction: tx)),
          ),
        );

    testWidgets('n\'affiche ni uuid, ni Loan, ni référence', (tester) async {
      await pump(
        tester,
        _tx(
          type: 'LOAN_REPAYMENT',
          counterparty: 'Loan $_uuid',
          description: 'Vault withdrawal',
          amount: 15000,
          totalDebited: 15000,
        ),
      );

      expect(find.text('Remboursement de prêt'), findsOneWidget);
      // Le séparateur de milliers est une espace insécable.
      expect(find.textContaining(RegExp(r'^−15\s000 F$')), findsOneWidget);
      expect(find.textContaining('Loan'), findsNothing);
      expect(find.textContaining(_uuid), findsNothing);
      expect(find.textContaining('TXN-'), findsNothing);
      // Une opération réussie n'a pas besoin d'afficher « Réussie ».
      expect(find.text('Réussie'), findsNothing);
    });

    testWidgets('signale une opération échouée', (tester) async {
      await pump(tester, _tx(status: 'FAILED', counterparty: 'Vacances'));
      expect(find.text('Échouée'), findsOneWidget);
    });
  });
}
