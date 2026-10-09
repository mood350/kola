import { useCallback, useEffect, useState } from 'react';

/**
 * Charge une ressource et la recharge quand `key` change.
 *
 * `load(signal)` reçoit un AbortSignal : une réponse arrivée après un
 * changement de filtre est abandonnée, sinon l'écran afficherait le résultat
 * de la requête la plus lente plutôt que celui du filtre actif.
 *
 * Le résultat mémorise la clé qui l'a produit ; « en chargement » en est déduit
 * (clé différente de la clé courante). Les données précédentes restent à
 * l'écran pendant le rechargement — la table ne clignote pas.
 */
export function useResource(load, key) {
  const [result, setResult] = useState({ key: null, data: null, error: null });
  const [attempt, setAttempt] = useState(0);
  const currentKey = `${key}#${attempt}`;

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal)
      .then((data) => setResult({ key: currentKey, data, error: null }))
      .catch((error) => {
        if (error?.name === 'AbortError') return;
        setResult((previous) => ({ key: currentKey, data: previous.data, error: error.message }));
      });
    return () => controller.abort();
    // `load` est reconstruit à chaque rendu ; `currentKey` décrit tout ce dont il dépend.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentKey]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);
  const replace = useCallback((data) => setResult((previous) => ({ ...previous, data })), []);

  return {
    data: result.data,
    error: result.key === currentKey ? result.error : null,
    loading: result.key !== currentKey,
    reload,
    replace,
  };
}
