import { disputeRepository } from '../repositories';

export const disputeService = {
  async getOverview() {
    const disputes = await disputeRepository.list();
    const featured = disputes.find((d) => d.status === 'chargeback_pending') || disputes[0] || null;
    const detail = featured ? await safeDetail(featured.ref) : null;
    return { disputes, detail };
  },

  chargeback: (ref) => disputeRepository.chargeback(ref),
  reject: (ref) => disputeRepository.reject(ref),
  validateChargeback: (ref) => disputeRepository.validateChargeback(ref),
};

async function safeDetail(ref) {
  try {
    return await disputeRepository.getDetail(ref);
  } catch {
    return null;
  }
}
