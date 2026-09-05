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

  // Kicks off the request without touching `loading` synchronously — safe to
  // call directly from an effect since state only changes inside the
  // (async) .then()/.catch() callbacks, never during the effect's own
  // synchronous execution.
  const runFetch = useCallback(() => {
    let cancelled = false;
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

  // Manual reload (retry button, after a mutation): flip back to loading
  // immediately since the previous data/error is stale.
  const reload = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }));
    return runFetch();
  }, [runFetch]);

  // Initial fetch on mount: state already starts as loading, so no
  // synchronous setState is needed before firing the request.
  useEffect(() => runFetch(), [runFetch]);

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
