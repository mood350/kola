"use client";

import {
  createContext,
  useCallback,
  useContext,
  useRef,
  useSyncExternalStore,
} from "react";
import Link from "next/link";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import {
  readConsent,
  writeConsent,
  clearConsent,
  subscribeConsent,
  consentServerSnapshot,
  type ConsentChoice,
} from "@/lib/consent";

/**
 * Détecte la fin de l'hydratation sans recopier d'état dans un effet.
 * `getServerSnapshot` renvoie false au rendu serveur et pendant l'hydratation ;
 * React re-rend ensuite avec `getSnapshot`, qui renvoie true.
 */
const noopSubscribe = () => () => {};

type ConsentContextValue = {
  choice: ConsentChoice | null;
  /** Rouvre la bannière pour revenir sur un choix déjà fait. */
  reopen: () => void;
};

const ConsentContext = createContext<ConsentContextValue>({
  choice: null,
  reopen: () => {},
});

/** Permet à n'importe quel composant de connaître le choix, ou de le rouvrir. */
export function useConsent() {
  return useContext(ConsentContext);
}

/**
 * Bandeau de consentement aux cookies.
 *
 * CE QU'IL GOUVERNE RÉELLEMENT
 *
 * Ce bandeau n'est pas une formalité décorative : il commande effectivement le
 * chargement de la mesure d'audience. `components/analytics/google-analytics.tsx`
 * lit le choix exposé ici par contexte et ne rend RIEN tant qu'il ne vaut pas
 * « granted » — pas de balise, pas de requête vers Google, pas de cookie `_ga`.
 * Le consentement précède le dépôt, jamais l'inverse.
 *
 * Deux conséquences à ne pas défaire : le refus doit rester aussi accessible
 * que l'acceptation (même taille, même poids), et tout nouveau script tiers
 * doit passer par ce même point de bascule ET figurer sur `/cookies` avant
 * d'être activé.
 *
 * Le refus est aussi accessible que l'acceptation, au même niveau visuel : un
 * bandeau qui ne propose que « J'accepte » ne recueille pas un consentement
 * valable.
 *
 * RENDU
 *
 * Rien n'est rendu tant que l'hydratation n'est pas terminée. Le serveur ne
 * connaît pas le cookie : afficher le bandeau puis le retirer côté client
 * produirait un clignotement, et le lire pendant le rendu serveur provoquerait
 * une erreur d'hydratation — le piège classique de ce type de composant.
 */
export function CookieConsent({ children }: { children: React.ReactNode }) {
  const banner = useRef<HTMLDivElement>(null);

  const hydrated = useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false
  );

  /* Le cookie est lu directement à chaque rendu, jamais recopié dans un état :
     écrire ou effacer le cookie notifie les abonnés, ce qui suffit à
     rafraîchir l'affichage. Une seule source de vérité, donc aucun risque de
     désynchronisation entre le cookie réel et ce que montre l'interface. */
  const choice = useSyncExternalStore(
    subscribeConsent,
    readConsent,
    consentServerSnapshot
  );

  const decide = useCallback((next: ConsentChoice) => {
    writeConsent(next);
  }, []);

  const reopen = useCallback(() => {
    clearConsent();
  }, []);

  // Rien n'est rendu avant la fin de l'hydratation : le serveur ignore le
  // cookie, afficher le bandeau puis le retirer produirait un clignotement.
  const visible = hydrated && choice === null;

  useGSAP(
    () => {
      if (!visible || prefersReducedMotion()) return;
      gsap.fromTo(
        banner.current,
        { autoAlpha: 0, y: 24 },
        { autoAlpha: 1, y: 0, duration: 0.45, ease: "power3.out", delay: 0.4 }
      );
    },
    { dependencies: [visible] }
  );

  return (
    <ConsentContext.Provider value={{ choice, reopen }}>
      {children}

      {visible ? (
        <div
          ref={banner}
          role="dialog"
          aria-modal="false"
          aria-labelledby="consent-titre"
          className="fixed inset-x-3 bottom-3 z-60 sm:inset-x-auto sm:right-5 sm:bottom-5 sm:max-w-md"
        >
          <div className="rounded-card bg-surface p-6 shadow-lifted hairline">
            <h2 id="consent-titre" className="text-base font-semibold">
              Cookies
            </h2>
            <p className="mt-2 text-[0.875rem] leading-relaxed text-ink-600">
              Ce site n&apos;utilise aucun cookie publicitaire. Nous aimerions
              mesurer l&apos;audience pour savoir quelles pages servent
              réellement — rien ne sera chargé sans votre accord. Si vous
              refusez, aucune requête n&apos;est envoyée : le site fonctionne
              identiquement dans les deux cas.
            </p>

            <div className="mt-5 flex flex-col gap-2 sm:flex-row">
              <button
                type="button"
                onClick={() => decide("granted")}
                className="inline-flex h-11 flex-1 cursor-pointer items-center justify-center rounded-full bg-kola-600 px-5 text-[0.875rem] font-medium text-white transition-colors duration-200 hover:bg-kola-700"
              >
                Accepter
              </button>
              {/* Même taille, même poids que « Accepter » : un refus relégué
                  en lien discret ne recueille pas un consentement libre. */}
              <button
                type="button"
                onClick={() => decide("denied")}
                className="inline-flex h-11 flex-1 cursor-pointer items-center justify-center rounded-full bg-kola-100 px-5 text-[0.875rem] font-medium text-kola-700 transition-colors duration-200 hover:bg-kola-200"
              >
                Refuser
              </button>
            </div>

            <Link
              href="/cookies"
              className="mt-4 inline-block text-[0.8125rem] text-ink-500 underline underline-offset-2 transition-colors hover:text-kola-600"
            >
              Détail des cookies utilisés
            </Link>
          </div>
        </div>
      ) : null}
    </ConsentContext.Provider>
  );
}
