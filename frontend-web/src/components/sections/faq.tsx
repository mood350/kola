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
    <Section id="faq">
      <JsonLd data={faqSchema()} />

      <Container>
        {/* Deux colonnes sur grand écran : le titre et la porte de sortie
            (« écrivez-nous ») restent à gauche, collés pendant qu'on lit les
            réponses à droite. Sur mobile, l'ordre de lecture reste le même. */}
        <div className="grid gap-12 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:gap-16">
          <div className="lg:sticky lg:top-32 lg:self-start">
            <SectionHeading
              eyebrow="Questions fréquentes"
              title="Ce qu'on nous demande avant d'ouvrir un compte"
              lead="Les réponses sur le fonctionnement réel du score, du crédit et de la sécurité du compte."
            />

            <p className="mt-8 text-[0.9375rem] leading-relaxed text-ink-500">
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
          </div>

          <RevealGroup className="flex flex-col gap-3">
            {FAQ_ITEMS.map((item, index) => (
              <details
                key={item.id}
                id={item.id}
                data-animate
                open={index === 0}
                className="card group scroll-mt-28 p-6 transition-colors open:border-kola-200 sm:px-7"
              >
                <summary
                  className={
                    /* `list-none` + `::-webkit-details-marker` : le triangle par
                       défaut ne suit ni la couleur ni l'alignement du reste, on
                       le remplace par le signe dessiné ci-dessous. */
                    "flex cursor-pointer list-none items-start justify-between gap-6 [&::-webkit-details-marker]:hidden"
                  }
                >
                  <h3 className="text-[1rem] font-semibold text-ink-950 sm:text-[1.0625rem]">
                    {item.question}
                  </h3>
                  <span
                    aria-hidden="true"
                    className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border border-hairline bg-mist text-ink-700 transition-[transform,background-color,color] duration-200 ease-[var(--ease-editorial)] group-open:rotate-45 group-open:bg-kola-600 group-open:text-white"
                  >
                    <svg viewBox="0 0 16 16" fill="none" className="h-3.5 w-3.5">
                      <path
                        d="M8 3.5v9M3.5 8h9"
                        stroke="currentColor"
                        strokeWidth="1.75"
                        strokeLinecap="round"
                      />
                    </svg>
                  </span>
                </summary>

                <p className="mt-4 max-w-2xl text-[0.9375rem] leading-relaxed text-ink-500">
                  {item.answer}
                </p>
              </details>
            ))}
          </RevealGroup>
        </div>
      </Container>
    </Section>
  );
}
