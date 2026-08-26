"use client";

import { useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { useSession } from "@/lib/session";
import { useTheme } from "@/context/ThemeContext";
import {
  Card,
  DataItem,
  LoadingBlock,
  PageHeading,
} from "@/components/kola/shell";
import { KycBadge } from "@/components/kola/status";
import Badge from "@/components/ui/badge/Badge";
import Button from "@/components/ui/button/Button";
import Label from "@/components/form/Label";
import { EyeCloseIcon, EyeIcon } from "@/icons";
import { formatDateTime } from "@/lib/format";
import type { CurrentUser } from "@/lib/types";

const CONTROL =
  "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-500 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:disabled:bg-gray-800";

/**
 * Paramètres du compte administrateur.
 *
 * ═══ CE QUE CETTE PAGE NE CONTIENT PAS, ET POURQUOI ═══
 *
 * Pas de réglages de PLATEFORME — barème de frais, plafonds KYC, seuils du
 * moteur anti-blanchiment, durée de validité d'un score. Ces valeurs existent
 * bien, mais elles vivent dans le code (`CreditTier`, `ScoringRule`,
 * `TransactionPolicy`, `RateLimitPolicy`) et dans `application.properties` :
 * aucun endpoint ne permet de les lire ni de les modifier.
 *
 * Afficher des champs qui n'écriraient nulle part serait le pire des travers
 * d'un back-office : un administrateur croirait avoir relevé un plafond, et
 * découvrirait le contraire au premier litige client. Le jour où ces réglages
 * deviendront modifiables, il faudra d'abord les sortir du code — une table de
 * configuration, une migration Flyway et des endpoints dédiés.
 *
 * Ne restent donc que les trois choses réellement paramétrables : le profil de
 * l'administrateur connecté, son mot de passe, et l'apparence de la console.
 */
export default function SettingsPage() {
  const { user, refresh } = useSession();

  if (!user) {
    return (
      <>
        <PageHeading title="Paramètres" />
        <Card>
          <LoadingBlock label="Chargement du profil…" />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeading
        title="Paramètres"
        description="Votre profil d'administrateur, votre mot de passe et l'apparence de la console."
      />

      <div className="space-y-6">
        <ProfileCard user={user} onSaved={refresh} />
        <PasswordCard />
        <AppearanceCard />
        <SecurityCard user={user} />
      </div>
    </>
  );
}

/* ---------------------------------------------------------------------------
   Profil
   ------------------------------------------------------------------------ */

/**
 * Informations modifiables du compte.
 *
 * L'ADRESSE E-MAIL EST EN LECTURE SEULE, et ce n'est pas un oubli :
 * `UpdateProfileRequest` l'exclut délibérément côté serveur parce qu'elle sert
 * d'identifiant de connexion — la changer exigerait une re-vérification par
 * e-mail, un flux qui n'existe pas. Un champ désactivé accompagné de son
 * explication vaut mieux qu'un champ absent : sans lui, on cherche où modifier
 * son adresse.
 */
function ProfileCard({
  user,
  onSaved,
}: {
  user: CurrentUser;
  onSaved: () => Promise<void>;
}) {
  const [firstName, setFirstName] = useState(user.firstName ?? "");
  const [lastName, setLastName] = useState(user.lastName ?? "");
  const [phoneNumber, setPhoneNumber] = useState(user.phoneNumber);
  const [state, setState] = useState<FormState>({ kind: "idle" });

  const dirty =
    firstName !== (user.firstName ?? "") ||
    lastName !== (user.lastName ?? "") ||
    phoneNumber !== user.phoneNumber;

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState({ kind: "saving" });

    try {
      /* `avatar` n'est pas envoyé : `null` signifie « ne touche pas » côté
         serveur, et la console n'a pas de sélecteur d'avatar — celui-ci est un
         écran de l'application mobile. Envoyer une chaîne vide écraserait le
         choix fait sur le téléphone. */
      await apiFetch("/users/me", {
        method: "PUT",
        body: { firstName, lastName, phoneNumber },
      });
      await onSaved();
      setState({ kind: "saved", message: "Profil enregistré." });
    } catch (caught) {
      setState({ kind: "error", message: describe(caught) });
    }
  }

  return (
    <Card
      title="Profil"
      description="Nom et numéro affichés dans la console et joints à vos actions."
    >
      <form onSubmit={submit} className="space-y-5 p-5">
        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <Label htmlFor="firstName">Prénom</Label>
            <input
              id="firstName"
              className={CONTROL}
              required
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="lastName">Nom</Label>
            <input
              id="lastName"
              className={CONTROL}
              required
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
            />
          </div>
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <Label htmlFor="phoneNumber">Numéro de téléphone</Label>
            <input
              id="phoneNumber"
              className={CONTROL}
              required
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)}
              placeholder="+22890000000"
            />
            <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
              Format international, indicatif compris. Ce numéro identifie le
              compte de façon unique.
            </p>
          </div>
          <div>
            <Label htmlFor="email">Adresse e-mail</Label>
            <input id="email" className={CONTROL} value={user.email} disabled />
            <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
              Non modifiable : elle sert d&apos;identifiant de connexion, et la
              changer exigerait une re-vérification par e-mail.
            </p>
          </div>
        </div>

        <FormFeedback state={state} />

        <div className="flex justify-end">
          {/* Désactivé tant que rien n'a bougé : un bouton actif sur un
              formulaire inchangé invite à un aller-retour réseau pour rien. */}
          <Button type="submit" size="sm" disabled={!dirty || state.kind === "saving"}>
            {state.kind === "saving" ? "Enregistrement…" : "Enregistrer"}
          </Button>
        </div>
      </form>
    </Card>
  );
}

