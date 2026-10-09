/**
 * Types de l'API Kola.
 *
 * Chaque bloc nomme le DTO Java dont il est le miroir : quand une signature
 * change côté backend, on sait exactement quel bloc corriger. Ne rien inventer
 * ici — un champ absent du DTO Java est un champ qui n'arrivera jamais.
 */

/* --- Auth (com.kola.backend.auth) ---------------------------------------- */

/** AuthenticationResponse — noms de champs en snake_case côté JSON. */
export type AuthTokens = {
  access_token: string;
  refresh_token: string;
};

/** GlobalExceptionHandler.ErrorResponse */
export type ApiErrorBody = {
  code: string;
  message: string;
  details: Record<string, string>;
  path: string;
  timestamp: string;
};

/* --- Utilisateur (user/UserResponse) -------------------------------------- */

export type KycLevel = "TIER_0" | "TIER_1" | "TIER_2" | "TIER_3";

export type CurrentUser = {
  id: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phoneNumber: string;
  countryCode: string;
  kycLevel: KycLevel;
  /** Identifiant d'avatar prédéfini, ex: "avatar_03". Jamais une URL. */
  avatar: string | null;
  lastKnownIp: string | null;
  lastKnownUserAgent: string | null;
  createdAt: string | null;
};

/* --- Wallet (wallet/WalletResponse) --------------------------------------- */

export type Wallet = {
  id: number;
  currency: string;
  balance: number;
  lockedBalance: number;
  availableBalance: number;
  active: boolean;
};

/* --- Transactions (transaction/TransactionResponse) ----------------------- */

export type TransactionType =
  | "DEPOSIT"
  | "WITHDRAWAL"
  | "TRANSFER_OUT"
  | "TRANSFER_IN"
  | "VAULT_LOCK"
  | "VAULT_UNLOCK"
  | "FEE"
  | "SCHEDULED_TRANSFER"
  | "LOAN_DISBURSEMENT"
  | "LOAN_REPAYMENT"
  | "MERCHANT_PAYMENT";

export type TransactionStatus =
  | "PENDING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED";

export type Transaction = {
  id: number;
  /** Clé d'accès publique : GET /api/transactions/{reference}. */
  reference: string;
  type: TransactionType;
  status: TransactionStatus;
  amount: number;
  fee: number;
  currency: string;
  receiverCurrency: string | null;
  exchangeRate: number | null;
  walletId: number | null;
  receiverPhoneNumber: string | null;
  receiverCountryCode: string | null;
  description: string | null;
  idempotencyKey: string | null;
  /**
   * Page où régler un dépôt encore en attente.
   *
   * Renseignée quand le prestataire ne pousse pas la demande sur le téléphone
   * du client mais le fait payer sur sa propre page. Nulle partout ailleurs —
   * y compris sur un dépôt déjà réglé. Elle survit au rechargement de l'écran
   * parce qu'elle est stockée côté serveur : reprendre un dépôt interrompu
   * consiste à relire l'opération, pas à en ouvrir une seconde.
   */
  paymentUrl: string | null;
  createdAt: string;
};

/** Page<T> de Spring Data — les champs réellement lus par cette application. */
export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
};

/* --- Moyens de paiement (payment/PaymentMethodController) ----------------- */

/**
 * Un opérateur Mobile Money encaissable ou payable.
 *
 * `code` est le nom de l'enum Java (`MTN_BENIN`), pas le code FedaPay
 * (`mtn_open`) : c'est ce que les endpoints attendent, et le code du
 * prestataire ne sort jamais du backend.
 */
export type PaymentMethod = {
  code: string;
  label: string;
  countryCode: string;
};

export type PaymentMethods = {
  /** Opérateurs du pays retenu — ce que l'écran propose par défaut. */
  available: PaymentMethod[];
  /** Tous les opérateurs, pour un utilisateur qui retire dans un autre pays. */
  all: PaymentMethod[];
  countryCode: string;
  /**
   * Faux si le prestataire n'est pas configuré sur ce serveur : seul le mode
   * de test est alors possible, et l'écran doit le dire avant de proposer un
   * opérateur qu'il faudrait refuser ensuite.
   */
  providerEnabled: boolean;
  /**
   * Vrai si la demande de débit s'affiche sur le téléphone du client ; faux
   * s'il règle sur une page hébergée par le prestataire.
   *
   * Le prélèvement sans redirection est une autorisation que tout compte
   * marchand n'a pas — l'écran doit annoncer le bon geste AVANT la saisie.
   */
  directCharge: boolean;
  /**
   * Vrai si un retrait vers Mobile Money peut aboutir.
   *
   * Distinct de `providerEnabled` : chez FedaPay, l'encaissement et le
   * versement s'ouvrent séparément sur un compte marchand. Un serveur peut
   * donc encaisser sans pouvoir verser, et l'écran de retrait doit le dire
   * plutôt que de laisser remplir un formulaire qui finira en 503.
   */
  withdrawalEnabled: boolean;
};

/* --- Coffres (vault/VaultResponse) ---------------------------------------- */

export type VaultStatus = "ACTIVE" | "UNLOCKED" | "CLOSED";

