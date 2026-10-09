import type { Metadata } from "next";
import Link from "next/link";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { PRIVACY_SECTIONS, PRIVACY_UPDATED_AT } from "@/lib/privacy";
import { CONTACT } from "@/lib/business";

/**
 * Politique de confidentialité.
 *
 * Même gabarit que les CGU — sommaire collant à gauche, sections numérotées à
 * droite : ces deux documents se consultent de la même façon, par recherche
 * d'un point précis plutôt qu'en lecture continue, et doivent donc se
 * manipuler pareil.
 *
 * Elle est indexable et liée depuis le pied de page de chaque page du site. Un
 * document de confidentialité atteignable uniquement par son URL directe ne
 * remplit aucune de ses fonctions, ni juridique ni de confiance.
 */
export const metadata: Metadata = {
  title: "Politique de confidentialité",
  description:
    "Quelles données Kola collecte, pourquoi, combien de temps elles sont conservées, avec qui elles sont partagées, et comment exercer vos droits.",
  alternates: { canonical: "/confidentialite" },
  robots: { index: true, follow: true },
};

export default function PrivacyPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Informations légales"
        title="Politique de confidentialité"
        lead="Ce document décrit exactement ce que Kola enregistre, à quoi chaque donnée sert, combien de temps elle est conservée et comment reprendre la main dessus."
        meta={`Dernière mise à jour : ${PRIVACY_UPDATED_AT}`}
        crumbs={[
          { label: "Confidentialité", href: "/confidentialite" },
        ]}
      />

      <Section className="pt-0">
        <Container>
          <div className="grid gap-12 lg:grid-cols-[minmax(0,16rem)_minmax(0,1fr)] lg:gap-16">
            <nav
              aria-label="Sommaire"
              className="lg:sticky lg:top-28 lg:self-start"
            >
              <h2 className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                Sommaire
              </h2>
              <ol className="mt-4 flex flex-col gap-2.5">
                {PRIVACY_SECTIONS.map((section, index) => (
                  <li key={section.id}>
                    <a
                      href={`#${section.id}`}
                      className="flex gap-3 text-[0.875rem] leading-snug text-ink-600 transition-colors hover:text-kola-600"
                    >
                      <span className="shrink-0 tabular-nums text-ink-400">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                      {section.title}
                    </a>
                  </li>
                ))}
              </ol>

              <div className="mt-8 border-t border-ink-200 pt-6">
                <h2 className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                  Documents liés
                </h2>
                <ul className="mt-4 flex flex-col gap-2.5">
                  {[
                    { href: "/cgu", label: "Conditions générales" },
                    { href: "/cookies", label: "Cookies déposés" },
                    { href: "/contact", label: "Nous écrire" },
                  ].map((item) => (
                    <li key={item.href}>
                      <Link
                        href={item.href}
                        className="text-[0.875rem] text-ink-600 transition-colors hover:text-kola-600"
                      >
                        {item.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </nav>

            <div>
              <RevealGroup className="flex flex-col gap-4">
                {PRIVACY_SECTIONS.map((section, index) => (
                  <article
                    key={section.id}
                    id={section.id}
                    data-animate
                    className="scroll-mt-28 rounded-card bg-surface p-7 shadow-soft sm:p-9"
                  >
                    <p className="font-display text-[0.8125rem] font-semibold text-kola-500 tabular-nums">
                      {String(index + 1).padStart(2, "0")}
                    </p>
                    <h2 className="mt-2 text-xl font-semibold">
                      {section.title}
                    </h2>

                    <div className="mt-4 flex flex-col gap-3.5">
                      {section.body.map((paragraph) => (
                        <p
                          key={paragraph.slice(0, 40)}
                          className="text-[0.9375rem] leading-relaxed text-ink-600"
                        >
                          {paragraph}
                        </p>
                      ))}
                    </div>

                    {section.list ? (
                      <ul className="mt-4 flex flex-col gap-2.5 border-t border-ink-200 pt-4">
                        {section.list.map((item) => (
                          <li
                            key={item.slice(0, 40)}
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
                <div className="mt-4 rounded-card bg-sunken p-7 sm:p-9">
                  <h2 className="text-lg font-semibold">
                    Exercer vos droits
                  </h2>
                  <p className="mt-3 text-[0.9375rem] leading-relaxed text-ink-600">
                    Une seule adresse pour toutes les demandes relatives à vos
                    données — accès, rectification, effacement, portabilité,
                    réexamen humain d&apos;une décision de crédit.
                  </p>
                  <a
                    href={`mailto:${CONTACT.support}`}
                    className="mt-5 inline-flex font-medium text-kola-700 underline underline-offset-2 transition-colors hover:text-kola-600"
                  >
                    {CONTACT.support}
                  </a>
                </div>
              </Reveal>

              <Reveal delay={0.14}>
                <p className="mt-4 rounded-card border border-ochre-300 bg-ochre-200/40 p-6 text-[0.875rem] leading-relaxed text-ink-700">
                  <strong className="font-semibold text-ink-950">
                    Document de travail.
                  </strong>{" "}
                  Ce texte est une rédaction préparatoire. Il n&apos;a pas été
                  revu par un conseil juridique et ne constitue pas un engagement
                  opposable. Avant toute mise en service, il doit être validé au
                  regard des textes applicables à la protection des données dans
                  les États de l&apos;UEMOA, ainsi que des obligations propres
                  aux établissements de paiement supervisés par la BCEAO.
                </p>
              </Reveal>
            </div>
          </div>
        </Container>
      </Section>
    </main>
  );
}