/* ---------------------------------------------------------------------------
   Mot de passe
   ------------------------------------------------------------------------ */

/**
 * Changement de mot de passe.
 *
 * L'ANCIEN MOT DE PASSE EST EXIGÉ par le serveur, et c'est ce qui fait la
 * différence avec le flux « mot de passe oublié » : sans lui, un jeton volé ou
 * un poste laissé déverrouillé suffirait à s'approprier le compte.
 *
 * La confirmation est vérifiée ICI, côté client, et nulle part ailleurs — le
 * backend ne la connaît pas. Elle ne protège de rien, elle rattrape une faute
 * de frappe sur un champ dont on ne voit pas le contenu.
 *
 * Les règles de robustesse sont annoncées AVANT la saisie plutôt que reprochées
 * après : le serveur refuse en dessous de huit caractères, ou sans majuscule,
 * minuscule et chiffre.
 */
function PasswordCard() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [shown, setShown] = useState(false);
  const [state, setState] = useState<FormState>({ kind: "idle" });

  const mismatch = confirm.length > 0 && next !== confirm;

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (next !== confirm) {
      setState({
        kind: "error",
        message: "Les deux saisies du nouveau mot de passe ne correspondent pas.",
      });
      return;
    }

    setState({ kind: "saving" });

    try {
      await apiFetch("/users/me/password", {
        method: "POST",
        body: { currentPassword: current, newPassword: next },
      });
      setCurrent("");
      setNext("");
      setConfirm("");
      setState({ kind: "saved", message: "Mot de passe modifié." });
    } catch (caught) {
      setState({ kind: "error", message: describe(caught) });
    }
  }

  return (
    <Card
      title="Mot de passe"
      description="Huit caractères minimum, avec au moins une majuscule, une minuscule et un chiffre."
    >
      <form onSubmit={submit} className="space-y-5 p-5">
        <div className="max-w-md">
          <Label htmlFor="current">Mot de passe actuel</Label>
          <input
            id="current"
            type="password"
            autoComplete="current-password"
            className={CONTROL}
            required
            value={current}
            onChange={(event) => setCurrent(event.target.value)}
          />
        </div>

        <div className="grid max-w-2xl gap-5 sm:grid-cols-2">
          <div>
            <Label htmlFor="next">Nouveau mot de passe</Label>
            <div className="relative">
              <input
                id="next"
                type={shown ? "text" : "password"}
                autoComplete="new-password"
                className={`${CONTROL} pr-11`}
                required
                minLength={8}
                value={next}
                onChange={(event) => setNext(event.target.value)}
              />
              <button
                type="button"
                onClick={() => setShown((value) => !value)}
                aria-label={
                  shown ? "Masquer le mot de passe" : "Afficher le mot de passe"
                }
                className="absolute top-1/2 right-3 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              >
                {shown ? (
                  <EyeIcon className="size-5" />
                ) : (
                  <EyeCloseIcon className="size-5" />
                )}
              </button>
            </div>
          </div>

          <div>
            <Label htmlFor="confirm">Confirmation</Label>
            <input
              id="confirm"
              type={shown ? "text" : "password"}
              autoComplete="new-password"
              className={CONTROL}
              required
              value={confirm}
              onChange={(event) => setConfirm(event.target.value)}
              aria-invalid={mismatch}
            />
            {/* Signalé pendant la saisie, pas à la soumission : corriger une
                faute de frappe est plus facile quand on l'apprend tout de
                suite. */}
            {mismatch ? (
              <p className="mt-1.5 text-theme-xs text-error-500">
                Les deux saisies diffèrent.
              </p>
            ) : null}
          </div>
        </div>

        {/* Limite documentée du backend, dite au bon endroit : `logout` efface
            les jetons côté client mais l'API ne tient aucune liste de
            révocation (cf. `AuthController.logout`). Un jeton émis avant le
            changement reste donc valable jusqu'à son expiration naturelle. Le
            taire laisserait croire à une déconnexion générale qui n'a pas
            lieu. */}
        <p className="max-w-2xl rounded-lg border border-warning-300 bg-warning-50 px-4 py-3 text-theme-xs leading-relaxed text-warning-700 dark:border-warning-500/40 dark:bg-warning-500/10 dark:text-warning-400">
          Changer le mot de passe ne ferme pas les sessions déjà ouvertes sur
          d&apos;autres appareils : les jetons émis avant restent valables
          jusqu&apos;à leur expiration. En cas de compromission avérée, faites
          verrouiller le compte par un autre administrateur.
        </p>

        <FormFeedback state={state} />

        <div className="flex justify-end">
          <Button
            type="submit"
            size="sm"
            disabled={
              state.kind === "saving" || !current || !next || next !== confirm
            }
          >
            {state.kind === "saving" ? "Modification…" : "Changer le mot de passe"}
          </Button>
        </div>
      </form>
    </Card>
  );
}

