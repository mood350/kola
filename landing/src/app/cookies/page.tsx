import type { Metadata } from "next";
import { Container, Section } from "@/components/ui/section";
import { PageHeader } from "@/components/ui/page-header";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { ConsentControls } from "@/components/sections/consent-controls";
import { CONSENT_COOKIE, CONSENT_MAX_AGE_DAYS } from "@/lib/consent";

export const metadata: Metadata = {
  title: "Cookies",
  description:
    "Liste exhaustive des cookies déposés par le site Kola, leur finalité et leur durée de conservation.",
  alternates: { canonical: "/cookies" },
};

/**
 * Inventaire réel, pas un modèle générique. Chaque ligne correspond à un cookie
 * que le code dépose effectivement.
 *
 * LES DEUX COOKIES DE MESURE NE SONT PAS DÉPOSÉS PAR DÉFAUT. Ils n'existent que
 * si deux conditions sont réunies : `NEXT_PUBLIC_GA_ID` est configuré au
 * déploiement, ET le visiteur a explicitement accepté depuis le bandeau. En cas
 * de refus — ou tant qu'aucun choix n'est fait — le script de mesure n'est pas
 * même téléchargé (voir `components/analytics/google-analytics.tsx`). Ils sont
 * listés ici quand même : un inventaire qui ne mentionne que ce qui est déjà
 * posé sur la machine du lecteur ne lui permet pas de décider en connaissance
 * de cause, ce qui est précisément l'objet de cette page.
 */
const COOKIES = [
  {
    name: CONSENT_COOKIE,
    purpose:
      "Mémorise votre décision sur ce bandeau, pour ne pas vous la redemander à chaque page.",
    kind: "Strictement nécessaire",
    duration: `${CONSENT_MAX_AGE_DAYS} jours`,
    consent: "Exempt de consentement",
  },
  {
    name: "_ga",
    purpose:
      "Distingue les visiteurs les uns des autres pour compter les visites. Déposé par Google Analytics, uniquement après acceptation.",
    kind: "Mesure d'audience",
    duration: "13 mois",
    consent: "Consentement requis",
  },
  {
    name: "_ga_*",
    purpose:
      "Conserve l'état de la session de mesure en cours. Le suffixe correspond à l'identifiant de la propriété Analytics. Déposé uniquement après acceptation.",
    kind: "Mesure d'audience",
    duration: "13 mois",
    consent: "Consentement requis",
  },
] as const;

export default function CookiesPage() {
  return (
    <main id="contenu">
      <PageHeader
        eyebrow="Informations légales"
        title="Cookies"
        lead="Ce site ne dépose aucun cookie publicitaire ni traceur de reciblage. Seule une mesure d'audience existe, et elle n'est chargée qu'après votre acceptation explicite. Voici la liste complète de ce qui peut être enregistré sur votre appareil."
        crumbs={[{ label: "Cookies", href: "/cookies" }]}
      />

      <Section className="pt-0">
        <Container>
          <RevealGroup className="flex flex-col gap-4">
            {COOKIES.map((cookie) => (
              <article
                key={cookie.name}
                data-animate
                className="rounded-card bg-surface p-7 shadow-soft sm:p-9"
              >
                <p className="font-display text-lg font-semibold text-kola-700">
                  {cookie.name}
                </p>
                <p className="mt-3 text-[0.9375rem] leading-relaxed text-ink-600">
                  {cookie.purpose}
                </p>
                <dl className="mt-5 grid gap-x-8 gap-y-4 border-t border-ink-200 pt-5 sm:grid-cols-3">
                  {[
                    ["Catégorie", cookie.kind],
                    ["Conservation", cookie.duration],
                    ["Base légale", cookie.consent],
                  ].map(([label, value]) => (
                    <div key={label}>
                      <dt className="text-[0.6875rem] font-medium tracking-[0.14em] text-ink-400 uppercase">
                        {label}
                      </dt>
                      <dd className="mt-1.5 text-[0.875rem] font-medium text-ink-900">
                        {value}
                      </dd>
                    </div>
                  ))}
                </dl>
              </article>
            ))}
          </RevealGroup>

          <Reveal delay={0.08}>
            <div className="mt-4 rounded-card bg-sunken p-7 sm:p-9">
              <h2 className="text-lg font-semibold">Ce que nous n&apos;utilisons pas</h2>
              <ul className="mt-4 flex flex-col gap-2.5">
                {[
                  "Aucun pixel publicitaire ni cookie de reciblage.",
                  "Aucun bouton de réseau social déposant un traceur.",
                  "Aucune police chargée depuis un domaine tiers : elles sont auto-hébergées.",
                  "Aucune carte embarquée au chargement : elle n'est insérée qu'après un clic explicite de votre part.",
                  "Aucun signal publicitaire dans la mesure d'audience : la personnalisation et les signaux Google sont désactivés, et l'adresse IP est anonymisée.",
                ].map((item) => (
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
              <p className="mt-5 text-[0.875rem] leading-relaxed text-ink-500">
                Le refus n&apos;est pas un réglage appliqué après coup : tant
                que vous n&apos;avez pas accepté, le script de mesure n&apos;est
                pas téléchargé du tout. Aucune requête n&apos;est adressée à
                Google, et votre adresse IP ne lui est donc jamais transmise.
                Revenir sur une acceptation empêche tout nouveau chargement ;
                les cookies déjà posés se suppriment depuis les réglages de
                votre navigateur.
              </p>
            </div>
          </Reveal>

          <Reveal delay={0.12}>
            <ConsentControls />
          </Reveal>
        </Container>
      </Section>
    </main>
  );
}
