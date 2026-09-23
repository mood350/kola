"use client";

import { useRef } from "react";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import { Container, Section, Eyebrow } from "@/components/ui/section";
import { Reveal } from "@/components/motion/reveal";
import { SCORE_RULES, CREDIT_TIERS, DEMO_SCORE } from "@/lib/content";

/** Géométrie de l'arc : demi-cercle de rayon 120 dans un viewBox 280×160. */
const RADIUS = 120;
const ARC_LENGTH = Math.PI * RADIUS;

export function CreditScore() {
  const root = useRef<HTMLDivElement>(null);
  const scoreValue = useRef<HTMLSpanElement>(null);

  useGSAP(
    () => {
      const reduced = prefersReducedMotion();

      /* --- Jauge ---------------------------------------------------------
         L'arc se remplit et le nombre défile en même temps, sur la même
         timeline : le chiffre et la géométrie racontent la même chose, les
         désynchroniser donnerait deux informations concurrentes.

         `strokeDashoffset` est animé plutôt que la largeur ou un `clip-path` :
         c'est la seule approche qui suive exactement la courbe, et le
         navigateur la compose sans redéclencher de calcul de mise en page. */
      const gauge = root.current?.querySelector("[data-gauge-progress]");
      if (!gauge) return;

      const counter = { value: 0 };

      const applyCount = () => {
        if (scoreValue.current) {
          scoreValue.current.textContent = String(Math.round(counter.value));
        }
      };

      if (reduced) {
        // Sans animation, on affiche directement l'état final.
        gsap.set(gauge, { strokeDashoffset: ARC_LENGTH * (1 - DEMO_SCORE / 100) });
        counter.value = DEMO_SCORE;
        applyCount();
        gsap.set("[data-rule-bar]", { scaleX: 1 });
        gsap.set("[data-tier-row]", { autoAlpha: 1, x: 0 });
        return;
      }

      const tl = gsap.timeline({
        scrollTrigger: {
          trigger: root.current,
          start: "top 65%",
          // Une seule lecture : rejouer le comptage à chaque passage
          // transformerait un moment de démonstration en tic nerveux.
          once: true,
        },
      });

      tl.fromTo(
        gauge,
        { strokeDashoffset: ARC_LENGTH },
        {
          strokeDashoffset: ARC_LENGTH * (1 - DEMO_SCORE / 100),
          duration: 1.9,
          ease: "power2.inOut",
        }
      )
        .to(
          counter,
          {
            value: DEMO_SCORE,
            duration: 1.9,
            ease: "power2.inOut",
            onUpdate: applyCount,
          },
          "<"
        )
        .to("[data-gauge-tier]", { autoAlpha: 1, y: 0, duration: 0.5 }, "-=0.5")

        /* --- Barres de pondération --------------------------------------
           `scaleX` avec origine à gauche plutôt que `width` : une animation de
           largeur force un recalcul de mise en page à chaque frame, alors que
           la transformation est composée par le GPU. */
        .fromTo(
          "[data-rule-bar]",
          { scaleX: 0 },
          { scaleX: 1, duration: 0.7, ease: "power3.out", stagger: 0.05 },
          "-=1.1"
        )
        .fromTo(
          "[data-tier-row]",
          { autoAlpha: 0, x: -12 },
          { autoAlpha: 1, x: 0, duration: 0.5, stagger: 0.07 },
          "-=0.4"
        );
    },
    { scope: root }
  );

  const reachedTier = [...CREDIT_TIERS].reverse().find((t) => DEMO_SCORE >= t.min);

  return (
    <Section id="score">
      <Container>
        {/* Titre porté par la page claire, contenu porté par un panneau sombre.
            C'est la bascule de traitement qui donne son poids à la section :
            le sujet est annoncé dans le flux normal, puis la démonstration
            bascule dans un objet à part, comme une double page encartée. */}
        <div className="mx-auto max-w-2xl text-center">
          <Reveal>
            <Eyebrow className="justify-center">Le cœur de Kola</Eyebrow>
          </Reveal>
          <Reveal delay={0.05}>
            <h2 className="text-h1 mt-5 font-semibold">
              Un score de confiance qui vous appartient.
            </h2>
          </Reveal>
          <Reveal delay={0.1}>
            <p className="text-lead mt-6 text-ink-600">
              Pas d&apos;algorithme opaque : huit critères, chacun avec son
              poids, tous consultables dans l&apos;application.
            </p>
          </Reveal>
        </div>

        <div
          ref={root}
          className="mt-14 rounded-panel bg-kola-950 p-7 shadow-glow sm:p-10 lg:mt-18 lg:p-14"
        >
          <div className="grid gap-12 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1fr)] lg:gap-16">
            {/* Colonne jauge. Plus de `sticky` depuis que le contenu vit dans
                un panneau : coller une colonne à l'intérieur d'un bloc borné
                la fait buter contre le bas du panneau au lieu de suivre la
                lecture. */}
            <div className="lg:self-start">
              <div className="rounded-card border border-white/10 bg-white/[0.05] p-8">
                <Gauge scoreRef={scoreValue} tierName={reachedTier?.name ?? "—"} />

                <p className="mt-8 border-t border-white/10 pt-6 text-sm leading-relaxed text-kola-200">
                  Le score est recalculé au fil de vos opérations et reste
                  valable 30 jours. Un défaut de paiement passé reste visible,
                  même une fois régularisé.
                </p>
              </div>
            </div>

            {/* Colonne détail */}
            <div>
              <h3 className="text-sm font-medium tracking-[0.14em] text-kola-300 uppercase">
                Comment il se construit
              </h3>

              <ul className="mt-7 flex flex-col gap-4">
                {SCORE_RULES.map((rule) => (
                  <li key={rule.label}>
                    <div className="flex items-baseline justify-between gap-4">
                      <span className="text-[0.9375rem] font-medium text-white">
                        {rule.label}
                      </span>
                      <span className="shrink-0 font-display text-sm font-semibold text-kola-200 tabular-nums">
                        {rule.weight} pts
                      </span>
                    </div>

                    {/* La barre double le chiffre déjà écrit à côté : elle
                        accélère la comparaison entre critères sans être le
                        seul support de l'information. */}
                    <div
                      aria-hidden="true"
                      className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/10"
                    >
                      <div
                        data-rule-bar
                        className="h-full origin-left rounded-full bg-kola-400"
                        style={{ width: `${(rule.weight / 15) * 100}%` }}
                      />
                    </div>
                  </li>
                ))}
              </ul>

              {/* Échelle des paliers */}
              <h3 className="mt-14 text-sm font-medium tracking-[0.14em] text-kola-300 uppercase">
                Ce qu&apos;il débloque
              </h3>

              {/* Sous 640px, les quatre colonnes se télescopaient : le nom du
                  palier collait à sa plage de score et le plafond au taux. On
                  passe donc à une liste empilée, où chaque palier devient un
                  bloc autonome. Une seule des deux vues est rendue à la fois —
                  celle qui est masquée n'est pas annoncée aux lecteurs
                  d'écran. */}
              <ul className="mt-6 flex flex-col gap-3 sm:hidden">
                {CREDIT_TIERS.map((tier) => (
                  <li
                    key={tier.name}
                    data-tier-row
                    className="rounded-xl border border-white/10 bg-white/[0.03] p-4"
                  >
                    <p className="flex items-center gap-2.5 text-[0.9375rem] font-medium text-white">
                      <span
                        aria-hidden="true"
                        className="h-2.5 w-2.5 shrink-0 rounded-full ring-1 ring-white/25"
                        style={{ backgroundColor: tier.swatch }}
                      />
                      {tier.name}
                      <span className="ml-auto text-[0.8125rem] font-normal text-kola-200 tabular-nums">
                        score {tier.range}
                      </span>
                    </p>
                    <dl className="mt-3 flex items-baseline justify-between gap-4 border-t border-white/10 pt-3">
                      <div>
                        <dt className="text-[0.6875rem] tracking-wider text-kola-300 uppercase">
                          Plafond
                        </dt>
                        <dd className="mt-0.5 text-[0.875rem] font-medium text-white tabular-nums">
                          {tier.ceiling}
                        </dd>
                      </div>
                      <div className="text-right">
                        <dt className="text-[0.6875rem] tracking-wider text-kola-300 uppercase">
                          Taux
                        </dt>
                        <dd className="mt-0.5 text-[0.875rem] text-kola-200 tabular-nums">
                          {tier.rate}
                        </dd>
                      </div>
                    </dl>
                  </li>
                ))}
              </ul>

              <table className="mt-6 hidden w-full border-collapse text-left sm:table">
                <caption className="sr-only">
                  Paliers de crédit Kola selon le score obtenu
                </caption>
                <thead>
                  <tr className="text-[0.75rem] tracking-wider text-kola-300 uppercase">
                    <th scope="col" className="pb-3 font-medium">Palier</th>
                    <th scope="col" className="pb-3 pl-4 font-medium">Score</th>
                    <th scope="col" className="pb-3 pl-4 text-right font-medium">
                      Plafond
                    </th>
                    <th scope="col" className="pb-3 pl-4 text-right font-medium">
                      Taux
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {CREDIT_TIERS.map((tier) => (
                    <tr
                      key={tier.name}
                      data-tier-row
                      className="border-t border-white/10"
                    >
                      <th
                        scope="row"
                        className="py-3.5 text-[0.9375rem] font-medium text-white"
                      >
                        <span className="flex items-center gap-2.5">
                          {/* La pastille est une aide visuelle ordinale ; le
                              nom du palier reste toujours écrit à côté, la
                              couleur n'est jamais seule porteuse de sens. */}
                          <span
                            aria-hidden="true"
                            className="h-2.5 w-2.5 shrink-0 rounded-full ring-1 ring-white/25"
                            style={{ backgroundColor: tier.swatch }}
                          />
                          {tier.name}
                        </span>
                      </th>
                      <td className="py-3.5 pl-4 text-[0.875rem] text-kola-200 tabular-nums">
                        {tier.range}
                      </td>
                      <td className="py-3.5 pl-4 text-right text-[0.875rem] font-medium text-white tabular-nums">
                        {tier.ceiling}
                      </td>
                      <td className="py-3.5 pl-4 text-right text-[0.875rem] text-kola-200 tabular-nums">
                        {tier.rate}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </Container>
    </Section>
  );
}

/** Jauge semi-circulaire : une magnitude unique, donc un seul chiffre héros. */
function Gauge({
  scoreRef,
  tierName,
}: {
  scoreRef: React.RefObject<HTMLSpanElement | null>;
  tierName: string;
}) {
  return (
    <figure className="flex flex-col items-center">
      <div className="relative w-full max-w-[280px]">
        <svg viewBox="0 0 280 160" className="w-full" role="img"
          aria-label={`Score de confiance de ${DEMO_SCORE} sur 100, palier ${tierName}`}>
          {/* Piste */}
          <path
            d="M 20 140 A 120 120 0 0 1 260 140"
            fill="none"
            stroke="rgba(255,255,255,0.10)"
            strokeWidth="14"
            strokeLinecap="round"
          />
          {/* Progression — teinte unique, dégradée en intensité seulement */}
          <defs>
            <linearGradient id="gauge-fill" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="var(--color-kola-500)" />
              <stop offset="100%" stopColor="var(--color-kola-300)" />
            </linearGradient>
          </defs>
          <path
            data-gauge-progress
            d="M 20 140 A 120 120 0 0 1 260 140"
            fill="none"
            stroke="url(#gauge-fill)"
            strokeWidth="14"
            strokeLinecap="round"
            strokeDasharray={ARC_LENGTH}
            strokeDashoffset={ARC_LENGTH}
          />
        </svg>

        {/* Chiffre héros, centré dans l'arc */}
        <div className="absolute inset-x-0 bottom-1 flex flex-col items-center">
          <span className="font-display text-6xl leading-none font-semibold text-white tabular-nums">
            <span ref={scoreRef}>0</span>
          </span>
          <span className="mt-1.5 text-xs tracking-[0.16em] text-kola-300 uppercase">
            sur 100
          </span>
        </div>
      </div>

      <figcaption
        data-gauge-tier
        className="mt-6 translate-y-2 opacity-0"
      >
        <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm font-medium text-white">
          <span
            aria-hidden="true"
            className="h-1.5 w-1.5 rounded-full bg-ochre-400"
          />
          Palier {tierName} atteint
        </span>
      </figcaption>
    </figure>
  );
}
