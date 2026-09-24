import 'package:flutter/widgets.dart';
import '../theme/kola_icons.dart';

import '../../models/transaction.dart';

/// Comment présenter un type de transaction : intitulé, icône, et sens du
/// mouvement (un crédit s'affiche en vert avec un `+`).
class TransactionMeta {
  const TransactionMeta(this.label, this.icon, {this.incoming = false});

  final String label;
  final IconData icon;
  final bool incoming;
}

const _meta = <String, TransactionMeta>{
  'CASH_IN': TransactionMeta(
    'Recharge du compte',
    KolaIcons.arrowDownOutline,
    incoming: true,
  ),
  'CASH_OUT': TransactionMeta('Retrait', KolaIcons.arrowUpOutline),
  'P2P_TRANSFER': TransactionMeta('Transfert P2P', KolaIcons.sendOutline),
  'MERCHANT_PAYMENT': TransactionMeta(
    'Paiement marchand',
    KolaIcons.storefrontOutline,
  ),
  'VAULT_DEPOSIT': TransactionMeta(
    'Versement au coffre',
    KolaIcons.lockClosedOutline,
  ),
  'VAULT_WITHDRAWAL': TransactionMeta(
    'Retrait du coffre',
    KolaIcons.lockOpenOutline,
    incoming: true,
  ),
  'SAVINGS_DEPOSIT': TransactionMeta(
    'Versement sur Bankivi',
    KolaIcons.businessOutline,
  ),
  'SAVINGS_WITHDRAWAL': TransactionMeta(
    'Retrait de Bankivi',
    KolaIcons.arrowUpCircleOutline,
    incoming: true,
  ),
  'BILL_PAYMENT': TransactionMeta(
    'Paiement de facture',
    KolaIcons.receiptOutline,
  ),
  'LOAN_DISBURSEMENT': TransactionMeta(
    'Crédit reçu',
    KolaIcons.cashOutline,
    incoming: true,
  ),
  'LOAN_REPAYMENT': TransactionMeta(
    'Remboursement de crédit',
    KolaIcons.cardOutline,
  ),
};

const _fallback = TransactionMeta(
  'Transaction KOLA',
  KolaIcons.swapHorizontalOutline,
);

TransactionMeta metaFor(String type) => _meta[type] ?? _fallback;

/// Statut lisible affiché sous chaque ligne du relevé.
String statusLabel(KolaTransaction transaction) => switch (transaction.status) {
  'COMPLETED' => 'Réussie',
  'FAILED' => 'Échouée',
  'PENDING' => 'En attente',
  _ => transaction.status,
};
