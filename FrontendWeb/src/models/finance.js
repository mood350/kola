/**
 * @typedef {Object} LiquidityBucket
 * @property {string} label
 * @property {string} value
 * @property {string} note
 * @property {number} pct - share of total liquidity, 0-100 (drives the stacked bar)
 */

/**
 * @typedef {Object} RevenueLine
 * @property {string} label
 * @property {string} value
 * @property {number} pct - 0-100
 */

/**
 * @typedef {Object} OperatorStatus
 * @property {string} name
 * @property {'reconciled'|'discrepancy'} status
 */

export {};
