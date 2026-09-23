import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { STEPS, PROBLEM_LEAD } from "@/lib/content";
import { STEP_ILLUSTRATIONS } from "@/components/ui/illustrations";

/**
 * Le mécanisme du produit, en trois panneaux illustrés.
 *
 * Remplace l'ancienne section problème/solution, qui exposait le sujet en
 * deux blocs de prose : il fallait lire une centaine de mots avant de
 * comprendre ce que fait Kola. Ici l'illustration porte le mécanisme et le
 * texte se contente de le nommer.
 *
 * La numérotation est explicite (01, 02, 03) : sur mobile les panneaux
 * s'empilent, et rien d'autre n'indiquerait qu'il s'agit d'une séquence.
 */
export function HowItWorks() {
  return (
    <Section id="probleme">
      <Container>
        <SectionHeading
          eyebrow="Comment ça marche"
          title="Trois étapes, aucune paperasse."
          lead={PROBLEM_LEAD}
          align="center"
        />

        <RevealGroup delay={0.1} className="mt-14 grid gap-4 lg:grid-cols-3">
          {STEPS.map((step, index) => {
            const Illustration = STEP_ILLUSTRATIONS[index];
            return (
              <article
                key={step.title}
                data-animate
                className="flex flex-col rounded-card bg-surface p-7 shadow-soft"
              >
                <div className="rounded-2xl bg-canvas px-6 py-5">
                  <Illustration />
                </div>

                <p className="font-display mt-7 text-[0.8125rem] font-semibold text-kola-500 tabular-nums">
                  {String(index + 1).padStart(2, "0")}
                </p>
                <h3 className="mt-2 text-xl font-semibold">{step.title}</h3>
                <p className="mt-2.5 text-[0.9375rem] leading-relaxed text-ink-600">
                  {step.body}
                </p>
              </article>
            );
          })}
        </RevealGroup>
      </Container>
    </Section>
  );
}
