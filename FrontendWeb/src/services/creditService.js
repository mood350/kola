import { creditRepository } from '../repositories';

export const creditService = {
  async getOverview() {
    const [stats, tierConfig, defaults] = await Promise.all([
      creditRepository.getStats(),
      creditRepository.getTierConfig(),
      creditRepository.getDefaults(),
    ]);
    return { stats, tierConfig, defaults };
  },

  saveTierConfig: (tiers) => creditRepository.updateTierConfig(tiers),
  remind: (loanId) => creditRepository.remind(loanId),
};
