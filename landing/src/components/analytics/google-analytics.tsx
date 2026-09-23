"use client";

import { useEffect, useRef } from "react";
import Script from "next/script";
import { usePathname } from "next/navigation";
import { useConsent } from "@/components/providers/cookie-consent";

/**
 * Google Analytics 4 — chargé UNIQUEMENT après consentement explicite.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 *   L'ORDRE EST LE POINT ESSENTIEL : LE CONSENTEMENT PRÉCÈDE LE CHARGEMENT.
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Le composant ne rend RIEN tant que `choice !== "granted"`. Aucun octet n'est
 * demandé à googletagmanager.com, aucun cookie `_ga` n'est déposé, aucune
 * requête n'est émise — pas même une requête « anonyme ». C'est ce que le
 * bandeau promet au visiteur, et ce que le RGPD exige : un traceur chargé puis
 * neutralisé après coup a déjà transmis l'adresse IP et l'empreinte du
 * navigateur. Le mode « consent mode » de Google, qui charge le script et lui
 * demande de se retenir, ne satisfait pas cette exigence.
 *
 * REFUS ET RÉTRACTATION : si le visiteur refuse, `choice` vaut `"denied"` et le
 * composant reste vide. S'il revient sur son accord depuis `/cookies`, le
 * cookie de consentement est effacé, ce composant est démonté et le script
 * cesse d'être injecté. Les cookies `_ga` déjà posés survivent au démontage —
 * la page `/cookies` explique comment les supprimer, aucun code côté client ne
 * peut le faire à la place du navigateur.
 *
 * POURQUOI PAS `@next/third-parties/google` : ce paquet charge le tag dès le
 * montage, sans point d'entrée pour conditionner le chargement au
 * consentement. Le contournement consisterait à monter le composant
 * conditionnellement — soit exactement ce que fait ce fichier, en trente lignes
 * et sans dépendance supplémentaire déclarée expérimentale par ses auteurs.
 *
 * ABSENCE D'IDENTIFIANT : sans `NEXT_PUBLIC_GA_ID`, rien n'est rendu. Le site
 * fonctionne à l'identique en développement et sur les environnements de
 * préversion, sans polluer les statistiques de production avec le trafic de
 * l'équipe.
 */

const GA_ID = process.env.NEXT_PUBLIC_GA_ID;

declare global {
  interface Window {
    dataLayer: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

export function GoogleAnalytics() {
  const { choice } = useConsent();
  const granted = Boolean(GA_ID) && choice === "granted";

  return granted ? <Tag id={GA_ID!} /> : null;
}

/**
 * Le tag proprement dit, isolé dans un composant enfant pour que les hooks de
 * suivi de navigation ne soient montés que lorsqu'il y a effectivement quelque
 * chose à suivre.
 */
function Tag({ id }: { id: string }) {
  const pathname = usePathname();

  /**
   * Vues de page sur navigation cliente.
   *
   * L'App Router ne recharge pas le document d'une route à l'autre : le
   * `page_view` initial envoyé par `gtag('config', …)` serait le seul de toute
   * la session, et Analytics n'enregistrerait qu'une page par visiteur.
   *
   * La première valeur de `pathname` est ignorée — elle correspond à la page
   * déjà comptée par `config`. Sans cette garde, chaque arrivée sur le site
   * produit deux vues pour la même page, et le taux de rebond devient
   * inexploitable.
   */
  const firstRun = useRef(true);

  useEffect(() => {
    if (firstRun.current) {
      firstRun.current = false;
      return;
    }
    window.gtag?.("event", "page_view", {
      page_path: pathname,
      page_location: window.location.href,
      page_title: document.title,
    });
  }, [pathname]);

  return (
    <>
      {/* `afterInteractive` : la mesure d'audience ne doit jamais entrer en
          concurrence avec le rendu du contenu. `beforeInteractive` retarderait
          l'affichage pour un script dont aucun pixel ne dépend. */}
      <Script
        id="ga-src"
        strategy="afterInteractive"
        src={`https://www.googletagmanager.com/gtag/js?id=${id}`}
      />
      <Script id="ga-init" strategy="afterInteractive">
        {`
          window.dataLayer = window.dataLayer || [];
          function gtag(){dataLayer.push(arguments);}
          window.gtag = gtag;
          gtag('js', new Date());
          gtag('config', '${id}', {
            anonymize_ip: true,
            allow_google_signals: false,
            allow_ad_personalization_signals: false
          });
        `}
      </Script>
    </>
  );
}
