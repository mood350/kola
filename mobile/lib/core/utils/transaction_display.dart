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

/// Libellés que le backend a écrits lui-même (souvent en anglais) : ils
/// redisent le type, déjà présenté en français par [metaFor].
const _systemDescriptions = {
  'cash-in',
  'cash-out',
  'bill payment',
  'vault deposit',
  'vault withdrawal',
};

final _uuid = RegExp(
  r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',
  caseSensitive: false,
);

/// Motif saisi par la personne, ou `null` quand la description n'est qu'un
/// libellé technique. Le backend y a stocké « Vault deposit », « Loan » suivi
/// d'un uuid, « Scheduled task » suivi d'un uuid, voire des clés d'idempotence
/// (`task:uuid:3`) : rien de cela n'a sa place à l'écran, le type suffit.
String? transactionNote(KolaTransaction transaction) {
  final text = transaction.description?.trim();
  if (text == null || text.isEmpty) return null;

  final lower = text.toLowerCase();
  if (_systemDescriptions.contains(lower)) return null;
  if (lower.startsWith('chargeback')) return 'Remboursement suite à un litige';
  if (lower.startsWith('loan ') ||
      lower.startsWith('scheduled task') ||
      lower.startsWith('task:') ||
      _uuid.hasMatch(text)) {
    return null;
  }
  return text;
}

/// Statut lisible affiché sous chaque ligne du relevé.
String statusLabel(KolaTransaction transaction) => switch (transaction.status) {
  'COMPLETED' => 'Réussie',
  'FAILED' => 'Échouée',
  'PENDING' => 'En attente',
  _ => transaction.status,
};
