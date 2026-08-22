import 'package:flutter/material.dart';
import '../../models/transaction.dart';
import '../theme/app_colors.dart';

/// Couleur associée à un type de transaction — utilisé par Home et
/// l'historique des transactions pour l'icône colorée de [TransactionItem].
Color colorForTransactionType(TransactionType type) {
  switch (type) {
    case TransactionType.deposit:
    case TransactionType.vaultUnlock:
    case TransactionType.loanDisbursement:
      return AppColors.success;
    case TransactionType.transferOut:
    case TransactionType.transferIn:
      return AppColors.primary;
    case TransactionType.withdrawal:
    case TransactionType.vaultLock:
      return AppColors.warning;
    case TransactionType.fee:
    case TransactionType.loanRepayment:
      return AppColors.danger;
    case TransactionType.scheduledTransfer:
      return AppColors.moovBlue;
    case TransactionType.merchantPayment:
      return AppColors.orangeMoney;
  }
}
