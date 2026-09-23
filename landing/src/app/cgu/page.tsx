import type { Metadata } from "next";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { CGU_ARTICLES, CGU_UPDATED_AT } from "@/lib/legal";

export const metadata: Metadata = {
  title: "Conditions générales d'utilisation",
  description:
    "Les conditions générales d'utilisation du service Kola : compte, plafonds, score de confiance, crédit, sécurité et données personnelles.",
  alternates: { canonical: "/cgu" },
  robots: { index: true, follow: true },
};

export default function CguPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Informations légales"
        title="Conditions générales d'utilisation"
        lead="Ce document décrit les règles d'accès et d'usage du service Kola : ouverture de compte, plafonds, fonctionnement du score de confiance, conditions de crédit et traitement de vos données."
        meta={`Dernière mise à jour : ${CGU_UPDATED_AT}`}
        crumbs={[{ label: "Conditions générales", href: "/cgu" }]}
      />

      <Section className="pt-0">
        <Container>
          <div className="grid gap-12 lg:grid-cols-[minmax(0,16rem)_minmax(0,1fr)] lg:gap-16">
            {/* Sommaire — collant en desktop.
                Ancré sur la page et non dans un panneau : ici la colonne est
                libre sur toute la hauteur du document, `sticky` suit donc bien
                la lecture. */}
            <nav
              aria-label="Sommaire"
              className="lg:sticky lg:top-28 lg:self-start"
            >
              <h2 className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                Sommaire
              </h2>
              <ol className="mt-4 flex flex-col gap-2.5">
                {CGU_ARTICLES.map((article, index) => (
                  <li key={article.id}>
                    <a
                      href={`#${article.id}`}
                      className="flex gap-3 text-[0.875rem] leading-snug text-ink-600 transition-colors hover:text-kola-600"
                    >
                      <span className="shrink-0 tabular-nums text-ink-400">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                      {article.title}
                    </a>
                  </li>
                ))}
              </ol>
            </nav>

            <div>
              <RevealGroup className="flex flex-col gap-4">
                {CGU_ARTICLES.map((article, index) => (
                  <article
                    key={article.id}
                    id={article.id}
                    data-animate
                    className="scroll-mt-28 rounded-card bg-surface p-7 shadow-soft sm:p-9"
                  >
                    <p className="font-display text-[0.8125rem] font-semibold text-kola-500 tabular-nums">
                      {String(index + 1).padStart(2, "0")}
                    </p>
                    <h2 className="mt-2 text-xl font-semibold">{article.title}</h2>

                    <div className="mt-4 flex flex-col gap-3.5">
                      {article.body.map((paragraph) => (
                        <p
                          key={paragraph.slice(0, 40)}
                          className="text-[0.9375rem] leading-relaxed text-ink-600"
                        >
                          {paragraph}
                        </p>
                      ))}
                    </div>

                    {article.list ? (
                      <ul className="mt-4 flex flex-col gap-2.5 border-t border-ink-200 pt-4">
                        {article.list.map((item) => (
                          <li
                            key={item}
                            className="flex gap-3 text-[0.9375rem] leading-relaxed text-ink-600"
                          >
                            <span
                              aria-hidden="true"
                              className="mt-2 h-1 w-1 shrink-0 rounded-full bg-ochre-400"
                            />
                            {item}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </article>
                ))}
              </RevealGroup>

              <Reveal delay={0.1}>
                <p className="mt-8 rounded-card border border-ochre-300 bg-ochre-200/40 p-6 text-[0.875rem] leading-relaxed text-ink-700">
                  <strong className="font-semibold text-ink-950">
                    Document de travail.
                  </strong>{" "}
                  Ce texte est une rédaction préparatoire. Il n&apos;a pas été
                  revu par un conseil juridique et ne constitue pas un engagement
                  contractuel opposable. Avant toute mise en service, il doit
                  être validé au regard de la réglementation applicable aux
                  établissements de paiement de l&apos;UEMOA et des exigences de
                  la BCEAO.
                </p>
              </Reveal>
            </div>
          </div>
        </Container>
      </Section>
    </main>
  );
}
