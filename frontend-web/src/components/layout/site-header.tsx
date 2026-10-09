"use client";

import { useState } from "react";
import Link from "next/link";
import { NAV_LINKS } from "@/lib/content";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";
import { KolaLogo } from "@/components/ui/kola-logo";

export function SiteHeader() {
  const [open, setOpen] = useState(false);


  return (
    /* La barre est une pilule détachée du bord plutôt qu'un bandeau collé en
       haut : elle flotte au-dessus du contenu, ce qui laisse le fond quadrillé
       respirer et signale immédiatement un produit, pas un site vitrine. */
    <header className="fixed inset-x-0 top-0 z-50 px-3 pt-3 sm:px-5 sm:pt-5">
      {/* Pilule présente dès le chargement — la faire apparaître au scroll
          donnait une navigation qui semblait absente. Blanc translucide et
          flou d'arrière-plan : le contenu qui défile dessous reste perceptible
          sans jamais gêner la lecture des liens. */}
      <div className="mx-auto flex h-15 w-full max-w-6xl items-center justify-between rounded-full border border-white/70 bg-white/75 pr-2 pl-4 shadow-card backdrop-blur-xl backdrop-saturate-150 sm:pl-5">
        <Link
          href="/"
          className="flex items-center gap-2.5 rounded-lg"
          aria-label="Kola, retour à l'accueil"
        >
          <KolaLogo className="h-7 w-7" />
          <span className="font-headline text-xl font-semibold tracking-tight text-ink-950">
            Kola
          </span>
        </Link>

        <nav aria-label="Navigation principale" className="hidden lg:block">
          <ul className="flex items-center gap-1">
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  className="inline-flex h-10 items-center rounded-full px-4 text-sm font-medium text-ink-600 transition-colors duration-200 hover:bg-mist hover:text-ink-950"
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        {/* Depuis la fusion, l'espace client est une route de ce site : le
            visiteur qui a déjà un compte doit pouvoir y entrer d'ici, sinon il
            cherche une adresse qu'il ne connaît pas. « Se connecter » reste
            discret — l'appel à l'action de la page vise l'inscription. */}
        <div className="hidden items-center gap-2 lg:flex">
          <Button href="/connexion" variant="ghost">
            Se connecter
          </Button>
          <Button href="/inscription">Ouvrir un compte</Button>
        </div>

        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          aria-controls="menu-mobile"
          aria-label={open ? "Fermer le menu" : "Ouvrir le menu"}
          className="inline-flex h-11 w-11 cursor-pointer items-center justify-center rounded-full text-ink-800 transition-colors hover:bg-ink-100 lg:hidden"
        >
          <span className="relative block h-3 w-5" aria-hidden="true">
            <span
              className={cn(
                "absolute left-0 block h-0.5 w-5 rounded-full bg-current transition-transform duration-300 ease-[var(--ease-editorial)]",
                open ? "top-1.5 rotate-45" : "top-0"
              )}
            />
            <span
              className={cn(
                "absolute left-0 block h-0.5 w-5 rounded-full bg-current transition-transform duration-300 ease-[var(--ease-editorial)]",
                open ? "top-1.5 -rotate-45" : "top-3"
              )}
            />
          </span>
        </button>
      </div>

      {/* Panneau mobile. `grid-rows` animé plutôt que `height: auto` :
          la hauteur reste calculée par le contenu tout en restant animable. */}
      <div
        id="menu-mobile"
        className={cn(
          "mx-auto mt-2 grid w-full max-w-6xl overflow-hidden rounded-3xl border border-hairline bg-surface shadow-card transition-[grid-template-rows] duration-300 ease-[var(--ease-editorial)] lg:hidden",
          open ? "grid-rows-[1fr]" : "grid-rows-[0fr]"
        )}
      >
        <div className="min-h-0">
          <nav aria-label="Navigation mobile" className="px-5 pt-3 pb-6">
            <ul className="flex flex-col">
              {NAV_LINKS.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    onClick={() => setOpen(false)}
                    className="flex h-13 items-center border-b border-hairline text-base font-medium text-ink-700"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
            <Button href="/connexion" variant="secondary" size="lg" className="mt-6 w-full">
            Se connecter
          </Button>
          <Button href="/inscription" size="lg" className="mt-3 w-full">
              Ouvrir un compte
            </Button>
          </nav>
        </div>
      </div>
    </header>
  );
}
