import { cn } from "@/lib/cn";
import { RESPONSE_PROMISE } from "@/lib/business";

/**
 * Promesse de délai de réponse.
 *
 * POURQUOI CE BLOC EXISTE : l'objection qui bloque un envoi de message n'est
 * presque jamais « comment vous écrire », c'est « est-ce que quelqu'un lira ».
 * Un délai chiffré et affiché AVANT le formulaire lève cette hésitation ; le
 * même délai affiché après l'envoi arrive trop tard pour servir à quoi que ce
 * soit.
 *
 * Le texte vient d'une constante unique (`business.ts`), partagée avec le
 * formulaire, la page de remerciement et les données structurées de
 * l'organisation. Une promesse recopiée à quatre endroits finit par exister en
 * quatre versions différentes — et celle que le visiteur a lue n'est jamais
 * celle qu'on croit avoir écrite.
 *
 * Deux formats pour deux emplacements : `badge` se glisse dans une ligne de
 * texte, `panel` occupe un bloc à part entière.
 */
export function ResponseTime({
  variant = "badge",
  className,
}: {
  variant?: "badge" | "panel";
  className?: string;
}) {
  /* Le point vert signale une astreinte active. Décoratif : l'information est
     intégralement portée par le texte à côté, jamais par la couleur seule. */
  const dot = (
    <span
      aria-hidden="true"
      className="relative flex h-2 w-2 shrink-0"
    >
      <span className="absolute inline-flex h-full w-full rounded-full bg-signal-400 opacity-60" />
      <span className="relative inline-flex h-2 w-2 rounded-full bg-signal-500" />
    </span>
  );

  if (variant === "badge") {
    return (
      <p
        className={cn(
          "inline-flex items-center gap-2.5 rounded-full bg-surface px-4 py-2 text-[0.8125rem] font-medium text-ink-700 shadow-soft",
          className
        )}
      >
        {dot}
        {RESPONSE_PROMISE.headline}
      </p>
    );
  }

  return (
    <div className={cn("rounded-card bg-surface p-7 shadow-soft", className)}>
      <div className="flex items-center gap-3">
        {dot}
        <h2 className="font-display text-lg font-semibold text-ink-950">
          {RESPONSE_PROMISE.headline}
        </h2>
      </div>
      <p className="mt-3 text-[0.9375rem] leading-relaxed text-ink-600">
        {RESPONSE_PROMISE.detail}
      </p>
      <p className="mt-4 border-t border-ink-200 pt-4 text-[0.8125rem] text-ink-400">
        {RESPONSE_PROMISE.hours}
      </p>
    </div>
  );
}
