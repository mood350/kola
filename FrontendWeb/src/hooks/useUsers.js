import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAsync, useActionRunner } from './useAsync';
import { userService } from '../services/userService';

const EMPTY_USERS = [];
const EMPTY_KYC = [];

export function useUsers() {
  const { data, loading, error, reload } = useAsync(() => userService.getOverview(), []);
  const { run, actionError, actionPending } = useActionRunner(reload);
  const [filter, setFilter] = useState('Tous');
  const [selectedId, setSelectedId] = useState(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get('q') || '';

  const allUsers = data?.users || EMPTY_USERS;
  const kycQueue = data?.kycQueue || EMPTY_KYC;
  const users = useMemo(() => {
    const byFilter = userService.filterUsers(allUsers, filter);
    return userService.searchUsers(byFilter, query);
  }, [allUsers, filter, query]);
  const selectedUser = allUsers.find((u) => u.id === selectedId) || null;

  const setQuery = (value) => {
    setSearchParams(value ? { q: value } : {}, { replace: true });
  };

  return {
    loading,
    error,
    actionError,
    actionPending,
    users,
    kycQueue,
    filter,
    setFilter,
    query,
    setQuery,
    selectedUser,
    selectUser: setSelectedId,
    clearSelection: () => setSelectedId(null),
    unblock: (id) => run(() => userService.unblock(id)),
    forceCloseVault: (id) => run(() => userService.forceCloseVault(id)),
    approveKyc: (id) => run(() => userService.approveKyc(id)),
    rejectKyc: (id) => run(() => userService.rejectKyc(id)),
  };
}
