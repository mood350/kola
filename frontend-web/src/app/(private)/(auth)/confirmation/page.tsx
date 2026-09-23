"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import { Alert, Button } from "@/components/ui/primitives";
import { CodeInput } from "@/components/ui/code-input";

/**
 * Activation du compte par le code à 6 chiffres reçu par e-mail.
 *
 * Cet écran est le passage obligé après l'inscription : tant qu'il n'a pas
 * abouti, le compte existe mais reste désactivé et la connexion échoue. Il est
 * atteignable directement (`/confirmation`) pour ceux qui ferment l'onglet
 * entre-temps — d'où le champ e-mail purement indicatif, transmis en paramètre
 * quand on vient de l'inscription.
 *
 * Le code voyage dans le CORPS de la requête, jamais dans l'URL : il active un
 * compte, et une URL est archivée dans les journaux du serveur comme dans
 * l'historique du navigateur (cf. `ConfirmAccountRequest`).
 */
export default function ConfirmPage() {
  return (
    <Suspense fallback={null}>
      <ConfirmForm />
    </Suspense>
  );
}

function ConfirmForm() {
  const router = useRouter();
  const email = useSearchParams().get("email");

  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await authApi.confirm(code);
      setDone(true);
      /* Court délai avant la bascule : la confirmation doit être LUE. Rediriger
         instantanément donne l'impression d'un formulaire qui s'est vidé tout
         seul, et laisse un doute sur ce qui a réellement été enregistré. */
      setTimeout(() => router.replace("/connexion"), 1600);
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <div className="rise">
        <Alert tone="positive" title="Compte activé">
          Vous allez être redirigé vers la connexion.
        </Alert>
      </div>
    );
  }

  return (
    <div className="rise">
      <h1 className="font-display text-3xl font-semibold tracking-tight text-ink-950">
        Confirmez votre compte
      </h1>
      <p className="mt-2 text-ink-500">
        Saisissez le code à 6 chiffres envoyé
        {email ? (
          <>
            {" à "}
            <span className="font-medium text-ink-800">{email}</span>
          </>
        ) : (
          " à votre adresse e-mail"
        )}
        .
      </p>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <CodeInput value={code} onChange={setCode} disabled={submitting} />

        <Button
          type="submit"
          size="lg"
          full
          loading={submitting}
          /* Six chiffres exactement : le backend refuse le reste
             (`^\d{6}$`), autant ne pas dépenser un aller-retour pour l'apprendre. */
          disabled={code.length !== 6}
        >
          Activer mon compte
        </Button>
      </form>

      <p className="mt-8 border-t border-line pt-6 text-sm text-ink-600">
        Code expiré ou jamais reçu ?{" "}
        <Link href="/inscription" className="font-semibold text-kola-600 hover:underline">
          Recommencer l&apos;inscription
        </Link>
      </p>
    </div>
  );
}
