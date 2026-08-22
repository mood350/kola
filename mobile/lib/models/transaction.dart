import 'package:flutter/material.dart';

/// Type de mouvement financier (cf. backend TransactionType).
enum TransactionType {
  deposit,
  withdrawal,
  transferOut,
  transferIn,
  vaultLock,
  vaultUnlock,
  fee,
  scheduledTransfer,
  loanDisbursement,
  loanRepayment,
  merchantPayment;

  static TransactionType fromBackend(String value) {
    switch (value) {
      case 'DEPOSIT':
        return TransactionType.deposit;
      case 'WITHDRAWAL':
        return TransactionType.withdrawal;
      case 'TRANSFER_OUT':
        return TransactionType.transferOut;
      case 'TRANSFER_IN':
        return TransactionType.transferIn;
      case 'VAULT_LOCK':
        return TransactionType.vaultLock;
      case 'VAULT_UNLOCK':
        return TransactionType.vaultUnlock;
      case 'FEE':
        return TransactionType.fee;
      case 'SCHEDULED_TRANSFER':
        return TransactionType.scheduledTransfer;
      case 'LOAN_DISBURSEMENT':
        return TransactionType.loanDisbursement;
      case 'LOAN_REPAYMENT':
        return TransactionType.loanRepayment;
      case 'MERCHANT_PAYMENT':
        return TransactionType.merchantPayment;
      default:
        return TransactionType.fee;
    }
  }

  String get label {
    switch (this) {
      case TransactionType.deposit:
        return 'Dépôt';
      case TransactionType.withdrawal:
        return 'Retrait';
      case TransactionType.transferOut:
        return 'Transfert envoyé';
      case TransactionType.transferIn:
        return 'Transfert reçu';
      case TransactionType.vaultLock:
        return 'Blocage coffre';
      case TransactionType.vaultUnlock:
        return 'Déblocage coffre';
      case TransactionType.fee:
        return 'Frais';
      case TransactionType.scheduledTransfer:
        return 'Transfert programmé';
      case TransactionType.loanDisbursement:
        return 'Déboursement de prêt';
      case TransactionType.loanRepayment:
        return 'Remboursement de prêt';
      case TransactionType.merchantPayment:
        return 'Paiement marchand';
    }
  }

  IconData get icon {
    switch (this) {
      case TransactionType.deposit:
        return Icons.arrow_downward_rounded;
      case TransactionType.withdrawal:
        return Icons.arrow_upward_rounded;
      case TransactionType.transferOut:
      case TransactionType.transferIn:
        return Icons.person_rounded;
      case TransactionType.vaultLock:
      case TransactionType.vaultUnlock:
        return Icons.lock_outline_rounded;
      case TransactionType.fee:
        return Icons.receipt_long_rounded;
      case TransactionType.scheduledTransfer:
        return Icons.schedule_rounded;
      case TransactionType.loanDisbursement:
      case TransactionType.loanRepayment:
        return Icons.account_balance_rounded;
      case TransactionType.merchantPayment:
        return Icons.qr_code_scanner_rounded;
    }
  }

  /// Vrai si ce type crédite le wallet (affichage "+" et couleur positive).
  bool get isCredit {
    switch (this) {
      case TransactionType.deposit:
      case TransactionType.transferIn:
      case TransactionType.vaultUnlock:
      case TransactionType.loanDisbursement:
        return true;
      case TransactionType.withdrawal:
      case TransactionType.transferOut:
      case TransactionType.vaultLock:
      case TransactionType.fee:
      case TransactionType.scheduledTransfer:
      case TransactionType.loanRepayment:
      case TransactionType.merchantPayment:
        return false;
    }
  }
}

/// Statut d'une transaction (cf. backend TransactionStatus).
enum TransactionStatus {
  pending,
  success,
  failed,
  cancelled,
  refunded;

  static TransactionStatus fromBackend(String value) {
    switch (value) {
      case 'PENDING':
        return TransactionStatus.pending;
      case 'SUCCESS':
        return TransactionStatus.success;
      case 'FAILED':
        return TransactionStatus.failed;
      case 'CANCELLED':
        return TransactionStatus.cancelled;
      case 'REFUNDED':
        return TransactionStatus.refunded;
      default:
        return TransactionStatus.pending;
    }
  }

  String get label {
    switch (this) {
      case TransactionStatus.pending:
        return 'En attente';
      case TransactionStatus.success:
        return 'Réussie';
      case TransactionStatus.failed:
        return 'Échouée';
      case TransactionStatus.cancelled:
        return 'Annulée';
      case TransactionStatus.refunded:
        return 'Remboursée';
    }
  }
}

/// Modèle représentant une transaction (cf. TransactionResponse backend).
class Transaction {
  final int id;
  final String reference; // clé pour GET /transactions/{reference}
  final TransactionType type;
  final TransactionStatus status;
  final double amount;
  final double fee;
  final String currency;
  final String? receiverCurrency;
  final double? exchangeRate;
  final int? walletId;
  final String? receiverPhoneNumber;
  final String? receiverCountryCode;
  final String? description;
  final String? idempotencyKey;
  final DateTime createdAt;

  const Transaction({
    required this.id,
    required this.reference,
    required this.type,
    required this.status,
    required this.amount,
    required this.fee,
    required this.currency,
    this.receiverCurrency,
    this.exchangeRate,
    this.walletId,
    this.receiverPhoneNumber,
    this.receiverCountryCode,
    this.description,
    this.idempotencyKey,
    required this.createdAt,
  });

  bool get isCredit => type.isCredit;
  IconData get icon => type.icon;

  /// Titre affiché dans les listes (Home, historique).
  String get displayTitle => type.label;

  /// Sous-titre affiché : numéro du bénéficiaire pour un transfert,
  /// sinon la description backend, sinon le statut si non abouti.
  String get displaySubtitle {
    if (receiverPhoneNumber != null && receiverPhoneNumber!.isNotEmpty) {
      return receiverPhoneNumber!;
    }
    if (description != null && description!.isNotEmpty) return description!;
    if (status != TransactionStatus.success) return status.label;
    return currency;
  }

  factory Transaction.fromJson(Map<String, dynamic> json) {
    return Transaction(
      id: json['id'] as int,
      reference: json['reference'] as String? ?? '',
      type: TransactionType.fromBackend(json['type'] as String? ?? 'FEE'),
      status: TransactionStatus.fromBackend(
        json['status'] as String? ?? 'PENDING',
      ),
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
      fee: (json['fee'] as num?)?.toDouble() ?? 0,
      currency: json['currency'] as String? ?? 'XOF',
      receiverCurrency: json['receiverCurrency'] as String?,
      exchangeRate: (json['exchangeRate'] as num?)?.toDouble(),
      walletId: json['walletId'] as int?,
      receiverPhoneNumber: json['receiverPhoneNumber'] as String?,
      receiverCountryCode: json['receiverCountryCode'] as String?,
      description: json['description'] as String?,
      idempotencyKey: json['idempotencyKey'] as String?,
      createdAt:
          DateTime.tryParse(json['createdAt'] as String? ?? '') ??
          DateTime.now(),
    );
  }
}
