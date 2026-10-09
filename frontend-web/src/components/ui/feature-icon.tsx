/**
 * Jeu d'icônes du site — SVG au trait, jamais d'emoji.
 *
 * Un emoji change de dessin selon la plateforme, ne suit pas la couleur du
 * texte et se fait annoncer par les lecteurs d'écran. Ces tracés partagent la
 * même grille 20×20 et la même graisse (1.6), condition pour qu'une rangée
 * d'icônes paraisse homogène.
 */

const PATHS: Record<string, React.ReactNode> = {
  wallet: (
    <>
      <path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H15a2 2 0 0 1 2 2v1" />
      <path d="M3 7.5V15a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-1.5" />
      <path d="M17 8.5h-3a2 2 0 0 0 0 4h3a.5.5 0 0 0 .5-.5v-3a.5.5 0 0 0-.5-.5Z" />
    </>
  ),
  transfert: (
    <>
      <path d="M3 7h11M11 4l3 3-3 3" />
      <path d="M17 13H6M9 10l-3 3 3 3" />
    </>
  ),
  coffres: (
    <>
      <rect x="3" y="4" width="14" height="12" rx="2" />
      <circle cx="10" cy="10" r="3" />
      <path d="M10 7v1.5M17 8v4" />
    </>
  ),
  programmes: (
    <>
      <circle cx="10" cy="10" r="7" />
      <path d="M10 6v4l2.5 1.5" />
    </>
  ),
  marchands: (
    <>
      <rect x="3" y="3" width="6" height="6" rx="1.5" />
      <rect x="11" y="3" width="6" height="6" rx="1.5" />
      <rect x="3" y="11" width="6" height="6" rx="1.5" />
      <path d="M11 11h2.5v2.5M17 15.5V17h-1.5M13.5 17H11v-1" />
    </>
  ),
  credit: (
    <>
      <path d="M3 10.5 10 3l7 7.5" />
      <path d="M10 17V9.5" />
      <path d="M6.5 13.5 10 17l3.5-3.5" />
    </>
  ),
};

export function FeatureIcon({ id }: { id: string }) {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="h-5 w-5"
    >
      {PATHS[id] ?? PATHS.wallet}
    </svg>
  );
}
