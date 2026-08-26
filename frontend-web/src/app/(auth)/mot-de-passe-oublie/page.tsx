"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import { Alert, Button, Field, Input } from "@/components/ui/primitives";

/**
 * Demande d'un code de réinitialisation.
 *
 * Le message de succès ne confirme PAS que l'adresse existe — il dit « si un
 * compte existe pour cette adresse ». Confirmer l'existence transformerait ce
 * formulaire en outil d'énumération de clients : il suffirait d'essayer des
 * adresses pour savoir lesquelles sont inscrites chez Kola.
 */
export default function ForgotPasswordPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await authApi.forgotPassword(email.trim());
      setSent(true);
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="rise">
      <h1 className="font-display text-3xl font-semibold tracking-tight text-ink-950">
        Mot de passe oublié
      </h1>
      <p className="mt-2 text-ink-500">
        Nous vous envoyons un code à 6 chiffres pour en choisir un nouveau.
      </p>

      {sent ? (
        <div className="mt-8 space-y-5">
          <Alert tone="positive" title="Demande enregistrée">
            Si un compte existe pour cette adresse, un code vient d&apos;y être
            envoyé. Il est valable un temps limité.
          </Alert>
          <Button
            size="lg"
            full
            onClick={() => router.push(`/reinitialiser?email=${encodeURIComponent(email.trim())}`)}
          >
            J&apos;ai reçu mon code
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-8 space-y-4" noValidate>
          {error ? <Alert tone="danger">{error}</Alert> : null}

          <Field label="Adresse e-mail" htmlFor="email">
            <Input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="vous@exemple.com"
            />
          </Field>

          <Button type="submit" size="lg" full loading={submitting}>
            Envoyer le code
          </Button>
        </form>
      )}

      <p className="mt-8 border-t border-line pt-6 text-sm text-ink-600">
        <Link href="/connexion" className="font-semibold text-kola-600 hover:underline">
          Retour à la connexion
        </Link>
      </p>
    </div>
  );
}
