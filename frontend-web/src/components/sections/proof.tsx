"use client";

import { useRef } from "react";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import { Container, Section, Eyebrow } from "@/components/ui/section";
import { Reveal } from "@/components/motion/reveal";
import { STATS, TESTIMONIAL } from "@/lib/content";

export function Proof() {
  const root = useRef<HTMLDivElement>(null);

  /**
   * Compteurs chiffrés.
   *
   * La valeur finale est déjà présente dans le HTML rendu côté serveur ; GSAP
   * ne fait que la remplacer temporairement pendant l'animation. L'ordre
   * importe : partir de 0 dans le markup afficherait des zéros aux moteurs
   * d'indexation et à quiconque n'exécute pas JavaScript.
   */
  useGSAP(
    () => {
      if (prefersReducedMotion()) return;

      const nodes = gsap.utils.toArray<HTMLElement>("[data-stat-value]");

      nodes.forEach((node) => {
        const target = Number(node.dataset.statValue ?? "0");
        if (!Number.isFinite(target)) return;

        const state = { value: 0 };
        node.textContent = "0";

        gsap.to(state, {
          value: target,
          duration: 1.4,
          ease: "power2.out",
          onUpdate: () => {
            node.textContent = String(Math.round(state.value));
          },
          scrollTrigger: { trigger: node, start: "top 88%", once: true },
        });
      });
    },
    { scope: root }
  );

  return (
    <Section>
      <Container>
        <div ref={root}>
          <Reveal>
            <Eyebrow>En chiffres</Eyebrow>
          </Reveal>

          <div className="mt-10 grid gap-px overflow-hidden rounded-card border border-hairline bg-hairline shadow-card sm:grid-cols-2 lg:grid-cols-4">
            {STATS.map((stat) => (
              <Reveal key={stat.label} className="bg-surface p-7">
                <p className="font-headline text-5xl font-semibold tracking-tight text-ink-950 tabular-nums">
                  <span data-stat-value={stat.value}>{stat.value}</span>
                  <span className="text-gradient-accent">{stat.suffix}</span>
                </p>
                <p className="mt-3 text-sm leading-relaxed text-ink-500">
                  {stat.label}
                </p>
              </Reveal>
            ))}
          </div>

          <Reveal delay={0.1}>
            <figure className="mt-14 grid gap-8 rounded-panel bg-surface p-8 border border-hairline shadow-card sm:p-12 lg:grid-cols-[auto_1fr] lg:items-center">
              {/* Guillemet typographique, purement ornemental */}
              <span
                aria-hidden="true"
                className="font-headline text-6xl leading-none text-kola-300"
              >
                &ldquo;
              </span>
              <div>
                <blockquote>
                  <p className="text-h2 font-headline font-medium text-ink-950">
                    {TESTIMONIAL.quote}
                  </p>
                </blockquote>
                <figcaption className="mt-6 text-sm text-ink-500">
                  <span className="font-medium text-ink-800">
                    {TESTIMONIAL.author}
                  </span>
                  {" · "}
                  {TESTIMONIAL.role}
                </figcaption>
              </div>
            </figure>
          </Reveal>
        </div>
      </Container>
    </Section>
  );
}
