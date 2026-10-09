import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Reveal } from "@/components/motion/reveal";

/** Gouttière commune à toutes les sections. */
export function Container({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("mx-auto w-full max-w-6xl px-5 sm:px-8", className)}>
      {children}
    </div>
  );
}

/**
 * Micro-titre de section, en pastille.
 *
 * Une étiquette posée sur la page plutôt qu'un texte en capitales : sur le
 * fond gris bleuté, la pastille blanche cernée se repère d'un coup d'œil et
 * annonce le sujet avant le titre. Le point coloré est décoratif, masqué aux
 * lecteurs d'écran.
 */
export function Eyebrow({
  children,
  className,
  tone = "brand",
}: {
  children: ReactNode;
  className?: string;
  tone?: "brand" | "invert";
}) {
  return (
    <span
      className={cn(
        "inline-flex w-fit items-center gap-2 rounded-full px-3.5 py-1.5 text-[0.8125rem] font-medium",
        tone === "brand"
          ? "border border-hairline bg-surface text-ink-700 shadow-card"
          : "border border-white/12 bg-white/5 text-kola-100",
        className
      )}
    >
      <span
        aria-hidden="true"
        className="h-1.5 w-1.5 rounded-full bg-linear-to-r from-kola-500 to-ochre-400"
      />
      {children}
    </span>
  );
}

/** Bloc titre + chapô, mis en place par une révélation au scroll. */
export function SectionHeading({
  eyebrow,
  title,
  lead,
  align = "left",
  tone = "brand",
  className,
}: {
  eyebrow?: string;
  title: ReactNode;
  lead?: ReactNode;
  align?: "left" | "center";
  tone?: "brand" | "invert";
  className?: string;
}) {
  return (
    <Reveal
      className={cn(
        "flex flex-col gap-5",
        align === "center" && "mx-auto",
        align === "center" && "items-center text-center",
        className
      )}
    >
      {eyebrow ? <Eyebrow tone={tone}>{eyebrow}</Eyebrow> : null}
      <h2
        className={cn(
          "text-title font-headline max-w-3xl font-semibold",
          tone === "invert" && "text-white"
        )}
      >
        {title}
      </h2>
      {lead ? (
        <p
          className={cn(
            "max-w-2xl text-[1.0625rem] leading-relaxed",
            tone === "invert" ? "text-kola-100/80" : "text-ink-500"
          )}
        >
          {lead}
        </p>
      ) : null}
    </Reveal>
  );
}

/** Enveloppe de section : rythme vertical homogène + ancre de navigation. */
export function Section({
  id,
  children,
  className,
}: {
  id?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      id={id}
      // scroll-mt compense l'en-tête fixe : sans ça, une ancre place le titre
      // de section sous la barre de navigation.
      className={cn("scroll-mt-24 py-12 sm:py-16 lg:py-20", className)}
    >
      {children}
    </section>
  );
}
