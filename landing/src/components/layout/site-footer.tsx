import Link from "next/link";
import { KolaLogo } from "@/components/ui/kola-logo";
import { Container } from "@/components/ui/section";
import { ResponseTime } from "@/components/ui/response-time";
import {
  FOOTER_DISCOVER,
  FOOTER_EXPLORE,
  FOOTER_LEGAL,
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
    <footer className="hairline-t bg-canvas pt-14 pb-32 lg:pb-14">
      <Container>
        <div className="flex flex-col gap-12 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-xs">
            <div className="flex items-center gap-2.5">
              <KolaLogo className="h-7 w-7" />
              <span className="font-display text-lg font-semibold tracking-tight text-ink-950">
                Kola
              </span>
            </div>
            <p className="mt-4 text-sm leading-relaxed text-ink-500">
              Portefeuille mobile money et score de confiance, conçus pour
              l&apos;Afrique de l&apos;Ouest. Montants en francs CFA (XOF).
            </p>

            <ResponseTime className="mt-6" />
          </div>

          <div className="grid gap-10 sm:grid-cols-3 sm:gap-12 lg:gap-16">
            <nav aria-label="Sections du site">
              <h2 className="text-[0.8125rem] font-medium tracking-[0.12em] text-ink-400 uppercase">
                Découvrir
              </h2>
              <ul className="mt-4 space-y-2.5">
                {FOOTER_DISCOVER.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-ink-600 transition-colors hover:text-kola-600"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>

            <nav aria-label="Pages du site">
              <h2 className="text-[0.8125rem] font-medium tracking-[0.12em] text-ink-400 uppercase">
                Explorer
              </h2>
              <ul className="mt-4 space-y-2.5">
                {FOOTER_EXPLORE.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-ink-600 transition-colors hover:text-kola-600"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>

            <nav aria-label="Informations légales">
              <h2 className="text-[0.8125rem] font-medium tracking-[0.12em] text-ink-400 uppercase">
                Légal
              </h2>
              <ul className="mt-4 space-y-2.5">
                {FOOTER_LEGAL.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-ink-600 transition-colors hover:text-kola-600"
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
        <p className="mt-12 border-t border-ink-200 pt-8 text-xs leading-relaxed text-ink-400">
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
