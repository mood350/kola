import type { Metadata } from "next";
import Link from "next/link";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { ContactForm } from "@/components/sections/contact-form";
import { CoverageMap } from "@/components/sections/coverage-map";
import { ResponseTime } from "@/components/ui/response-time";
import { CONTACT_CHANNELS, CONTACT_FAQ } from "@/lib/content";

export const metadata: Metadata = {
  title: "Contact",
  description:
    "Contacter Kola : support utilisateur, presse et partenariats, formulaire direct. Réponse sous deux jours ouvrés, zone UEMOA.",
  alternates: { canonical: "/contact" },
};

export default function ContactPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Nous joindre"
        title="Une question ? Écrivez-nous."
        lead="Support utilisateur, presse, partenariats marchands : voici comment nous atteindre, et sous quel délai vous aurez une réponse."
        crumbs={[{ label: "Contact", href: "/contact" }]}
      />

      <Section className="pt-0">
        <Container>
          {/* La promesse de délai est le premier bloc de la page, avant même
              les adresses. C'est l'objection qui bloque l'envoi d'un message —
              non pas « où écrire », mais « est-ce que quelqu'un lira ». Placée
              après le formulaire, elle arriverait une fois la décision prise. */}
          <Reveal>
            <ResponseTime variant="panel" />
          </Reveal>

          <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)]">
            {/* Canaux directs, avant le formulaire : beaucoup de visiteurs
                préfèrent leur propre client mail, et une adresse visible évite
                de faire remplir un formulaire pour rien. */}
            <div className="flex flex-col gap-4">
              <RevealGroup className="flex flex-col gap-4">
                {CONTACT_CHANNELS.map((channel) => (
                  <a
                    key={channel.id}
                    href={channel.href}
                    data-animate
                    className="group rounded-card bg-surface p-7 shadow-soft transition-transform duration-200 ease-[var(--ease-editorial)] hover:-translate-y-0.5"
                  >
                    <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                      {channel.label}
                    </p>
                    <p className="font-display mt-2.5 flex items-center gap-2 text-lg font-semibold text-kola-700">
                      {channel.value}
                      <svg
                        viewBox="0 0 16 16"
                        fill="none"
                        aria-hidden="true"
                        className="h-4 w-4 transition-transform duration-200 ease-[var(--ease-editorial)] group-hover:translate-x-1"
                      >
                        <path
                          d="M2 8h11M9 4l4 4-4 4"
                          stroke="currentColor"
                          strokeWidth="1.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </p>
                    <p className="mt-2 text-[0.875rem] leading-relaxed text-ink-600">
                      {channel.note}
                    </p>
                  </a>
                ))}
              </RevealGroup>

              <Reveal delay={0.1}>
                <div className="rounded-card bg-sunken p-7">
                  <h2 className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                    Avant d&apos;écrire
                  </h2>
                  <dl className="mt-5 flex flex-col gap-5">
                    {CONTACT_FAQ.map((item) => (
                      <div key={item.q}>
                        <dt className="text-[0.9375rem] font-semibold text-ink-950">
                          {item.q}
                        </dt>
                        <dd className="mt-1.5 text-[0.875rem] leading-relaxed text-ink-600">
                          {item.a}
                        </dd>
                      </div>
                    ))}
                  </dl>
                  <p className="mt-6 border-t border-ink-200 pt-5 text-[0.875rem] leading-relaxed text-ink-600">
                    Les questions sur le fonctionnement du score, les montants
                    empruntables et la sécurité du compte sont traitées dans{" "}
                    <Link
                      href="/#faq"
                      className="font-medium text-kola-700 underline underline-offset-2 transition-colors hover:text-kola-600"
                    >
                      les questions fréquentes
                    </Link>
                    .
                  </p>
                </div>
              </Reveal>
            </div>

            {/* `scroll-mt-28` compense l'en-tête fixe : la barre d'action fixe
                sur mobile pointe vers cette ancre, et sans marge de défilement
                le titre du formulaire se retrouverait sous la navigation. */}
            <div id="formulaire" className="scroll-mt-28">
              <Reveal delay={0.05}>
                <ContactForm />
              </Reveal>
            </div>
          </div>

          <Reveal delay={0.1}>
            <div className="mt-4">
              <CoverageMap />
            </div>
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}
