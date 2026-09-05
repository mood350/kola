import { useAsync } from './useAsync';
import { dashboardService } from '../services/dashboardService';

export function useDashboard() {
  const { data, loading, error, reload } = useAsync(() => dashboardService.getOverview());
  return {
    loading,
    error,
    reload,
    metrics: data?.metrics || [],
    chart: data?.chart || [],
    alerts: data?.alerts || [],
    loanBook: data?.loanBook || null,
  };
}
