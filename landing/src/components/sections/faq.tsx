import Link from "next/link";
import { Container, Section, SectionHeading } from "@/components/ui/section";
import { RevealGroup } from "@/components/motion/reveal";
import { JsonLd } from "@/components/ui/json-ld";
import { FAQ_ITEMS } from "@/lib/faq";
import { faqSchema } from "@/lib/schema";

/**
 * Questions fréquentes.
 *
 * COMPOSANT SERVEUR, SANS UNE LIGNE DE JAVASCRIPT. Le pli/dépli repose sur
 * `<details>` / `<summary>`, natifs : clavier, lecteurs d'écran et recherche
 * dans la page (Ctrl+F sait ouvrir un `<details>` fermé) fonctionnent sans que
 * nous écrivions quoi que ce soit. Une réimplémentation en React coûterait un
 * état, un gestionnaire de clavier, `aria-expanded`, `aria-controls` — pour
 * reproduire exactement ce comportement, moins bien.
 *
 * LES RÉPONSES SONT DANS LE HTML, PLIÉES MAIS PRÉSENTES. C'est la condition
 * pour que le balisage `FAQPage` soit valide : Google exige que le texte
 * balisé soit celui rendu dans la page. Un accordéon qui n'injecte la réponse
 * qu'à l'ouverture rendrait le balisage caduc, et le contenu invisible aux
 * robots.
 *
 * La première question est ouverte par défaut : un bloc entièrement fermé se
 * lit comme une liste de titres et n'annonce pas qu'il contient des réponses.
 */
export function Faq() {
  return (
    <Section id="faq" className="bg-canvas">
      <JsonLd data={faqSchema()} />

      <Container>
        <SectionHeading
          eyebrow="Questions fréquentes"
          title="Ce qu'on nous demande avant d'ouvrir un compte"
          lead="Cinq réponses sur le fonctionnement réel du score, du crédit et de la sécurité du compte."
        />

        <RevealGroup className="mt-12 flex flex-col gap-3">
          {FAQ_ITEMS.map((item, index) => (
            <details
              key={item.id}
              id={item.id}
              data-animate
              open={index === 0}
              className="group scroll-mt-28 rounded-card bg-surface p-6 shadow-soft sm:p-8"
            >
              <summary
                className={
                  /* `list-none` + `::-webkit-details-marker` : le triangle par
                     défaut ne suit ni la couleur ni l'alignement du reste, on
                     le remplace par le chevron dessiné ci-dessous. */
                  "flex cursor-pointer list-none items-start justify-between gap-6 [&::-webkit-details-marker]:hidden"
                }
              >
                <h3 className="font-display text-[1.0625rem] font-semibold text-ink-950 sm:text-lg">
                  {item.question}
                </h3>
                <span
                  aria-hidden="true"
                  className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-kola-100 text-kola-700 transition-transform duration-200 ease-[var(--ease-editorial)] group-open:rotate-180"
                >
                  <svg viewBox="0 0 16 16" fill="none" className="h-3.5 w-3.5">
                    <path
                      d="m4 6 4 4 4-4"
                      stroke="currentColor"
                      strokeWidth="1.75"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </span>
              </summary>

              <p className="mt-4 max-w-3xl text-[0.9375rem] leading-relaxed text-ink-600">
                {item.answer}
              </p>
            </details>
          ))}
        </RevealGroup>

        <p className="mt-8 text-[0.9375rem] text-ink-500">
          Une question qui n&apos;est pas là ?{" "}
          <Link
            href="/contact"
            className="font-medium text-kola-700 underline underline-offset-2 transition-colors hover:text-kola-600"
          >
            Écrivez-nous
          </Link>
          , ou lisez{" "}
          <Link
            href="/etudes-de-cas"
            className="font-medium text-kola-700 underline underline-offset-2 transition-colors hover:text-kola-600"
          >
            les études de cas
          </Link>{" "}
          pour voir le score à l&apos;œuvre sur un parcours complet.
        </p>
      </Container>
    </Section>
  );
}
