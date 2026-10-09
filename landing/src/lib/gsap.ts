"use client";

import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { SplitText } from "gsap/SplitText";
import { useGSAP } from "@gsap/react";

/**
 * Point d'enregistrement unique des plugins GSAP.
 *
 * `registerPlugin` est idempotent, mais le centraliser ici évite qu'un
 * composant oublie l'appel et échoue silencieusement en production, où le
 * tree-shaking supprime les plugins jamais référencés.
 *
 * Depuis GSAP 3.13 l'intégralité des plugins (dont SplitText, longtemps
 * réservé au Club) est gratuite : aucune licence à vérifier avant mise en
 * ligne.
 */
if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger, SplitText, useGSAP);
}

/** Durées et courbes partagées, pour que tout le site bouge de la même façon. */
export const MOTION = {
  /** Courbe maison, jumelle du --ease-editorial côté CSS. */
  ease: "power3.out",
  easeExpo: "expo.out",
  fast: 0.35,
  base: 0.6,
  slow: 0.9,
  /** Décalage inter-éléments. Au-delà de ~0.08s, les derniers items traînent. */
  stagger: 0.07,
} as const;

/**
 * Respect de `prefers-reduced-motion`.
 *
 * Lu à l'exécution et non mis en cache au chargement du module : l'utilisateur
 * peut changer le réglage système sans recharger la page.
 */
export function prefersReducedMotion(): boolean {
  if (typeof window === "undefined") return false;
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export { gsap, ScrollTrigger, SplitText, useGSAP };
