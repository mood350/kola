import type {
  CreditTier,
  KycLevel,
  LoanStatus,
  MobileNetwork,
  NotificationType,
  ScheduleFrequency,
  ScheduleStatus,
  TransactionStatus,
  TransactionType,
  VaultStatus,
} from "@/lib/types";

/**
 * Traduction des énumérations du backend.
 *
 * UN SEUL endroit décide qu'un `TRANSFER_OUT` s'écrit « Transfert envoyé » et
 * se peint en indigo. C'est ce qui garantit qu'un même statut ne change pas de
 * couleur d'un écran à l'autre — ce qui, sur une application d'argent, se lit
 * comme deux états différents. Les libellés reprennent mot pour mot ceux du
 * mobile (`models/transaction.dart`, `models/loan.dart`…) : les deux clients
 * décrivent la même opération avec les mêmes mots.
 */

/** Tonalité visuelle partagée par les badges, pastilles et icônes. */
export type Tone = "neutral" | "brand" | "positive" | "warning" | "danger" | "info";

/* --- Transactions --------------------------------------------------------- */

export const TRANSACTION_TYPE_LABEL: Record<TransactionType, string> = {
  DEPOSIT: "Dépôt",
  WITHDRAWAL: "Retrait",
  TRANSFER_OUT: "Transfert envoyé",
  TRANSFER_IN: "Transfert reçu",
  VAULT_LOCK: "Blocage coffre",
  VAULT_UNLOCK: "Déblocage coffre",
  FEE: "Frais",
  SCHEDULED_TRANSFER: "Transfert programmé",
  LOAN_DISBURSEMENT: "Déboursement de prêt",
  LOAN_REPAYMENT: "Remboursement de prêt",
  MERCHANT_PAYMENT: "Paiement marchand",
};

/**
 * Sens du mouvement pour le wallet.
 *
 * Recopié de `TransactionType.isCredit` côté mobile plutôt que déduit d'une
 * heuristique sur le nom : `VAULT_UNLOCK` crédite le wallet alors que rien dans
 * son intitulé ne le dit, et `LOAN_DISBURSEMENT` aussi.
 */
export const CREDIT_TYPES: ReadonlySet<TransactionType> = new Set<TransactionType>([
  "DEPOSIT",
  "TRANSFER_IN",
  "VAULT_UNLOCK",
  "LOAN_DISBURSEMENT",
]);

export function isCredit(type: TransactionType): boolean {
  return CREDIT_TYPES.has(type);
}

export const TRANSACTION_TYPE_TONE: Record<TransactionType, Tone> = {
  DEPOSIT: "positive",
  VAULT_UNLOCK: "positive",
  LOAN_DISBURSEMENT: "positive",
  TRANSFER_IN: "brand",
  TRANSFER_OUT: "brand",
  WITHDRAWAL: "warning",
  VAULT_LOCK: "warning",
  FEE: "danger",
  LOAN_REPAYMENT: "danger",
  SCHEDULED_TRANSFER: "info",
  MERCHANT_PAYMENT: "warning",
};

export const TRANSACTION_STATUS_LABEL: Record<TransactionStatus, string> = {
  PENDING: "En attente",
  SUCCESS: "Réussie",
  FAILED: "Échouée",
  CANCELLED: "Annulée",
  REFUNDED: "Remboursée",
};

export const TRANSACTION_STATUS_TONE: Record<TransactionStatus, Tone> = {
  PENDING: "warning",
  SUCCESS: "positive",
  FAILED: "danger",
  CANCELLED: "neutral",
  REFUNDED: "info",
};

/* --- Coffres -------------------------------------------------------------- */

export const VAULT_STATUS_LABEL: Record<VaultStatus, string> = {
  ACTIVE: "Bloqué",
  UNLOCKED: "Débloqué",
  CLOSED: "Fermé",
};

export const VAULT_STATUS_TONE: Record<VaultStatus, Tone> = {
  ACTIVE: "brand",
  UNLOCKED: "positive",
  CLOSED: "neutral",
};

/* --- Crédit --------------------------------------------------------------- */

