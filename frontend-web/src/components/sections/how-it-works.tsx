import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { STEPS } from "@/lib/content";
import { STEP_ILLUSTRATIONS } from "@/components/ui/illustrations";

/**
 * Le mécanisme du produit, en trois panneaux illustrés.
 *
 * L'illustration porte le mécanisme et le texte se contente de le nommer. La
 * numérotation est explicite (« Étape 1 ») : sur mobile les panneaux
 * s'empilent, et rien d'autre n'indiquerait qu'il s'agit d'une séquence.
 *
 * L'ancre reste `#probleme` : c'est celle que visent la navigation et le pied
 * de page, et des liens extérieurs peuvent déjà y pointer.
 */
export function HowItWorks() {
  return (
    <Section id="probleme">
      <Container>
        <SectionHeading
          eyebrow="Comment ça marche"
          title={
            <>
              Kola en <span className="text-gradient-accent">trois étapes</span>,
              aucune paperasse.
            </>
          }
          lead="Pas de formulaire à remplir pour prouver votre sérieux : c'est votre usage quotidien qui constitue le dossier."
          align="center"
        />

        <RevealGroup delay={0.1} className="mt-14 grid gap-4 lg:grid-cols-3">
          {STEPS.map((step, index) => {
            const Illustration = STEP_ILLUSTRATIONS[index];
            return (
              <article key={step.title} data-animate className="card flex flex-col p-3">
                <div className="rounded-[14px] border border-hairline bg-mist px-8 py-6">
                  <Illustration />
                </div>

                <div className="px-4 pt-6 pb-5">
                  <span className="inline-flex rounded-full bg-kola-50 px-2.5 py-1 text-[0.75rem] font-semibold text-kola-700">
                    Étape {index + 1}
                  </span>
                  <h3 className="mt-4 text-xl font-semibold tracking-tight">
                    {step.title}
                  </h3>
                  <p className="mt-2 text-[0.9375rem] leading-relaxed text-ink-500">
                    {step.body}
                  </p>
                </div>
              </article>
            );
          })}
        </RevealGroup>
      </Container>
    </Section>
  );
}
