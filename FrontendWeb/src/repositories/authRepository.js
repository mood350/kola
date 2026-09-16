// Repository contract (implemented identically by both classes below):
//   login(email, password)              -> Promise<{ token: string, user: AdminIdentity }>
//   me(token)                            -> Promise<AdminIdentity>
//   logout()                             -> Promise<void>
//   requestPasswordReset(email)          -> Promise<void>
//   changePassword(currentPwd, newPwd)   -> Promise<void>
//
// Swap which one `repositories/index.js` hands to the service layer and
// nothing above this file needs to change.

import { httpClient, getAuthToken } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay } from './mockUtils';

const ADMIN_SEED = [
  { id: 'sena.ametepe@kola.io', name: 'Sena Amétépé', email: 'sena.ametepe@kola.io', role: 'Super-admin', scope: 'Accès total · configuration produit', lastLoginAt: 'Il y a 12 min' },
  { id: 'koffi.messan@kola.io', name: 'Koffi Messan', email: 'koffi.messan@kola.io', role: 'Agent conformité', scope: 'KYC, litiges, chargebacks (2e validation)', lastLoginAt: 'Il y a 12 min' },
  { id: 'aya.djobo@kola.io', name: 'Aya Djobo', email: 'aya.djobo@kola.io', role: 'Analyste crédit', scope: 'Scoring, paliers de prêt, défauts', lastLoginAt: 'Il y a 12 min' },
  { id: 'prisca.lawson@kola.io', name: 'Prisca Lawson', email: 'prisca.lawson@kola.io', role: 'Support', scope: 'Tickets, consultation comptes (lecture seule)', lastLoginAt: 'Il y a 12 min' },
];

const normalizeEmail = (email) => (email || '').trim().toLowerCase();
const encodeToken = (email) => btoa(unescape(encodeURIComponent(normalizeEmail(email))));
const decodeToken = (token) => {
  try {
    return decodeURIComponent(escape(atob(token)));
  } catch {
    return null;
  }
};

/** No backend yet: looks the email up in a seeded list, password is not checked. */
export class MockAuthRepository {
  async login(email, _password) {
    await delay();
    const account = ADMIN_SEED.find((a) => normalizeEmail(a.email) === normalizeEmail(email));
    if (!account) {
      throw new Error('Aucun compte admin ne correspond à cet e-mail.');
    }
    return { token: encodeToken(account.email), user: { ...account } };
  }

  async me() {
    await delay(120);
    const token = getAuthToken();
    const email = token ? decodeToken(token) : null;
    const account = email && ADMIN_SEED.find((a) => normalizeEmail(a.email) === email);
    if (!account) throw new Error('Session invalide.');
    return { ...account };
  }

  async logout() {
    await delay(80);
  }

  /** Never reveals whether the e-mail matched — avoids account enumeration. */
  async requestPasswordReset(_email) {
    await delay(300);
  }

  async changePassword(currentPassword, newPassword) {
    await delay(300);
    if (!currentPassword) throw new Error('Mot de passe actuel requis.');
    if (!newPassword || newPassword.length < 8) {
      throw new Error('Le nouveau mot de passe doit contenir au moins 8 caractères.');
    }
  }
}

/** Real backend: see BACKEND.md for the exact request/response contract. */
export class HttpAuthRepository {
  async login(email, password) {
    return httpClient.post(endpoints.auth.login(), { email, password });
  }

  async me() {
    return httpClient.get(endpoints.auth.me());
  }

  async logout() {
    return httpClient.post(endpoints.auth.logout(), {});
  }

  async requestPasswordReset(email) {
    return httpClient.post(endpoints.auth.requestPasswordReset(), { email });
  }

  async changePassword(currentPassword, newPassword) {
    return httpClient.post(endpoints.auth.changePassword(), { currentPassword, newPassword });
  }
}
