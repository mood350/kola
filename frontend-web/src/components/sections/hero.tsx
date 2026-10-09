"use client";

import { useRef } from "react";
import {
  gsap,
  useGSAP,
  SplitText,
  MOTION,
  prefersReducedMotion,
} from "@/lib/gsap";
import { Container } from "@/components/ui/section";
import { Button, ArrowRight } from "@/components/ui/button";
import { HeroPreview } from "@/components/sections/hero-preview";
import { COVERAGE } from "@/lib/business";

/** Garanties sous les boutons — des faits vérifiables, pas des promesses. */
const TRUST = [
  "Aucun dossier à monter",
  "Compte ouvert en quelques minutes",
  "Aucun frais d'ouverture",
];

export function Hero() {
  const root = useRef<HTMLElement>(null);
  const headline = useRef<HTMLHeadingElement>(null);

  useGSAP(
    () => {
      if (prefersReducedMotion()) {
        gsap.set("[data-animate]", { autoAlpha: 1, y: 0 });
        return;
      }

      const tl = gsap.timeline({
        defaults: { ease: MOTION.easeExpo, duration: 0.9 },
      });

      /* Titre découpé en lignes, SANS masque : le mot en dégradé porte une
         marge basse pour ses jambages, qu'un `overflow: hidden` par ligne
         rognerait. On anime opacité et translation, sans rognage. */
      let split: SplitText | null = null;
      if (headline.current) {
        split = new SplitText(headline.current, { type: "lines" });
        gsap.set(headline.current, { autoAlpha: 1 });
        tl.from(split.lines, { yPercent: 40, opacity: 0, stagger: 0.09 }, 0.1);
      }

      tl.to("[data-hero-badge]", { autoAlpha: 1, y: 0, duration: 0.6 }, 0)
        .to("[data-hero-lead]", { autoAlpha: 1, y: 0, duration: 0.7 }, "-=0.5")
        .to("[data-hero-actions]", { autoAlpha: 1, y: 0, duration: 0.7 }, "-=0.55")
        .to(
          "[data-hero-trust] > *",
          { autoAlpha: 1, y: 0, duration: 0.5, stagger: 0.06 },
          "-=0.5"
        )
        .to(
          "[data-hero-preview]",
          { autoAlpha: 1, y: 0, duration: 1.1 },
          "-=0.6"
        );

      return () => {
        split?.revert();
      };
    },
    { scope: root }
  );

  return (
    /* Espacements resserrés sous `sm` : le bouton « Ouvrir un compte » doit
       tenir au-dessus de la ligne de flottaison sur un écran de 667 px de
       haut, barre d'adresse déduite. */
    <section ref={root} className="relative overflow-hidden pt-28 sm:pt-36 lg:pt-40">
      <div aria-hidden="true" className="dot-grid pointer-events-none absolute inset-0" />
      {/* Deux diffusions — indigo au centre, ocre décalée — reprennent les deux
          extrémités du dégradé du titre : le fond annonce déjà le mot-clé. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 -top-48 h-[44rem] bg-[radial-gradient(ellipse_50%_45%_at_50%_0%,rgb(123_127_236/0.35)_0%,transparent_70%)]"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute top-24 -right-40 h-[26rem] w-[26rem] rounded-full bg-[radial-gradient(circle,rgb(233_162_59/0.16)_0%,transparent_65%)]"
      />

      <Container className="relative">
        <div className="relative mx-auto max-w-4xl text-center">
          <span
            data-hero-badge
            data-animate
            className="inline-flex translate-y-3 items-center gap-2.5 rounded-full border border-hairline bg-surface/80 py-1 pr-4 pl-1 text-[0.8125rem] font-medium text-ink-700 shadow-card backdrop-blur"
          >
            <span className="rounded-full bg-kola-600 px-2.5 py-0.5 text-[0.75rem] font-semibold text-white">
              XOF
            </span>
            Mobile money · Afrique de l&apos;Ouest
          </span>

          <h1
            ref={headline}
            data-animate
            className="text-hero font-headline mt-6 font-semibold text-ink-950 sm:mt-8"
          >
            Votre argent construit votre{" "}
            <span className="text-gradient-accent">score</span>. Il ouvre
            l&apos;accès au crédit.
          </h1>

          <p
            data-hero-lead
            data-animate
            className="mx-auto mt-6 max-w-xl translate-y-4 text-[1.0625rem] leading-relaxed text-ink-500 sm:mt-7 sm:text-lg"
          >
            Payez, épargnez, transférez. Kola note votre fiabilité de 0 à 100
            et vous prête en conséquence.
          </p>

          <div
            data-hero-actions
            data-animate
            className="mt-8 flex translate-y-4 flex-col items-center justify-center gap-3 sm:mt-10 sm:flex-row"
          >
            <Button href="/inscription" size="lg" className="w-full sm:w-auto">
              Ouvrir un compte
              <ArrowRight />
            </Button>
            <Button
              href="#score"
              variant="secondary"
              size="lg"
              className="w-full sm:w-auto"
            >
              Comprendre le score
            </Button>
          </div>

          <ul
            data-hero-trust
            className="mt-7 flex flex-wrap items-center justify-center gap-x-6 gap-y-2.5"
          >
            {TRUST.map((item) => (
              <li
                key={item}
                data-animate
                className="flex translate-y-3 items-center gap-2 text-[0.875rem] text-ink-500"
              >
                <CheckDot />
                {item}
              </li>
            ))}
          </ul>
        </div>

        <div data-hero-preview data-animate className="mt-14 translate-y-10 sm:mt-18">
          <HeroPreview />
        </div>
      </Container>

      <CoverageMarquee />
    </section>
  );
}

function CheckDot() {
  return (
    <span
      aria-hidden="true"
      className="flex h-4.5 w-4.5 items-center justify-center rounded-full bg-kola-600 text-white"
    >
      <svg viewBox="0 0 12 12" fill="none" className="h-2.5 w-2.5">
        <path
          d="M2.5 6.2 4.8 8.5 9.5 3.8"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}

/**
 * Bandeau des pays desservis.
 *
 * L'emplacement que les sites SaaS réservent aux logos clients. Kola n'a pas
 * de logos à afficher sans mentir sur un partenariat — il affiche donc ce qui
 * est vrai et utile au visiteur : où le service fonctionne. Même source que le
 * pied de page et que `areaServed` des données structurées.
 *
 * La liste est rendue deux fois pour le défilement continu ; la copie est
 * masquée aux lecteurs d'écran, qui n'entendent chaque pays qu'une fois.
 */
function CoverageMarquee() {
  return (
    <div className="relative mt-16 border-y border-hairline bg-surface/60 py-6 backdrop-blur sm:mt-20">
      <p className="text-center text-[0.8125rem] font-medium text-ink-500">
        Disponible dans les huit États de l&apos;UEMOA
      </p>
      <div className="fade-x mt-4 overflow-hidden">
        <div className="marquee flex w-max">
          {[0, 1].map((copy) => (
            <ul
              key={copy}
              aria-hidden={copy === 1 ? true : undefined}
              className="flex shrink-0 items-center"
            >
              {COVERAGE.map((country) => (
                <li
                  key={country.code}
                  className="flex items-center gap-3 px-7 text-lg font-semibold whitespace-nowrap text-ink-800 sm:px-10"
                >
                  <span className="font-headline rounded-md border border-hairline bg-mist px-1.5 py-0.5 text-[0.6875rem] font-semibold tracking-wider text-ink-500">
                    {country.code}
                  </span>
                  <span className="font-headline tracking-tight">{country.name}</span>
                </li>
              ))}
            </ul>
          ))}
        </div>
      </div>
    </div>
  );
}