/* ---------------------------------------------------------------------------
   Apparence
   ------------------------------------------------------------------------ */

/**
 * Thème clair ou sombre.
 *
 * PRÉFÉRENCE PUREMENT LOCALE : elle est enregistrée dans `localStorage` de ce
 * navigateur et ne remonte à aucun serveur. Elle ne suit donc pas
 * l'administrateur d'un poste à l'autre — c'est ce qu'on attend d'un réglage
 * d'affichage, et ça évite un aller-retour réseau pour changer une couleur.
 */
function AppearanceCard() {
  const { theme, toggleTheme } = useTheme();

  return (
    <Card
      title="Apparence"
      description="Réglage propre à ce navigateur, mémorisé localement."
    >
      <div className="flex flex-wrap items-center justify-between gap-4 p-5">
        <div>
          <p className="text-sm font-medium text-gray-800 dark:text-white/90">
            Thème {theme === "dark" ? "sombre" : "clair"}
          </p>
          <p className="mt-1 text-theme-xs text-gray-500 dark:text-gray-400">
            Le thème sombre réduit la fatigue visuelle sur les longues sessions
            de traitement d&apos;alertes.
          </p>
        </div>
        <Button type="button" size="sm" variant="outline" onClick={toggleTheme}>
          Passer en thème {theme === "dark" ? "clair" : "sombre"}
        </Button>
      </div>
    </Card>
  );
}

