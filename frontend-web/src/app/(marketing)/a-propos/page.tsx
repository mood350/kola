import type { Metadata } from "next";
import Link from "next/link";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal } from "@/components/motion/reveal";
import { Button, ArrowRight } from "@/components/ui/button";
import { Team } from "@/components/sections/team";
import { ResponseTime } from "@/components/ui/response-time";
import { PROBLEM_LEAD } from "@/lib/content";

/**
 * Page « à propos ».
 *
 * Elle existe pour une raison précise : un service financier demande qu'on lui
 * confie de l'argent, et la première question d'un visiteur méfiant n'est pas
 * « comment ça marche » mais « qui est derrière ». Une page d'accueil ne peut
 * pas y répondre sans casser son propre argumentaire.
 *
 * Elle sert aussi de pivot de maillage : elle relie l'accueil aux études de
 * cas, au contact et aux pages légales, et reçoit en retour des liens depuis le
 * pied de page et la navigation.
 */
export const metadata: Metadata = {
  title: "À propos",
  description:
    "Pourquoi Kola existe, comment le score de confiance a été conçu, et qui construit le service. Portefeuille mobile money et micro-crédit en zone UEMOA.",
  alternates: { canonical: "/a-propos" },
};

const PRINCIPLES = [
  {
    title: "Un score qu'on peut décomposer",
    body: "Chaque critère est affiché avec son poids et le niveau atteint. Une décision de crédit qu'on ne peut pas expliquer est une décision qu'on ne peut pas contester — et c'est précisément ce que vit aujourd'hui quiconque se voit refuser un financement sans motif.",
  },
  {
    title: "Aucune donnée extérieure",
    body: "Le score ne se nourrit d'aucun fichier bancaire ni d'aucune source tierce. Il ne mesure que votre usage de Kola, ce qui permet de partir de zéro sans être pénalisé par un passé qu'on n'a jamais eu.",
  },
  {
    title: "Un historique qu'on ne supprime pas",
    body: "Portefeuilles, coffres et opérations sont désactivés, jamais effacés. Un registre financier tronqué est inexploitable en cas de litige — y compris, et surtout, pour se défendre.",
  },
  {
    title: "Des promesses que le code tient",
    body: "Les plafonds, taux et délais affichés sur ce site sont ceux que le moteur calcule réellement. Écrire ici un chiffre plus flatteur transformerait une page de présentation en fausse déclaration.",
  },
];

export default function AboutPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="À propos"
        title="Casser la boucle qui exclut la moitié d'un continent"
        lead={PROBLEM_LEAD}
        crumbs={[{ label: "À propos", href: "/a-propos" }]}
      />

      <Section className="pt-0">
        <Container>
          <Reveal>
            <div className="max-w-3xl">
              <h2 className="text-h2 font-semibold">Pourquoi Kola existe</h2>
              <div className="mt-6 flex flex-col gap-4 text-[1.0625rem] leading-relaxed text-ink-600">
                <p>
                  Le blocage n&apos;est pas l&apos;absence de revenus : des
                  millions de personnes gagnent leur vie, régulièrement, depuis
                  des années. Le blocage est l&apos;absence de{" "}
                  <strong className="font-medium text-ink-900">trace</strong>.
                  Une activité réglée en espèces ne laisse rien qu&apos;un
                  organisme de crédit puisse lire, et ce vide est interprété
                  comme un risque.
                </p>
                <p>
                  D&apos;où le raisonnement inverse : plutôt que de demander un
                  historique en préalable, produire cet historique à partir de
                  l&apos;usage. Chaque dépôt, chaque épargne bloquée, chaque
                  paiement encaissé devient une observation. Huit critères les
                  agrègent en une note de 0 à 100, et cette note — et elle seule
                  — détermine le plafond et le taux.
                </p>
                <p>
                  Le point de bascule tient en une phrase : ce n&apos;est plus
                  le passé bancaire qui ouvre le crédit, c&apos;est le
                  comportement présent.
                </p>
              </div>
            </div>
          </Reveal>

          <div className="mt-14 grid gap-px overflow-hidden rounded-card bg-ink-200 sm:grid-cols-2">
            {PRINCIPLES.map((principle) => (
              <Reveal key={principle.title} className="bg-surface p-7 sm:p-9">
                <h3 className="font-display text-lg font-semibold text-ink-950">
                  {principle.title}
                </h3>
                <p className="mt-3 text-[0.9375rem] leading-relaxed text-ink-600">
                  {principle.body}
                </p>
              </Reveal>
            ))}
          </div>
        </Container>
      </Section>

      <Team />

      <Section className="bg-sunken">
        <Container>
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
            <Reveal>
              <ResponseTime variant="panel" />
            </Reveal>

            <Reveal delay={0.05}>
              <div className="rounded-card bg-surface p-7 shadow-soft sm:p-9">
                <h2 className="font-display text-lg font-semibold text-ink-950">
                  Aller plus loin
                </h2>
                <ul className="mt-5 flex flex-col gap-3.5">
                  {[
                    {
                      href: "/etudes-de-cas",
                      label: "Les études de cas",
                      note: "Le score suivi de bout en bout sur trois parcours.",
                    },
                    {
                      href: "/#score",
                      label: "Le barème du score",
                      note: "Les huit critères, leur poids, les quatre paliers.",
                    },
                    {
                      href: "/confidentialite",
                      label: "Politique de confidentialité",
                      note: "Ce qui est collecté, pourquoi, et pour combien de temps.",
                    },
                    {
                      href: "/cgu",
                      label: "Conditions générales",
                      note: "Compte, plafonds, crédit, sécurité.",
                    },
                  ].map((item) => (
                    <li key={item.href}>
                      <Link href={item.href} className="group flex flex-col">
                        <span className="flex items-center gap-2 text-[0.9375rem] font-medium text-kola-700 transition-colors group-hover:text-kola-600">
                          {item.label}
                          <ArrowRight />
                        </span>
                        <span className="text-[0.875rem] text-ink-500">
                          {item.note}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </Reveal>
          </div>

          <Reveal delay={0.1}>
            <div className="mt-10 flex flex-col gap-3 sm:flex-row">
              <Button href="/#cta" size="lg" className="w-full sm:w-auto">
                Ouvrir un compte
                <ArrowRight />
              </Button>
              <Button
                href="/contact"
                variant="secondary"
                size="lg"
                className="w-full sm:w-auto"
              >
                Nous écrire
              </Button>
            </div>
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}
