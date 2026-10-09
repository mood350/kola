/**
 * Illustrations du parcours produit.
 *
 * Dessinées en SVG et non générées en image : elles pèsent quelques
 * centaines d'octets, restent nettes sur écran haute densité, suivent les
 * tokens de couleur de la marque, et n'imposent aucun décalage de mise en
 * page au chargement.
 *
 * Elles portent le sens là où le texte était trop long : chacune montre une
 * étape du parcours plutôt que de la décrire. Elles restent malgré tout
 * décoratives — l'étape est toujours écrite à côté — donc masquées aux
 * technologies d'assistance.
 *
 * Toutes partagent le même viewBox 200×150 et le même vocabulaire graphique
 * (aplats lavande, traits indigo, une seule touche d'ocre) pour se lire comme
 * une série et non comme trois dessins juxtaposés.
 */

type Props = { className?: string };

const BASE = "h-auto w-full";

/** Étape 1 — l'usage quotidien : des mouvements réguliers sur le portefeuille. */
export function UsageIllustration({ className }: Props) {
  return (
    <svg
      viewBox="0 0 200 150"
      fill="none"
      aria-hidden="true"
      className={`${BASE} ${className ?? ""}`}
    >
      {/* Portefeuille */}
      <rect x="26" y="52" width="92" height="62" rx="12" fill="var(--color-kola-100)" />
      <rect
        x="26"
        y="52"
        width="92"
        height="62"
        rx="12"
        stroke="var(--color-kola-600)"
        strokeWidth="2.5"
      />
      <path d="M26 72h92" stroke="var(--color-kola-600)" strokeWidth="2.5" />
      <circle cx="100" cy="93" r="7" fill="var(--color-kola-600)" />

      {/* Mouvements entrants, réguliers et croissants */}
      <rect x="132" y="86" width="12" height="28" rx="4" fill="var(--color-kola-300)" />
      <rect x="150" y="70" width="12" height="44" rx="4" fill="var(--color-kola-500)" />
      <rect x="168" y="54" width="12" height="60" rx="4" fill="var(--color-kola-600)" />

      {/* Jeton entrant */}
      <circle cx="52" cy="30" r="14" fill="var(--color-ochre-300)" />
      <circle cx="52" cy="30" r="14" stroke="var(--color-ochre-500)" strokeWidth="2.5" />
      {/* Flèche vers le bas : le jeton entre dans le portefeuille. Un symbole
          monétaire à cette taille se réduisait à un gribouillis illisible. */}
      <path
        d="M52 23v13M46.5 30.5 52 36l5.5-5.5"
        stroke="var(--color-ochre-500)"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M52 44v6"
        stroke="var(--color-kola-400)"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeDasharray="2 5"
      />
    </svg>
  );
}

/** Étape 2 — le score : l'arc se remplit à mesure que l'usage s'accumule. */
export function ScoreIllustration({ className }: Props) {
  return (
    <svg
      viewBox="0 0 200 150"
      fill="none"
      aria-hidden="true"
      className={`${BASE} ${className ?? ""}`}
    >
      {/* Piste */}
      <path
        d="M40 112a60 60 0 0 1 120 0"
        stroke="var(--color-kola-100)"
        strokeWidth="16"
        strokeLinecap="round"
      />
      {/* Part acquise — environ 78 % de l'arc */}
      <path
        d="M40 112a60 60 0 0 1 106-38"
        stroke="var(--color-kola-600)"
        strokeWidth="16"
        strokeLinecap="round"
      />
      {/* Valeur */}
      <text
        x="100"
        y="104"
        textAnchor="middle"
        fill="var(--color-ink-950)"
        fontSize="34"
        fontWeight="600"
        fontFamily="var(--font-display)"
      >
        78
      </text>
      {/* Repère de progression */}
      <circle cx="146" cy="74" r="7" fill="var(--color-ochre-400)" />
      <circle cx="146" cy="74" r="7" stroke="var(--color-surface)" strokeWidth="3" />
    </svg>
  );
}

/** Étape 3 — le crédit : le verrou s'ouvre, le plafond apparaît. */
export function CreditIllustration({ className }: Props) {
  return (
    <svg
      viewBox="0 0 200 150"
      fill="none"
      aria-hidden="true"
      className={`${BASE} ${className ?? ""}`}
    >
      {/* Anse ouverte, basculée vers la gauche */}
      <path
        d="M74 74V56a20 20 0 0 0-40 0v10"
        stroke="var(--color-kola-400)"
        strokeWidth="8"
        strokeLinecap="round"
      />
      {/* Corps du cadenas */}
      <rect x="62" y="72" width="76" height="58" rx="14" fill="var(--color-kola-600)" />
      <circle cx="100" cy="96" r="7" fill="var(--color-kola-100)" />
      <path
        d="M100 103v11"
        stroke="var(--color-kola-100)"
        strokeWidth="5"
        strokeLinecap="round"
      />

      {/* Montant débloqué */}
      <rect x="86" y="22" width="88" height="34" rx="10" fill="var(--color-surface)" />
      <rect
        x="86"
        y="22"
        width="88"
        height="34"
        rx="10"
        stroke="var(--color-kola-200)"
        strokeWidth="2"
      />
      <text
        x="130"
        y="45"
        textAnchor="middle"
        fill="var(--color-ink-950)"
        fontSize="17"
        fontWeight="600"
        fontFamily="var(--font-display)"
      >
        500 000
      </text>
    </svg>
  );
}

export const STEP_ILLUSTRATIONS = [
  UsageIllustration,
  ScoreIllustration,
  CreditIllustration,
];
