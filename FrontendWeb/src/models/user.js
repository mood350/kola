/**
 * @typedef {Object} ClientUser
 * @property {number} id
 * @property {string} initials
 * @property {string} name
 * @property {string} phone
 * @property {'TIER_0'|'TIER_1'|'TIER_2'|'TIER_3'} tier
 * @property {number} score - 0-100
 * @property {string} age - membership length, e.g. "14 mois"
 * @property {'Actif'|'Gelé'|'Litige'} state
 * @property {number} vaults - active vault count
 * @property {string} loan - human-readable current loan, e.g. "100 000 XOF" or "Aucun"
 */

/**
 * @typedef {Object} KycSubmission
 * @property {string} id
 * @property {string} name
 * @property {string} fromTier
 * @property {string} toTier
 * @property {string} receivedAt - human-readable relative time
 */

export {};
