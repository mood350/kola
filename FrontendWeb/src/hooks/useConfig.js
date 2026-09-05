import { useAsync, useActionRunner } from './useAsync';
import { configService } from '../services/configService';

export function useConfig() {
  const { data, loading, error, reload } = useAsync(() => configService.getOverview());
  const { run, actionError, actionPending } = useActionRunner(reload);

  return {
    loading,
    error,
    actionError,
    actionPending,
    reload,
    fees: data?.fees || [],
    merchants: data?.merchants || [],
    toggleMerchantStatus: (merchant) => run(() => configService.toggleMerchantStatus(merchant)),
  };
}
