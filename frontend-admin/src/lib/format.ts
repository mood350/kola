/**
 * Formatage des valeurs affichées.
 *
 * Centralisé pour une raison de fond : dans une console financière, deux écrans
 * qui formatent le même montant différemment font douter de la donnée
 * elle-même. Un opérateur qui lit « 25000 » ici et « 25 000 F CFA » là se
 * demande s'il regarde la même chose.
 *
 * Les instances `Intl` sont construites UNE fois au chargement du module :
 * créer un formateur est coûteux, et un tableau de deux cents lignes en
 * appellerait autant. C'est l'optimisation qui compte réellement sur ces
 * écrans.
 */

const XOF = new Intl.NumberFormat("fr-FR", {
  style: "currency",
  currency: "XOF",
  /* Le franc CFA n'a pas de subdivision : afficher des centimes suggérerait
     une précision qui n'existe pas dans la devise. */
  maximumFractionDigits: 0,
});

const INTEGER = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 });

const DATE = new Intl.DateTimeFormat("fr-FR", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

const DATE_TIME = new Intl.DateTimeFormat("fr-FR", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

/** Montant en francs CFA. */
export function formatAmount(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  return XOF.format(value);
}

/** Nombre entier, séparateurs de milliers compris. */
export function formatCount(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  return INTEGER.format(value);
}

/**
 * Taux de croissance renvoyé par le backend.
 *
 * `AdminAnalyticsService.growthRate()` produit DÉJÀ un pourcentage (12.5 pour
 * « +12,5 % ») : le multiplier par cent le centuplerait. Le signe est toujours
 * explicite — sans le « + », une hausse et une baisse se ressemblent trop dans
 * un tableau de bord parcouru rapidement.
 */
export function formatGrowth(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  const rounded = Math.round(value * 10) / 10;
  const sign = rounded > 0 ? "+" : "";
  return `${sign}${rounded.toString().replace(".", ",")} %`;
}

/**
 * Taux mensuel d'un prêt.
 *
 * Stocké en fraction décimale côté base (`0.015`), affiché en pourcentage
 * mensuel (« 1,5 % / mois ») — la forme sous laquelle le taux est annoncé à
 * l'emprunteur. Omettre « / mois » laisserait croire à un taux annuel et
 * fausserait la lecture d'un facteur douze.
 */
export function formatMonthlyRate(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  const percent = Math.round(value * 100 * 100) / 100;
  return `${percent.toString().replace(".", ",")} % / mois`;
}

/** Date seule. Les valeurs de l'API sont des chaînes ISO. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : DATE.format(date);
}

/** Date et heure — pour tout ce qui doit être daté à la minute. */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : DATE_TIME.format(date);
}

/** Libellé « AAAA-MM » du graphique mensuel, rendu lisible : « août 2026 ». */
export function formatMonthLabel(label: string): string {
  const [year, month] = label.split("-").map(Number);
  if (!year || !month) return label;
  return new Intl.DateTimeFormat("fr-FR", {
    month: "short",
    year: "2-digit",
  }).format(new Date(year, month - 1, 1));
}

/** Nom affichable, avec repli sur l'e-mail quand l'état civil est incomplet. */
export function displayName(
  firstName: string | null,
  lastName: string | null,
  email: string
): string {
  const full = [firstName, lastName].filter(Boolean).join(" ").trim();
  return full || email;
}
