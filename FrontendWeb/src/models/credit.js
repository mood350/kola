/**
 * @typedef {Object} CreditStats
 * @property {string} outstandingTotal
 * @property {string} defaultRate
 * @property {number} lateLoans
 */

/**
 * @typedef {Object} TierConfig
 * @property {string} name - e.g. "TIER 1"
 * @property {number} minScore
 * @property {string} maxAmount - e.g. "50 000 XOF"
 * @property {string} monthlyRate - e.g. "10 %/mois"
 */

/**
 * @typedef {Object} LoanDefault
 * @property {string} id
 * @property {string} borrowerName
 * @property {string} amount
 * @property {number} daysLate
 */

export {};
