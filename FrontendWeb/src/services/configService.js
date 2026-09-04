import { configRepository } from '../repositories';

const NEXT_STATUS = { active: 'suspended', suspended: 'active', pending: 'active' };

export const configService = {
  async getOverview() {
    const [fees, merchants] = await Promise.all([configRepository.getFees(), configRepository.getMerchants()]);
    return { fees, merchants };
  },

  saveFees: (fees) => configRepository.updateFees(fees),

  /** Business rule for what the single action button does per current status. */
  toggleMerchantStatus(merchant) {
    const next = NEXT_STATUS[merchant.status] || 'active';
    return configRepository.updateMerchantStatus(merchant.id, next);
  },
};
