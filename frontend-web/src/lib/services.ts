import { apiFetch, queryString } from "@/lib/api";
import type {
  AppNotification,
  AuthTokens,
  Beneficiary,
  CurrentUser,
  Loan,
  LoanCapacity,
  Merchant,
  MobileNetwork,
  Page,
  ScheduleFrequency,
  ScheduledTransfer,
  ScoreBreakdown,
  Transaction,
  Vault,
  Wallet,
} from "@/lib/types";

/**
 * Surface d'API de l'application.
 *
 * Un module par domaine du backend, une fonction par endpoint, aucun appel
 * `apiFetch` ailleurs dans le code. L'intérêt est vérifiable : quand un chemin
 * change côté Spring, il ne se corrige qu'ici, et un endpoint qui n'apparaît
 * pas dans ce fichier n'est utilisé nulle part.
 *
 * Les corps de requête reprennent NOM POUR NOM les DTOs Java — `firstname` en
 * un mot à l'inscription mais `firstName` à la mise à jour du profil, par
 * exemple : cette incohérence est celle du backend, la reproduire est ce qui
 * fait que les requêtes passent.
 */

/* ---------------------------------------------------------------------------
   Authentification — /api/auth/**
   ------------------------------------------------------------------------ */

export const authApi = {
  /** POST /auth/login → paire de jetons. */
  login(email: string, password: string) {
    return apiFetch<AuthTokens>("/auth/login", {
      method: "POST",
      body: { email, password },
      anonymous: true,
    });
  },

  /**
   * POST /auth/register → 202 Accepted, SANS corps.
   *
   * Aucune session n'est ouverte : le compte reste désactivé jusqu'à la saisie
   * du code à 6 chiffres reçu par e-mail. C'est le même contrat que sur mobile
   * (`AuthProvider.register` laisse volontairement l'état à « non connecté »),
   * et l'écran d'inscription redirige donc vers la confirmation, pas vers
   * l'accueil.
   */
  register(input: {
    firstname: string;
    lastname: string;
    email: string;
    phoneNumber: string;
    countryCode: string;
    password: string;
  }) {
    return apiFetch<void>("/auth/register", {
      method: "POST",
      body: input,
      anonymous: true,
    });
  },

  /**
   * POST /auth/confirm — le code voyage dans le CORPS.
   *
   * Il transitait autrefois en paramètre d'URL ; or ce code active le compte, et
   * une URL finit dans les journaux du serveur et l'historique du navigateur
   * (cf. le commentaire de `ConfirmAccountRequest`). Ne pas le remettre en
   * query string « pour faire un lien cliquable ».
   */
  confirm(token: string) {
    return apiFetch<void>("/auth/confirm", {
      method: "POST",
      body: { token },
      anonymous: true,
    });
  },

  forgotPassword(email: string) {
    return apiFetch<void>("/auth/forgot-password", {
      method: "POST",
      body: { email },
      anonymous: true,
    });
  },

  resetPassword(token: string, newPassword: string) {
    return apiFetch<void>("/auth/reset-password", {
      method: "POST",
      body: { token, newPassword },
      anonymous: true,
    });
  },

  /** POST /auth/logout → 204. Le retrait des jetons locaux reste à la charge du client. */
  logout() {
    return apiFetch<void>("/auth/logout", { method: "POST" });
  },
};

/* ---------------------------------------------------------------------------
   Profil — /api/users/**
   ------------------------------------------------------------------------ */

export const userApi = {
  me(signal?: AbortSignal) {
    return apiFetch<CurrentUser>("/users/me", { signal });
  },

  updateProfile(input: {
    firstName: string;
    lastName: string;
    phoneNumber: string;
    avatar?: string | null;
  }) {
    return apiFetch<CurrentUser>("/users/me", { method: "PUT", body: input });
  },

  updateAvatar(avatar: string) {
    return apiFetch<CurrentUser>("/users/me/avatar", {
      method: "PUT",
      body: { avatar },
    });
  },

  changePassword(currentPassword: string, newPassword: string) {
    return apiFetch<void>("/users/me/password", {
      method: "POST",
      body: { currentPassword, newPassword },
    });
  },
};

/* ---------------------------------------------------------------------------
   Wallets — /api/wallets/**
   ------------------------------------------------------------------------ */

