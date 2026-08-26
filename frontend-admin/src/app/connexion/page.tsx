"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api";
import { NotAnAdminError, useSession } from "@/lib/session";
import { KolaMark } from "@/components/kola/brand";
import Label from "@/components/form/Label";
import Button from "@/components/ui/button/Button";
import { EyeCloseIcon, EyeIcon } from "@/icons";

/**
 * Écran de connexion.
 *
 * Mise en page à deux colonnes de TailAdmin : formulaire à gauche, aplat de
 * marque à droite au-delà de `lg`. Le panneau de droite ne porte aucune
 * information nécessaire — il disparaît sous `lg` sans rien coûter.
 *
 * ═══ MESSAGES D'ERREUR — LE POINT DÉLICAT ═══
 *
 * Ils sont dérivés du `code`/statut renvoyé par le backend, jamais de son texte :
 * le code est stable, le libellé change au premier ajustement de formulation.
 *
 * Et surtout, ils ne révèlent jamais si un compte EXISTE. Le backend distingue
 * « aucun compte associé à cet email » de « mot de passe incorrect » — les deux
 * arrivent ici en 401 et sont fondus dans un message unique. Afficher la
 * différence transformerait ce formulaire en oracle : n'importe qui pourrait
 * énumérer les adresses inscrites en observant lequel des deux s'affiche.
 *
 * Deux états sont en revanche annoncés explicitement, parce qu'ils décrivent une
 * situation sur laquelle l'utilisateur peut agir : compte verrouillé (423) et
 * compte non administrateur. Ni l'un ni l'autre ne renseigne un attaquant qui ne
 * connaîtrait pas déjà le mot de passe.
 */
export default function LoginPage() {
  const { status, login } = useSession();
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  /* Session déjà ouverte : on n'affiche pas un formulaire de connexion à
     quelqu'un de connecté. `replace` et non `push` — cet écran ne doit pas
     rester dans l'historique, sinon le bouton « retour » y ramène depuis la
     console. */
  useEffect(() => {
    if (status === "authenticated") router.replace("/");
  }, [status, router]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await login(email, password);
      router.replace("/");
    } catch (caught) {
      setError(messageFor(caught));
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen bg-white dark:bg-gray-900">
      <main
        id="contenu"
        className="flex w-full flex-1 items-center justify-center px-4 py-10 sm:px-8"
      >
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3">
            <KolaMark className="h-9 w-9 text-brand-500" />
            <div>
              <p className="text-lg font-semibold text-gray-900 dark:text-white/90">
                Console Kola
              </p>
              <p className="text-theme-xs text-gray-500 dark:text-gray-400">
                Administration interne
              </p>
            </div>
          </div>

          <h1 className="mb-2 text-title-sm font-semibold text-gray-800 dark:text-white/90">
            Connexion
          </h1>
          <p className="mb-7 text-sm text-gray-500 dark:text-gray-400">
            Accès réservé aux comptes disposant du rôle administrateur.
          </p>

          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            <div>
              <Label htmlFor="email">
                Adresse e-mail <span className="text-error-500">*</span>
              </Label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="vous@kola.africa"
                className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-white/30"
              />
            </div>

            <div>
              <Label htmlFor="password">
                Mot de passe <span className="text-error-500">*</span>
              </Label>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 pr-11 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-white/30"
                />
                {/* `type="button"` explicite : dans un formulaire, un bouton sans
                    type vaut `submit` et enverrait le formulaire à chaque clic
                    sur l'œil. */}
                <button
                  type="button"
                  onClick={() => setShowPassword((shown) => !shown)}
                  aria-label={
                    showPassword
                      ? "Masquer le mot de passe"
                      : "Afficher le mot de passe"
                  }
                  className="absolute top-1/2 right-3 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  {showPassword ? (
                    <EyeIcon className="size-5" />
                  ) : (
                    <EyeCloseIcon className="size-5" />
                  )}
                </button>
              </div>
            </div>

            {/* `role="alert"` : l'échec est annoncé immédiatement par les
                lecteurs d'écran, sans déplacer le focus — l'utilisateur reste
                dans le champ qu'il vient de quitter. */}
            {error ? (
              <p
                role="alert"
                className="rounded-lg border border-error-300 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-500/40 dark:bg-error-500/10 dark:text-error-400"
              >
                {error}
              </p>
            ) : null}

            <Button className="w-full" disabled={submitting}>
              {submitting ? "Connexion…" : "Se connecter"}
            </Button>
          </form>

          <p className="mt-6 text-theme-xs leading-relaxed text-gray-500 dark:text-gray-400">
            Cinq tentatives échouées verrouillent le compte pendant trente
            minutes. Toute connexion depuis un appareil inconnu déclenche une
            notification par e-mail.
          </p>
        </div>
      </main>

      {/* Panneau de marque — purement décoratif, masqué aux lecteurs d'écran et
          absent sous `lg`. */}
      <aside
        aria-hidden="true"
        className="relative hidden w-1/2 items-center justify-center overflow-hidden bg-brand-950 lg:flex"
      >
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_50%_0%,rgba(123,127,236,0.35)_0%,transparent_70%)]" />
        <div className="relative flex flex-col items-center px-10 text-center">
          <KolaMark className="h-16 w-16 text-white" />
          <p className="mt-6 text-2xl font-semibold text-white">Kola</p>
          <p className="mt-3 max-w-sm text-sm leading-relaxed text-white/60">
            Portefeuille mobile money et score de confiance pour l&apos;Afrique
            de l&apos;Ouest. Console de pilotage des comptes, du crédit et de la
            conformité.
          </p>
        </div>
      </aside>
    </div>
  );
}

/**
 * Traduction d'une erreur en message affichable. Le cas par défaut reste vague :
 * une erreur inattendue ne doit pas recracher un détail technique du serveur.
 */
function messageFor(caught: unknown): string {
  if (caught instanceof NotAnAdminError) return caught.message;

  if (caught instanceof ApiError) {
    switch (caught.status) {
      case 401:
        /* 401 recouvre « compte inexistant » ET « mot de passe erroné ». Les
           deux se disent de la même façon, exprès. */
        return "Adresse e-mail ou mot de passe incorrect.";
      case 423:
        return "Ce compte est verrouillé. Réessayez dans trente minutes, ou contactez un autre administrateur.";
      case 403:
        return "Ce compte n'est pas activé. Confirmez l'adresse e-mail avant de vous connecter.";
      case 429:
        return "Trop de tentatives. Patientez quelques instants avant de réessayer.";
      default:
        return caught.message;
    }
  }

  /* `fetch` ne rejette que sur une panne réseau — backend éteint, ou origine que
     le CORS refuse. Les deux se voient surtout en développement et méritent
     d'être nommées, sinon on cherche l'erreur dans le formulaire. */
  return "Impossible de joindre le serveur. Vérifiez que l'API est démarrée sur http://localhost:8081.";
}
