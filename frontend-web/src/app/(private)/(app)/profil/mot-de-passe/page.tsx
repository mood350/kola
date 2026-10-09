"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { userApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import { PASSWORD_RULES, isValidPassword } from "@/lib/countries";
import { cn } from "@/lib/cn";
import { Alert, Button, Card, Field } from "@/components/ui/primitives";
import { PasswordInput } from "@/components/ui/password-input";
import { PageHeader } from "@/components/layout/page-header";
import { CheckIcon } from "@/components/ui/icons";

/**
 * Changement de mot de passe.
 *
 * Le mot de passe ACTUEL est exigé par le backend (`ChangePasswordRequest`), et
 * c'est ce qui protège une session laissée ouverte sur un poste partagé : sans
 * lui, quiconque passe devant l'écran pourrait s'approprier le compte en une
 * saisie.
 *
 * La confirmation est vérifiée côté client uniquement — le backend n'attend
 * qu'un seul champ. Elle existe pour attraper la faute de frappe qui, sinon, ne
 * se découvrirait qu'à la prochaine connexion, mot de passe déjà changé.
 */
export default function ChangePasswordPage() {
  const router = useRouter();

  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const mismatch = confirm !== "" && confirm !== next;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!isValidPassword(next)) {
      setError("Le nouveau mot de passe ne respecte pas les règles ci-dessous.");
      return;
    }
    if (mismatch) {
      setError("Les deux mots de passe saisis ne correspondent pas.");
      return;
    }

    setSubmitting(true);
    try {
      await userApi.changePassword(current, next);
      setDone(true);
      setTimeout(() => router.push("/profil"), 1600);
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Changer mon mot de passe"
        backHref="/profil"
        backLabel="Profil"
      />

      {done ? (
        <Alert tone="positive" title="Mot de passe modifié">
          Votre nouveau mot de passe est actif. Utilisez-le à votre prochaine
          connexion.
        </Alert>
      ) : (
        <Card>
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {error ? <Alert tone="danger">{error}</Alert> : null}

            <Field label="Mot de passe actuel" htmlFor="current-password">
              <PasswordInput
                id="current-password"
                autoComplete="current-password"
                required
                value={current}
                onChange={(event) => setCurrent(event.target.value)}
              />
            </Field>

            <Field label="Nouveau mot de passe" htmlFor="next-password">
              <PasswordInput
                id="next-password"
                autoComplete="new-password"
                required
                value={next}
                onChange={(event) => setNext(event.target.value)}
              />
            </Field>

            <ul className="grid gap-1.5 sm:grid-cols-2">
              {PASSWORD_RULES.map((rule) => {
                const satisfied = rule.test(next);
                return (
                  <li
                    key={rule.label}
                    className={cn(
                      "flex items-center gap-2 text-sm",
                      satisfied ? "text-positive-600" : "text-ink-400"
                    )}
                  >
                    <CheckIcon className={cn("text-base", !satisfied && "opacity-40")} />
                    {rule.label}
                  </li>
                );
              })}
            </ul>

            <Field
              label="Confirmer le nouveau mot de passe"
              htmlFor="confirm-password"
              error={mismatch ? "Les deux saisies diffèrent." : null}
            >
              <PasswordInput
                id="confirm-password"
                autoComplete="new-password"
                required
                invalid={mismatch}
                value={confirm}
                onChange={(event) => setConfirm(event.target.value)}
              />
            </Field>

            <Button
              type="submit"
              full
              loading={submitting}
              disabled={!current || !next || mismatch}
            >
              Changer mon mot de passe
            </Button>
          </form>
        </Card>
      )}
    </>
  );
}
