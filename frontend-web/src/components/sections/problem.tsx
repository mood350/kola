import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { PROBLEM, PROBLEM_LEAD } from "@/lib/content";

/**
 * Le constat, juste sous le hero.
 *
 * Le visiteur doit se reconnaître avant qu'on lui explique le mécanisme : trois
 * impasses concrètes, puis la section suivante montre comment Kola en sort.
 * Composant serveur — rien ne bouge ici hors la révélation au scroll.
 */
export function Problem() {
  return (
    <Section id="constat">
      <Container>
        <SectionHeading
          eyebrow="Le constat"
          title={PROBLEM.title}
          lead={PROBLEM_LEAD}
          align="center"
        />

        <RevealGroup delay={0.1} className="mt-14 grid gap-4 md:grid-cols-3">
          {PROBLEM.points.map((point, index) => (
            <article key={point.title} data-animate className="card flex flex-col p-7">
              <span
                aria-hidden="true"
                className="font-headline flex h-10 w-10 items-center justify-center rounded-xl border border-hairline bg-mist text-[0.9375rem] font-semibold text-ink-400 tabular-nums"
              >
                {String(index + 1).padStart(2, "0")}
              </span>
              <h3 className="mt-6 text-lg font-semibold tracking-tight">
                {point.title}
              </h3>
              <p className="mt-2.5 text-[0.9375rem] leading-relaxed text-ink-500">
                {point.body}
              </p>
            </article>
          ))}
        </RevealGroup>
      </Container>
    </Section>
  );
}
