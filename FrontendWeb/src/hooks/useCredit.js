import { useState } from 'react';
import { useAsync, useActionRunner } from './useAsync';
import { creditService } from '../services/creditService';

export function useCredit() {
  const { data, loading, error, reload } = useAsync(() => creditService.getOverview());
  const { run, actionError, actionPending } = useActionRunner(reload);
  const [draftTiers, setDraftTiers] = useState([]);
  const [syncedTierConfig, setSyncedTierConfig] = useState(null);

  // Re-baseline the editable draft whenever fresh server data lands (first
  // load, and again after a successful save). Done during render rather
  // than in an effect, per React's guidance for adjusting state when a prop
  // changes — avoids the extra render pass a useEffect would cost here.
  if (data?.tierConfig && data.tierConfig !== syncedTierConfig) {
    setSyncedTierConfig(data.tierConfig);
    setDraftTiers(data.tierConfig);
  }

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
