"use client";

import { useRef, type ElementType, type ReactNode } from "react";
import { gsap, useGSAP, MOTION, prefersReducedMotion } from "@/lib/gsap";

/**
 * Révélations au scroll — la brique de base du site.
 *
 * `useGSAP` scopé au conteneur révoque automatiquement tweens ET ScrollTriggers
 * au démontage. C'est indispensable en App Router : sans ça, chaque navigation
 * client laisserait derrière elle des ScrollTrigger orphelins, qui continuent
 * d'écouter le scroll et retiennent des nœuds DOM détachés.
 *
 * L'état de départ vient du CSS via `[data-animate]` et non d'un `gsap.set` :
 * JavaScript s'exécute après le premier paint, l'élément apparaîtrait donc une
 * frame avant de disparaître pour être animé.
 *
 * On anime `autoAlpha` et non `opacity` : il pilote aussi `visibility`, ce qui
 * retire l'élément du hit-testing tant qu'il est invisible. Avec `opacity`
 * seule, on peut cliquer et surtout tabuler dans du contenu qu'on ne voit pas.
 */

type BaseProps = {
  children: ReactNode;
  className?: string;
  as?: ElementType;
  /** Retard après déclenchement, en secondes. */
  delay?: number;
  /** Amplitude verticale. Faible volontairement : ça doit lire comme un fondu. */
  y?: number;
};

/** Révèle le bloc entier d'un seul tenant. */
export function Reveal({
  children,
  className,
  as: Tag = "div",
  delay = 0,
  y = 24,
}: BaseProps) {
  const ref = useRef<HTMLElement>(null);

  useGSAP(
    () => {
      const root = ref.current;
      if (!root) return;

      if (prefersReducedMotion()) {
        gsap.set(root, { autoAlpha: 1, y: 0 });
        return;
      }

      gsap.fromTo(
        root,
        { autoAlpha: 0, y },
        {
          autoAlpha: 1,
          y: 0,
          duration: MOTION.base,
          ease: MOTION.ease,
          delay,
          // « top 95% » : le bloc s'anime dès qu'il entre à l'écran. À 85 %, un
          // défilement rapide laissait voir de grandes zones vides le temps
          // que le contenu arrive.
          scrollTrigger: { trigger: root, start: "top 95%" },
        }
      );
    },
    { scope: ref, dependencies: [delay, y] }
  );

  return (
    <Tag
      ref={ref as React.Ref<HTMLDivElement>}
      data-animate=""
      className={className}
    >
      {children}
    </Tag>
  );
}

/**
 * Révèle en cascade tous les descendants marqués `data-animate`.
 *
 * Le ciblage se fait par attribut et non par index d'enfant : React réordonne
 * ou remonte librement les nœuds, un ciblage positionnel casserait
 * silencieusement au premier re-render.
 */
export function RevealGroup({
  children,
  className,
  as: Tag = "div",
  delay = 0,
  y = 24,
}: BaseProps) {
  const ref = useRef<HTMLElement>(null);

  useGSAP(
    () => {
      const root = ref.current;
      if (!root) return;

      const items = root.querySelectorAll("[data-animate]");
      if (!items.length) return;

      if (prefersReducedMotion()) {
        gsap.set(items, { autoAlpha: 1, y: 0 });
        return;
      }

      gsap.fromTo(
        items,
        { autoAlpha: 0, y },
        {
          autoAlpha: 1,
          y: 0,
          duration: MOTION.base,
          ease: MOTION.ease,
          delay,
          // Au-delà de ~0.08s par élément, les derniers de la liste traînent.
          stagger: MOTION.stagger,
          // « top 95% » : le bloc s'anime dès qu'il entre à l'écran. À 85 %, un
          // défilement rapide laissait voir de grandes zones vides le temps
          // que le contenu arrive.
          scrollTrigger: { trigger: root, start: "top 95%" },
        }
      );
    },
    { scope: ref, dependencies: [delay, y] }
  );

  return (
    <Tag ref={ref as React.Ref<HTMLDivElement>} className={className}>
      {children}
    </Tag>
  );
}
