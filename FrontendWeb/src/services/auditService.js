import { auditRepository } from '../repositories';

export const auditService = {
  async getOverview() {
    const [log, reports] = await Promise.all([auditRepository.getLog(), auditRepository.getReports()]);
    return { log, reports };
  },

  exportLog: () => auditRepository.exportLog(),
  exportReport: (id) => auditRepository.exportReport(id),
};
