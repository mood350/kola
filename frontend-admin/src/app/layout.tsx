import type { Metadata } from "next";
import { Outfit } from "next/font/google";
import "./globals.css";
import { SidebarProvider } from "@/context/SidebarContext";
import { ThemeProvider } from "@/context/ThemeContext";
import { SessionProvider } from "@/lib/session";

/**
 * Outfit — la typographie de TailAdmin, auto-hébergée par `next/font`.
 *
 * Le template la charge depuis Google Fonts ; ici elle est téléchargée au build
 * et servie depuis notre propre domaine. Sur une console interne ce n'est pas
 * qu'une question de performance : chaque chargement de police distante signale
 * à un tiers qui consulte le back-office, et depuis quelle adresse.
 */
const outfit = Outfit({
  subsets: ["latin"],
  variable: "--font-outfit-loaded",
  display: "swap",
});

/**
 * `noindex, nofollow` — non négociable.
 *
 * La console affiche des identités clientes, des soldes et des alertes de
 * conformité. Elle est protégée par authentification, mais l'écran de connexion
 * ne l'est pas : indexé, il révélerait publiquement l'existence et l'adresse du
 * back-office, ce qui suffit à en faire une cible.
 */
export const metadata: Metadata = {
  title: {
    default: "Console Kola",
    template: "%s · Console Kola",
  },
  description: "Console d'administration interne Kola.",
  robots: { index: false, follow: false, nocache: true },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    /* `suppressHydrationWarning` : `ThemeProvider` pose la classe `dark` sur
       <html> après lecture de localStorage, que le serveur ne connaît pas.
       C'est le seul écart de rendu attendu entre serveur et client. */
    <html lang="fr" suppressHydrationWarning>
      <body className={`${outfit.className} dark:bg-gray-900`}>
        <a
          href="#contenu"
          className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-999999 focus:rounded-lg focus:bg-brand-500 focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-white"
        >
          Aller au contenu principal
        </a>
        <ThemeProvider>
          <SidebarProvider>
            {/* La session enveloppe le tout : l'en-tête y lit l'utilisateur
                connecté, et les écrans y lisent le jeton. */}
            <SessionProvider>{children}</SessionProvider>
          </SidebarProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
