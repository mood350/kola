/**
 * Miroir TypeScript des DTO du backend.
 *
 * CE FICHIER EST UN CONTRAT, PAS UNE COMMODITÉ. Chaque type correspond à un
 * `record` Java précis ; le nom du fichier source est cité au-dessus de chaque
 * bloc. Quand un DTO bouge côté Java, c'est ici que la correction doit être
 * répercutée — le compilateur TypeScript signalera alors tous les écrans à
 * ajuster, ce qu'aucune vérification à l'exécution ne ferait.
 *
 * SUR LES MONTANTS. Les `BigDecimal` Java sont sérialisés par Jackson en
 * nombres JSON, et arrivent donc en `number`. C'est exact pour des montants en
 * XOF — une devise sans subdivision, dont les magnitudes réelles (jusqu'à
 * quelques millions) tiennent très largement dans un entier sûr en JavaScript.
 * Ce ne serait PAS vrai d'une devise à forte inflation ou d'un calcul cumulé
 * sur l'ensemble du registre : ces valeurs ne servent ici qu'à l'affichage,
 * jamais à une arithmétique dont dépendrait une écriture comptable.
 */

/* ---------------------------------------------------------------------------
   Enveloppes communes
   ------------------------------------------------------------------------ */

/**
 * Page Spring Data, telle que sérialisée par `Page<T>`.
 *
 * Seuls les champs réellement consommés sont déclarés : la charge utile en
 * contient davantage (`pageable`, `sort`, `empty`…), mais les typer engagerait
 * à les maintenir sans rien en faire.
 */
export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  /** Index de page, à partir de 0. */
  number: number;
  size: number;
  first: boolean;
  last: boolean;
};

/** `GlobalExceptionHandler.ErrorResponse` — forme unique des erreurs de l'API. */
export type ApiErrorBody = {
  code: string;
  message: string;
  details?: Record<string, string>;
  path?: string;
  timestamp?: string;
};

/* ---------------------------------------------------------------------------
   auth/AuthenticationResponse.java
   ------------------------------------------------------------------------ */

export type AuthTokens = {
  access_token: string;
  refresh_token: string;
};

/* ---------------------------------------------------------------------------
   user/UserResponse.java
   ------------------------------------------------------------------------ */

/**
 * Profil de l'administrateur connecté, servi par `GET /api/users/me`.
 *
 * IL N'Y A PAS DE CHAMP `roles`, ET CE N'EST PAS UN OUBLI : `UserResponse`
 * l'exclut explicitement (« pas de rôles/authorities »), et le JWT émis par
 * `AuthenticationService` ne porte aucun claim de rôle non plus — il n'a que
 * `sub`, `type`, `iat` et `exp`.
 *
 * La console ne peut donc PAS déduire la qualité d'administrateur du jeton ni
 * de ce profil. Elle la constate en appelant un endpoint réservé et en lisant
 * la réponse : 200 signifie ADMIN, 403 signifie non. Voir `session.tsx`.
 */
export type CurrentUser = {
  id: number;
  firstName: string | null;
  lastName: string | null;
  email: string;
  phoneNumber: string;
  countryCode: string | null;
  kycLevel: KycLevel;
  avatar: string | null;
  lastKnownIp: string | null;
  lastKnownUserAgent: string | null;
  createdAt: string;
};

/* ---------------------------------------------------------------------------
   admin/AdminOverviewResponse.java + admin/AdminMetric.java
   ------------------------------------------------------------------------ */

export type AdminMetric = {
  label: string;
  count: number;
  amount: number;
};

export type AdminOverview = {
  activeUsers: number;
  totalUsers: number;
  adminUsers: number;
  activeWallets: number;
  activeVaults: number;
  totalTransactionVolume: number;
  totalFees: number;
  activeWalletBalance: number;
  lockedVaultAmount: number;
  /** Pourcentages déjà calculés côté serveur — ne pas les multiplier par 100. */
  userGrowthRate: number;
  transactionVolumeGrowthRate: number;
  usersByCountry: AdminMetric[];
  usersByKycLevel: AdminMetric[];
  walletsByCurrency: AdminMetric[];
  transactionsByStatus: AdminMetric[];
  transactionsByType: AdminMetric[];
  transactionsByCurrency: AdminMetric[];
  transactionsByCountry: AdminMetric[];
  /** Libellés au format « AAAA-MM », six mois glissants. */
  monthlyTransactions: AdminMetric[];
};

/* ---------------------------------------------------------------------------
   user/KycLevel.java + admin/AdminUserDtos.java
   ------------------------------------------------------------------------ */

export const KYC_LEVELS = ["TIER_0", "TIER_1", "TIER_2", "TIER_3"] as const;
export type KycLevel = (typeof KYC_LEVELS)[number];

/** Libellés d'après la documentation de l'enum Java. */
export const KYC_LABELS: Record<KycLevel, string> = {
  TIER_0: "Inscription simple",
  TIER_1: "E-mail vérifié",
  TIER_2: "Pièce soumise",
  TIER_3: "Identité validée",
};

export type AdminUserSummary = {
  id: number;
  firstName: string | null;
  lastName: string | null;
  email: string;
  phoneNumber: string;
  countryCode: string | null;
  kycLevel: KycLevel;
  enabled: boolean;
  accountLocked: boolean;
  failedLoginAttempts: number;
  /**
   * Nul sur un verrou ADMINISTRATIF — c'est ce qui l'empêche de s'auto-lever au
   * bout de trente minutes, contrairement au verrou automatique déclenché par
   * cinq échecs de connexion. Voir `AdminUserService.lock()`. L'interface doit
   * distinguer les deux : ils ne se résolvent pas de la même façon.
   */
  lockedAt: string | null;
  roles: string[];
  createdAt: string;
};

