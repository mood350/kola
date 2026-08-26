import type { CreditTier } from "@/lib/types";

/**
 * Jauge du score de crédit (0 à 100).
 *
 * Un demi-cercle plutôt qu'un simple nombre : le score n'a de sens que rapporté
 * à son maximum, et « 62 » seul ne dit pas si c'est bon. L'arc montre la
 * position sur l'échelle sans qu'aucune légende ne soit nécessaire.
 *
 * Dessinée en SVG à la main — un `<canvas>` ne serait ni sélectionnable ni
 * lisible par un lecteur d'écran, et une bibliothèque de graphiques pèserait
 * plus lourd que toute cette page pour un seul arc.
 *
 * LA COULEUR SUIT LE PALIER, pas le score : c'est le palier
 * (`CreditTier`) qui détermine ce à quoi l'utilisateur a droit, et les deux
 * doivent donc dire la même chose au même moment.
 */

const TIER_COLOR: Record<CreditTier, string> = {
  INELIGIBLE: "var(--color-ink-400)",
  BASIC: "var(--color-warning-500)",
  STANDARD: "var(--color-info-500)",
  PREMIUM: "var(--color-kola-600)",
  ELITE: "var(--color-positive-500)",
};

export function ScoreGauge({
  score,
  tier,
  size = 200,
}: {
  score: number;
  tier: CreditTier;
  size?: number;
}) {
  const radius = 80;
  const circumference = Math.PI * radius;
  const ratio = Math.min(1, Math.max(0, score / 100));

  return (
    <svg
      viewBox="0 0 200 110"
      width={size}
      height={size * 0.55}
      role="img"
      aria-label={`Score de crédit : ${score} sur 100`}
      className="mx-auto"
    >
      {/* Fond de l'arc : c'est lui qui matérialise le maximum atteignable. */}
      <path
        d="M 20 100 A 80 80 0 0 1 180 100"
        fill="none"
        stroke="var(--color-ink-100)"
        strokeWidth="14"
        strokeLinecap="round"
      />
      <path
        d="M 20 100 A 80 80 0 0 1 180 100"
        fill="none"
        stroke={TIER_COLOR[tier]}
        strokeWidth="14"
        strokeLinecap="round"
        strokeDasharray={`${circumference * ratio} ${circumference}`}
        style={{ transition: "stroke-dasharray 700ms ease-out" }}
      />
      <text
        x="100"
        y="88"
        textAnchor="middle"
        className="font-display"
        style={{ fontSize: 34, fontWeight: 600, fill: "var(--color-ink-950)" }}
      >
        {score}
      </text>
      <text
        x="100"
        y="104"
        textAnchor="middle"
        style={{ fontSize: 11, fill: "var(--color-ink-500)" }}
      >
        sur 100
      </text>
    </svg>
  );
}
