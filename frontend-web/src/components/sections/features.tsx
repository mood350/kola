import type { ReactNode } from "react";
import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { CREDIT_TIERS, FEATURES, type Feature } from "@/lib/content";
import { FeatureIcon } from "@/components/ui/feature-icon";
import { cn } from "@/lib/cn";

/**
 * Grille des fonctions, chacune avec un fragment d'interface.
 *
 * Une icône dit « il y a une fonction » ; un fragment d'écran montre à quoi
 * elle ressemble. Les fragments sont dessinés en HTML avec les tokens réels, et
 * sont décoratifs : le titre et la description disent tout, ce qui compte sur
 * tactile comme pour un lecteur d'écran.
 *
 * Composant serveur : le survol se limite à la couleur du filet, en CSS.
 */
export function Features() {
  return (
    <Section id="fonctionnalites">
      <Container>
        <SectionHeading
          eyebrow="Fonctionnalités"
          title={
            <>
              Tout ce qu&apos;un compte aurait dû vous donner{" "}
              <span className="text-gradient-accent">depuis le début</span>.
            </>
          }
          lead="Chaque fonction sert le même objectif : rendre visible et exploitable une activité financière qui, jusqu'ici, ne laissait aucune trace."
          align="center"
        />

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
      </Container>
    </Section>
  );
}

/**
 * Répartition bento plutôt que colonnes égales : les deux usages d'entrée
 * occupent une demi-largeur, les trois suivants un tiers, et le micro-crédit —
 * l'argument différenciant — la largeur entière.
 */
const SPANS: Record<string, string> = {
  wallet: "lg:col-span-3",
  transfert: "lg:col-span-3",
  coffres: "lg:col-span-2",
  programmes: "lg:col-span-2",
  marchands: "lg:col-span-2",
  credit: "sm:col-span-2 lg:col-span-6",
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
      className={cn(
        // Pas de transition sur `transform` : GSAP anime déjà le `y` de la
        // carte à son apparition, et une transition CSS sur la même propriété
        // retarde chaque frame — la carte arrivait en retard, laissant un vide.
        "card group flex flex-col p-3 transition-colors duration-300 hover:border-kola-200",
        span,
        wide && "lg:flex-row-reverse lg:items-stretch"
      )}
    >
      <div
        aria-hidden="true"
        className={cn(
          "relative flex min-h-44 items-center justify-center overflow-hidden rounded-[14px] border border-hairline bg-mist p-6",
          wide && "lg:w-1/2"
        )}
      >
        <div aria-hidden="true" className="dot-grid absolute inset-0 opacity-60" />
        <div className="relative w-full max-w-xs">{VISUALS[feature.id]}</div>
      </div>

      <div className={cn("px-4 pt-6 pb-5", wide && "lg:flex lg:flex-1 lg:flex-col lg:justify-center lg:px-8")}>
        <span
          aria-hidden="true"
          className="flex h-9 w-9 items-center justify-center rounded-lg bg-kola-50 text-kola-600 [&_svg]:h-5 [&_svg]:w-5"
        >
          <FeatureIcon id={feature.id} />
        </span>
        <h3
          className={cn(
            "mt-4 font-semibold tracking-tight",
            wide ? "font-headline text-2xl lg:text-3xl" : "text-lg"
          )}
        >
          {feature.title}
        </h3>
        <p
          className={cn(
            "mt-2 text-[0.9375rem] leading-relaxed text-ink-500",
            wide && "lg:max-w-md lg:text-base"
          )}
        >
          {feature.description}
        </p>
      </div>
    </article>
  );
}

/* ---------------------------------------------------------------------------
   Fragments d'interface — montants d'exemple, sans valeur contractuelle.
   ------------------------------------------------------------------------ */

function Chip({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn("rounded-xl border border-hairline bg-surface shadow-card", className)}>
      {children}
    </div>
  );
}

