import Link from "next/link";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { Button, ArrowRight } from "@/components/ui/button";
import { CASE_STUDIES } from "@/lib/case-studies";

/**
 * Page 404.
 *
 * CE QU'ELLE ÉVITE : la sortie du site. Une 404 par défaut annonce l'échec et
 * n'offre rien — le visiteur revient au moteur de recherche, c'est-à-dire chez
 * un concurrent. Celle-ci traite l'erreur comme un carrefour : elle nomme le
 * problème en une phrase, puis propose les destinations réellement utiles.
 *
 * ELLE EST AUSSI UNE PAGE DE MAILLAGE INTERNE. Les moteurs l'explorent comme
 * les autres, et les liens qui la traversent redistribuent l'autorité vers les
 * pages qui comptent. C'est la raison pour laquelle elle liste les études de
 * cas — dérivées de `CASE_STUDIES`, donc jamais périmées — plutôt qu'un unique
 * « retour à l'accueil ».
 *
 * PAS D'EXPORT `metadata` ICI : Next l'ignore sur `not-found.tsx`, qui rend à
 * l'intérieur du layout racine et hérite donc de son titre. Le moteur injecte
 * en revanche `noindex` automatiquement dès que la réponse porte un statut 404,
 * ce qui est le comportement recherché — cette page ne doit jamais apparaître
 * dans les résultats de recherche.
 */

const DESTINATIONS = [
  {
    href: "/",
    label: "Page d'accueil",
    description:
      "Le fonctionnement du portefeuille, du score de confiance et du micro-crédit, en une page.",
  },
  {
    href: "/#score",
    label: "Comprendre le score",
    description:
      "Les huit critères, leur poids, et les quatre paliers de crédit qu'ils débloquent.",
  },
  {
    href: "/#faq",
    label: "Questions fréquentes",
    description:
      "Cinq réponses sur le calcul du score, les montants empruntables et la sécurité du compte.",
  },
  {
    href: "/contact",
    label: "Nous écrire",
    description:
      "Support, presse, partenariats. Réponse sous deux jours ouvrés.",
  },
];

export default function NotFound() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Erreur 404"
        title="Cette page n'existe pas."
        lead="L'adresse est peut-être incomplète, ou la page a été déplacée depuis que le lien a été créé. Voici où aller à la place."
      />

      <Section className="pt-0">
        <Container>
          <Reveal>
            <div className="flex flex-col gap-3 sm:flex-row">
              <Button href="/" size="lg" className="w-full sm:w-auto">
                Retour à l&apos;accueil
                <ArrowRight />
              </Button>
              <Button
                href="/#cta"
                variant="secondary"
                size="lg"
                className="w-full sm:w-auto"
              >
                Ouvrir un compte
              </Button>
            </div>
          </Reveal>

          <RevealGroup className="mt-12 grid gap-px overflow-hidden rounded-card bg-ink-200 sm:grid-cols-2">
            {DESTINATIONS.map((destination) => (
              <Link
                key={destination.href}
                href={destination.href}
                data-animate
                className="group bg-surface p-7 transition-colors duration-200 hover:bg-kola-50"
              >
                <p className="font-display flex items-center gap-2 text-lg font-semibold text-ink-950">
                  {destination.label}
                  <ArrowRight className="text-kola-600" />
                </p>
                <p className="mt-2.5 text-[0.9375rem] leading-relaxed text-ink-600">
                  {destination.description}
                </p>
              </Link>
            ))}
          </RevealGroup>

          <Reveal delay={0.1}>
            <div className="mt-4 rounded-card bg-sunken p-7 sm:p-9">
              <h2 className="text-lg font-semibold">
                Vous cherchiez peut-être une étude de cas
              </h2>
              <ul className="mt-5 flex flex-col gap-3">
                {CASE_STUDIES.map((study) => (
                  <li key={study.slug}>
                    <Link
                      href={`/etudes-de-cas/${study.slug}`}
                      className="group flex flex-col gap-1 text-[0.9375rem] leading-relaxed"
                    >
                      <span className="font-medium text-kola-700 underline underline-offset-2 transition-colors group-hover:text-kola-600">
                        {study.headline}
                      </span>
                      <span className="text-ink-500">
                        {study.sector} · {study.city}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}
