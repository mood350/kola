import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { SECURITY_ITEMS } from "@/lib/content";

export function Security() {
  return (
    <Section id="securite">
      <Container>
        <SectionHeading
          eyebrow="Confiance"
          title="La sécurité n'est pas une promesse, c'est un comportement observable."
          lead="Voici précisément ce que fait Kola pour protéger un compte — pas une liste de labels, mais les mécanismes réellement en place."
          align="center"
        />

        {/* Chaque garantie devient un panneau autonome plutôt qu'une ligne
            séparée par un filet. Posées à même le fond, ces six entrées se
            lisaient comme une note de bas de page ; en panneaux, elles ont le
            même statut visuel que les fonctionnalités — ce qui est exactement
            le propos de la section. */}
        <RevealGroup delay={0.1} className="mt-14 grid gap-4 sm:grid-cols-2">
          {SECURITY_ITEMS.map((item) => (
            <div
              key={item.title}
              data-animate
              className="flex gap-4 rounded-card bg-surface p-7 shadow-soft"
            >
              <span
                aria-hidden="true"
                className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-kola-100"
              >
                <svg viewBox="0 0 12 12" fill="none" className="h-3.5 w-3.5">
                  <path
                    d="M2.5 6.2 4.8 8.5 9.5 3.8"
                    stroke="var(--color-kola-600)"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </span>
              <div>
                <h3 className="text-base font-semibold">{item.title}</h3>
                <p className="mt-2 text-[0.9375rem] leading-relaxed text-ink-600">
                  {item.body}
                </p>
              </div>
            </div>
          ))}
        </RevealGroup>
      </Container>
    </Section>
  );
}