export const CREDIT_TIER_LABEL: Record<CreditTier, string> = {
  INELIGIBLE: "Non éligible",
  BASIC: "Basique",
  STANDARD: "Standard",
  PREMIUM: "Premium",
  ELITE: "Élite",
};

export const CREDIT_TIER_TONE: Record<CreditTier, Tone> = {
  INELIGIBLE: "neutral",
  BASIC: "warning",
  STANDARD: "info",
  PREMIUM: "brand",
  ELITE: "positive",
};

export const LOAN_STATUS_LABEL: Record<LoanStatus, string> = {
  PENDING: "En attente",
  APPROVED: "Approuvé",
  REJECTED: "Refusé",
  DISBURSED: "Décaissé",
  REPAID: "Remboursé",
  DEFAULTED: "En défaut",
};

export const LOAN_STATUS_TONE: Record<LoanStatus, Tone> = {
  PENDING: "warning",
  APPROVED: "info",
  REJECTED: "danger",
  DISBURSED: "brand",
  REPAID: "positive",
  DEFAULTED: "danger",
};

/* --- Virements programmés ------------------------------------------------- */

export const SCHEDULE_FREQUENCY_LABEL: Record<ScheduleFrequency, string> = {
  MONTHLY: "Mensuel",
  WEEKLY: "Hebdomadaire",
};

export const SCHEDULE_STATUS_LABEL: Record<ScheduleStatus, string> = {
  ACTIVE: "Actif",
  PAUSED: "En pause",
  FAILED_PERMANENTLY: "En échec",
};

export const SCHEDULE_STATUS_TONE: Record<ScheduleStatus, Tone> = {
  ACTIVE: "positive",
  PAUSED: "neutral",
  FAILED_PERMANENTLY: "danger",
};

const WEEKDAYS = [
  "lundi",
  "mardi",
  "mercredi",
  "jeudi",
  "vendredi",
  "samedi",
  "dimanche",
];

/**
 * Récurrence en toutes lettres (« Le 15 de chaque mois »).
 *
 * `executionDay` porte deux significations selon la fréquence — quantième du
 * mois, ou rang du jour dans la semaine. Une liste qui afficherait « jour 3 »
 * laisserait le lecteur faire lui-même cette traduction, et se tromper une fois
 * sur deux.
 */
export function scheduleLabel(
  frequency: ScheduleFrequency,
  executionDay: number
): string {
  if (frequency === "MONTHLY") return `Le ${executionDay} de chaque mois`;
  const index = Math.min(Math.max(executionDay - 1, 0), 6);
  return `Chaque ${WEEKDAYS[index]}`;
}

/* --- Bénéficiaires -------------------------------------------------------- */

export const NETWORK_LABEL: Record<MobileNetwork, string> = {
  MIXX_BY_YAS: "Mixx by Yas",
  MOOV_TOGO: "Moov Togo",
  WAVE: "Wave",
  ORANGE_MONEY: "Orange Money",
  FREE_MONEY: "Free Money",
  MTN_MOMO: "MTN MoMo",
  VODAFONE_CASH: "Vodafone Cash",
  AIRTELTIGO: "AirtelTigo",
  OPAY: "OPay",
  PALMPAY: "PalmPay",
  WESTERN_UNION: "Western Union",
  MONEYGRAM: "MoneyGram",
};

export const NETWORKS = Object.keys(NETWORK_LABEL) as MobileNetwork[];

/**
 * Couleur de marque du réseau, pour la pastille des bénéficiaires.
 *
 * Reprend les couleurs officielles déjà retenues côté mobile
 * (`AppColors.orangeMoney`, `mtnYellow`, `waveBlue`, `moovBlue`) ; les réseaux
 * sans couleur définie retombent sur l'encre neutre plutôt que d'inventer une
 * identité visuelle à la place de leur propriétaire.
 */
export const NETWORK_COLOR: Partial<Record<MobileNetwork, string>> = {
  ORANGE_MONEY: "#FF7900",
  MTN_MOMO: "#FFCC00",
  WAVE: "#1DA1F2",
  MOOV_TOGO: "#00539B",
  MIXX_BY_YAS: "#2E32C7",
};

