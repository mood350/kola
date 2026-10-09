import type { Metadata } from "next";
import Link from "next/link";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { Button, ArrowRight } from "@/components/ui/button";
import { CASE_STUDIES, CASE_STUDIES_DISCLAIMER } from "@/lib/case-studies";

/**
 * Index des études de cas.
 *
 * SA FONCTION DANS LE MAILLAGE : c'est le nœud qui relie l'accueil aux pages
 * profondes. Sans lui, chaque étude de cas serait une page orpheline, atteinte
 * seulement depuis le sitemap — un signal faible pour les moteurs, et une
 * impasse pour un visiteur qui voudrait en lire une seconde.
 *
 * Le titre et la description sont propres à cette page : le gabarit
 * « %s · Kola » du layout racine ajoute la marque, il ne faut donc pas la
 * répéter ici.
 */
export const metadata: Metadata = {
  title: "Études de cas",
  description:
    "Trois parcours détaillés — commerce, transport, services à domicile — montrant comment le score de confiance Kola se construit et débloque un plafond de crédit.",
  alternates: { canonical: "/etudes-de-cas" },
};

export default function CaseStudiesPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Études de cas"
        title="Le score, appliqué à des parcours entiers"
        lead="Trois activités, trois blocages différents, une même mécanique : l'usage produit la trace, la trace produit le score, le score ouvre le crédit. Chaque étude suit le calcul jusqu'au montant obtenu."
        crumbs={[{ label: "Études de cas", href: "/etudes-de-cas" }]}
      />

      <Section className="pt-0">
        <Container>
          <Reveal>
            <p className="rounded-card border border-ochre-300 bg-ochre-200/40 p-6 text-[0.875rem] leading-relaxed text-ink-700">
              <strong className="font-semibold text-ink-950">
                Scénarios illustratifs.
              </strong>{" "}
              {CASE_STUDIES_DISCLAIMER}
            </p>
          </Reveal>

          <RevealGroup className="mt-4 flex flex-col gap-4">
            {CASE_STUDIES.map((study) => (
              <article
                key={study.slug}
                data-animate
                className="rounded-card bg-surface p-7 shadow-soft sm:p-9"
              >
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                  <span>{study.sector}</span>
                  <span aria-hidden="true" className="text-ink-300">
                    ·
                  </span>
                  <span>{study.city}</span>
                  <span aria-hidden="true" className="text-ink-300">
                    ·
                  </span>
                  <span>{study.duration}</span>
                </div>

                <h2 className="font-display mt-3 text-h2 font-semibold">
                  {/* Le lien est posé sur le titre et couvre toute la carte via
                      `after:absolute` : la zone cliquable est la carte entière,
                      mais le lecteur d'écran n'annonce qu'un seul lien, dont
                      l'intitulé est le titre — pas « lire la suite ». */}
                  <Link
                    href={`/etudes-de-cas/${study.slug}`}
                    className="group relative after:absolute after:inset-0 after:content-['']"
                  >
                    {study.headline}
                  </Link>
                </h2>

                <p className="mt-4 max-w-3xl text-[0.9375rem] leading-relaxed text-ink-600">
                  {study.summary}
                </p>

                <dl className="mt-7 grid gap-x-8 gap-y-5 border-t border-ink-200 pt-6 sm:grid-cols-4">
                  {study.metrics.map((metric) => (
                    <div key={metric.label}>
                      <dt className="text-[0.6875rem] font-medium tracking-[0.14em] text-ink-400 uppercase">
                        {metric.label}
                      </dt>
                      <dd className="font-display mt-1.5 text-[1.0625rem] font-semibold text-ink-950">
                        {metric.value}
                      </dd>
                      {metric.note ? (
                        <dd className="mt-1 text-[0.8125rem] text-ink-500">
                          {metric.note}
                        </dd>
                      ) : null}
                    </div>
                  ))}
                </dl>

                <p className="mt-7 flex items-center gap-2 text-[0.9375rem] font-medium text-kola-700">
                  Lire l&apos;étude complète
                  <ArrowRight />
                </p>
              </article>
            ))}
          </RevealGroup>

          <Reveal delay={0.1}>
            <div className="mt-12 rounded-card bg-sunken p-7 sm:p-9">
              <h2 className="text-lg font-semibold">
                Le mécanisme, sans le récit
              </h2>
              <p className="mt-3 max-w-2xl text-[0.9375rem] leading-relaxed text-ink-600">
                Les huit critères du score, leur pondération exacte et les quatre
                paliers de crédit sont détaillés sur la page d&apos;accueil. Les
                questions les plus fréquentes y répondent également.
              </p>
              <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                <Button href="/#score" size="lg" className="w-full sm:w-auto">
                  Voir le barème du score
                  <ArrowRight />
                </Button>
                <Button
                  href="/#faq"
                  variant="secondary"
                  size="lg"
                  className="w-full sm:w-auto"
                >
                  Questions fréquentes
                </Button>
              </div>
            </div>
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}
