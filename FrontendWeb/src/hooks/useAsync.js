import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Generic data-fetching hook: runs `factory()` once on mount, tracks
 * loading/error/data, and exposes `reload()` so callers can refresh after a
 * mutation. Every domain hook (useUsers, useCredit, ...) is a thin wrapper
 * around this — none of them re-implement loading/error bookkeeping.
 */
export function useAsync(factory) {
  const [state, setState] = useState({ data: null, loading: true, error: null });

  // Keep the latest `factory` closure available to `reload` without making
  // `reload`'s identity depend on it (callers pass a fresh arrow function
  // every render). Synced in an effect, never mutated during render.
  const factoryRef = useRef(factory);
  useEffect(() => {
    factoryRef.current = factory;
  });

  const reload = useCallback(() => {
    let cancelled = false;
    setState((s) => ({ ...s, loading: true, error: null }));
    factoryRef
      .current()
      .then((data) => {
        if (!cancelled) setState({ data, loading: false, error: null });
      })
      .catch((error) => {
        if (!cancelled) setState({ data: null, loading: false, error });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => reload(), [reload]);

  return { ...state, reload };
}

/**
 * Runs a mutation (`run(fn)`), then reloads the owning `useAsync` list on
 * success. Centralizes the try/reload/catch dance so domain hooks don't
 * each reimplement it for every button (unblock, approve, chargeback, ...).
 */
export function useActionRunner(reload) {
  const [actionError, setActionError] = useState(null);
  const [actionPending, setActionPending] = useState(false);

  const run = useCallback(
    async (fn) => {
      setActionError(null);
      setActionPending(true);
      try {
        const result = await fn();
        reload();
        return result;
      } catch (error) {
        setActionError(error?.message || String(error));
        throw error;
      } finally {
        setActionPending(false);
      }
    },
    [reload]
  );

  return { run, actionError, actionPending };
}
