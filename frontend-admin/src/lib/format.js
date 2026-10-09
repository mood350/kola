// Mise en forme et libellés : l'API sert des valeurs brutes (nombres, dates
// ISO, codes d'enum), c'est ici — et seulement ici — qu'elles deviennent du
// texte lisible.

const numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
const dateFormat = new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
const dateTimeFormat = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

/** 25000 → « 25 000 XOF ». Le XOF n'a pas de centimes : on arrondit à l'unité. */
export function money(amount, currency = 'XOF') {
  if (amount === null || amount === undefined) return '—';
  return `${numberFormat.format(Number(amount))} ${currency}`;
}

export function count(value) {
  return value === null || value === undefined ? '—' : numberFormat.format(value);
}

export function date(value) {
  return value ? dateFormat.format(new Date(value)) : '—';
}

export function dateTime(value) {
  return value ? dateTimeFormat.format(new Date(value)) : '—';
}

/** « Kola Testeur » → « KT » : l'avatar d'un compte qui n'a pas de photo. */
export function initials(name) {
  return (name || '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('') || '·';
}

const LABELS = {
  kycTier: { TIER_0: 'Niveau 0', TIER_1: 'Niveau 1', TIER_2: 'Niveau 2', TIER_3: 'Niveau 3' },
  userStatus: { ACTIVE: 'Actif', SUSPENDED: 'Suspendu' },
  walletType: { CURRENT: 'Compte courant', SAVINGS: 'Épargne' },
  vaultStatus: { ACTIVE: 'Ouvert', CLOSED: 'Fermé' },
  loanStatus: { ACTIVE: 'En cours', OVERDUE: 'En retard', REPAID: 'Remboursé', DEFAULTED: 'En défaut' },
  txStatus: { PENDING: 'En attente', COMPLETED: 'Réussie', FAILED: 'Échouée', REVERSED: 'Contre-passée' },
  txType: {
    CASH_IN: 'Dépôt',
    CASH_OUT: 'Retrait',
    P2P_TRANSFER: 'Transfert',
    MERCHANT_PAYMENT: 'Paiement marchand',
    BILL_PAYMENT: 'Facture',
    VAULT_DEPOSIT: 'Vers un coffre',
    VAULT_WITHDRAWAL: 'Depuis un coffre',
    SAVINGS_DEPOSIT: 'Vers l\'épargne',
    SAVINGS_WITHDRAWAL: 'Depuis l\'épargne',
    LOAN_DISBURSEMENT: 'Versement de prêt',
    LOAN_REPAYMENT: 'Remboursement',
    CHARGEBACK: 'Contre-passation',
  },
  taskType: {
    P2P_TRANSFER: 'Transfert',
    MERCHANT_PAYMENT: 'Paiement marchand',
    VAULT_DEPOSIT: 'Versement coffre',
    SAVINGS_DEPOSIT: 'Versement épargne',
    BILL_PAYMENT: 'Facture',
  },
  frequency: { ONCE: 'Une fois', DAILY: 'Chaque jour', WEEKLY: 'Chaque semaine', MONTHLY: 'Chaque mois' },
  taskStatus: { ACTIVE: 'Actif', PAUSED: 'En pause', FAILED: 'En échec', COMPLETED: 'Terminé' },
  docType: {
    NATIONAL_ID: 'Carte d\'identité',
    PASSPORT: 'Passeport',
    DRIVING_LICENCE: 'Permis de conduire',
    VOTER_CARD: 'Carte d\'électeur',
    SELFIE: 'Selfie',
    PROOF_OF_ADDRESS: 'Justificatif de domicile',
  },
  docStatus: { PENDING: 'À vérifier', APPROVED: 'Approuvée', REJECTED: 'Rejetée' },
};

/** Libellé d'un code d'enum ; le code brut s'il est inconnu, plutôt qu'un vide. */
export function label(kind, code) {
  if (!code) return '—';
  return LABELS[kind]?.[code] ?? code;
}

export const options = (kind) => Object.entries(LABELS[kind]);

/** Ton du badge : les couleurs ne disent qu'un statut, jamais une décoration. */
const TONES = {
  userStatus: { ACTIVE: 'ok', SUSPENDED: 'bad' },
  loanStatus: { ACTIVE: '', OVERDUE: 'warn', REPAID: 'ok', DEFAULTED: 'bad' },
  txStatus: { PENDING: 'warn', COMPLETED: 'ok', FAILED: 'bad', REVERSED: '' },
  taskStatus: { ACTIVE: 'ok', PAUSED: '', FAILED: 'bad', COMPLETED: '' },
  docStatus: { PENDING: 'warn', APPROVED: 'ok', REJECTED: 'bad' },
};

export const tone = (kind, code) => TONES[kind]?.[code] ?? '';
