import { useEffect, useState } from 'react';
import { useAsync, useActionRunner } from './useAsync';
import { creditService } from '../services/creditService';

export function useCredit() {
  const { data, loading, error, reload } = useAsync(() => creditService.getOverview());
  const { run, actionError, actionPending } = useActionRunner(reload);
  const [draftTiers, setDraftTiers] = useState([]);

  // Re-baseline the editable draft whenever fresh server data lands
  // (first load, and again after a successful save).
  useEffect(() => {
    if (data?.tierConfig) setDraftTiers(data.tierConfig);
  }, [data?.tierConfig]);

  const updateTierField = (index, field, value) => {
    setDraftTiers((prev) => prev.map((t, i) => (i === index ? { ...t, [field]: value } : t)));
  };

  return {
    loading,
    error,
    actionError,
    actionPending,
    stats: data?.stats || null,
    defaults: data?.defaults || [],
    tierConfig: draftTiers,
    updateTierField,
    saveTierConfig: () => run(() => creditService.saveTierConfig(draftTiers)),
    remind: (loanId) => run(() => creditService.remind(loanId)),
  };
}