/* ---------------------------------------------------------------------------
   Sécurité de la session
   ------------------------------------------------------------------------ */

/**
 * État de sécurité du compte, en lecture seule.
 *
 * Ces informations ne sont pas décoratives : la dernière adresse IP et le
 * dernier appareil connus sont ce qui permet à un administrateur de repérer une
 * connexion qu'il ne reconnaît pas. Le backend les met à jour à chaque
 * authentification et déclenche une notification par e-mail quand elles
 * changent.
 */
function SecurityCard({ user }: { user: CurrentUser }) {
  return (
    <Card
      title="Sécurité et session"
      description="Dernière connexion enregistrée pour ce compte. Une connexion que vous ne reconnaissez pas doit être signalée immédiatement."
    >
      <dl className="grid gap-5 p-5 sm:grid-cols-2 lg:grid-cols-4">
        <DataItem label="Rôle">
          {/* Le rôle n'est pas dans `/users/me` — le DTO exclut les autorités.
              S'il est affiché ici, c'est que la sonde d'autorisation a répondu
              200 à l'ouverture de session : cet écran n'existe que pour un
              administrateur. */}
          <Badge size="sm" color="primary">
            ADMIN
          </Badge>
        </DataItem>
        <DataItem label="Niveau de vérification">
          <KycBadge level={user.kycLevel} />
        </DataItem>
        <DataItem label="Compte créé le">
          {formatDateTime(user.createdAt)}
        </DataItem>
        <DataItem label="Pays">{user.countryCode ?? "Non renseigné"}</DataItem>
        <DataItem label="Dernière adresse IP" mono>
          {user.lastKnownIp ?? "—"}
        </DataItem>
        <DataItem label="Dernier appareil connu" mono>
          {user.lastKnownUserAgent ?? "—"}
        </DataItem>
      </dl>

      <div className="border-t border-gray-100 px-5 py-4 dark:border-gray-800">
        <p className="text-theme-xs leading-relaxed text-gray-500 dark:text-gray-400">
          Cinq tentatives de connexion échouées verrouillent automatiquement le
          compte pendant trente minutes. Toute connexion depuis un appareil ou
          une adresse inconnue déclenche une notification par e-mail.
        </p>
      </div>
    </Card>
  );
}

/* ---------------------------------------------------------------------------
   Retour de formulaire
   ------------------------------------------------------------------------ */

type FormState =
  | { kind: "idle" }
  | { kind: "saving" }
  | { kind: "saved"; message: string }
  | { kind: "error"; message: string };

/**
 * `role="status"` pour un succès, `role="alert"` pour un échec : le second
 * interrompt la lecture en cours d'un lecteur d'écran, le premier attend une
 * pause. Un message de confirmation n'a pas à couper la parole.
 */
function FormFeedback({ state }: { state: FormState }) {
  if (state.kind === "saved") {
    return (
      <p
        role="status"
        className="max-w-2xl rounded-lg border border-success-300 bg-success-50 px-4 py-3 text-sm text-success-700 dark:border-success-500/40 dark:bg-success-500/10 dark:text-success-500"
      >
        {state.message}
      </p>
    );
  }

  if (state.kind === "error") {
    return (
      <p
        role="alert"
        className="max-w-2xl rounded-lg border border-error-300 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-500/40 dark:bg-error-500/10 dark:text-error-400"
      >
        {state.message}
      </p>
    );
  }

  return null;
}

/**
 * Message affichable pour une erreur d'écriture.
 *
 * Les erreurs de validation Bean Validation arrivent dans `details`, champ par
 * champ, et sont plus précises que le message général : « Le mot de passe doit
 * contenir au moins une majuscule… » plutôt que « Les données envoyées sont
 * invalides ». On les préfère quand elles existent.
 */
function describe(caught: unknown): string {
  if (caught instanceof ApiError) {
    const fieldErrors = Object.values(caught.details);
    if (fieldErrors.length > 0) return fieldErrors.join(" ");
    return caught.message;
  }
  return "Impossible de joindre le serveur.";
}
