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
  'P2P_TRANSFER': TransactionMeta('Envoi d’argent', KolaIcons.sendOutline),
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
    'Prêt reçu',
    KolaIcons.cashOutline,
    incoming: true,
  ),
  'LOAN_REPAYMENT': TransactionMeta(
    'Remboursement de prêt',
    KolaIcons.cardOutline,
  ),
  'CHARGEBACK': TransactionMeta(
    'Remboursement suite à un litige',
    KolaIcons.swapHorizontalOutline,
  ),
};

const _fallback = TransactionMeta(
  'Transaction KOLA',
  KolaIcons.swapHorizontalOutline,
);

TransactionMeta metaFor(String type) => _meta[type] ?? _fallback;

/// Sens du mouvement vu du compte courant : un crédit s'affiche en vert.
bool isIncoming(KolaTransaction transaction) =>
    metaFor(transaction.type).incoming || transaction.type == 'REFUND';

/// Montant signé : une sortie coûte le total débité (frais compris), pas
/// seulement le montant transféré.
double signedAmount(KolaTransaction transaction) => isIncoming(transaction)
    ? transaction.amount
    : -(transaction.totalDebited > 0
          ? transaction.totalDebited
          : transaction.amount);

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

final _hasLetter = RegExp(r'\p{L}', unicode: true);

/// Vrai pour un texte qui n'est qu'un identifiant : « Loan » suivi d'un uuid,
/// « Scheduled task » suivi d'un uuid, une clé d'idempotence (`task:uuid:3`)
/// ou une référence `TXN-…`. Le backend les range aussi bien dans la
/// description que dans la contrepartie, selon l'opération.
bool _isTechnical(String text) {
  final lower = text.toLowerCase();
  return lower.startsWith('task:') ||
      lower.startsWith('txn-') ||
      ((lower.startsWith('loan ') || lower.startsWith('scheduled task')) &&
          _uuid.hasMatch(text));
}

/// Motif saisi par la personne, ou `null` quand la description n'est qu'un
/// libellé technique : le type, déjà en français, suffit alors.
String? transactionNote(KolaTransaction transaction) {
  final text = transaction.description?.trim();
  if (text == null || text.isEmpty) return null;

  final lower = text.toLowerCase();
  if (_systemDescriptions.contains(lower)) return null;
  if (lower.startsWith('chargeback')) return 'Remboursement suite à un litige';
  if (_isTechnical(text)) return null;
  return text;
}

/// Nom lisible de l'autre partie (bénéficiaire, coffre, marchand), ou `null`
/// quand le champ ne contient qu'un identifiant — c'est le cas d'un prêt
/// (« Loan » suivi d'un uuid) ou d'un remboursement de litige (référence
/// d'origine).
String? transactionParty(KolaTransaction transaction) {
  final text = transaction.counterparty?.trim();
  if (text == null || text.isEmpty || _isTechnical(text)) return null;
  return text;
}

/// Titre d'une ligne : la personne ou le coffre concerné, à défaut le motif,
/// à défaut le type d'opération.
String transactionTitle(KolaTransaction transaction) =>
    transactionParty(transaction) ??
    transactionNote(transaction) ??
    metaFor(transaction.type).label;

/// Initiales du bénéficiaire d'un transfert pour l'avatar, `null` pour un
/// numéro de téléphone ou un identifiant (l'icône du type prend le relais).
String? transactionInitials(KolaTransaction transaction) {
  if (transaction.type != 'P2P_TRANSFER') return null;
  final party = transactionParty(transaction);
  if (party == null || !_hasLetter.hasMatch(party)) return null;

  final words = party
      .split(RegExp(r'\s+'))
      .where((word) => _hasLetter.hasMatch(word.substring(0, 1)))
      .toList();
  if (words.isEmpty) return null;
  final first = words.first.substring(0, 1);
  final last = words.length > 1 ? words.last.substring(0, 1) : '';
  return (first + last).toUpperCase();
}

/// Statut lisible affiché sous chaque ligne du relevé.
String statusLabel(KolaTransaction transaction) => switch (transaction.status) {
  'COMPLETED' => 'Réussie',
  'FAILED' => 'Échouée',
  'PENDING' => 'En attente',
  _ => transaction.status,
};
