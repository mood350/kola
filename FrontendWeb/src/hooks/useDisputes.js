import { useAsync, useActionRunner } from './useAsync';
import { disputeService } from '../services/disputeService';

export function useDisputes() {
  const { data, loading, error, reload } = useAsync(() => disputeService.getOverview());
  const { run, actionError, actionPending } = useActionRunner(reload);

  return {
    loading,
    error,
    actionError,
    actionPending,
    disputes: data?.disputes || [],
    detail: data?.detail || null,
    chargeback: (ref) => run(() => disputeService.chargeback(ref)),
    reject: (ref) => run(() => disputeService.reject(ref)),
    validateChargeback: (ref) => run(() => disputeService.validateChargeback(ref)),
  };
}
