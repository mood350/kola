// Single source of truth for backend route paths. Every Http*Repository
// builds its URLs from here — nothing hardcodes a path string elsewhere.
// Keep this in sync with BACKEND.md, which documents the request/response
// shape for each one.

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
    detail: (id) => `/users/${id}`,
    unblock: (id) => `/users/${id}/unblock`,
    forceCloseVault: (id) => `/users/${id}/force-close-vault`,
    kycQueue: () => '/users/kyc-queue',
    approveKyc: (submissionId) => `/users/kyc-queue/${submissionId}/approve`,
    rejectKyc: (submissionId) => `/users/kyc-queue/${submissionId}/reject`,
  },

  credit: {
    stats: () => '/credit/stats',
    tierConfig: () => '/credit/tier-config',
    defaults: () => '/credit/defaults',
    remind: (loanId) => `/credit/defaults/${loanId}/remind`,
  },

  finance: {
    liquidity: () => '/finance/liquidity',
    revenue: () => '/finance/revenue',
    operatorReconciliation: () => '/finance/operator-reconciliation',
  },

  disputes: {
    list: () => '/disputes',
    detail: (ref) => `/disputes/${ref}`,
    chargeback: (ref) => `/disputes/${ref}/chargeback`,
    reject: (ref) => `/disputes/${ref}/reject`,
    validateChargeback: (ref) => `/disputes/${ref}/validate`,
  },

  config: {
    fees: () => '/config/fees',
    merchants: () => '/config/merchants',
    updateMerchantStatus: (id) => `/config/merchants/${id}/status`,
  },

  audit: {
    log: () => '/audit/log',
    exportLog: () => '/audit/log/export',
    reports: () => '/audit/reports',
    exportReport: (id) => `/audit/reports/${id}/export`,
  },

  roles: {
    admins: () => '/roles/admins',
    updatePermissions: (id) => `/roles/admins/${id}/permissions`,
  },

  support: {
    tickets: () => '/support/tickets',
    manualActions: () => '/support/manual-actions',
    takeCharge: (ref) => `/support/tickets/${ref}/take-charge`,
    resolve: (ref) => `/support/tickets/${ref}/resolve`,
  },
};
