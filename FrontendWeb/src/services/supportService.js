import { supportRepository } from '../repositories';

export const supportService = {
  async getOverview() {
    const [tickets, manualActions] = await Promise.all([
      supportRepository.getTickets(),
      supportRepository.getManualActions(),
    ]);
    return { tickets, manualActions };
  },

  takeCharge: (ref) => supportRepository.takeCharge(ref),
  resolve: (ref) => supportRepository.resolve(ref),
};
