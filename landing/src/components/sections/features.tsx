"use client";

import { useRef } from "react";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { FEATURES, type Feature } from "@/lib/content";
import { FeatureIcon } from "@/components/ui/feature-icon";
import { cn } from "@/lib/cn";

export function Features() {
  const root = useRef<HTMLDivElement>(null);

  /**
   * Micro-interaction au survol des cartes, pilotée par GSAP.
   *
   * Deux raisons de ne pas la faire en CSS ici :
   *  - `quickTo` réutilise le même tween d'une frame à l'autre au lieu d'en
   *    recréer un à chaque `mouseenter`, ce qui reste fluide même en balayant
   *    rapidement la grille ;
   *  - l'interruption est propre : passer d'une carte à l'autre en plein
   *    mouvement repart de la position courante, là où une transition CSS
   *    redémarrerait sa durée complète et donnerait une impression de retard.
   *
   * Le survol reste un enrichissement : toute l'information est déjà lisible
   * sans lui, ce qui est indispensable sur tactile où il n'existe pas.
   */
  useGSAP(
    () => {
      if (prefersReducedMotion()) return;

      const cards = gsap.utils.toArray<HTMLElement>("[data-feature-card]");
      const cleanups: Array<() => void> = [];

      cards.forEach((card) => {
        const glow = card.querySelector("[data-feature-glow]");
        const icon = card.querySelector("[data-feature-icon]");

        const lift = gsap.quickTo(card, "y", { duration: 0.4, ease: "power3.out" });
        const fade = gsap.quickTo(glow, "opacity", { duration: 0.4, ease: "power2.out" });
        const nudge = gsap.quickTo(icon, "y", { duration: 0.45, ease: "power3.out" });

        const enter = () => {
          lift(-6);
          fade(1);
          nudge(-3);
        };
        const leave = () => {
          lift(0);
          fade(0);
          nudge(0);
        };

        card.addEventListener("mouseenter", enter);
        card.addEventListener("mouseleave", leave);
        // Le clavier déclenche le même retour visuel : sans ça, un utilisateur
        // qui tabule ne perçoit aucun changement d'état.
        card.addEventListener("focusin", enter);
        card.addEventListener("focusout", leave);

        cleanups.push(() => {
          card.removeEventListener("mouseenter", enter);
          card.removeEventListener("mouseleave", leave);
          card.removeEventListener("focusin", enter);
          card.removeEventListener("focusout", leave);
        });
      });

      return () => cleanups.forEach((fn) => fn());
    },
    { scope: root }
  );

  return (
    <Section id="fonctionnalites">
      <Container>
        <SectionHeading
          eyebrow="Ce que vous pouvez faire"
          title="Un portefeuille complet, pas une démo."
          lead="Chaque fonction sert le même objectif : rendre visible et exploitable une activité financière qui, jusqu'ici, ne laissait aucune trace."
          align="center"
        />

        <div ref={root}>
          <RevealGroup
            delay={0.1}
            className="mt-14 grid gap-4 sm:grid-cols-2 lg:grid-cols-6"
          >
            {FEATURES.map((feature) => (
              <FeatureCard
                key={feature.id}
                feature={feature}
                span={SPANS[feature.id] ?? "lg:col-span-2"}
                wide={feature.id === "credit"}
              />
            ))}
          </RevealGroup>
        </div>
      </Container>
    </Section>
  );
}

/**
 * Répartition bento plutôt que trois colonnes égales.
 *
 * Une rangée de cartes identiques donne à chaque fonction le même poids, donc
 * aucune hiérarchie : l'œil balaie sans savoir par où commencer. Ici les deux
 * usages d'entrée occupent une demi-largeur, les trois suivants un tiers, et le
 * micro-crédit — l'argument différenciant — prend la largeur entière.
 */
const SPANS: Record<string, string> = {
  wallet: "lg:col-span-3",
  transfert: "lg:col-span-3",
  coffres: "lg:col-span-2",
  programmes: "lg:col-span-2",
  marchands: "lg:col-span-2",
  credit: "lg:col-span-6",
};

function FeatureCard({
  feature,
  span,
  wide,
}: {
  feature: Feature;
  span: string;
  wide?: boolean;
}) {
  return (
    <article
      data-animate
      data-feature-card
      tabIndex={0}
      className={cn(
        "group relative flex flex-col overflow-hidden rounded-card bg-surface p-7 shadow-soft",
        span,
        wide && "lg:flex-row lg:items-center lg:gap-10 lg:p-9"
      )}
    >
      {/* Lueur révélée au survol. Décorative : opacité pilotée par GSAP. */}
      <span
        data-feature-glow
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-0 bg-[radial-gradient(120%_80%_at_50%_0%,var(--color-kola-50)_0%,transparent_70%)]"
      />

      <span
        data-feature-icon
        aria-hidden="true"
        className={cn("relative flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-kola-100 text-kola-600", wide && "lg:h-14 lg:w-14")}
      >
        <FeatureIcon id={feature.id} />
      </span>

      <div className={cn("relative", wide && "lg:flex-1")}>
        <h3
          className={cn(
            "mt-6 font-semibold",
            wide ? "text-xl lg:mt-0 lg:text-2xl" : "text-lg"
          )}
        >
          {feature.title}
        </h3>

        <p
          className={cn(
            "mt-2.5 text-[0.9375rem] leading-relaxed text-ink-600",
            wide && "lg:max-w-xl lg:text-base"
          )}
        >
          {feature.description}
        </p>
      </div>

    </article>
  );
}
