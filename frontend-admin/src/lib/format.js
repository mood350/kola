// Mise en forme et libellés : l'API sert des valeurs brutes (nombres, dates
// ISO, codes d'enum), c'est ici — et seulement ici — qu'elles deviennent du
// texte lisible.

const numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });

// Le fuseau métier : le serveur découpe ses jours en UTC, et Lomé est à UTC+0 toute l'année.
// On le nomme plutôt que de suivre le fuseau du navigateur : deux administrateurs à deux
// endroits du monde doivent lire la même date sur la même opération.
const BUSINESS_TIME_ZONE = 'Africa/Lome';

const dayParts = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: BUSINESS_TIME_ZONE,
});
const timeParts = new Intl.DateTimeFormat('fr-FR', {
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone: BUSINESS_TIME_ZONE,
});

const pick = (parts, type) => parts.find((part) => part.type === type)?.value ?? '';

/** Le code ISO reste XOF dans l'API ; à l'écran, c'est le franc CFA que l'opérateur lit. */
const shownCurrency = (code) => (code === 'XOF' ? 'FCFA' : code);

/** 25000 → « 25 000 FCFA ». Le franc CFA n'a pas de centimes : on arrondit à l'unité. */
export function money(amount, currency = 'XOF') {
  if (amount === null || amount === undefined) return '—';
  return `${numberFormat.format(Number(amount))} ${shownCurrency(currency)}`;
}

export function count(value) {
  return value === null || value === undefined ? '—' : numberFormat.format(value);
}

/** « 09/10/2026 » : le jour, au format français JJ/MM/AAAA. */
export function date(value) {
  if (!value) return '—';
  const parts = dayParts.formatToParts(new Date(value));
  return `${pick(parts, 'day')}/${pick(parts, 'month')}/${pick(parts, 'year')}`;
}

/** « 09/10/2026 10:42 » : le jour et l'heure, heure de Lomé. */
export function dateTime(value) {
  if (!value) return '—';
  const time = timeParts.formatToParts(new Date(value));
  return `${date(value)} ${pick(time, 'hour')}:${pick(time, 'minute')}`;
}

const DAY_MS = 86_400_000;

/** Le numéro du jour calendaire (Lomé) d'un instant : de quoi compter des jours entiers, sans heures. */
function dayNumber(value) {
  const [d, m, y] = date(value).split('/').map(Number);
  return Date.UTC(y, m - 1, d) / DAY_MS;
}

/** « aujourd'hui », « hier », « il y a 3 jours » : depuis combien de temps quelque chose attend. */
export function since(value, now = new Date()) {
  if (!value) return '—';
  const days = dayNumber(now) - dayNumber(value);
  if (days <= 0) return "aujourd'hui";
  if (days === 1) return 'hier';
  return `il y a ${days} jours`;
}

/**
 * Qui paie, qui reçoit, en une ligne de texte : un compte Kola quand il y en a un, sinon la
 * contrepartie externe. Un mouvement entre deux comptes du même client n'a qu'un nom : « X → X »
 * n'apprendrait rien, et la précision se lit dans le détail de l'opération.
 */
export function partiesText(t) {
  const who = (id, name) => (id ? name || 'Compte Kola' : null);
  if (t.senderId && t.senderId === t.recipientId) return who(t.senderId, t.senderName);
  // Le backend note « EXTERNAL » l'autre bout d'un dépôt ou d'un retrait Mobile Money.
  const raw = t.counterpartyName || t.counterparty;
  const outside = raw === 'EXTERNAL' ? 'Extérieur' : raw;
  const from = who(t.senderId, t.senderName) || (t.recipientId ? outside : null) || '—';
  const to = who(t.recipientId, t.recipientName) || (t.senderId ? outside : null) || '—';
  return `${from} → ${to}`;
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
  userStatus: { ACTIVE: 'Actif', SUSPENDED: 'Suspendu', CLOSED: 'Fermé', LOCKED: 'Verrouillé (PIN)' },
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
  docStatus: { PENDING: 'À vérifier', APPROVED: 'Acceptée', REJECTED: 'Refusée' },
};

/** Libellé d'un code d'enum ; le code brut s'il est inconnu, plutôt qu'un vide. */
export function label(kind, code) {
  if (!code) return '—';
  return LABELS[kind]?.[code] ?? code;
}

export const options = (kind) => Object.entries(LABELS[kind]);

/** Ton du badge : les couleurs ne disent qu'un statut, jamais une décoration. */
const TONES = {
  userStatus: { ACTIVE: 'ok', SUSPENDED: 'bad', CLOSED: '', LOCKED: 'warn' },
  loanStatus: { ACTIVE: '', OVERDUE: 'warn', REPAID: 'ok', DEFAULTED: 'bad' },
  txStatus: { PENDING: 'warn', COMPLETED: 'ok', FAILED: 'bad', REVERSED: '' },
  taskStatus: { ACTIVE: 'ok', PAUSED: '', FAILED: 'bad', COMPLETED: '' },
  docStatus: { PENDING: 'warn', APPROVED: 'ok', REJECTED: 'bad' },
};

export const tone = (kind, code) => TONES[kind]?.[code] ?? '';

/**
 * L'état d'un compte tel qu'on le lit : un verrouillage PIN laisse le statut à ACTIVE côté serveur,
 * mais pour l'opérateur le compte est bloqué — on le dit.
 */
export function userState(user) {
  return user.status === 'ACTIVE' && user.locked ? 'LOCKED' : user.status;
}

/**
 * L'état d'un prêt tel qu'on le lit. Le serveur ne pose le statut « en retard » que lorsqu'on touche
 * au prêt : un prêt en cours dont l'échéance est passée est en retard, quel que soit le statut stocké.
 */
export function loanState(loan, now = Date.now()) {
  const running = loan.status === 'ACTIVE' || loan.status === 'OVERDUE';
  return running && loan.dueAt && new Date(loan.dueAt).getTime() < now ? 'OVERDUE' : loan.status;
}

/** « moins d'une minute », « 12 min », « 3 h », « 2 jours » : le temps écoulé entre deux instants. */
export function elapsed(from, to) {
  const ms = new Date(to) - new Date(from);
  if (!(ms >= 0)) return '—';
  const minutes = Math.floor(ms / 60_000);
  if (minutes < 1) return 'moins d\'une minute';
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} h`;
  const days = Math.floor(hours / 24);
  return `${days} jour${days > 1 ? 's' : ''}`;
}

/** « aujourd'hui », « demain », « dans 5 jours », « hier », « il y a 3 jours » : un jour par rapport à aujourd'hui (Lomé). */
export function relativeDay(value, now = Date.now()) {
  if (!value) return '—';
  const days = dayNumber(value) - dayNumber(now);
  if (days === 0) return "aujourd'hui";
  if (days === 1) return 'demain';
  if (days === -1) return 'hier';
  return days > 0 ? `dans ${days} jours` : `il y a ${-days} jours`;
}

