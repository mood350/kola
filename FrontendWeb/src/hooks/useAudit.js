import { useAsync, useActionRunner } from './useAsync';
import { auditService } from '../services/auditService';

export function useAudit() {
  const { data, loading, error, reload } = useAsync(() => auditService.getOverview());
  const { run, actionError, actionPending } = useActionRunner(reload);

  return {
    loading,
    error,
    actionError,
    actionPending,
    reload,
    log: data?.log || [],
    reports: data?.reports || [],
    exportLog: () => run(() => auditService.exportLog()),
    exportReport: (id) => run(() => auditService.exportReport(id)),
  };
}
