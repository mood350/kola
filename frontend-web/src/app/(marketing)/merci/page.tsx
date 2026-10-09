import type { Metadata } from "next";
import Link from "next/link";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { Button, ArrowRight } from "@/components/ui/button";
import { ResponseTime } from "@/components/ui/response-time";
import { CONTACT, RESPONSE_PROMISE } from "@/lib/business";
import { CASE_STUDIES } from "@/lib/case-studies";

/**
 * Page de remerciement, atteinte après l'envoi du formulaire de contact.
 *
 * POURQUOI UNE PAGE ET NON UN MESSAGE EN LIGNE. Un bandeau « message envoyé »
 * affiché sous le formulaire disparaît au premier changement de page et ne
 * laisse aucune trace mesurable. Une URL distincte, elle :
 *
 *   - constitue un objectif de conversion propre — une visite de `/merci`
 *     correspond exactement à un formulaire soumis, sans avoir à instrumenter
 *     un clic ;
 *   - offre l'espace de RÉPONDRE À LA QUESTION SUIVANTE. Quelqu'un qui vient
 *     d'écrire se demande quand il aura une réponse et quoi faire en attendant.
 *     C'est le moment le plus disponible de toute la visite, et le laisser sur
 *     une confirmation vide le gaspille ;
 *   - donne un point de retour stable : rafraîchir la page ne renvoie pas le
 *     message, contrairement à un rechargement après soumission.
 *
 * `noindex` : cette page n'a de sens qu'atteinte après un envoi. Indexée, elle
 * capterait un trafic arrivant sur une confirmation sans objet et fausserait la
 * mesure — chaque visite directe compterait comme un message reçu. Elle est
 * pour la même raison exclue du sitemap et interdite d'exploration dans
 * `robots.ts`.
 */
export const metadata: Metadata = {
  title: "Message bien reçu",
  description:
    "Votre message a été transmis à l'équipe Kola. Réponse sous deux jours ouvrés.",
  robots: { index: false, follow: true },
};

export default function MerciPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Message envoyé"
        title="C'est reçu. Merci."
        lead="Votre message est arrivé jusqu'à l'équipe. Vous n'avez rien d'autre à faire — nous revenons vers vous par e-mail."
        crumbs={[
          { label: "Contact", href: "/contact" },
          { label: "Message envoyé", href: "/merci" },
        ]}
      />

      <Section className="pt-0">
        <Container>
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)]">
            <Reveal>
              <ResponseTime variant="panel" />
            </Reveal>

            <Reveal delay={0.05}>
              <div className="rounded-card bg-surface/50 p-7 sm:p-9">
                <h2 className="text-lg font-semibold">
                  Si c&apos;est urgent
                </h2>
                <p className="mt-3 text-[0.9375rem] leading-relaxed text-ink-500">
                  Un compte verrouillé se débloque seul au bout de trente
                  minutes, sans intervention de notre part. Pour une opération
                  contestée, écrivez directement au support en indiquant la
                  référence de la transaction, au format KLA-AAAA-XXXXXXXX.
                </p>
                <a
                  href={`mailto:${CONTACT.support}`}
                  className="mt-5 inline-flex font-medium text-kola-700 underline underline-offset-2 transition-colors hover:text-kola-600"
                >
                  {CONTACT.support}
                </a>
                <p className="mt-5 border-t border-hairline pt-4 text-[0.8125rem] text-ink-400">
                  {RESPONSE_PROMISE.hours}
                </p>
              </div>
            </Reveal>
          </div>

          {/* En attendant la réponse — le moment le plus propice pour montrer
              le produit à quelqu'un qui vient de manifester son intérêt. */}
          <Reveal delay={0.1}>
            <h2 className="font-headline mt-16 text-h2 font-semibold">
              En attendant notre réponse
            </h2>
          </Reveal>

          <RevealGroup className="mt-8 grid gap-px overflow-hidden rounded-card bg-ink-200 sm:grid-cols-3">
            {CASE_STUDIES.map((study) => (
              <Link
                key={study.slug}
                href={`/etudes-de-cas/${study.slug}`}
                data-animate
                className="group bg-surface p-7 transition-colors duration-200 hover:bg-kola-50"
              >
                <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                  {study.sector}
                </p>
                <p className="font-headline mt-2.5 text-[1.0625rem] leading-snug font-semibold text-ink-950">
                  {study.headline}
                </p>
                <p className="mt-3 flex items-center gap-2 text-[0.875rem] font-medium text-kola-700">
                  Lire l&apos;étude
                  <ArrowRight />
                </p>
              </Link>
            ))}
          </RevealGroup>

          <Reveal delay={0.15}>
            <div className="mt-10 flex flex-col gap-3 sm:flex-row">
              <Button href="/#cta" size="lg" className="w-full sm:w-auto">
                Ouvrir un compte
                <ArrowRight />
              </Button>
              <Button
                href="/#faq"
                variant="secondary"
                size="lg"
                className="w-full sm:w-auto"
              >
                Lire les questions fréquentes
              </Button>
            </div>
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}
