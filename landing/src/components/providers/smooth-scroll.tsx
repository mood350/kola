"use client";

import { useEffect } from "react";
import Lenis from "lenis";
import { gsap, ScrollTrigger, prefersReducedMotion } from "@/lib/gsap";

/**
 * Défilement inertiel (Lenis) synchronisé avec ScrollTrigger.
 *
 * Deux pièges que cette intégration évite :
 *
 * 1. Deux boucles d'animation concurrentes. Lenis a son propre requestAnimationFrame
 *    et GSAP le sien ; les laisser tourner en parallèle produit un décalage
 *    d'une frame entre la position réelle et celle que croit ScrollTrigger,
 *    ce qui fait « flotter » les éléments épinglés. On pilote donc Lenis
 *    depuis le ticker GSAP, une seule boucle fait foi.
 *
 * 2. lagSmoothing. Par défaut GSAP « rattrape » les frames perdues, ce qui
 *    fait sauter le scroll après un à-coup. Désactivé ici, le défilement
 *    reste solidaire de la molette.
 */
export function SmoothScroll({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    // Le défilement inertiel est un effet de confort : il est purement et
    // simplement désactivé si l'utilisateur demande moins de mouvement, ce qui
    // rend la molette au navigateur.
    if (prefersReducedMotion()) return;

    const lenis = new Lenis({
      duration: 1.05,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      // Le tactile garde le défilement natif : le remplacer donne toujours une
      // sensation « collante » sur mobile et casse le rebond du système.
      smoothWheel: true,
      syncTouch: false,
    });

    lenis.on("scroll", ScrollTrigger.update);

    const raf = (time: number) => lenis.raf(time * 1000);
    gsap.ticker.add(raf);
    gsap.ticker.lagSmoothing(0);

    return () => {
      gsap.ticker.remove(raf);
      gsap.ticker.lagSmoothing(500, 33);
      lenis.destroy();
    };
  }, []);

  /**
   * Les polices web changent les métriques du texte après le premier paint :
   * sans ce refresh, toutes les positions calculées par ScrollTrigger avant
   * leur chargement sont décalées de quelques dizaines de pixels et les
   * révélations se déclenchent au mauvais moment.
   */
  useEffect(() => {
    if (!("fonts" in document)) return;
    let cancelled = false;
    document.fonts.ready.then(() => {
      if (!cancelled) ScrollTrigger.refresh();
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return <>{children}</>;
}
