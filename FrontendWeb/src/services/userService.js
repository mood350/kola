import { userRepository } from '../repositories';

export const userService = {
  async getOverview() {
    const [users, kycQueue] = await Promise.all([userRepository.list(), userRepository.getKycQueue()]);
    return { users, kycQueue };
  },

  getById: (id) => userRepository.getById(id),
  unblock: (id) => userRepository.unblock(id),
  forceCloseVault: (id) => userRepository.forceCloseVault(id),
  approveKyc: (submissionId) => userRepository.approveKyc(submissionId),
  rejectKyc: (submissionId) => userRepository.rejectKyc(submissionId),

  /** Same combined filter the UI exposes: a tier value OR a state value. */
  filterUsers(users, filter) {
    if (!filter || filter === 'Tous') return users;
    return users.filter((u) => u.tier === filter || u.state === filter);
  },

  /** Free-text match on name/phone, used by the header search bar. */
  searchUsers(users, query) {
    const q = (query || '').trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) => u.name.toLowerCase().includes(q) || u.phone.toLowerCase().includes(q));
  },
};
