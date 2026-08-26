import type { ReactNode } from "react";
import Badge from "@/components/ui/badge/Badge";
import { ArrowDownIcon, ArrowUpIcon } from "@/icons";
import { formatGrowth } from "@/lib/format";

/**
 * Tuile d'indicateur, sur le modèle des cartes du template.
 *
 * POURQUOI UNE TUILE ET NON UN GRAPHIQUE : une valeur unique n'a rien à
 * comparer. Un histogramme à une barre encode une magnitude sans échelle de
 * référence — il occupe dix fois la place du chiffre pour dire la même chose.
 *
 * ═══ LE DELTA — LE POINT DÉLICAT ═══
 *
 * Sa couleur dépend de DEUX choses : le sens de la variation, et le fait qu'une
 * hausse soit une bonne ou une mauvaise nouvelle. Plus d'utilisateurs est bon ;
 * plus de prêts en défaut ne l'est pas. D'où `upIsGood` — sans lui, on
 * colorierait en vert une dégradation.
 *
 * La couleur n'est jamais seule à porter le sens : la flèche donne la direction,
 * le signe la redonne, et « vs mois précédent » nomme la référence. Un opérateur
 * daltonien lit exactement la même information.
 */
export function MetricCard({
  label,
  value,
  icon,
  delta,
  upIsGood = true,
  hint,
}: {
  label: string;
  value: string;
  icon?: ReactNode;
  /** Pourcentage DÉJÀ calculé par le backend (12.5 = « +12,5 % »). */
  delta?: number;
  upIsGood?: boolean;
  hint?: string;
}) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      {icon ? (
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-white/90">
          {icon}
        </div>
      ) : null}

      <div className={icon ? "mt-5" : undefined}>
        <span className="text-sm text-gray-500 dark:text-gray-400">{label}</span>

        <div className="mt-2 flex flex-wrap items-end justify-between gap-2">
          {/* Chasse PROPORTIONNELLE, pas tabulaire : à cette taille,
              `tabular-nums` donne à chaque chiffre la largeur d'un zéro et fait
              paraître « 121 » anormalement lâche. La chasse fixe est réservée
              aux colonnes de tableau, qui doivent s'aligner verticalement. */}
          <h3 className="text-title-sm font-bold text-gray-800 dark:text-white/90">
            {value}
          </h3>

          {delta !== undefined ? <Delta value={delta} upIsGood={upIsGood} /> : null}
        </div>

        {hint ? (
          <p className="mt-2 text-theme-xs text-gray-500 dark:text-gray-400">
            {hint}
          </p>
        ) : null}
      </div>
    </div>
  );
}

function Delta({ value, upIsGood }: { value: number; upIsGood: boolean }) {
  /* Zéro n'est ni bon ni mauvais : il reste neutre. Le peindre en vert ou en
     rouge inventerait une tendance là où il n'y en a pas. */
  const flat = Math.round(value * 10) / 10 === 0;
  const positive = value > 0;
  const color = flat ? "light" : positive === upIsGood ? "success" : "error";

  return (
    <Badge
      size="sm"
      color={color}
      startIcon={
        flat ? undefined : positive ? (
          <ArrowUpIcon className="size-3.5" />
        ) : (
          <ArrowDownIcon className="size-3.5" />
        )
      }
    >
      {formatGrowth(value)}
    </Badge>
  );
}
