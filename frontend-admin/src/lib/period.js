// Périodes : des jours calendaires UTC (l'heure de Lomé), bornes incluses —
// exactement ce que l'API attend dans `from` / `to` (AAAA-MM-JJ).

const iso = (date) => date.toISOString().slice(0, 10);

function daysAgo(n) {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() - n);
  return iso(d);
}

function monthStart() {
  const d = new Date();
  return iso(new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 1)));
}

/** Préréglages proposés partout où l'on filtre par date. */
export const PRESETS = [
  { key: 'today', label: 'Aujourd\'hui', range: () => ({ from: daysAgo(0), to: daysAgo(0) }) },
  { key: 'yesterday', label: 'Hier', range: () => ({ from: daysAgo(1), to: daysAgo(1) }) },
  { key: '7d', label: '7 jours', range: () => ({ from: daysAgo(6), to: daysAgo(0) }) },
  { key: '30d', label: '30 jours', range: () => ({ from: daysAgo(29), to: daysAgo(0) }) },
  { key: 'month', label: 'Ce mois', range: () => ({ from: monthStart(), to: daysAgo(0) }) },
  { key: 'all', label: 'Tout', range: () => ({ from: '', to: '' }) },
];

/** Le préréglage qui correspond exactement à la période, ou null (période libre). */
export function activePreset(from, to) {
  const match = PRESETS.find((p) => {
    const r = p.range();
    return r.from === (from || '') && r.to === (to || '');
  });
  return match ? match.key : null;
}

const frDay = (value) => value.split('-').reverse().join('/');

/** « du 01/09/2026 au 24/09/2026 », pour dire ce que couvre un total ou un export. */
export function describePeriod(from, to) {
  if (!from && !to) return 'toutes dates';
  if (from && to && from === to) return `le ${frDay(from)}`;
  if (from && to) return `du ${frDay(from)} au ${frDay(to)}`;
  if (from) return `depuis le ${frDay(from)}`;
  return `jusqu'au ${frDay(to)}`;
}
