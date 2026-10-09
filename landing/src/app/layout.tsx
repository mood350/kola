import type { Metadata, Viewport } from "next";
import { Sora, Inter } from "next/font/google";
import "./globals.css";
import { SmoothScroll } from "@/components/providers/smooth-scroll";
import { RouteTransition } from "@/components/motion/route-transition";
import { SiteHeader } from "@/components/layout/site-header";
import { SiteFooter } from "@/components/layout/site-footer";
import { CookieConsent } from "@/components/providers/cookie-consent";
import { StickyMobileCta } from "@/components/layout/sticky-mobile-cta";
import { GoogleAnalytics } from "@/components/analytics/google-analytics";
import { JsonLd } from "@/components/ui/json-ld";
import { organizationSchema, webSiteSchema } from "@/lib/schema";
import { SITE_URL } from "@/lib/business";

/**
 * next/font télécharge et auto-héberge les fichiers au build : aucune requête
 * vers Google Fonts au runtime, et `display: swap` combiné au fallback ajusté
 * évite le décalage de mise en page au chargement (CLS).
 */
const sora = Sora({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-sora",
  display: "swap",
});

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

/**
 * Métadonnées communes à toutes les routes.
 *
 * `metadataBase` est ce qui autorise les chemins relatifs partout ailleurs :
 * une page peut déclarer `canonical: "/contact"`, Next le résout en URL
 * absolue. Sans lui, les réseaux sociaux reçoivent des chemins relatifs qu'ils
 * ne savent pas résoudre, et l'aperçu de partage tombe.
 *
 * `title.template` fait que chaque page n'a plus qu'à déclarer SON titre : le
 * suffixe de marque est ajouté ici, une fois. C'est ce qui rend praticable la
 * règle « un titre unique par page » — un titre écrit en entier sur chaque page
 * finit toujours par être copié-collé d'une route à l'autre.
 *
 * L'image de partage n'est PAS déclarée ici : `app/opengraph-image.tsx` et
 * `app/twitter-image.tsx` sont détectés automatiquement par Next, qui insère
 * les balises `og:image` et `twitter:image` — dimensions et type inclus, sans
 * risque de les décrire de travers à la main.
 */
export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "Kola — Votre argent, votre score, vos possibilités",
    template: "%s · Kola",
  },
  description:
    "Le wallet mobile money qui transforme vos habitudes financières en score de confiance, et votre score en accès au crédit. Conçu pour l'Afrique de l'Ouest.",
  keywords: [
    "mobile money",
    "wallet",
    "Afrique de l'Ouest",
    "score de crédit",
    "microcrédit",
    "épargne",
    "XOF",
  ],
  openGraph: {
    type: "website",
    locale: "fr_FR",
    url: SITE_URL,
    siteName: "Kola",
    title: "Kola — Votre argent, votre score, vos possibilités",
    description:
      "Le wallet mobile money qui transforme vos habitudes financières en score de confiance, et votre score en accès au crédit.",
  },
  twitter: {
    card: "summary_large_image",
    title: "Kola — Votre argent, votre score, vos possibilités",
    description:
      "Le wallet mobile money qui transforme vos habitudes financières en accès au crédit.",
  },
  robots: { index: true, follow: true },
  alternates: { canonical: "/" },
};

/**
 * `viewport` est un export distinct de `metadata` dans l'App Router. La balise
 * meta viewport elle-même est ajoutée automatiquement par Next.
 */
export const viewport: Viewport = {
  /* Teinte de la barre système sur mobile : la couleur du fond de page, pas
     celle de la marque — sinon un bandeau indigo vif surmonte une page claire. */
  themeColor: "#F5F3FD",
  colorScheme: "light",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr" className={`${sora.variable} ${inter.variable}`}>
      <body className="antialiased">
        {/* Données structurées de l'entité, posées une fois pour tout le site.
            `WebSite` renvoie à `Organization` par son identifiant plutôt que de
            recopier ses champs : les moteurs reconstituent un graphe, et deux
            descriptions concurrentes de la même entité les font arbitrer entre
            elles — arbitrage qu'on perd toujours. */}
        <JsonLd data={organizationSchema()} />
        <JsonLd data={webSiteSchema()} />

        <a
          href="#contenu"
          className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-100 focus:rounded-full focus:bg-kola-600 focus:px-5 focus:py-3 focus:text-sm focus:font-medium focus:text-white"
        >
          Aller au contenu principal
        </a>
        {/* En-tête et pied de page vivent dans le layout : ils sont communs à
            toutes les routes et, laissés dans chaque page, ils seraient
            démontés puis remontés à chaque navigation — la barre clignoterait. */}
        <SmoothScroll>
          {/* Tout ce qui dépend du consentement vit à l'intérieur de
              `CookieConsent` : c'est lui qui expose le choix par contexte.
              `GoogleAnalytics` ne charge rien tant qu'il vaut « granted », et
              `StickyMobileCta` s'efface tant que le bandeau est ouvert, pour ne
              pas recouvrir le bouton « Refuser ». */}
          <CookieConsent>
            <SiteHeader />
            <RouteTransition>{children}</RouteTransition>
            <SiteFooter />
            <StickyMobileCta />
            <GoogleAnalytics />
          </CookieConsent>
        </SmoothScroll>
      </body>
    </html>
  );
}
