// Single source of truth for backend route paths. Every Http*Repository
// builds its URLs from here — nothing hardcodes a path string elsewhere.
// Keep this in sync with BACKEND.md, which documents the request/response
// shape for each one.

/**
 * Escapes one path segment. Not decoration: a support ticket is referenced as
 * "#8821", and an unencoded '#' turns the rest of the path into a URL fragment —
 * the request then leaves as POST /support/tickets/ and comes back 404.
 */
const seg = (value) => encodeURIComponent(String(value));

export const endpoints = {
  auth: {
    login: () => '/auth/login',
    me: () => '/auth/me',
    logout: () => '/auth/logout',
    requestPasswordReset: () => '/auth/password-reset-request',
    changePassword: () => '/auth/change-password',
  },

  dashboard: {
    metrics: () => '/dashboard/metrics',
    transactionVolume: () => '/dashboard/transaction-volume',
    alerts: () => '/dashboard/alerts',
    loanBookSummary: () => '/dashboard/loan-book-summary',
  },

  users: {
    list: () => '/users',
    detail: (id) => `/users/${seg(id)}`,
    unblock: (id) => `/users/${seg(id)}/unblock`,
    forceCloseVault: (id) => `/users/${seg(id)}/force-close-vault`,
    kycQueue: () => '/users/kyc-queue',
    approveKyc: (submissionId) => `/users/kyc-queue/${seg(submissionId)}/approve`,
    rejectKyc: (submissionId) => `/users/kyc-queue/${seg(submissionId)}/reject`,
    // Served by the KYC module, still under the /admin prefix the console is based on.
    kycDocumentFile: (documentId) => `/kyc/documents/${seg(documentId)}/file`,
  },

  credit: {
    stats: () => '/credit/stats',
    tierConfig: () => '/credit/tier-config',
    defaults: () => '/credit/defaults',
    remind: (loanId) => `/credit/defaults/${seg(loanId)}/remind`,
  },

  finance: {
    liquidity: () => '/finance/liquidity',
    revenue: () => '/finance/revenue',
    operatorReconciliation: () => '/finance/operator-reconciliation',
  },

  disputes: {
    list: () => '/disputes',
    detail: (ref) => `/disputes/${seg(ref)}`,
    chargeback: (ref) => `/disputes/${seg(ref)}/chargeback`,
    reject: (ref) => `/disputes/${seg(ref)}/reject`,
    validateChargeback: (ref) => `/disputes/${seg(ref)}/validate`,
  },

  config: {
    fees: () => '/config/fees',
    merchants: () => '/config/merchants',
    updateMerchantStatus: (id) => `/config/merchants/${seg(id)}/status`,
  },

  audit: {
    log: () => '/audit/log',
    exportLog: () => '/audit/log/export',
    reports: () => '/audit/reports',
    exportReport: (id) => `/audit/reports/${seg(id)}/export`,
  },

  roles: {
    admins: () => '/roles/admins',
    updatePermissions: (id) => `/roles/admins/${seg(id)}/permissions`,
  },

  support: {
    tickets: () => '/support/tickets',
    manualActions: () => '/support/manual-actions',
    takeCharge: (ref) => `/support/tickets/${seg(ref)}/take-charge`,
    resolve: (ref) => `/support/tickets/${seg(ref)}/resolve`,
  },
};
