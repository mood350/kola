import 'package:flutter/material.dart';

enum TransactionType { merchantPayment, deposit, transferSent, transferReceived, withdrawal, billPayment }

/// Modèle représentant une transaction Kola (wallet).
class Transaction {
  final String id;
  final TransactionType type;
  final String title;
  final String subtitle;
  final double amount; // négatif = sortant, positif = entrant
  final DateTime date;

  Transaction({
    required this.id,
    required this.type,
    required this.title,
    required this.subtitle,
    required this.amount,
    required this.date,
  });

  bool get isPositive => amount > 0;

  /// Icône associée au type de transaction (cf. DESIGN.md Fintech Specifics).
  IconData get icon {
    switch (type) {
      case TransactionType.merchantPayment:
        return Icons.storefront_rounded;
      case TransactionType.deposit:
        return Icons.arrow_downward_rounded;
      case TransactionType.transferSent:
      case TransactionType.transferReceived:
        return Icons.person_rounded;
      case TransactionType.withdrawal:
        return Icons.arrow_upward_rounded;
      case TransactionType.billPayment:
        return Icons.receipt_long_rounded;
    }
  }

  factory Transaction.fromJson(Map<String, dynamic> json) {
    return Transaction(
      id: json['id'] as String,
      type: TransactionType.values.firstWhere(
            (e) => e.name == json['type'],
        orElse: () => TransactionType.transferSent,
      ),
      title: json['title'] as String,
      subtitle: json['subtitle'] as String,
      amount: (json['amount'] as num).toDouble(),
      date: DateTime.parse(json['date'] as String),
    );
  }
}