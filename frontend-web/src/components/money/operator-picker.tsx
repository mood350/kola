"use client";

import { cn } from "@/lib/cn";
import { OPERATOR_COLOR } from "@/lib/labels";
import type { PaymentMethod } from "@/lib/types";

/**
 * Choix de l'opérateur Mobile Money.
 *
 * ═══ DES PASTILLES, PAS UNE LISTE DÉROULANTE ═══
 *
 * Parce qu'on choisit ici un OPÉRATEUR, pas une valeur abstraite : Moov et MTN
 * se reconnaissent à leur couleur avant de se lire. Une liste déroulante cache
 * les options derrière un clic et oblige à lire neuf lignes pour en trouver une
 * qu'on aurait repérée du coin de l'œil.
 *
 * La couleur vient de `OPERATOR_COLOR` — table distincte de celle des
 * bénéficiaires, parce que les deux énumérations ne décrivent pas la même
 * chose. Les opérateurs sans couleur de marque établie retombent sur l'indigo
 * plutôt que d'en inventer une à la place de leur propriétaire.
 */
export function OperatorPicker({
  methods,
  selected,
  onSelect,
  disabled,
}: {
  methods: PaymentMethod[];
  selected: string | null;
  onSelect: (code: string) => void;
  disabled?: boolean;
}) {
  return (
    <div className="space-y-1.5">
      <span className="block text-sm font-medium text-ink-800">Opérateur</span>
      <div
        role="radiogroup"
        aria-label="Opérateur Mobile Money"
        className="grid grid-cols-2 gap-2 sm:grid-cols-3"
      >
        {methods.map((method) => {
          const active = selected === method.code;
          const color = OPERATOR_COLOR[method.code];

          return (
            <label
              key={method.code}
              className={cn(
                "flex cursor-pointer items-center gap-2.5 rounded-field border p-3 text-sm transition-colors",
                active
                  ? "border-kola-500 bg-kola-50"
                  : "border-line-strong bg-white hover:bg-ink-50",
                disabled && "cursor-not-allowed opacity-50"
              )}
            >
              <input
                type="radio"
                name="operator"
                className="sr-only"
                checked={active}
                disabled={disabled}
                onChange={() => onSelect(method.code)}
              />
              <span
                aria-hidden
                className="size-6 shrink-0 rounded-full"
                style={{ backgroundColor: color ?? "var(--color-kola-500)" }}
              />
              <span className="min-w-0 flex-1 font-medium text-ink-900">
                {method.label}
              </span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
