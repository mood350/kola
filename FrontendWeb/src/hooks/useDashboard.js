import { useState } from 'react';
import { useAsync } from './useAsync';
import { dashboardService } from '../services/dashboardService';

/** Periods the chart offers, in display order. Values are the API's own. */
export const CHART_PERIODS = [
  { value: '14d', label: '14 j' },
  { value: '30d', label: '30 j' },
];

export function useDashboard() {
  const [period, setPeriod] = useState(CHART_PERIODS[0].value);
  // `period` doubles as the refetch key: changing it re-issues the four reads
  // rather than leaving the chart showing the range that is no longer selected.
  const { data, loading, error, reload } = useAsync(
    () => dashboardService.getOverview(period),
    period
  );
  return {
    loading,
    error,
    reload,
    period,
    setPeriod,
    metrics: data?.metrics || [],
    chart: data?.chart || [],
    alerts: data?.alerts || [],
    loanBook: data?.loanBook || null,
  };
}