export const walletApi = {
  list(signal?: AbortSignal) {
    return apiFetch<Wallet[]>("/wallets", { signal });
  },

  get(id: number, signal?: AbortSignal) {
    return apiFetch<Wallet>(`/wallets/${id}`, { signal });
  },

  create(currency = "XOF") {
    return apiFetch<Wallet>("/wallets", { method: "POST", body: { currency } });
  },
};

/* ---------------------------------------------------------------------------
   Transactions — /api/transactions/**
   ------------------------------------------------------------------------ */

export const transactionApi = {
  deposit(input: {
    walletId: number;
    amount: number;
    externalReference?: string;
    idempotencyKey: string;
  }) {
    return apiFetch<Transaction>("/transactions/deposit", {
      method: "POST",
      body: input,
    });
  },

  withdraw(input: {
    walletId: number;
    amount: number;
    idempotencyKey: string;
  }) {
    return apiFetch<Transaction>("/transactions/withdraw", {
      method: "POST",
      body: input,
    });
  },

  transfer(input: {
    sourceWalletId: number;
    beneficiaryId: number;
    amount: number;
    description?: string;
    idempotencyKey: string;
  }) {
    return apiFetch<Transaction>("/transactions/transfer", {
      method: "POST",
      body: input,
    });
  },

  payMerchant(input: {
    sourceWalletId: number;
    merchantCode: string;
    amount: number;
    idempotencyKey: string;
  }) {
    return apiFetch<Transaction>("/transactions/pay-merchant", {
      method: "POST",
      body: input,
    });
  },

  /** GET /transactions/wallet/{id} — pagination Spring Data (page, size). */
  history(
    walletId: number,
    page: number,
    size: number,
    signal?: AbortSignal
  ) {
    return apiFetch<Page<Transaction>>(
      `/transactions/wallet/${walletId}${queryString({ page, size })}`,
      { signal }
    );
  },

  /** GET /transactions/{reference} — la référence, jamais l'identifiant technique. */
  byReference(reference: string, signal?: AbortSignal) {
    return apiFetch<Transaction>(`/transactions/${reference}`, { signal });
  },
};

/* ---------------------------------------------------------------------------
   Coffres — /api/vaults/**
   ------------------------------------------------------------------------ */

export const vaultApi = {
  list(signal?: AbortSignal) {
    return apiFetch<Vault[]>("/vaults", { signal });
  },

  get(id: number, signal?: AbortSignal) {
    return apiFetch<Vault>(`/vaults/${id}`, { signal });
  },

  create(input: {
    walletId: number;
    name: string;
    purpose?: string;
    targetAmount?: number;
    initialAmount: number;
    /** Format AAAA-MM-JJ : le backend attend une `LocalDate`, pas un instant. */
    unlockDate?: string;
  }) {
    return apiFetch<Vault>("/vaults", { method: "POST", body: input });
  },

  addFunds(vaultId: number, amount: number) {
    return apiFetch<Vault>(`/vaults/${vaultId}/add-funds`, {
      method: "POST",
      body: { amount },
    });
  },

  /** Déblocage à échéance — refusé par le backend avant la date (`VaultLockedException`). */
  unlock(vaultId: number) {
    return apiFetch<Vault>(`/vaults/${vaultId}/unlock`, { method: "POST" });
  },

  /** Fermeture anticipée : possible avant l'échéance, avec la pénalité prévue côté backend. */
  closeEarly(vaultId: number) {
    return apiFetch<Vault>(`/vaults/${vaultId}/close`, { method: "POST" });
  },
};

/* ---------------------------------------------------------------------------
   Bénéficiaires — /api/beneficiaries/**
   ------------------------------------------------------------------------ */

export const beneficiaryApi = {
  list(signal?: AbortSignal) {
    return apiFetch<Beneficiary[]>("/beneficiaries", { signal });
  },

  create(input: {
    alias: string;
    phoneNumber: string;
    countryCode: string;
    network: MobileNetwork;
  }) {
    return apiFetch<Beneficiary>("/beneficiaries", {
      method: "POST",
      body: input,
    });
  },

  /**
   * DELETE /beneficiaries/{id} — désactivation, jamais suppression réelle.
   *
   * Politique assumée côté backend (cf. `User.java`) : un bénéficiaire ayant
   * servi à un virement reste référencé par le grand livre. L'interface dit
   * donc « retirer », pas « supprimer définitivement ».
   */
  remove(id: number) {
    return apiFetch<void>(`/beneficiaries/${id}`, { method: "DELETE" });
  },
};

