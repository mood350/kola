import { twMerge } from "tailwind-merge";

/**
 * Marque Kola.
 *
 * Rendue en SVG inline plutôt que par un fichier dans `public/images/logo/` —
 * comme le fait TailAdmin d'origine — pour trois raisons :
 *
 *  • `currentColor` : la même marque sert sur le rail clair et sur fond sombre
 *    sans qu'il faille maintenir deux fichiers (`logo.svg` / `logo-dark.svg`)
 *    et les basculer à la classe `dark:` ;
 *  • aucune requête réseau, aucun décalage de mise en page au chargement ;
 *  • le libellé « Kola » est du VRAI texte HTML, pas un tracé : il reste
 *    sélectionnable, lisible par un lecteur d'écran, et ne dépend pas d'une
 *    police embarquée dans le SVG.
 *
 * Trois disques en rosace : simplification du logo complet, dont les courbes
 * sont indiscernables de cercles aux tailles utilisées ici (24–32 px).
 */
export function KolaMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      fill="currentColor"
      aria-hidden="true"
      className={twMerge("shrink-0", className)}
    >
      <circle cx="12" cy="9" r="7.5" />
      <circle cx="23" cy="15" r="7.5" opacity="0.7" />
      <circle cx="11" cy="22" r="7.5" opacity="0.85" />
    </svg>
  );
}

/**
 * Bloc de marque complet : symbole + nom + qualificatif.
 *
 * `compact` sert le rail replié, où seul le symbole tient dans les 90 px de
 * large que laisse TailAdmin.
 */
export function KolaBrand({
  compact = false,
  className,
}: {
  compact?: boolean;
  className?: string;
}) {
  return (
    <span className={twMerge("flex items-center gap-2.5", className)}>
      <KolaMark className="h-8 w-8 text-brand-500" />
      {!compact ? (
        <span className="flex flex-col leading-none">
          <span className="text-lg font-semibold text-gray-900 dark:text-white/90">
            Kola
          </span>
          <span className="mt-0.5 text-theme-xs text-gray-500 dark:text-gray-400">
            Console d&apos;administration
          </span>
        </span>
      ) : null}
    </span>
  );
}
