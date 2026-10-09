// Dates calendaires : des chaînes AAAA-MM-JJ, calculées en UTC.
//
// Le serveur découpe ses jours en UTC, qui est l'heure de Lomé toute l'année : « un jour » est donc
// une chaîne, jamais un instant. Calculer avec Date.UTC évite que le fuseau du navigateur décale
// une date d'un jour — le défaut classique d'un calendrier fait avec des `new Date(année, mois, jour)`.

const pad = (n) => String(n).padStart(2, '0');

export const toIso = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`;

/** { y, m, d } d'une chaîne AAAA-MM-JJ qui désigne un vrai jour (le 31 février n'en est pas un), sinon null. */
export function parseIso(iso) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso || '');
  if (!match) return null;
  const [y, m, d] = match.slice(1).map(Number);
  const probe = new Date(Date.UTC(y, m - 1, d));
  return probe.getUTCFullYear() === y && probe.getUTCMonth() === m - 1 && probe.getUTCDate() === d ? { y, m, d } : null;
}

const fromTime = (time) => {
  const date = new Date(time);
  return toIso(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
};

export function addDays(iso, days) {
  const { y, m, d } = parseIso(iso);
  return fromTime(Date.UTC(y, m - 1, d + days));
}

export const daysInMonth = (y, m) => new Date(Date.UTC(y, m, 0)).getUTCDate();

/** Même jour, `months` mois plus loin ; le 31 janvier + 1 mois donne le 28 (ou 29) février, pas le 3 mars. */
export function addMonths(iso, months) {
  const { y, m, d } = parseIso(iso);
  const index = y * 12 + (m - 1) + months;
  const ny = Math.floor(index / 12);
  const nm = (index % 12) + 1;
  return toIso(ny, nm, Math.min(d, daysInMonth(ny, nm)));
}

/** 0 pour lundi … 6 pour dimanche : la semaine française commence le lundi. */
export function weekday(iso) {
  const { y, m, d } = parseIso(iso);
  return (new Date(Date.UTC(y, m - 1, d)).getUTCDay() + 6) % 7;
}

/** Les semaines d'un mois : des lignes de 7 cases, une chaîne AAAA-MM-JJ par jour du mois et null autour. */
export function monthWeeks(y, m) {
  const lead = weekday(toIso(y, m, 1));
  const cells = [
    ...Array(lead).fill(null),
    ...Array.from({ length: daysInMonth(y, m) }, (_, i) => toIso(y, m, i + 1)),
  ];
  while (cells.length % 7 !== 0) cells.push(null);
  return Array.from({ length: cells.length / 7 }, (_, row) => cells.slice(row * 7, row * 7 + 7));
}

/** Le jour d'aujourd'hui (UTC, c'est-à-dire Lomé). */
export const todayIso = () => fromTime(Date.now());

/** « 2026-10-09 » → « 09/10/2026 ». */
export const displayIso = (iso) => (parseIso(iso) ? iso.split('-').reverse().join('/') : '');

/** Ramène une date dans [min, max] ; une borne absente ne borne pas. */
export function clampIso(iso, min, max) {
  if (min && iso < min) return min;
  if (max && iso > max) return max;
  return iso;
}
