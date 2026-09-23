"use client";

import { cn } from "@/lib/cn";

/**
 * Bascule entre l'opération réelle et l'opération de test.
 *
 * ═══ POURQUOI CE CHOIX EST VISIBLE, ET NON CACHÉ DANS UN RÉGLAGE ═══
 *
 * Parce que les deux modes ne font pas la même chose du tout : l'un déplace de
 * l'argent chez un opérateur, l'autre écrit une ligne dans une base. Confondre
 * les deux, c'est croire avoir été payé. Le mode actif est donc lisible en
 * permanence au-dessus du formulaire, et le mode test porte un avertissement
 * explicite plutôt qu'une simple étiquette.
 *
 * Le mode réel est celui par défaut : le test est l'exception, pas l'inverse.
 */
export function ModeSwitch({
  value,
  onChange,
  realLabel,
  realHint,
  testLabel,
  testHint,
}: {
  value: "mobile" | "test";
  onChange: (mode: "mobile" | "test") => void;
  realLabel: string;
  realHint: string;
  testLabel: string;
  testHint: string;
}) {
  const options = [
    { key: "mobile" as const, label: realLabel, hint: realHint },
    { key: "test" as const, label: testLabel, hint: testHint },
  ];

  return (
    <div
      role="radiogroup"
      aria-label="Mode de l'opération"
      className="grid gap-2 sm:grid-cols-2"
    >
      {options.map((option) => {
        const active = value === option.key;
        return (
          <label
            key={option.key}
            className={cn(
              "cursor-pointer rounded-field border p-3 transition-colors",
              active
                ? option.key === "test"
                  ? "border-warning-500 bg-warning-50"
                  : "border-kola-500 bg-kola-50"
                : "border-line-strong bg-white hover:bg-ink-50"
            )}
          >
            <input
              type="radio"
              name="operation-mode"
              className="sr-only"
              checked={active}
              onChange={() => onChange(option.key)}
            />
            <span className="flex items-center gap-2">
              <span
                aria-hidden
                className={cn(
                  "size-4 shrink-0 rounded-full border-2",
                  active
                    ? option.key === "test"
                      ? "border-warning-500 bg-warning-500 ring-2 ring-white ring-inset"
                      : "border-kola-600 bg-kola-600 ring-2 ring-white ring-inset"
                    : "border-ink-300"
                )}
              />
              <span className="text-sm font-semibold text-ink-900">
                {option.label}
              </span>
            </span>
            <span className="mt-1 block pl-6 text-xs text-ink-500">
              {option.hint}
            </span>
          </label>
        );
      })}
    </div>
  );
}
