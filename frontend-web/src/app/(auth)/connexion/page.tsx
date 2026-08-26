"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useSession } from "@/lib/session";
import { describeActionError } from "@/lib/use-resource";
import { Alert, Button, Field, Input } from "@/components/ui/primitives";
import { PasswordInput } from "@/components/ui/password-input";

/**
 * Connexion.
 *
 * Un seul chemin : e-mail + mot de passe. Il n'y a pas de connexion par code à
 * usage unique côté backend — l'écran OTP du mobile est réservé à une
 * éventuelle double authentification et n'est câblé nulle part
 * (cf. `app_routes.dart`). Ne pas en inventer une ici.
 */
export default function LoginPage() {
  return (
    /* `useSearchParams` impose une frontière de suspense : sans elle, Next
       refuse de pré-rendre la page. */
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const { login } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await login(email.trim(), password);
      /* Retour à la page demandée avant la redirection. `startsWith("/")`
         écarte une destination absolue : sans ce contrôle, un lien
         `?suite=https://…` transformerait cet écran en tremplin de
         redirection vers un site tiers. */
      const next = searchParams.get("suite");
      router.replace(next && next.startsWith("/") ? next : "/");
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  return (
    <div className="rise">
      <h1 className="font-display text-3xl font-semibold tracking-tight text-ink-950">
        Bon retour
      </h1>
      <p className="mt-2 text-ink-500">
        Connectez-vous pour accéder à votre compte Kola.
      </p>

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

        <Field label="Mot de passe" htmlFor="password">
          <PasswordInput
            id="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="••••••••"
          />
        </Field>

        <div className="flex justify-end">
          <Link
            href="/mot-de-passe-oublie"
            className="text-sm font-medium text-kola-600 hover:underline"
          >
            Mot de passe oublié ?
          </Link>
        </div>

        <Button type="submit" size="lg" full loading={submitting}>
          Se connecter
        </Button>
      </form>

      {/* Le verrouillage automatique après cinq échecs est une règle du backend
          (`AuthenticationService`), pas un incident : l'annoncer ici évite de
          la découvrir au moment où le compte se ferme pour trente minutes. */}
      <p className="mt-6 text-xs text-ink-400">
        Après cinq tentatives infructueuses, votre compte est verrouillé pendant
        30 minutes.
      </p>

      <p className="mt-8 border-t border-line pt-6 text-sm text-ink-600">
        Pas encore de compte ?{" "}
        <Link href="/inscription" className="font-semibold text-kola-600 hover:underline">
          Créer un compte
        </Link>
      </p>
    </div>
  );
}
