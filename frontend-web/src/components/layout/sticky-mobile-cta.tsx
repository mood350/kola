"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import { useConsent } from "@/components/providers/cookie-consent";
import { RESPONSE_PROMISE } from "@/lib/business";

/** Au-delà de cette distance, le hero et son bouton sont hors champ. */
const REVEAL_AFTER_PX = 520;

/**
 * Barre d'action fixe, mobile uniquement.
 *
 * LE PROBLÈME QU'ELLE RÉSOUT : sur un écran de téléphone, le bouton du hero
 * sort du champ après un seul geste de défilement. Passé ce point, un visiteur
 * convaincu au milieu de la page n'a plus rien à toucher — il doit remonter
 * jusqu'en haut ou descendre jusqu'au CTA final. Cette barre supprime ce
 * trajet.
 *
 * TROIS RÈGLES QUI LA RENDENT ACCEPTABLE PLUTÔT QU'INTRUSIVE
 *
 * 1. Elle n'apparaît qu'APRÈS le seuil : tant que le bouton du hero est
 *    visible, deux appels à l'action identiques se disputeraient l'attention.
 *
 * 2. Elle s'efface quand le bandeau de consentement est ouvert. Les deux sont
 *    fixés en bas de l'écran : superposés, la barre masquerait le bouton
 *    « Refuser » — ce qui transformerait un choix libre en acceptation par
 *    empêchement.
 *
 * 3. Elle est strictement `lg:hidden`. Sur grand écran, la barre de navigation
 *    reste visible en permanence avec son propre bouton.
 *
 * DÉTECTION DU DÉFILEMENT : un écouteur `scroll` passif, pas un ScrollTrigger.
 * Sur mobile, Lenis laisse le défilement natif au navigateur (`syncTouch:
 * false`) et se désactive entièrement sous `prefers-reduced-motion` — l'
 * événement `scroll` du navigateur est donc la seule source de vérité présente
 * dans tous les cas. `passive: true` garantit que l'écouteur ne peut jamais
 * bloquer le défilement.
 */
export function StickyMobileCta() {
  const bar = useRef<HTMLDivElement>(null);
  const [past, setPast] = useState(false);
  const { choice } = useConsent();
  const pathname = usePathname();

  useEffect(() => {
    const update = () => setPast(window.scrollY > REVEAL_AFTER_PX);
    update();
    window.addEventListener("scroll", update, { passive: true });
    return () => window.removeEventListener("scroll", update);
  }, []);

  /* Le bandeau de consentement n'est rendu que tant qu'aucun choix n'est fait :
     tant qu'il est là, la barre s'efface. */
  const visible = past && choice !== null;

  useGSAP(
    () => {
      const el = bar.current;
      if (!el) return;

      if (prefersReducedMotion()) {
        gsap.set(el, { autoAlpha: visible ? 1 : 0, y: 0 });
        return;
      }

      gsap.to(el, {
        autoAlpha: visible ? 1 : 0,
        y: visible ? 0 : 24,
        duration: 0.3,
        ease: "power2.out",
      });
    },
    { dependencies: [visible] }
  );

  return (
    <div
      ref={bar}
      /* `inert` retire l'ensemble du parcours clavier et de l'arbre
         d'accessibilité tant que la barre est masquée. Sans lui, on peut
         tabuler sur un bouton invisible — le défaut le plus courant de ce
         motif. */
      inert={!visible}
      className="fixed inset-x-0 bottom-0 z-40 invisible opacity-0 lg:hidden"
    >
      <div className="border-t border-hairline bg-surface/90 px-4 pt-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] shadow-lifted backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="truncate text-[0.9375rem] font-semibold text-ink-950">
              Ouvrir un compte Kola
            </p>
            <p className="truncate text-[0.75rem] text-ink-500">
              {/* Sur la page contact, la barre sert à écrire, pas à s'inscrire :
                  la promesse de délai y est l'argument pertinent. */}
              {pathname === "/contact"
                ? RESPONSE_PROMISE.headline
                : "Quelques minutes · Aucun frais d'ouverture"}
            </p>
          </div>

          <Link
            href={pathname === "/contact" ? "#formulaire" : "/#cta"}
            className="inline-flex h-11 shrink-0 items-center justify-center rounded-full bg-kola-600 px-5 text-[0.875rem] font-medium text-white shadow-gloss transition-colors duration-200 hover:bg-kola-500 active:bg-kola-700"
          >
            {pathname === "/contact" ? "Écrire" : "Commencer"}
          </Link>
        </div>
      </div>
    </div>
  );
}
