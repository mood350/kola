"use client";

import { useConsent } from "@/components/providers/cookie-consent";

const LABELS: Record<string, string> = {
  granted: "Accepté",
  denied: "Refusé",
};

/**
 * Rappel du choix courant et moyen de revenir dessus.
 *
 * Le retrait du consentement doit être aussi simple que son recueil : sans ce
 * bloc, une décision prise dans le bandeau serait définitive pendant six mois,
 * ce qui n'est pas acceptable.
 */
export function ConsentControls() {
  const { choice, reopen } = useConsent();

  return (
    <div className="mt-4 flex flex-col gap-5 rounded-card bg-surface p-7 border border-hairline shadow-card sm:flex-row sm:items-center sm:justify-between sm:p-9">
      <div>
        <h2 className="text-lg font-semibold">Votre choix</h2>
        <p className="mt-1.5 text-[0.9375rem] text-ink-500">
          {choice
            ? `Vous avez ${LABELS[choice].toLowerCase()} le dépôt de cookies.`
            : "Vous n'avez pas encore fait de choix."}
        </p>
      </div>

      <button
        type="button"
        onClick={reopen}
        className="inline-flex h-11 shrink-0 cursor-pointer items-center justify-center rounded-full bg-kola-100 px-6 text-[0.875rem] font-medium text-kola-700 transition-colors duration-200 hover:bg-kola-200"
      >
        Modifier mon choix
      </button>
    </div>
  );
}