const VISUALS: Record<string, ReactNode> = {
  wallet: (
    <div className="flex flex-col gap-2">
      <Chip className="flex items-center justify-between px-4 py-3">
        <span className="text-[0.8125rem] text-ink-500">Depuis Mobile Money</span>
        <span className="text-[0.8125rem] font-semibold text-positive-600 tabular-nums">+ 10 000 XOF</span>
      </Chip>
      <Chip className="ml-6 flex items-center justify-between px-4 py-3">
        <span className="text-[0.8125rem] text-ink-500">Solde Kola</span>
        <span className="font-headline text-base font-semibold text-ink-950 tabular-nums">58 000 XOF</span>
      </Chip>
    </div>
  ),
  transfert: (
    <Chip className="flex items-center gap-3 px-4 py-3.5">
      <span className="font-headline flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-kola-100 text-sm font-semibold text-kola-700">
        K
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-[0.8125rem] font-medium text-ink-900">Kofi A.</span>
        <span className="block text-[0.6875rem] text-ink-400">+228 •• •• 56 12</span>
      </span>
      <span className="text-right">
        <span className="block text-[0.8125rem] font-semibold text-ink-900 tabular-nums">5 000 XOF</span>
        <span className="mt-0.5 inline-block rounded-full bg-positive-50 px-2 py-0.5 text-[0.625rem] font-medium text-positive-600">
          Reçu
        </span>
      </span>
    </Chip>
  ),
  coffres: (
    <Chip className="px-4 py-3.5">
      <div className="flex items-center justify-between text-[0.8125rem]">
        <span className="font-medium text-ink-900">Coffre « Moto »</span>
        <span className="text-ink-400 tabular-nums">72 %</span>
      </div>
      <div className="mt-2.5 h-2 overflow-hidden rounded-full bg-mist">
        <div className="h-full w-[72%] rounded-full bg-linear-to-r from-kola-500 to-ochre-400" />
      </div>
      <p className="mt-2 text-[0.6875rem] text-ink-400">Bloqué jusqu&apos;au 15 déc.</p>
    </Chip>
  ),
  programmes: (
    <Chip className="px-4 py-3.5">
      <p className="text-[0.8125rem] font-medium text-ink-900">2 000 XOF chaque lundi</p>
      <div className="mt-3 grid grid-cols-7 gap-1">
        {["L", "M", "M", "J", "V", "S", "D"].map((day, i) => (
          <span
            key={i}
            className={cn(
              "flex h-7 items-center justify-center rounded-md text-[0.6875rem] font-medium",
              i === 0 ? "bg-kola-600 text-white" : "bg-mist text-ink-400"
            )}
          >
            {day}
          </span>
        ))}
      </div>
    </Chip>
  ),
  marchands: (
    <div className="flex items-center justify-center gap-3">
      <Chip className="grid grid-cols-5 gap-0.5 p-3">
        {Array.from({ length: 25 }, (_, i) => (
          <span
            key={i}
            className={cn(
              "h-2.5 w-2.5 rounded-[2px]",
              [0, 1, 3, 4, 5, 9, 12, 15, 19, 20, 21, 23, 24, 7, 17].includes(i)
                ? "bg-ink-900"
                : "bg-transparent"
            )}
          />
        ))}
      </Chip>
      <Chip className="px-3.5 py-2.5">
        <span className="block text-[0.6875rem] text-ink-400">Commission</span>
        <span className="font-headline block text-xl font-semibold text-ink-950">0 %</span>
      </Chip>
    </div>
  ),
  credit: (
    <Chip className="px-5 py-4">
      <div className="flex items-baseline justify-between">
        <span className="text-[0.8125rem] text-ink-500">Montant demandé</span>
        <span className="font-headline text-lg font-semibold text-ink-950 tabular-nums">150 000 XOF</span>
      </div>
      <div className="relative mt-4 h-2 rounded-full bg-mist">
        <div className="h-full w-[30%] rounded-full bg-linear-to-r from-kola-500 to-ochre-400" />
        <span className="absolute top-1/2 left-[30%] h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white bg-kola-600 shadow-card" />
      </div>
      <div className="mt-4 flex flex-wrap gap-1.5">
        {CREDIT_TIERS.map((tier) => (
          <span
            key={tier.name}
            className={cn(
              "rounded-full px-2.5 py-1 text-[0.6875rem] font-medium",
              tier.name === "Premium" ? "bg-kola-600 text-white" : "bg-mist text-ink-500"
            )}
          >
            {tier.name}
          </span>
        ))}
      </div>
    </Chip>
  ),
};
