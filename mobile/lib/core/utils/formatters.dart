import 'package:intl/intl.dart';

final NumberFormat _amount = NumberFormat.decimalPattern('fr_FR');

/// Montant en séparateurs français, sans décimales : le franc CFA n'a pas de
/// subdivision en circulation.
String money(num value) => _amount.format(value.round());

/// Montant suivi de sa devise, pour les libellés courants.
String moneyWithCurrency(num value, [String currency = 'FCFA']) =>
    '${money(value)} $currency';

/// Date longue d'un reçu ("12 mars 2026 à 14:05").
String receiptDate(DateTime date) =>
    DateFormat("d MMMM y 'à' HH:mm", 'fr_FR').format(date);

/// Date courte d'une échéance ("12/03/2026").
String shortDate(DateTime date) => DateFormat('dd/MM/yyyy').format(date);
