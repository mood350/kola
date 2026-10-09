import Link from "next/link";
import { cn } from "@/lib/cn";
import { JsonLd } from "@/components/ui/json-ld";
import { breadcrumbSchema, type Crumb } from "@/lib/schema";

/**
 * Fil d'Ariane.
 *
 * Il fait deux choses distinctes, et les deux comptent :
 *
 * 1. POUR LE VISITEUR — il situe la page dans le site. C'est décisif pour qui
 *    arrive depuis un moteur sur une page profonde (une étude de cas, par
 *    exemple) : sans lui, la seule remontée possible est le bouton « retour »
 *    du navigateur, qui ramène au moteur et non au site.
 *
 * 2. POUR LES MOTEURS — le balisage `BreadcrumbList` remplace l'URL brute par
 *    ce chemin sous le titre du résultat de recherche. « kola.africa › Études
 *    de cas › Commerce de détail » se lit mieux qu'une URL, et fait apparaître
 *    la structure du site dans la page de résultats.
 *
 * ACCESSIBILITÉ
 *
 * Le séparateur est un pseudo-contenu CSS et non un caractère dans le texte :
 * inséré dans le flux, un lecteur d'écran annoncerait « barre oblique » entre
 * chaque niveau. La page courante est un `<span>` porteur de
 * `aria-current="page"` et non un lien — proposer un lien vers l'endroit où
 * l'on se trouve déjà est un piège classique de la navigation au clavier.
 *
 * MISE EN PAGE : ce composant ne porte ni gouttière ni marge haute. Il est
 * destiné à être posé à l'intérieur d'un conteneur existant — en pratique
 * `PageHeader`, qui l'affiche au-dessus du sur-titre.
 *
 * Le premier maillon est toujours l'accueil : la liste passée en `items` ne
 * contient que ce qui vient après.
 */
export function Breadcrumbs({
  items,
  className,
}: {
  items: Crumb[];
  className?: string;
}) {
  const crumbs: Crumb[] = [{ label: "Accueil", href: "/" }, ...items];

  return (
    <>
      <JsonLd data={breadcrumbSchema(crumbs)} />

      <nav aria-label="Fil d'Ariane" className={className}>
        <ol className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[0.8125rem] text-ink-500">
          {crumbs.map((crumb, index) => {
            const isCurrent = index === crumbs.length - 1;

            return (
              <li
                key={crumb.href}
                className={cn(
                  "flex items-center",
                  /* Le chevron est porté par l'élément de liste, jamais par le
                     texte : purement ornemental, il ne doit être ni
                     sélectionnable ni annoncé. */
                  index > 0 &&
                    "before:mr-2 before:text-ink-300 before:content-['›']"
                )}
              >
                {isCurrent ? (
                  <span
                    aria-current="page"
                    className="font-medium text-ink-700"
                  >
                    {crumb.label}
                  </span>
                ) : (
                  <Link
                    href={crumb.href}
                    className="rounded transition-colors hover:text-kola-600"
                  >
                    {crumb.label}
                  </Link>
                )}
              </li>
            );
          })}
        </ol>
      </nav>
    </>
  );
}
