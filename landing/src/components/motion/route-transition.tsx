"use client";

import { useRef } from "react";
import { usePathname } from "next/navigation";
import { gsap, useGSAP, ScrollTrigger, prefersReducedMotion } from "@/lib/gsap";

/**
 * Transition entre les routes.
 *
 * Trois responsabilités, toutes déclenchées par le changement de `pathname` :
 *
 * 1. **Un fondu court** sur le contenu entrant. Volontairement bref (0,35 s) :
 *    une transition de page est du temps d'attente ajouté, elle doit masquer la
 *    substitution du contenu, pas se faire remarquer.
 *
 * 2. **Le retour en haut de page.** Lenis remplace le défilement natif, la
 *    restauration automatique de Next ne s'applique donc plus : sans ça, on
 *    arrive au milieu des CGU après avoir cliqué depuis le bas de l'accueil.
 *
 * 3. **Le rafraîchissement de ScrollTrigger.** Chaque page a une hauteur
 *    différente ; les positions calculées pour la précédente sont fausses et
 *    les révélations se déclenchent au mauvais moment.
 *
 * Le premier rendu est explicitement exclu : au chargement initial, le hero
 * joue déjà sa propre séquence d'entrée, et un fondu supplémentaire par-dessus
 * la retarderait sans rien apporter.
 */
export function RouteTransition({ children }: { children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  const pathname = usePathname();
  const firstRender = useRef(true);

  useGSAP(
    () => {
      if (firstRender.current) {
        firstRender.current = false;
        return;
      }

      window.scrollTo(0, 0);
      ScrollTrigger.refresh();

      if (prefersReducedMotion()) return;

      gsap.fromTo(
        ref.current,
        { autoAlpha: 0, y: 10 },
        { autoAlpha: 1, y: 0, duration: 0.35, ease: "power2.out" }
      );
    },
    { dependencies: [pathname] }
  );

  return <div ref={ref}>{children}</div>;
}
