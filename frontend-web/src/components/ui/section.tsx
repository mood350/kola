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
 * Micro-titre de section.
 *
 * Le point coloré porte du sens visuel mais aucune information : il est masqué
 * aux lecteurs d'écran, qui n'ont que le texte à annoncer.
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
        "inline-flex items-center gap-2.5 text-[0.8125rem] font-medium tracking-[0.14em] uppercase",
        tone === "brand" ? "text-kola-600" : "text-kola-200",
        className
      )}
    >
      <span
        aria-hidden="true"
        className={cn(
          "h-1.5 w-1.5 rounded-full",
          tone === "brand" ? "bg-ochre-400" : "bg-ochre-300"
        )}
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
        align === "center" && "items-center text-center",
        className
      )}
    >
      {eyebrow ? <Eyebrow tone={tone}>{eyebrow}</Eyebrow> : null}
      <h2
        className={cn(
          "text-h2 max-w-3xl font-semibold",
          tone === "invert" && "text-white"
        )}
      >
        {title}
      </h2>
      {lead ? (
        <p
          className={cn(
            "text-lead max-w-2xl",
            tone === "invert" ? "text-kola-100" : "text-ink-600"
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
      className={cn("scroll-mt-24 py-20 sm:py-28 lg:py-36", className)}
    >
      {children}
    </section>
  );
}
