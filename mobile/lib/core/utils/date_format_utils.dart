import 'package:intl/intl.dart';

/// Formate une date en "Aujourd'hui, HH:mm" / "Hier, HH:mm" / "d MMM, HH:mm".
/// Utilisé par Home, l'historique des transactions et le détail d'une transaction.
String formatRelativeDate(DateTime date) {
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final txDate = DateTime(date.year, date.month, date.day);
  final timeStr = DateFormat('HH:mm').format(date);

  if (txDate == today) {
    return "Aujourd'hui, $timeStr";
  } else if (txDate == today.subtract(const Duration(days: 1))) {
    return 'Hier, $timeStr';
  } else {
    return '${DateFormat('d MMM', 'fr_FR').format(date)}, $timeStr';
  }
}
