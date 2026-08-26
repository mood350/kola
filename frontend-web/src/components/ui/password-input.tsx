"use client";

import { useState } from "react";
import type { InputHTMLAttributes } from "react";
import { Input } from "@/components/ui/primitives";
import { EyeIcon, EyeOffIcon } from "@/components/ui/icons";

/**
 * Champ de mot de passe avec bascule de visibilité.
 *
 * POURQUOI LA BASCULE : les règles du backend (8 caractères, une majuscule, une
 * minuscule, un chiffre — `RegistrationRequest`) produisent des mots de passe
 * qu'on se trompe à saisir. Sans possibilité de relire, l'échec ne se découvre
 * qu'après l'envoi, et l'utilisateur ne sait pas s'il a mal tapé ou si le compte
 * est bloqué. Le champ revient masqué à chaque montage : la révélation est un
 * geste explicite, jamais un état persistant.
 *
 * `autoComplete` est laissé à l'appelant : « current-password » à la connexion,
 * « new-password » à l'inscription. Se tromper fait proposer par le
 * gestionnaire de mots de passe l'ancien à l'endroit du nouveau.
 */
export function PasswordInput({
  id,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { id: string; invalid?: boolean }) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <Input id={id} type={visible ? "text" : "password"} className="pr-11" {...props} />
      <button
        type="button"
        onClick={() => setVisible((current) => !current)}
        aria-label={visible ? "Masquer le mot de passe" : "Afficher le mot de passe"}
        aria-pressed={visible}
        className="absolute inset-y-0 right-0 flex w-11 items-center justify-center rounded-r-field text-ink-500 hover:text-ink-800"
      >
        {visible ? <EyeOffIcon className="text-lg" /> : <EyeIcon className="text-lg" />}
      </button>
    </div>
  );
}
