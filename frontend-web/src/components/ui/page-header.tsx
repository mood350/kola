import type { ReactNode } from "react";
import { Container } from "@/components/ui/section";
import { Reveal } from "@/components/motion/reveal";
import { Eyebrow } from "@/components/ui/section";
import { Breadcrumbs } from "@/components/ui/breadcrumbs";
import type { Crumb } from "@/lib/schema";

/**
 * Bandeau d'ouverture des pages secondaires (CGU, contact, études de cas).
 *
 * Il reprend la trame technique et la diffusion indigo du hero d'accueil, en
 * plus court : ces pages doivent appartenir visiblement au même site, sans
 * prétendre au même poids que la page d'accueil.
 *
 * Le fil d'Ariane est rendu ICI plutôt qu'au-dessus du bandeau. Placé avant, il
 * flotterait sur le fond de page nu et imposerait un second bloc d'espacement
 * sous l'en-tête fixe. Intégré, il partage la trame de fond et occupe la ligne
 * qui précède naturellement le sur-titre — l'ordre de lecture attendu : où je
 * suis, puis quelle page je lis.
 */
export function PageHeader({
  eyebrow,
  title,
  lead,
  meta,
  crumbs,
}: {
  eyebrow: string;
  title: string;
  lead?: ReactNode;
  meta?: string;
  /** Maillons suivant l'accueil, qui est ajouté automatiquement. */
  crumbs?: Crumb[];
}) {
  return (
    <header className="relative overflow-hidden pt-32 pb-14 sm:pt-36 lg:pt-40 lg:pb-18">
      <div
        aria-hidden="true"
        className="dot-grid pointer-events-none absolute inset-0"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 -top-40 h-[30rem] bg-[radial-gradient(ellipse_55%_50%_at_50%_0%,rgb(123_127_236/0.3)_0%,transparent_70%)]"
      />

      <Container className="relative">
        <div className="max-w-3xl">
          {crumbs ? <Breadcrumbs items={crumbs} className="mb-7" /> : null}
          <Reveal>
            <Eyebrow>{eyebrow}</Eyebrow>
          </Reveal>
          <Reveal delay={0.05}>
            <h1 className="text-title mt-6 font-semibold">{title}</h1>
          </Reveal>
          {lead ? (
            <Reveal delay={0.1}>
              <p className="mt-6 max-w-2xl text-[1.0625rem] leading-relaxed text-ink-500 sm:text-lg">{lead}</p>
            </Reveal>
          ) : null}
          {meta ? (
            <Reveal delay={0.15}>
              <p className="mt-8 border-t border-hairline pt-5 text-[0.8125rem] text-ink-400">
                {meta}
              </p>
            </Reveal>
          ) : null}
        </div>
      </Container>
    </header>
  );
}
