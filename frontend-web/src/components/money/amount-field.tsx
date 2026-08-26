"use client";

import { cn } from "@/lib/cn";
import { formatAmount } from "@/lib/format";

/**
 * Saisie d'un montant.
 *
 * Trois décisions, toutes dictées par la devise et par ce qu'un montant mal
 * saisi coûte :
 *
 * 1. `inputMode="numeric"` ouvre le pavé numérique sur téléphone, mais le champ
 *    reste un `type="text"` filtré à la main : un `type="number"` accepte
 *    « 1e5 » et « -50 », deux valeurs qu'aucune opération d'argent ne doit
 *    laisser passer jusqu'au serveur.
 * 2. Aucune décimale. Le franc CFA n'a pas de subdivision — offrir une virgule
 *    inviterait à saisir « 1 500,50 » pour se voir refuser ou arrondir.
 * 3. Le montant est relu en toutes lettres sous le champ. « 250000 » et
 *    « 25000 » se ressemblent trop pour qu'on s'en remette à la seule saisie
 *    brute ; le rappel formaté est ce qui rend l'erreur d'un zéro visible avant
 *    l'envoi.
 */
export function AmountField({
  value,
  onChange,
  /** Bornes issues du solde disponible ou du plafond de prêt, jamais devinées. */
  max,
  min = 1,
  disabled,
  id = "amount",
  label = "Montant",
  quickAmounts,
  helper,
}: {
  value: string;
  onChange: (value: string) => void;
  max?: number;
  min?: number;
  disabled?: boolean;
  id?: string;
  label?: string;
  quickAmounts?: number[];
  helper?: string;
}) {
  const numeric = Number(value);
  const valid = value !== "" && Number.isFinite(numeric);
  const tooLow = valid && numeric < min;
  const tooHigh = valid && max !== undefined && numeric > max;

  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-medium text-ink-800">
        {label}
      </label>

      <div
        className={cn(
          "flex items-center gap-2 rounded-field border bg-white px-4 py-3 transition-colors focus-within:ring-2 focus-within:ring-kola-100",
          tooLow || tooHigh
            ? "border-danger-500"
            : "border-line-strong focus-within:border-kola-500"
        )}
      >
        <input
          id={id}
          value={value}
          disabled={disabled}
          inputMode="numeric"
          autoComplete="off"
          placeholder="0"
          aria-describedby={`${id}-echo`}
          onChange={(event) => {
            /* Filtrage à la source : tout ce qui n'est pas un chiffre est écarté
               à la frappe. Corriger après coup laisserait passer un état
               intermédiaire invalide dans l'état du formulaire. */
            const digits = event.target.value.replace(/\D/g, "").replace(/^0+(?=\d)/, "");
            onChange(digits);
          }}
          className="tabular w-full min-w-0 bg-transparent font-display text-2xl font-semibold text-ink-950 outline-none placeholder:text-ink-300"
        />
        <span className="shrink-0 text-sm font-semibold text-ink-400">FCFA</span>
      </div>

      {quickAmounts && quickAmounts.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {quickAmounts.map((amount) => (
            <button
              key={amount}
              type="button"
              disabled={disabled}
              onClick={() => onChange(String(amount))}
              className="rounded-full bg-ink-100 px-3 py-1.5 text-xs font-semibold text-ink-700 transition-colors hover:bg-kola-50 hover:text-kola-700 disabled:opacity-50"
            >
              {formatAmount(amount)}
            </button>
          ))}
        </div>
      ) : null}

      <p
        id={`${id}-echo`}
        className={cn(
          "text-sm",
          tooLow || tooHigh ? "text-danger-600" : "text-ink-500"
        )}
      >
        {tooHigh
          ? `Montant supérieur au maximum disponible (${formatAmount(max)}).`
          : tooLow
            ? `Montant minimum : ${formatAmount(min)}.`
            : valid
              ? formatAmount(numeric)
              : (helper ?? "Saisissez un montant en francs CFA.")}
      </p>
    </div>
  );
}

/** Montant saisi, exploitable — `null` tant qu'il n'est pas utilisable. */
export function parseAmount(
  value: string,
  { min = 1, max }: { min?: number; max?: number } = {}
): number | null {
  const numeric = Number(value);
  if (!value || !Number.isFinite(numeric)) return null;
  if (numeric < min) return null;
  if (max !== undefined && numeric > max) return null;
  return numeric;
}