/* ---------------------------------------------------------------------------
   Marchands — /api/merchants/**
   ------------------------------------------------------------------------ */

export const merchantApi = {
  /** GET /merchants/{code} — aucun solde exposé, juste de quoi confirmer qui l'on paie. */
  byCode(code: string, signal?: AbortSignal) {
    return apiFetch<Merchant>(`/merchants/${encodeURIComponent(code)}`, {
      signal,
    });
  },
};

/* ---------------------------------------------------------------------------
   Crédit — /api/credit/**
   ------------------------------------------------------------------------ */

export const creditApi = {
  /** GET /credit/score — recalculé paresseusement par le backend si le cache a expiré. */
  score(signal?: AbortSignal) {
    return apiFetch<ScoreBreakdown>("/credit/score", { signal });
  },

  refreshScore() {
    return apiFetch<ScoreBreakdown>("/credit/score/refresh", { method: "POST" });
  },

  scoreHistory(signal?: AbortSignal) {
    return apiFetch<ScoreBreakdown[]>("/credit/score/history", { signal });
  },

  /**
   * GET /credit/capacity — le MONTANT, par durée.
   *
   * Séparé du score à dessein : le score dit si l'on prête et à quel taux, la
   * capacité dit combien. C'est cette route que l'écran de demande interroge.
   */
  capacity(signal?: AbortSignal) {
    return apiFetch<LoanCapacity>("/credit/capacity", { signal });
  },

  loans(signal?: AbortSignal) {
    return apiFetch<Loan[]>("/credit/loans", { signal });
  },

  loan(id: number, signal?: AbortSignal) {
    return apiFetch<Loan>(`/credit/loans/${id}`, { signal });
  },

  apply(input: {
    walletId: number;
    requestedAmount: number;
    durationMonths: number;
    purpose?: string;
  }) {
    return apiFetch<Loan>("/credit/loans", { method: "POST", body: input });
  },

  /** Remboursement intégral : le backend débite le wallet et met le score à jour. */
  repay(loanId: number) {
    return apiFetch<Loan>(`/credit/loans/${loanId}/repay`, { method: "POST" });
  },
};

/* ---------------------------------------------------------------------------
   Virements programmés — /api/scheduled-transfers/**
   ------------------------------------------------------------------------ */

export const scheduledTransferApi = {
  list(signal?: AbortSignal) {
    return apiFetch<ScheduledTransfer[]>("/scheduled-transfers", { signal });
  },

  create(input: {
    walletId: number;
    targetVaultId?: number;
    frequency: ScheduleFrequency;
    executionDay: number;
    amount: number;
    description?: string;
  }) {
    return apiFetch<ScheduledTransfer>("/scheduled-transfers", {
      method: "POST",
      body: input,
    });
  },

  pause(id: number) {
    return apiFetch<ScheduledTransfer>(`/scheduled-transfers/${id}/pause`, {
      method: "POST",
    });
  },

  resume(id: number) {
    return apiFetch<ScheduledTransfer>(`/scheduled-transfers/${id}/resume`, {
      method: "POST",
    });
  },

  remove(id: number) {
    return apiFetch<void>(`/scheduled-transfers/${id}`, { method: "DELETE" });
  },
};

/* ---------------------------------------------------------------------------
   Notifications — /api/notifications/**
   ------------------------------------------------------------------------ */

export const notificationApi = {
  list(page: number, size: number, signal?: AbortSignal) {
    return apiFetch<Page<AppNotification>>(
      `/notifications${queryString({ page, size })}`,
      { signal }
    );
  },

  /** Réponse `{ "count": n }` — le nombre n'arrive pas nu. */
  unreadCount(signal?: AbortSignal) {
    return apiFetch<{ count: number }>("/notifications/unread-count", { signal });
  },

  markAsRead(id: number) {
    return apiFetch<void>(`/notifications/${id}/read`, { method: "POST" });
  },

  markAllAsRead() {
    return apiFetch<void>("/notifications/read-all", { method: "POST" });
  },
};
