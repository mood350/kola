"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import { PASSWORD_RULES, isValidPassword } from "@/lib/countries";
import { Alert, Button, Field } from "@/components/ui/primitives";
import { PasswordInput } from "@/components/ui/password-input";
import { CodeInput } from "@/components/ui/code-input";
import { CheckIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";

/**
 * Choix d'un nouveau mot de passe à partir du code reçu.
 *
 * Les règles de robustesse sont les mêmes qu'à l'inscription, et
 * volontairement : sans cela, ce flux deviendrait une porte dérobée permettant
 * de poser un mot de passe plus faible que ce que l'inscription autorise
 * (cf. le commentaire de `ResetPasswordRequest`).
 */
export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const router = useRouter();

  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!isValidPassword(password)) {
      setError("Le mot de passe ne respecte pas les règles ci-dessous.");
      return;
    }

    setSubmitting(true);
    try {
      await authApi.resetPassword(code, password);
      setDone(true);
      setTimeout(() => router.replace("/connexion"), 1600);
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <div className="rise">
        <Alert tone="positive" title="Mot de passe modifié">
          Vous allez être redirigé vers la connexion.
        </Alert>
      </div>
    );
  }

  return (
    <div className="rise">
      <h1 className="font-display text-3xl font-semibold tracking-tight text-ink-950">
        Nouveau mot de passe
      </h1>
      <p className="mt-2 text-ink-500">
        Saisissez le code reçu par e-mail, puis choisissez votre nouveau mot de
        passe.
      </p>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <div className="space-y-1.5">
          <span className="block text-sm font-medium text-ink-800">
            Code de réinitialisation
          </span>
          <CodeInput value={code} onChange={setCode} disabled={submitting} />
        </div>

        <Field label="Nouveau mot de passe" htmlFor="new-password">
          <PasswordInput
            id="new-password"
            autoComplete="new-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Field>

        <ul className="grid gap-1.5 sm:grid-cols-2">
          {PASSWORD_RULES.map((rule) => {
            const satisfied = rule.test(password);
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

        <Button
          type="submit"
          size="lg"
          full
          loading={submitting}
          disabled={code.length !== 6}
        >
          Changer mon mot de passe
        </Button>
      </form>

      <p className="mt-8 border-t border-line pt-6 text-sm text-ink-600">
        <Link
          href="/mot-de-passe-oublie"
          className="font-semibold text-kola-600 hover:underline"
        >
          Demander un nouveau code
        </Link>
      </p>
    </div>
  );
}
