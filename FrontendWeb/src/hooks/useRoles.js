import { useAsync, useActionRunner } from './useAsync';
import { roleService } from '../services/roleService';

export function useRoles() {
  const { data, loading, error, reload } = useAsync(() => roleService.getAdmins());
  const { run, actionError, actionPending } = useActionRunner(reload);
  return {
    loading,
    error,
    actionError,
    actionPending,
    reload,
    admins: data || [],
    updatePermissions: (id, changes) => run(() => roleService.updatePermissions(id, changes)),
  };
}
