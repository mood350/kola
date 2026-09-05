/**
 * @typedef {Object} Dispute
 * @property {string} ref - transaction reference, e.g. "TX-99C41A"
 * @property {'fraud'|'double_debit'|'p2p'} tag
 * @property {string} tagLabel
 * @property {string} amount
 * @property {string} title
 * @property {string} meta
 * @property {'open'|'chargeback_pending'|'resolved'|'rejected'} status
 */

/**
 * @typedef {Object} DisputeDetail
 * @property {string} ref
 * @property {string} debitedAccount
 * @property {string} creditedAccount
 * @property {string} amount
 * @property {number} validationsRequired
 * @property {number} validationsDone
 * @property {string} lastValidationNote
 */

export {};
