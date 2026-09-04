import { authRepository } from '../repositories';
import { setAuthToken } from '../api/httpClient';

export const authService = {
  /** @returns {Promise<import('../models/auth').AdminIdentity>} */
  async login(email, password) {
    const { token, user } = await authRepository.login(email, password);
    setAuthToken(token);
    return user;
  },

  /** Called once on app boot to restore a session from a stored token. */
  async restoreSession() {
    try {
      return await authRepository.me();
    } catch {
      setAuthToken(null);
      return null;
    }
  },

  async logout() {
    try {
      await authRepository.logout();
    } finally {
      setAuthToken(null);
    }
  },

  requestPasswordReset: (email) => authRepository.requestPasswordReset(email),

  changePassword: (currentPassword, newPassword) =>
    authRepository.changePassword(currentPassword, newPassword),
};
