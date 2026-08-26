"use client";

import { useRef } from "react";
import { cn } from "@/lib/cn";

/**
 * Saisie d'un code à 6 chiffres (activation de compte, réinitialisation).
 *
 * Six cases plutôt qu'un champ unique : le code arrive par e-mail et se recopie
 * chiffre par chiffre, souvent d'un écran à l'autre. Le découpage montre
 * combien il en reste à saisir et rend l'erreur de frappe visible sur place.
 *
 * Trois comportements sont indispensables, et c'est pour eux que ce composant
 * existe plutôt que six `<input>` posés côte à côte :
 *
 * — LE COLLER. Un code copié depuis un e-mail arrive en une fois ; sans
 *   interception, seul le premier chiffre entrerait dans la case focalisée.
 * — LE RETOUR ARRIÈRE sur une case vide remonte à la précédente, sinon corriger
 *   une faute oblige à cliquer.
 * — LE FILTRAGE des non-chiffres : le backend impose `^\d{6}$`, un espace collé
 *   par mégarde ferait échouer la requête sans que rien ne soit visible.
 */
export function CodeInput({
  value,
  onChange,
  disabled,
  id = "code",
}: {
  /** Chaîne de 0 à 6 chiffres — la source de vérité reste au parent. */
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  id?: string;
}) {
  const refs = useRef<Array<HTMLInputElement | null>>([]);
  const digits = value.padEnd(6, " ").slice(0, 6).split("");

  const focusAt = (index: number) => {
    refs.current[Math.min(Math.max(index, 0), 5)]?.focus();
  };

  const setDigit = (index: number, digit: string) => {
    const next = digits.map((current) => (current === " " ? "" : current));
    next[index] = digit;
    onChange(next.join("").slice(0, 6));
  };

  return (
    <div className="flex justify-between gap-2" role="group" aria-label="Code à 6 chiffres">
      {digits.map((digit, index) => (
        <input
          key={index}
          id={index === 0 ? id : `${id}-${index}`}
          ref={(element) => {
            refs.current[index] = element;
          }}
          value={digit.trim()}
          disabled={disabled}
          inputMode="numeric"
          autoComplete={index === 0 ? "one-time-code" : "off"}
          maxLength={1}
          aria-label={`Chiffre ${index + 1}`}
          onChange={(event) => {
            const typed = event.target.value.replace(/\D/g, "");
            if (!typed) {
              setDigit(index, "");
              return;
            }
            setDigit(index, typed.slice(-1));
            focusAt(index + 1);
          }}
          onKeyDown={(event) => {
            if (event.key === "Backspace" && !digit.trim()) {
              event.preventDefault();
              setDigit(index - 1, "");
              focusAt(index - 1);
            }
            if (event.key === "ArrowLeft") focusAt(index - 1);
            if (event.key === "ArrowRight") focusAt(index + 1);
          }}
          onPaste={(event) => {
            event.preventDefault();
            const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
            if (!pasted) return;
            onChange(pasted);
            focusAt(pasted.length - 1);
          }}
          className={cn(
            "h-14 w-full min-w-0 rounded-field border border-line-strong bg-white text-center font-display text-xl font-semibold text-ink-900 transition-colors",
            "focus:border-kola-500 focus:ring-2 focus:ring-kola-100 focus:outline-none",
            "disabled:bg-ink-50 disabled:text-ink-400"
          )}
        />
      ))}
    </div>
  );
}