/* --- Opérateurs Mobile Money (payment/MobileMoneyMode) -------------------- */

/**
 * Couleur de marque des opérateurs encaissables.
 *
 * ═══ DISTINCTE DE `NETWORK_COLOR`, ET IL LE FAUT ═══
 *
 * `NETWORK_COLOR` décrit les réseaux d'un DESTINATAIRE de virement
 * (`MobileNetwork`, qui inclut Western Union ou MoneyGram). Celle-ci décrit les
 * canaux par lesquels FedaPay sait encaisser et verser (`MobileMoneyMode`). Les
 * deux énumérations ne se recouvrent qu'à moitié : les indexer sur la même
 * table laissait sept opérateurs sur neuf sans couleur.
 *
 * Seules les marques dont la couleur est établie figurent ici — MTN et son
 * jaune, Moov et son bleu. Les autres retombent sur l'indigo de marque : mieux
 * vaut une pastille neutre qu'une couleur inventée à la place de son
 * propriétaire.
 */
export const OPERATOR_COLOR: Record<string, string> = {
  MTN_BENIN: "#FFCC00",
  MTN_CI: "#FFCC00",
  MTN_GUINEE: "#FFCC00",
  MOOV_BENIN: "#00539B",
  MOOV_TOGO: "#00539B",
};

/* --- KYC ------------------------------------------------------------------ */

export const KYC_LABEL: Record<KycLevel, string> = {
  TIER_0: "Niveau 0",
  TIER_1: "Niveau 1",
  TIER_2: "Niveau 2",
  TIER_3: "Niveau 3",
};

/**
 * Un compte au niveau 0 n'est pas « vérifié » : le badge le signale en orange,
 * comme sur mobile (`KycBadgeStatus.pending`). C'est la seule information de
 * l'en-tête qui appelle une action de la part de l'utilisateur.
 */
export const KYC_TONE: Record<KycLevel, Tone> = {
  TIER_0: "warning",
  TIER_1: "positive",
  TIER_2: "positive",
  TIER_3: "positive",
};

/* --- Notifications -------------------------------------------------------- */

export const NOTIFICATION_TYPE_LABEL: Record<NotificationType, string> = {
  TRANSACTION: "Transaction",
  SECURITY: "Sécurité",
  SYSTEM: "Système",
};

export const NOTIFICATION_TYPE_TONE: Record<NotificationType, Tone> = {
  TRANSACTION: "brand",
  SECURITY: "danger",
  SYSTEM: "neutral",
};

/* --- Avatars -------------------------------------------------------------- */

/**
 * Catalogue d'avatars prédéfinis — mêmes identifiants que le mobile
 * (`KolaAvatars.ids`). Le serveur ne stocke qu'une chaîne (« avatar_03 ») :
 * aucun fichier n'est téléversé, aucun binaire n'est à servir, et l'avatar
 * choisi sur le téléphone est celui qui s'affiche ici.
 */
export const AVATAR_IDS = [
  "avatar_01",
  "avatar_02",
  "avatar_03",
  "avatar_04",
  "avatar_05",
  "avatar_06",
  "avatar_07",
  "avatar_08",
] as const;

export const AVATAR_COLOR: Record<string, string> = {
  avatar_01: "#2E32C7",
  avatar_02: "#494FDF",
  avatar_03: "#00A87E",
  avatar_04: "#EC7E00",
  avatar_05: "#FF7900",
  avatar_06: "#00539B",
  avatar_07: "#E23B4A",
  avatar_08: "#4F55F1",
};

/** Emoji associé à chaque avatar, pendant des icônes Material du mobile. */
export const AVATAR_EMOJI: Record<string, string> = {
  avatar_01: "🙂",
  avatar_02: "😃",
  avatar_03: "😄",
  avatar_04: "🚀",
  avatar_05: "🐾",
  avatar_06: "🌿",
  avatar_07: "⚽",
  avatar_08: "🎵",
};
