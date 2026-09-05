import { financeRepository } from '../repositories';

export const financeService = {
  async getOverview() {
    const [liquidity, revenue, operators] = await Promise.all([
      financeRepository.getLiquidity(),
      financeRepository.getRevenue(),
      financeRepository.getOperatorReconciliation(),
    ]);
    return { liquidity, revenue, operators };
  },
};
