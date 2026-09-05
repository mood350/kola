/**
 * @typedef {Object} AdminIdentity
 * @property {string} id
 * @property {string} name
 * @property {string} email
 * @property {'Super-admin'|'Agent conformité'|'Analyste crédit'|'Support'} role
 * @property {string} scope - human-readable permission summary
 */

/**
 * @typedef {Object} Session
 * @property {string} token
 * @property {AdminIdentity} user
 */

export {};
