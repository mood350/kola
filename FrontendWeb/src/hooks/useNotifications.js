import { useAsync } from './useAsync';
import { dashboardService } from '../services/dashboardService';

/** Reuses the dashboard's priority alerts as the header notification feed. */
export function useNotifications() {
  const { data, loading, error, reload } = useAsync(() => dashboardService.getOverview());
  return { loading, error, reload, alerts: data?.alerts || [] };
}
