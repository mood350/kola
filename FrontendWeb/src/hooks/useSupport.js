import { useAsync, useActionRunner } from './useAsync';
import { supportService } from '../services/supportService';

export function useSupport() {
  const { data, loading, error, reload } = useAsync(() => supportService.getOverview(), []);
  const { run, actionError, actionPending } = useActionRunner(reload);
  return {
    loading,
    error,
    actionError,
    actionPending,
    reload,
    tickets: data?.tickets || [],
    manualActions: data?.manualActions || [],
    takeCharge: (ref) => run(() => supportService.takeCharge(ref)),
    resolve: (ref) => run(() => supportService.resolve(ref)),
  };
}
