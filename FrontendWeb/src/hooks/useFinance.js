import { useAsync } from './useAsync';
import { financeService } from '../services/financeService';

export function useFinance() {
  const { data, loading, error, reload } = useAsync(() => financeService.getOverview());
  return {
    loading,
    error,
    reload,
    liquidity: data?.liquidity || [],
    revenue: data?.revenue || null,
    operators: data?.operators || [],
  };
}
