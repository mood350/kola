import { SmoothScroll } from "@/components/providers/smooth-scroll";
import { RouteTransition } from "@/components/motion/route-transition";
import { SiteHeader } from "@/components/layout/site-header";
import { SiteFooter } from "@/components/layout/site-footer";
import { CookieConsent } from "@/components/providers/cookie-consent";
import { StickyMobileCta } from "@/components/layout/sticky-mobile-cta";
import { GoogleAnalytics } from "@/components/analytics/google-analytics";

/**
 * Coque du site public.
 *
 * ═══ POURQUOI CETTE COQUE NE MONTE PAS À LA RACINE ═══
 *
 * Parce qu'elle ne concerne que les pages ouvertes. En-tête marketing, pied de
 * page à trente liens, défilement fluide piloté par Lenis, bandeau de
 * consentement : rien de tout cela n'a sa place au-dessus d'un tableau de bord,
 * qui porte sa propre navigation.
 *
 * ═══ CE QUE LA FUSION CHANGE POUR LES MESURES ═══
 *
 * `GoogleAnalytics` reste ici, et NULLE PART AILLEURS. Ce n'est pas un détail
 * d'organisation : les jetons de session vivent dans le `localStorage` de cette
 * origine, désormais partagée par le site public et l'espace client. Un script
 * tiers chargé sur une page privée pourrait les lire. En le confinant au groupe
 * marketing, aucune page authentifiée ne charge de script tiers — ce qui était
 * déjà l'engagement de l'espace client avant la fusion.
 *
 * IL RESTE UN RISQUE RÉSIDUEL, ET IL EST RÉEL : même confiné, ce script
 * s'exécute sur la même origine. Un compromis de Google Analytics resterait
 * hors des pages privées, mais partagerait leur origine. Le fermer vraiment
 * suppose de sortir les jetons du `localStorage` — cookie `HttpOnly`, donc
 * travail côté backend.
 *
 * Le consentement continue de commander le chargement : `GoogleAnalytics` ne
 * demande rien tant que `useConsent()` ne vaut pas « granted ».
 */
export default function MarketingLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SmoothScroll>
      {/* Tout ce qui dépend du consentement vit à l'intérieur de
          `CookieConsent` : c'est lui qui expose le choix par contexte.
          `StickyMobileCta` s'efface tant que le bandeau est ouvert, pour ne pas
          recouvrir le bouton « Refuser ». */}
      <CookieConsent>
        {/* Le site public a son propre fond, gris bleuté froid ; l'espace
            client garde la toile lavande du corps de page. */}
        <div className="site-public min-h-dvh bg-mist">
          <SiteHeader />
          <RouteTransition>{children}</RouteTransition>
          <SiteFooter />
        </div>
        <StickyMobileCta />
        <GoogleAnalytics />
      </CookieConsent>
    </SmoothScroll>
  );
}
