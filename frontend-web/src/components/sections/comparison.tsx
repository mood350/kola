import { Container, Section, SectionHeading } from "@/components/ui/section";
import { Reveal } from "@/components/motion/reveal";
import { COMPARISON } from "@/lib/content";

/**
 * Comparatif « compte ordinaire / Kola ».
 *
 * Deux colonnes et non un tableau : chaque ligne est une phrase, pas une
 * valeur, et un `<table>` à deux cellules de prose se lirait mal sur mobile.
 * Les colonnes s'empilent alors, et chaque liste garde son titre.
 *
 * Le contraste de traitement (gris retenu à gauche, panneau nocturne à droite)
 * porte le propos ; les icônes le doublent, et le texte le dit sans elles — la
 * couleur n'est jamais seule porteuse de sens.
 */
export function Comparison() {
  return (
    <Section id="comparatif">
      <Container>
        <SectionHeading
          eyebrow="La différence"
          title={COMPARISON.title}
          align="center"
        />

        <Reveal delay={0.1} className="mx-auto mt-14 grid max-w-5xl gap-4 md:grid-cols-2">
          <div className="card p-7 sm:p-9">
            <h3 className="text-[0.9375rem] font-semibold text-ink-500">
              Un compte ordinaire
            </h3>
            <ul className="mt-6 flex flex-col">
              {COMPARISON.without.map((item) => (
                <li
                  key={item}
                  className="flex items-start gap-3 border-t border-hairline py-4 text-[0.9375rem] text-ink-500"
                >
                  <span
                    aria-hidden="true"
                    className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-mist text-ink-400"
                  >
                    <svg viewBox="0 0 12 12" fill="none" className="h-2.5 w-2.5">
                      <path d="m3 3 6 6M9 3 3 9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                    </svg>
                  </span>
                  {item}
                </li>
              ))}
            </ul>
          </div>

          <div className="relative overflow-hidden rounded-surface bg-night p-7 shadow-[0_32px_64px_-24px_rgb(46_50_199/0.55)] sm:p-9">
            <div
              aria-hidden="true"
              className="pointer-events-none absolute -top-24 -right-24 h-64 w-64 rounded-full bg-kola-500/40 blur-3xl"
            />
            <div
              aria-hidden="true"
              className="pointer-events-none absolute -bottom-28 -left-20 h-56 w-56 rounded-full bg-ochre-400/15 blur-3xl"
            />
            <h3 className="relative flex items-center gap-2.5 text-[0.9375rem] font-semibold text-white">
              <span className="font-headline text-lg tracking-tight">Avec Kola</span>
            </h3>
            <ul className="relative mt-6 flex flex-col">
              {COMPARISON.with.map((item) => (
                <li
                  key={item}
                  className="flex items-start gap-3 border-t border-white/10 py-4 text-[0.9375rem] font-medium text-white"
                >
                  <span
                    aria-hidden="true"
                    className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-linear-to-br from-kola-400 to-kola-600 text-white shadow-[0_0_0_3px_rgb(123_127_236/0.2)]"
                  >
                    <svg viewBox="0 0 12 12" fill="none" className="h-2.5 w-2.5">
                      <path d="M2.5 6.2 4.8 8.5 9.5 3.8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </span>
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </Reveal>
      </Container>
    </Section>
  );
}