export type WalletSummary = {
  id: number;
  currency: string;
  balance: number;
  lockedBalance: number;
  active: boolean;
};

export type VaultSummary = {
  id: number;
  name: string;
  currency: string;
  currentAmount: number;
  targetAmount: number | null;
  unlockDate: string | null;
  status: "ACTIVE" | "UNLOCKED" | "CLOSED";
};

export type CreditSummary = {
  score: number;
  tier: CreditTier;
  maxLoanAmount: number;
  monthlyRate: number;
  expiresAt: string;
  computedAt: string;
};

export type LoanSummary = {
  id: number;
  requestedAmount: number;
  totalRepayment: number;
  durationMonths: number;
  status: LoanStatus;
  dueDate: string | null;
  defaultedAt: string | null;
  createdAt: string;
};

export type AdminUserDetail = {
  identity: AdminUserSummary;
  avatar: string | null;
  lastKnownIp: string | null;
  lastKnownUserAgent: string | null;
  wallets: WalletSummary[];
  vaults: VaultSummary[];
  /** Nul tant qu'aucun score n'a été calculé — cas normal d'un compte récent. */
  credit: CreditSummary | null;
  loans: LoanSummary[];
};

/* ---------------------------------------------------------------------------
   credit/CreditTier.java + credit/LoanStatus.java + admin/AdminLoanDtos.java
   ------------------------------------------------------------------------ */

export const CREDIT_TIERS = [
  "INELIGIBLE",
  "BASIC",
  "STANDARD",
  "PREMIUM",
  "ELITE",
] as const;
export type CreditTier = (typeof CREDIT_TIERS)[number];

export const TIER_LABELS: Record<CreditTier, string> = {
  INELIGIBLE: "Inéligible",
  BASIC: "Basique",
  STANDARD: "Standard",
  PREMIUM: "Premium",
  ELITE: "Élite",
};

export const LOAN_STATUSES = [
  "PENDING",
  "APPROVED",
  "REJECTED",
  "DISBURSED",
  "REPAID",
  "DEFAULTED",
] as const;
export type LoanStatus = (typeof LOAN_STATUSES)[number];

export const LOAN_STATUS_LABELS: Record<LoanStatus, string> = {
  PENDING: "En attente",
  APPROVED: "Approuvé",
  REJECTED: "Refusé",
  DISBURSED: "Versé",
  REPAID: "Remboursé",
  DEFAULTED: "En défaut",
};

export type AdminLoanSummary = {
  id: number;
  borrowerId: number | null;
  borrowerFullName: string | null;
  borrowerEmail: string | null;
  requestedAmount: number;
  totalRepayment: number;
  /** Taux MENSUEL en fraction décimale : 0.015 = 1,5 % par mois. */
  monthlyRate: number;
  durationMonths: number;
  status: LoanStatus;
  purpose: string | null;
  dueDate: string | null;
  rejectionReason: string | null;
  creditScoreAtRequest: number;
  tierAtRequest: CreditTier | null;
  defaultedAt: string | null;
  createdAt: string;
};

export type LoanStatusBucket = {
  status: LoanStatus;
  count: number;
  principal: number;
  totalRepayment: number;
};

export type AdminLoanOverview = {
  total: number;
  outstandingPrincipal: number;
  outstandingRepayment: number;
  byStatus: LoanStatusBucket[];
};

/* ---------------------------------------------------------------------------
   aml/AmlAlertResponse.java + aml/AmlAlertStatus.java + aml/AmlRiskLevel.java
   ------------------------------------------------------------------------ */

export const AML_STATUSES = [
  "OPEN",
  "REVIEWING",
  "CLEARED",
  "CONFIRMED",
] as const;
export type AmlAlertStatus = (typeof AML_STATUSES)[number];

export const AML_STATUS_LABELS: Record<AmlAlertStatus, string> = {
  OPEN: "Ouverte",
  REVIEWING: "En cours d'examen",
  CLEARED: "Classée sans suite",
  CONFIRMED: "Soupçon confirmé",
};

export const AML_RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export type AmlRiskLevel = (typeof AML_RISK_LEVELS)[number];

export const AML_RISK_LABELS: Record<AmlRiskLevel, string> = {
  LOW: "Faible",
  MEDIUM: "Modéré",
  HIGH: "Élevé",
  CRITICAL: "Critique",
};

export type AmlAlert = {
  id: number;
  userId: number | null;
  userFullName: string | null;
  userEmail: string | null;
  transactionReference: string | null;
  transactionAmount: number | null;
  transactionType: string | null;
  riskScore: number;
  riskLevel: AmlRiskLevel;
  status: AmlAlertStatus;
  /**
   * Chaîne JSON produite par le moteur de règles, PAS un objet. Le backend la
   * stocke et la relaie telle quelle ; c'est au client de l'analyser, en
   * acceptant qu'elle puisse être malformée ou nulle.
   */
  triggeredRulesJson: string | null;
  reviewNotes: string | null;
  reviewedBy: string | null;
  createdAt: string;
};

/** `AmlAlertService.overview()` renvoie une Map, d'où la forme par index. */
export type AmlOverview = {
  total: number;
  byStatus: Record<AmlAlertStatus, number>;
  byRiskLevel: Record<AmlRiskLevel, number>;
};
