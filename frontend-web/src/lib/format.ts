/**
 * Formatage des valeurs affichées.
 *
 * Centralisé pour une raison de fond : dans une application financière, deux
 * écrans qui formatent le même montant différemment font douter de la donnée
 * elle-même. Quelqu'un qui lit « 25000 » ici et « 25 000 F CFA » là se demande
 * s'il regarde la même chose.
 *
 * Les instances `Intl` sont construites UNE fois au chargement du module :
 * créer un formateur coûte cher, et une liste de deux cents transactions en
 * appellerait autant.
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

/**
 * Montant signé d'un mouvement, du point de vue du wallet.
 *
 * Le signe n'est pas décoratif : c'est la seule chose qui distingue, dans une
 * liste, un transfert reçu d'un transfert envoyé du même montant.
 */
export function formatSignedAmount(value: number, credit: boolean): string {
  return `${credit ? "+" : "−"} ${XOF.format(Math.abs(value))}`;
}

/** Nombre entier, séparateurs de milliers compris. */
export function formatCount(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  return INTEGER.format(value);
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

/**
 * Ancienneté lisible (« il y a 3 h »), avec repli sur la date au-delà d'une
 * semaine — passé ce délai, « il y a 23 jours » demande un calcul mental que la
 * date évite.
 */
export function formatRelative(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";

  const seconds = Math.round((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return "à l'instant";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `il y a ${minutes} min`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `il y a ${hours} h`;
  const days = Math.round(hours / 24);
  if (days === 1) return "hier";
  if (days < 7) return `il y a ${days} jours`;
  return DATE.format(date);
}

/**
 * Nombre de jours restants avant une échéance, au format « dans 12 jours ».
 * Utilisé par les coffres (date de déblocage) et les prêts (date d'échéance).
 */
export function formatCountdown(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";

  const days = Math.ceil((date.getTime() - Date.now()) / 86_400_000);
  if (days < 0) return `échu depuis ${Math.abs(days)} jour${Math.abs(days) > 1 ? "s" : ""}`;
  if (days === 0) return "aujourd'hui";
  if (days === 1) return "demain";
  return `dans ${days} jours`;
}

/**
 * Vrai si l'échéance est atteinte ou dépassée.
 *
 * Regroupée ici avec les autres fonctions de date, et pas écrite dans les
 * composants : lire l'heure courante est une impureté, et la laisser au milieu
 * d'un rendu React est précisément ce que le compilateur signale. Concentrée
 * dans ce module, elle reste testable et n'apparaît qu'à un seul endroit.
 *
 * La précision est celle du jour : une date de déblocage est une `LocalDate`
 * côté backend (pas un instant), et la comparer à la milliseconde près
 * donnerait un « bientôt disponible » qui bascule à une heure arbitraire.
 */
export function isDueOrPast(value: string | null | undefined): boolean {
  if (!value) return true;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return true;
  return date.getTime() <= Date.now();
}

/** Date au format attendu par un `<input type="date">` (AAAA-MM-JJ). */
export function toDateInputValue(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Nom affichable, avec repli sur l'e-mail quand l'état civil est incomplet. */
export function displayName(
  firstName: string | null | undefined,
  lastName: string | null | undefined,
  fallback: string | null | undefined
): string {
  const full = [firstName, lastName].filter(Boolean).join(" ").trim();
  return full || fallback || "—";
}

/** Initiales, pour l'avatar de repli. */
export function initials(firstName: string, lastName: string): string {
  const first = firstName.trim().charAt(0);
  const last = lastName.trim().charAt(0);
  return `${first}${last}`.toUpperCase() || "?";
}

/**
 * Progression d'un coffre vers son objectif, bornée à 100 %.
 *
 * Sans borne, un coffre suralimenté afficherait une barre débordant de son
 * conteneur — et surtout un « 137 % » qui se lit comme une anomalie de calcul.
 */
export function progressRatio(current: number, target: number | null): number | null {
  if (!target || target <= 0) return null;
  return Math.min(1, current / target);
}
