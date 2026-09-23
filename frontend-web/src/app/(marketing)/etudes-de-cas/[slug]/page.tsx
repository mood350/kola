import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { Button, ArrowRight } from "@/components/ui/button";
import {
  CASE_STUDIES,
  CASE_STUDIES_DISCLAIMER,
  findCaseStudy,
  type CaseStudy,
} from "@/lib/case-studies";

/**
 * Détail d'une étude de cas.
 *
 * PRÉ-RENDU COMPLET : `generateStaticParams` énumère les trois segments connus,
 * qui sont donc construits au build et servis en HTML statique. Toute autre
 * valeur tombe sur `notFound()`, c'est-à-dire sur `app/not-found.tsx` avec un
 * statut 404 réel — et non sur une page vide renvoyée en 200, qui laisserait
 * les moteurs indexer autant d'URL creuses qu'on peut en inventer.
 *
 * `params` est une promesse : dans cette version de Next, les paramètres de
 * route sont asynchrones et doivent être attendus avant lecture, dans le
 * composant comme dans `generateMetadata`.
 */

export function generateStaticParams() {
  return CASE_STUDIES.map((study) => ({ slug: study.slug }));
}

/**
 * Titre et description PROPRES À CHAQUE ÉTUDE.
 *
 * C'est le point où se joue l'utilité de ces pages en recherche : trois pages
 * partageant « Études de cas · Kola » seraient traitées comme des quasi-
 * doublons, et une seule ressortirait. Le titre reprend donc l'accroche de
 * l'étude, la description son résumé — chacun écrit une fois, dans
 * `lib/case-studies.ts`, et jamais recopié.
 */
export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const study = findCaseStudy(slug);

  if (!study) {
    return { title: "Étude de cas introuvable" };
  }

  return {
    title: study.headline,
    description: study.summary,
    alternates: { canonical: `/etudes-de-cas/${study.slug}` },
    openGraph: {
      type: "article",
      title: `${study.headline} · Kola`,
      description: study.summary,
      url: `/etudes-de-cas/${study.slug}`,
    },
  };
}

export default async function CaseStudyPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const study = findCaseStudy(slug);

  if (!study) notFound();

  /* Étude suivante, en boucle : la dernière renvoie à la première. Une carte
     « lire la suite » vide en fin de liste serait le seul cas où cette page ne
     mène nulle part. */
  const index = CASE_STUDIES.findIndex((item) => item.slug === study.slug);
  const next = CASE_STUDIES[(index + 1) % CASE_STUDIES.length];

  return (
    <main id="contenu">
      <PageHeader
        eyebrow={study.sector}
        title={study.headline}
        lead={study.summary}
        meta={`${study.city} · Période observée : ${study.duration}`}
        crumbs={[
          { label: "Études de cas", href: "/etudes-de-cas" },
          { label: study.title, href: `/etudes-de-cas/${study.slug}` },
        ]}
      />

      <Section className="pt-0">
        <Container>
          <Reveal>
            <p className="rounded-card border border-ochre-300 bg-ochre-200/40 p-6 text-[0.875rem] leading-relaxed text-ink-700">
              <strong className="font-semibold text-ink-950">
                Scénario illustratif.
              </strong>{" "}
              {CASE_STUDIES_DISCLAIMER}
            </p>
          </Reveal>

          {/* Résultats chiffrés en tête : c'est ce qu'on vient chercher, et ce
              qu'on emporte si on ne lit rien d'autre. */}
          <RevealGroup className="mt-4 grid gap-px overflow-hidden rounded-card bg-ink-200 sm:grid-cols-2 lg:grid-cols-4">
            {study.metrics.map((metric) => (
              <div key={metric.label} data-animate className="bg-surface p-7">
                <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                  {metric.label}
                </p>
                <p className="font-display mt-2.5 text-2xl font-semibold text-ink-950 tabular-nums">
                  {metric.value}
                </p>
                {metric.note ? (
                  <p className="mt-2 text-[0.8125rem] leading-relaxed text-ink-500">
                    {metric.note}
                  </p>
                ) : null}
              </div>
            ))}
          </RevealGroup>

          <div className="mt-4 flex flex-col gap-4">
            <Chapter
              number="01"
              title="Le blocage"
              paragraphs={study.challenge}
            />
            <Chapter
              number="02"
              title="Ce que Kola change"
              paragraphs={study.approach}
            />
            <Chapter
              number="03"
              title="Où l'on aboutit"
              paragraphs={study.outcome}
            />
          </div>

          <Reveal delay={0.08}>
            <figure className="mt-4 rounded-panel bg-surface p-8 shadow-soft sm:p-12 hairline">
              <blockquote>
                <p className="font-display text-h2 font-medium text-ink-950">
                  &ldquo;{study.quote.text}&rdquo;
                </p>
              </blockquote>
              <figcaption className="mt-6 text-sm text-ink-500">
                <span className="font-medium text-ink-800">
                  {study.quote.author}
                </span>
                {" · "}
                {study.quote.role}
              </figcaption>
            </figure>
          </Reveal>

          {/* Sortie de page : une étude de plus, ou le produit lui-même. */}
          <Reveal delay={0.12}>
            <div className="mt-12 grid gap-4 lg:grid-cols-2">
              <Link
                href={`/etudes-de-cas/${next.slug}`}
                className="group rounded-card bg-sunken p-7 transition-colors duration-200 hover:bg-kola-100 sm:p-9"
              >
                <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                  Étude suivante · {next.sector}
                </p>
                <p className="font-display mt-3 text-xl leading-snug font-semibold text-ink-950">
                  {next.headline}
                </p>
                <p className="mt-4 flex items-center gap-2 text-[0.9375rem] font-medium text-kola-700">
                  Continuer
                  <ArrowRight />
                </p>
              </Link>

              <div className="rounded-card bg-kola-600 p-7 sm:p-9">
                <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-kola-200 uppercase">
                  Votre tour
                </p>
                <p className="font-display mt-3 text-xl leading-snug font-semibold text-white">
                  Votre historique commence à votre premier dépôt.
                </p>
                <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                  <Button
                    href="/#cta"
                    variant="inverse"
                    className="w-full sm:w-auto"
                  >
                    Ouvrir un compte
                    <ArrowRight />
                  </Button>
                  <Button
                    href="/contact"
                    variant="outlineInverse"
                    className="w-full sm:w-auto"
                  >
                    Poser une question
                  </Button>
                </div>
              </div>
            </div>
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}

/** Un chapitre de l'étude : numéro, titre, paragraphes. */
function Chapter({
  number,
  title,
  paragraphs,
}: {
  number: string;
  title: string;
  paragraphs: CaseStudy["challenge"];
}) {
  return (
    <Reveal>
      <section className="rounded-card bg-surface p-7 shadow-soft sm:p-9">
        <p className="font-display text-[0.8125rem] font-semibold text-kola-500 tabular-nums">
          {number}
        </p>
        <h2 className="mt-2 text-xl font-semibold">{title}</h2>
        <div className="mt-4 flex max-w-3xl flex-col gap-3.5">
          {paragraphs.map((paragraph) => (
            <p
              key={paragraph.slice(0, 40)}
              className="text-[0.9375rem] leading-relaxed text-ink-600"
            >
              {paragraph}
            </p>
          ))}
        </div>
      </section>
    </Reveal>
  );
}
