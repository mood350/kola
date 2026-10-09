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

/** Repères produit sous le hero — des faits vérifiables, pas des promesses. */
const META = [
  { key: "Devise", value: "XOF" },
  { key: "Score", value: "0 à 100" },
  { key: "Critères", value: "8, tous explicités" },
  { key: "Dossier", value: "Aucun" },
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

      /* Titre découpé en lignes, SANS masque.
         Le masque `overflow: hidden` par ligne rognerait le bloc surligné, dont
         le fond dépasse la boîte de la ligne — on anime donc opacité et
         translation, sans rognage. */
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
          "[data-hero-meta] > *",
          { autoAlpha: 1, y: 0, duration: 0.6, stagger: 0.06 },
          "-=0.5"
        );


      return () => {
        split?.revert();
      };
    },
    { scope: root }
  );

  return (
    /* ESPACEMENTS RESSERRÉS SUR MOBILE, INCHANGÉS AU-DESSUS DE `sm`.
       Objectif précis : que le bouton « Ouvrir un compte » tienne au-dessus de
       la ligne de flottaison sur un écran de 667 px de haut — le plus petit
       encore courant — barre d'adresse du navigateur déduite. Avec les valeurs
       de bureau, badge, titre, chapô et boutons totalisaient environ 560 px
       pour ~567 px utiles : l'action principale affleurait le bord bas, donc
       n'existait pas pour qui ne fait pas défiler. Chaque `mt-*` de ce bloc
       porte donc une valeur mobile plus courte et sa valeur d'origine à partir
       de `sm`. */
    <section ref={root} className="relative overflow-hidden pt-26 sm:pt-36 lg:pt-40">
      {/* Trame technique + diffusion. La maille dit « produit » ; la diffusion
          indigo, unique source de couleur du fond, dit à quelle marque il
          appartient. */}
      <div
        aria-hidden="true"
        className="tech-grid pointer-events-none absolute inset-0"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 -top-40 h-[42rem] bg-[radial-gradient(ellipse_60%_50%_at_50%_0%,rgb(158_161_246/0.45)_0%,transparent_70%)]"
      />

      <Container className="relative">
        <div className="relative mx-auto max-w-3xl text-center">
          <span
            data-hero-badge
            data-animate
            className="inline-flex translate-y-3 items-center gap-2.5 rounded-full bg-surface px-4 py-1.5 text-[0.8125rem] font-medium text-ink-700 shadow-soft"
          >
            <span
              aria-hidden="true"
              className="h-1.5 w-1.5 rounded-full bg-signal-500"
            />
            Mobile money · Afrique de l&apos;Ouest
          </span>

          <h1
            ref={headline}
            data-animate
            className="text-display mt-5 font-semibold text-ink-950 sm:mt-7"
          >
            Votre argent construit votre{" "}
            {/* Bloc plein plutôt que dégradé de texte : sur fond sombre, il
                donne à l'expression-clé le poids d'un objet posé sur la page,
                là où un texte dégradé se dilue dans le fond. */}
            <span className="relative inline-block rounded-2xl bg-kola-500 px-3 pb-1 text-white shadow-[0_12px_40px_-10px_rgb(73_79_223/0.55)]">
              score
            </span>
            . Il ouvre l&apos;accès au crédit.
          </h1>

          <p
            data-hero-lead
            data-animate
            className="mx-auto mt-6 max-w-xl translate-y-4 text-[1.0625rem] leading-relaxed text-ink-600 sm:mt-8"
          >
            Payez, épargnez, transférez. Kola note votre fiabilité de 0 à 100
            et vous prête en conséquence.
          </p>

          <div
            data-hero-actions
            data-animate
            className="mt-8 flex translate-y-4 flex-col items-center justify-center gap-3 sm:mt-10 sm:flex-row"
          >
            <Button href="#cta" size="lg" className="w-full sm:w-auto">
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
        </div>

        {/* Repères produit */}
        <div className="mt-16 sm:mt-20 lg:mt-28">
          <span aria-hidden="true" className="block h-px w-full bg-ink-200" />
          <dl
            data-hero-meta
            className="grid grid-cols-2 gap-y-6 py-7 sm:grid-cols-4"
          >
            {META.map((item, index) => (
              <div
                key={item.key}
                data-animate
                className={
                  index > 0
                    ? "translate-y-3 sm:border-l sm:border-ink-200 sm:pl-6"
                    : "translate-y-3"
                }
              >
                <dt className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                  {item.key}
                </dt>
                <dd className="font-display mt-1.5 text-[0.9375rem] font-medium text-ink-900">
                  {item.value}
                </dd>
              </div>
            ))}
          </dl>
        </div>
      </Container>
    </section>
  );
}
