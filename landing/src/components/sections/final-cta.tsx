"use client";

import { useRef } from "react";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import { Container, Section } from "@/components/ui/section";
import { Reveal } from "@/components/motion/reveal";
import { Button, ArrowRight } from "@/components/ui/button";
import { FINAL_CTA } from "@/lib/content";

export function FinalCta() {
  const root = useRef<HTMLDivElement>(null);

  /**
   * Dernière respiration : le halo se dilate lentement pendant que la section
   * traverse le viewport. Scrub très doux — l'effet doit se sentir, pas se
   * voir. Décoratif uniquement, aucune information n'en dépend.
   */
  useGSAP(
    () => {
      if (prefersReducedMotion()) return;

      gsap.fromTo(
        "[data-cta-halo]",
        { scale: 0.85, opacity: 0.35 },
        {
          scale: 1.1,
          opacity: 0.6,
          ease: "none",
          scrollTrigger: {
            trigger: root.current,
            start: "top bottom",
            end: "bottom top",
            scrub: 1,
          },
        }
      );
    },
    { scope: root }
  );

  return (
    <Section id="cta" className="pb-24 sm:pb-32">
      <Container>
        <div
          ref={root}
          className="noise relative overflow-hidden rounded-[2rem] bg-kola-600 px-6 py-20 text-center sm:px-12 sm:py-28"
        >
          <div
            data-cta-halo
            aria-hidden="true"
            className="pointer-events-none absolute top-1/2 left-1/2 h-[38rem] w-[38rem] -translate-x-1/2 -translate-y-1/2 rounded-full bg-[radial-gradient(circle,var(--color-kola-400)_0%,transparent_62%)]"
          />

          <div className="relative mx-auto max-w-2xl">
            <Reveal>
              <h2 className="text-h1 font-semibold text-white">
                {FINAL_CTA.title}
              </h2>
            </Reveal>

            <Reveal delay={0.08}>
              <p className="text-lead mx-auto mt-6 max-w-xl text-kola-100">
                {FINAL_CTA.body}
              </p>
            </Reveal>

            <Reveal delay={0.16}>
              <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
                <Button
                  href="#"
                  size="lg"
                  variant="inverse"
                  className="w-full sm:w-auto"
                >
                  Ouvrir un compte
                  <ArrowRight />
                </Button>
                <Button
                  href="#score"
                  size="lg"
                  variant="outlineInverse"
                  className="w-full sm:w-auto"
                >
                  Revoir le score
                </Button>
              </div>
            </Reveal>

            <Reveal delay={0.22}>
              <p className="mt-8 text-sm text-kola-200">
                Vérification d&apos;identité en deux minutes · Aucun frais
                d&apos;ouverture
              </p>
            </Reveal>
          </div>
        </div>
      </Container>
    </Section>
  );
}
