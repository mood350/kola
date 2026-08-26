import Link from "next/link";
import type { ReactNode } from "react";
import { ChevronLeftIcon } from "@/components/ui/icons";

/**
 * Titre de page, avec retour optionnel.
 *
 * Le lien de retour est un `<Link>` vers une destination NOMMÉE, jamais un
 * `router.back()` : après une opération réussie, l'historique renvoie vers le
 * formulaire qu'on vient de soumettre. Un retour explicite mène là où l'écran
 * suivant a du sens.
 */
export function PageHeader({
  title,
  description,
  backHref,
  backLabel = "Retour",
  action,
}: {
  title: string;
  description?: string;
  backHref?: string;
  backLabel?: string;
  action?: ReactNode;
}) {
  return (
    <header className="mb-6">
      {backHref ? (
        <Link
          href={backHref}
          className="mb-3 inline-flex items-center gap-1 text-sm font-medium text-ink-500 transition-colors hover:text-ink-900"
        >
          <ChevronLeftIcon />
          {backLabel}
        </Link>
      ) : null}

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold tracking-tight text-ink-950">
            {title}
          </h1>
          {description ? (
            <p className="mt-1 text-sm text-ink-500">{description}</p>
          ) : null}
        </div>
        {action}
      </div>
    </header>
  );
}
