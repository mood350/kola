import { useSearchParams } from 'react-router-dom';

/**
 * Filtres gardés dans l'URL : une vue filtrée se partage par lien, survit au
 * rechargement et au bouton « retour ».
 *
 * `defaults` donne les clés connues et leur valeur par défaut. Une valeur égale
 * à sa valeur par défaut n'apparaît pas dans l'URL. Tout changement de filtre
 * remet la pagination à la première page ; seul un changement de `page` la garde.
 */
export function useFilters(defaults) {
  const [params, setParams] = useSearchParams();

  const values = {};
  for (const [key, fallback] of Object.entries(defaults)) {
    const raw = params.get(key);
    values[key] = raw === null ? fallback : typeof fallback === 'number' ? Number(raw) : raw;
  }

  const update = (next) => {
    const merged = { ...values, ...next };
    if (!('page' in next) && 'page' in defaults) merged.page = defaults.page;
    const out = {};
    for (const [key, value] of Object.entries(merged)) {
      if (value !== '' && value !== null && value !== undefined && value !== defaults[key]) out[key] = String(value);
    }
    setParams(out);
  };

  const reset = () => setParams({});
  const dirty = Object.keys(defaults).some((k) => k !== 'page' && k !== 'size' && values[k] !== defaults[k]);

  return [values, update, reset, dirty];
}
