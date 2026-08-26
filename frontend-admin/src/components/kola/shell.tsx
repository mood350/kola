import type { ReactNode } from "react";
import Link from "next/link";
import { twMerge } from "tailwind-merge";
import Button from "@/components/ui/button/Button";

/**
 * Pièces récurrentes des écrans, dans le langage visuel de TailAdmin.
 *
 * Elles comblent ce que le template ne fournit pas : son `PageBreadCrumb` est
 * anglophone et fixe le premier maillon à « Home », et il n'offre ni état de
 * chargement, ni état vide, ni pagination.
 */

/* ---------------------------------------------------------------------------
   Titre de page
   ------------------------------------------------------------------------ */

export function PageHeading({
  title,
  description,
  backHref,
  backLabel,
  actions,
}: {
  title: string;
  description?: string;
  /** Retour vers la liste, sur les écrans de détail. */
  backHref?: string;
  backLabel?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6">
      {backHref ? (
        <nav aria-label="Fil d'Ariane" className="mb-3">
          <Link
            href={backHref}
            className="inline-flex items-center gap-1.5 text-sm text-gray-500 transition-colors hover:text-brand-500 dark:text-gray-400"
          >
            <span aria-hidden="true">←</span>
            {backLabel ?? "Retour"}
          </Link>
        </nav>
      ) : null}

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-800 dark:text-white/90">
            {title}
          </h1>
          {description ? (
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {description}
            </p>
          ) : null}
        </div>
        {actions ? (
          <div className="flex flex-wrap items-center gap-2">{actions}</div>
        ) : null}
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Panneau
   ------------------------------------------------------------------------ */

/**
 * Carte de contenu.
 *
 * Reprend l'habillage de `ComponentCard` (coins 2xl, filet, fond blanc / blanc
 * translucide en sombre) mais laisse le corps SANS rembourrage : un tableau doit
 * toucher les bords de sa carte, là où `ComponentCard` impose un `p-6` qui
 * décollerait toutes les lignes.
 */
export function Card({
  title,
  description,
  actions,
  children,
  className,
}: {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={twMerge(
        "rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]",
        className
      )}
    >
      {title ? (
        <header className="flex flex-wrap items-start justify-between gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
          <div className="min-w-0">
            <h2 className="text-base font-medium text-gray-800 dark:text-white/90">
              {title}
            </h2>
            {description ? (
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {description}
              </p>
            ) : null}
          </div>
          {actions ? (
            <div className="flex items-center gap-2">{actions}</div>
          ) : null}
        </header>
      ) : null}
      {children}
    </section>
  );
}

/* ---------------------------------------------------------------------------
   États
   ------------------------------------------------------------------------ */

/**
 * Chargement, vide et erreur sont des composants à part entière, et non des
 * `null` discrets : ce sont eux que l'opérateur voit quand quelque chose ne va
 * pas. Un tableau vide sans explication laisse croire à une absence de données
 * là où il y a peut-être une panne — c'est exactement la distinction qu'il faut
 * lui donner.
 */
export function LoadingBlock({ label = "Chargement…" }: { label?: string }) {
  return (
    <div className="px-5 py-12 text-center">
      <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
      {/* Barre indéterminée : elle n'annonce aucune durée et ne déplace rien. */}
      <span
        aria-hidden="true"
        className="mx-auto mt-4 block h-1 w-40 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800"
      >
        <span className="block h-full w-1/3 animate-[kola-sweep_1.1s_ease-in-out_infinite] rounded-full bg-brand-500" />
      </span>
    </div>
  );
}

export function EmptyBlock({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="px-5 py-14 text-center">
      <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
        {title}
      </p>
      {hint ? (
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{hint}</p>
      ) : null}
    </div>
  );
}

export function ErrorBlock({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div
      role="alert"
      className="m-5 rounded-xl border border-error-300 bg-error-50 px-4 py-3 dark:border-error-500/40 dark:bg-error-500/10"
    >
      <p className="text-sm text-error-700 dark:text-error-400">{message}</p>
      {onRetry ? (
        <div className="mt-3">
          <Button size="sm" variant="outline" onClick={onRetry}>
            Réessayer
          </Button>
        </div>
      ) : null}
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Pagination
   ------------------------------------------------------------------------ */

/**
 * `page` est l'index tel que le renvoie Spring — À PARTIR DE 0. L'affichage
 * compte à partir de 1 : personne ne lit « page 0 sur 5 ». La conversion se fait
 * ici et nulle part ailleurs, pour qu'aucun écran n'ait à se souvenir de quelle
 * convention il manipule.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (next: number) => void;
}) {
  const count = `${totalElements} résultat${totalElements > 1 ? "s" : ""}`;

  if (totalPages <= 1) {
    return (
      <div className="border-t border-gray-100 px-5 py-3 text-theme-xs text-gray-500 dark:border-gray-800 dark:text-gray-400">
        {count}
      </div>
    );
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 px-5 py-3 dark:border-gray-800">
      <p className="text-theme-xs text-gray-500 dark:text-gray-400">
        Page {page + 1} sur {totalPages} · {count}
      </p>
      <div className="flex items-center gap-2">
        <Button
          size="sm"
          variant="outline"
          disabled={page <= 0}
          onClick={() => onChange(page - 1)}
        >
          Précédent
        </Button>
        <Button
          size="sm"
          variant="outline"
          disabled={page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        >
          Suivant
        </Button>
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Paire libellé / valeur
   ------------------------------------------------------------------------ */

/** La brique des fiches de détail. */
export function DataItem({
  label,
  children,
  mono = false,
}: {
  label: string;
  children: ReactNode;
  mono?: boolean;
}) {
  return (
    <div>
      <dt className="text-theme-xs font-medium tracking-wide text-gray-500 uppercase dark:text-gray-400">
        {label}
      </dt>
      <dd
        className={twMerge(
          "mt-1.5 text-sm text-gray-800 dark:text-white/90",
          mono && "font-mono text-theme-xs break-all"
        )}
      >
        {children}
      </dd>
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Tableau
   ------------------------------------------------------------------------ */

/**
 * `overflow-x-auto` est indispensable : ces tableaux ont sept à neuf colonnes et
 * ne tiennent pas sur un écran étroit. Sans lui, c'est la PAGE entière qui
 * défilerait horizontalement, emportant le rail avec elle.
 */
export function TableScroll({ children }: { children: ReactNode }) {
  return (
    <div className="max-w-full overflow-x-auto">
      <div className="min-w-[52rem]">{children}</div>
    </div>
  );
}

/** Cellule d'en-tête. */
export function Th({
  children,
  align = "left",
}: {
  children: ReactNode;
  align?: "left" | "right";
}) {
  return (
    <th
      scope="col"
      className={twMerge(
        "border-b border-gray-100 px-5 py-3 text-theme-xs font-medium text-gray-500 dark:border-gray-800 dark:text-gray-400",
        align === "right" ? "text-right" : "text-left"
      )}
    >
      {children}
    </th>
  );
}

/** Cellule de corps. Les chiffres sont en chasse fixe pour s'aligner. */
export function Td({
  children,
  align = "left",
  className,
}: {
  children: ReactNode;
  align?: "left" | "right";
  className?: string;
}) {
  return (
    <td
      className={twMerge(
        "border-b border-gray-100 px-5 py-3.5 align-middle text-theme-sm text-gray-700 tabular-nums dark:border-gray-800 dark:text-gray-300",
        align === "right" ? "text-right" : "text-left",
        className
      )}
    >
      {children}
    </td>
  );
}
