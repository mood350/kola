import Link from "next/link";
import { KolaLogo } from "@/components/ui/kola-logo";
import { Container } from "@/components/ui/section";
import { ResponseTime } from "@/components/ui/response-time";
import {
  FOOTER_DISCOVER,
  FOOTER_EXPLORE,
  FOOTER_LEGAL,
  CONTACT_EMAIL,
} from "@/lib/content";
import { COVERAGE } from "@/lib/business";

/**
 * Pied de page.
 *
 * IL PORTE L'ESSENTIEL DU MAILLAGE INTERNE DU SITE. Présent sur toutes les
 * routes, c'est par lui qu'un robot d'indexation atteint les pages profondes
 * depuis n'importe quel point d'entrée, et par lui qu'un visiteur arrivé en bas
 * d'une page sans avoir trouvé son sujet repart ailleurs plutôt que de fermer
 * l'onglet.
 *
 * Trois colonnes pour trois intentions séparées — parcourir l'accueil, changer
 * de page, consulter un document légal — plutôt qu'une liste unique où rien ne
 * se distingue. Les listes viennent de `lib/content.ts` : ajouter une page là
 * la fait apparaître ici, ce qui évite la page orpheline que personne ne pense
 * à relier.
 *
 * `pb-32 lg:pb-14` : la barre d'action fixe recouvre le bas de l'écran sur
 * mobile. Sans cette réserve, elle masque la dernière ligne du pied de page —
 * en pratique la mention légale.
 */
export function SiteFooter() {
  return (
    <footer className="border-t border-hairline bg-surface pt-16 pb-32 lg:pb-12">
      <Container>
        <div className="flex flex-col gap-12 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-xs">
            <div className="flex items-center gap-2.5">
              <KolaLogo className="h-7 w-7" />
              <span className="font-headline text-xl font-semibold tracking-tight text-ink-950">
                Kola
              </span>
            </div>
            <p className="mt-4 text-sm leading-relaxed text-ink-500">
              Portefeuille mobile money et score de confiance, conçus pour
              l&apos;Afrique de l&apos;Ouest. Montants en francs CFA (XOF).
            </p>

            <a
              href={`mailto:${CONTACT_EMAIL}`}
              className="mt-6 inline-flex items-center gap-2 rounded-full border border-hairline bg-mist px-4 py-2 text-sm font-medium text-ink-800 transition-colors hover:border-kola-200 hover:text-kola-700"
            >
              <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" className="h-4 w-4 text-kola-600">
                <rect x="2" y="3.5" width="12" height="9" rx="2" stroke="currentColor" strokeWidth="1.4" />
                <path d="m2.5 4.5 5.5 4 5.5-4" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
              </svg>
              {CONTACT_EMAIL}
            </a>

            <ResponseTime className="mt-6" />
          </div>

          <div className="grid gap-10 sm:grid-cols-3 sm:gap-12 lg:gap-16">
            <nav aria-label="Sections du site">
              <h2 className="text-[0.8125rem] font-semibold text-ink-950">
                Découvrir
              </h2>
              <ul className="mt-4 space-y-2.5">
                {FOOTER_DISCOVER.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-ink-500 transition-colors hover:text-ink-950"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>

            <nav aria-label="Pages du site">
              <h2 className="text-[0.8125rem] font-semibold text-ink-950">
                Explorer
              </h2>
              <ul className="mt-4 space-y-2.5">
                {FOOTER_EXPLORE.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-ink-500 transition-colors hover:text-ink-950"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>

            <nav aria-label="Informations légales">
              <h2 className="text-[0.8125rem] font-semibold text-ink-950">
                Légal
              </h2>
              <ul className="mt-4 space-y-2.5">
                {FOOTER_LEGAL.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-ink-500 transition-colors hover:text-ink-950"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          </div>
        </div>

        {/* Zone desservie, en toutes lettres. Elle répond à la question qu'un
            visiteur se pose avant toute autre — « est-ce que ça marche chez
            moi ? » — et elle dit la même chose que le champ `areaServed` des
            données structurées, les deux venant de la même constante. */}
        <p className="mt-12 border-t border-hairline pt-8 text-xs leading-relaxed text-ink-400">
          Service disponible dans les huit États de l&apos;UEMOA :{" "}
          {COVERAGE.map((country) => country.name).join(", ")}.
        </p>

        <p className="mt-3 text-xs text-ink-400">
          © {new Date().getFullYear()} Kola. Les montants, plafonds et taux
          présentés sont indicatifs et dépendent de votre niveau de
          vérification.
        </p>
      </Container>
    </footer>
  );
}
