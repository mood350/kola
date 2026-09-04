import { dashboardRepository } from '../repositories';

export const dashboardService = {
  /** Orchestrates the four independent reads the dashboard page needs. */
  async getOverview(period = '14d') {
    const [metrics, chart, alerts, loanBook] = await Promise.all([
      dashboardRepository.getMetrics(),
      dashboardRepository.getTransactionVolume(period),
      dashboardRepository.getAlerts(),
      dashboardRepository.getLoanBookSummary(),
    ]);
    return { metrics, chart, alerts, loanBook };
  },
};
