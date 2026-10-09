/**
 * Consentement aux cookies.
 *
 * Le choix est stocké dans un cookie de première partie et non dans
 * localStorage : il reste lisible côté serveur, ce qui permettra de décider du
 * chargement des scripts de mesure au rendu plutôt qu'après coup, et il
 * expire tout seul — un consentement n'est pas censé valoir indéfiniment.
 */

export const CONSENT_COOKIE = "kola_consent";

/** Six mois : durée usuellement retenue pour la validité d'un consentement. */
export const CONSENT_MAX_AGE_DAYS = 180;

export type ConsentChoice = "granted" | "denied";

export function readConsent(): ConsentChoice | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(
    new RegExp(`(?:^|;\\s*)${CONSENT_COOKIE}=(granted|denied)`)
  );
  return match ? (match[1] as ConsentChoice) : null;
}

export function writeConsent(choice: ConsentChoice) {
  if (typeof document === "undefined") return;
  const maxAge = CONSENT_MAX_AGE_DAYS * 24 * 60 * 60;
  // `SameSite=Lax` suffit : ce cookie n'est jamais lu depuis un contexte
  // tiers. `Secure` n'est posé qu'en HTTPS, sinon le cookie serait rejeté en
  // développement sur http://localhost.
  const secure = location.protocol === "https:" ? "; Secure" : "";
  document.cookie = `${CONSENT_COOKIE}=${choice}; path=/; max-age=${maxAge}; SameSite=Lax${secure}`;
  notify();
}

export function clearConsent() {
  if (typeof document === "undefined") return;
  document.cookie = `${CONSENT_COOKIE}=; path=/; max-age=0; SameSite=Lax`;
  notify();
}

/* ---------------------------------------------------------------------------
   Source externe abonnable
   ------------------------------------------------------------------------ */

/**
 * Le cookie est une source de vérité EXTÉRIEURE à React : rien ne prévient
 * l'application quand il change. On expose donc un abonnement, ce qui permet
 * de le lire avec `useSyncExternalStore` plutôt qu'en recopiant sa valeur dans
 * un état via un effet — recopie qui déclenche un rendu en cascade et que le
 * lint React signale à juste titre.
 */
const listeners = new Set<() => void>();

export function subscribeConsent(callback: () => void) {
  listeners.add(callback);
  return () => {
    listeners.delete(callback);
  };
}

function notify() {
  for (const listener of listeners) listener();
}

/** Instantané serveur : le cookie n'est pas connu au rendu. */
export function consentServerSnapshot(): ConsentChoice | null {
  return null;
}