export type Vault = {
  id: number;
  name: string;
  purpose: string | null;
  targetAmount: number | null;
  currentAmount: number;
  currency: string;
  unlockDate: string | null;
  status: VaultStatus;
  walletId: number;
};

/* --- Bénéficiaires (beneficiary/BeneficiaryResponse) ---------------------- */

export type MobileNetwork =
  | "MIXX_BY_YAS"
  | "MOOV_TOGO"
  | "WAVE"
  | "ORANGE_MONEY"
  | "FREE_MONEY"
  | "MTN_MOMO"
  | "VODAFONE_CASH"
  | "AIRTELTIGO"
  | "OPAY"
  | "PALMPAY"
  | "WESTERN_UNION"
  | "MONEYGRAM";

export type Beneficiary = {
  id: number;
  alias: string;
  phoneNumber: string;
  countryCode: string;
  network: MobileNetwork;
};

/* --- Marchand (merchant/MerchantResponse) --------------------------------- */

export type Merchant = {
  id: number;
  name: string;
  category: string | null;
  merchantCode: string;
};

/* --- Crédit (credit/ScoreBreakdown, credit/LoanDtos.LoanResponse) --------- */

export type CreditTier =
  | "INELIGIBLE"
  | "BASIC"
  | "STANDARD"
  | "PREMIUM"
  | "ELITE";

/**
 * ScoreBreakdown.RuleScore — `label` et `explanation` arrivent déjà rédigés en
 * français par le backend. On les affiche tels quels : les redériver côté
 * client ferait diverger l'explication du calcul qu'elle décrit.
 */
export type RuleScore = {
  rule: string;
  label: string;
  points: number;
  maxPoints: number;
  explanation: string;
};

export type ScoreBreakdown = {
  totalScore: number;
  tier: CreditTier;
  maxLoanAmount: number;
  /** Fraction décimale (0.015 = 1,5 % / mois), cf. formatMonthlyRate. */
  monthlyRate: number;
  computedAt: string | null;
  expiresAt: string | null;
  details: RuleScore[];
};

/**
 * Capacité d'emprunt (credit/LoanCapacityResponse).
 *
 * ═══ À NE PAS CONFONDRE AVEC `ScoreBreakdown.maxLoanAmount` ═══
 *
 * Le score dit la SOLVABILITÉ : il fixe le taux et un plafond absolu de palier.
 * Cette structure dit le MONTANT, calculé sur les flux réels des 90 derniers
 * jours. Deux personnes au même score obtiennent ici des valeurs différentes si
 * leurs entrées et sorties diffèrent — c'est exactement le but.
 *
 * L'écran de demande doit borner le champ « montant » sur
 * `maxAmountByDuration`, jamais sur `maxLoanAmount` du score : ce dernier est le
 * plafond du palier, que la capacité peut rendre inatteignable.
 */
export type LoanCapacity = {
  monthlyInflow: number;
  monthlyOutflow: number;
  monthlyDisposable: number;
  /** Mois, sur les 3 observés, ayant vu au moins une entrée. */
  activeMonths: number;
  /** Décote appliquée à des revenus irréguliers (0 à 1). */
  stabilityFactor: number;
  tierCeiling: number;
  graduationCeiling: number;
  /** Clés JSON : les durées en mois, sous forme de chaînes ("1" … "12"). */
  maxAmountByDuration: Record<string, number>;
  limitingFactor: "CASH_FLOW" | "TIER" | "GRADUATION" | "NO_ACTIVITY";
  limitingFactorLabel: string;
  tier: CreditTier;
  monthlyRate: number;
  /** Au-delà, la demande passe en examen manuel : pas de versement immédiat. */
  manualReviewThreshold: number;
};

export type LoanStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "DISBURSED"
  | "REPAID"
  | "DEFAULTED";

export type Loan = {
  id: number;
  requestedAmount: number;
  totalRepayment: number;
  monthlyRate: number;
  durationMonths: number;
  status: LoanStatus;
  purpose: string | null;
  dueDate: string | null;
  rejectionReason: string | null;
  creditScoreAtRequest: number;
  tierAtRequest: CreditTier;
};

/* --- Virements programmés (scheduler/ScheduledTransferResponse) ----------- */

export type ScheduleFrequency = "MONTHLY" | "WEEKLY";
export type ScheduleStatus = "ACTIVE" | "PAUSED" | "FAILED_PERMANENTLY";

export type ScheduledTransfer = {
  id: number;
  frequency: ScheduleFrequency;
  executionDay: number;
  amount: number;
  currency: string;
  description: string | null;
  status: ScheduleStatus;
  lastExecutedAt: string | null;
  nextExecutionDate: string | null;
  walletId: number | null;
  targetVaultId: number | null;
  targetVaultName: string | null;
};

/* --- Notifications (notification/NotificationResponse) -------------------- */

export type NotificationType = "TRANSACTION" | "SECURITY" | "SYSTEM";

export type AppNotification = {
  id: number;
  title: string;
  body: string;
  type: NotificationType;
  read: boolean;
  createdAt: string;
};
