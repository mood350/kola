import type { Metadata, Viewport } from "next";
import { Sora, Inter, Bricolage_Grotesque } from "next/font/google";
import "./globals.css";
import { JsonLd } from "@/components/ui/json-ld";
import { organizationSchema, webSiteSchema } from "@/lib/schema";
import { SITE_URL } from "@/lib/business";

/**
 * Racine commune au site public et à l'espace client.
 *
 * ═══ CE QUI VIT ICI, ET CE QUI N'Y VIT PLUS ═══
 *
 * Une seule application sert désormais deux publics : les pages marketing,
 * ouvertes et indexées, et l'espace client, authentifié et volontairement
 * absent des moteurs. Ce layout ne garde donc que ce qui vaut pour LES DEUX —
 * la langue, les polices, les données structurées de l'entité, le lien
 * d'évitement.
 *
 * L'en-tête, le pied de page, le défilement fluide et le bandeau de
 * consentement sont descendus dans `(marketing)/layout.tsx` : appliqués ici,
 * ils encadreraient aussi le tableau de bord, qui a sa propre coque. Le
 * fournisseur de session est descendu dans `(private)/layout.tsx` : laissé
 * ici, la page d'accueil publique interrogerait l'API à chaque visite d'un
 * visiteur porteur d'un jeton, pour n'en rien faire.
 *
 * Les polices restent ici : `next/font` les auto-héberge au build, et les
 * déclarer deux fois produirait deux préchargements du même fichier.
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

/* Titres du site public. Une grotesque à caractère, réservée aux grands titres
   marketing : dans l'espace client, un solde ou un intitulé d'écran reste en
   Sora, plus neutre. Chargée ici avec les autres pour ne pas dédoubler le
   préchargement, mais seul le groupe `(marketing)` l'utilise. */
const bricolage = Bricolage_Grotesque({
  subsets: ["latin"],
  weight: ["500", "600", "700"],
  variable: "--font-bricolage",
  display: "swap",
});

/**
 * Métadonnées par défaut : celles du SITE PUBLIC.
 *
 * C'est lui qui est indexé, partagé et cité. L'espace client les remplace pour
 * son propre périmètre (`(private)/layout.tsx` impose `noindex`) — l'exception
 * est déclarée là où elle s'applique, pas ici.
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
      "Le wallet mobile money qui transforme vos habitudes financières en score de confiance, et votre score en accès au crédit.",
  },
  alternates: { canonical: "/" },
};

export const viewport: Viewport = {
  themeColor: "#2e32c7",
  colorScheme: "light",
  /* `maximum-scale` n'est PAS bridé : empêcher le zoom sur des pages où l'on
     lit des montants est une régression d'accessibilité. */
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fr" className={`${sora.variable} ${inter.variable} ${bricolage.variable}`}>
      <body className="min-h-dvh bg-canvas font-sans text-ink-900 antialiased">
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

        {children}
      </body>
    </html>
  );
}
