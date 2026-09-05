/**
 * @typedef {Object} Metric
 * @property {string} label
 * @property {string} value
 * @property {string} delta - human-readable delta, e.g. "+ 14,2 % vs hier"
 * @property {boolean} up - whether the delta is favorable (drives color)
 */

/**
 * @typedef {Object} ChartPoint
 * @property {string} day - day-of-month label shown under the bar
 * @property {number} h - bar height as a percentage (0-100)
 * @property {boolean} last - true for the most recent point (highlighted)
 */

/**
 * @typedef {Object} Alert
 * @property {string} title
 * @property {string} detail
 * @property {'critical'|'warning'} severity
 */

/**
 * @typedef {Object} LoanBookSummary
 * @property {string} outstanding - e.g. "1,86 Md XOF"
 * @property {number} allocatedPct - 0-100
 * @property {string} defaultRate - e.g. "2,8 %"
 * @property {string} defaultRateNote
 * @property {string} userGrowth - e.g. "+ 3 940 / sem."
 * @property {string} activeUsersTotal - e.g. "184 320 comptes actifs au total"
 */

export {};
