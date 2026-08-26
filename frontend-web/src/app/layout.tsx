import type { Metadata, Viewport } from "next";
import { Sora, Inter } from "next/font/google";
import "./globals.css";
import { SessionProvider } from "@/lib/session";

/**
 * `next/font` télécharge et auto-héberge les fichiers au build : aucune requête
 * vers un domaine tiers au runtime. Ce n'est pas qu'une question de
 * performance — les jetons de session vivent dans le `localStorage` de cette
 * origine, et l'unique atténuation retenue face au XSS est de ne charger
 * strictement aucune ressource extérieure (cf. le commentaire de `lib/api.ts`).
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

export const metadata: Metadata = {
  title: {
    default: "Kola — Mon compte",
    template: "%s · Kola",
  },
  description:
    "Votre wallet Kola dans le navigateur : soldes, transferts, coffres d'épargne, score de crédit et prêts.",
  /* Application privée : rien à indexer, et une page de compte qui remonterait
     dans un moteur de recherche serait une fuite de surface, pas une visite. */
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  themeColor: "#2e32c7",
  /* `maximum-scale` n'est PAS bridé : empêcher le zoom sur une interface où
     l'on lit des montants est une régression d'accessibilité. */
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fr" className={`${sora.variable} ${inter.variable}`}>
      <body className="min-h-dvh bg-canvas font-sans text-ink-900 antialiased">
        <SessionProvider>{children}</SessionProvider>
      </body>
    </html>
  );
}
