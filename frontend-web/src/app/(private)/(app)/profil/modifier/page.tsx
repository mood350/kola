"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/lib/session";
import { userApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import { isValidPhone, normalizePhone } from "@/lib/countries";
import { AVATAR_COLOR, AVATAR_EMOJI, AVATAR_IDS } from "@/lib/labels";
import { cn } from "@/lib/cn";
import { Alert, Button, Card, Field, Input, Skeleton } from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";

/**
 * Modification du profil.
 *
 * L'AVATAR EST UN CHOIX DANS UN CATALOGUE, pas un fichier à téléverser : le
 * backend ne stocke qu'un identifiant (« avatar_03 »). Rien n'est envoyé, rien
 * n'est hébergé, et l'avatar choisi ici est celui qui s'affichera sur le
 * téléphone.
 *
 * L'e-mail ne figure pas dans ce formulaire : `UpdateProfileRequest` ne
 * l'accepte pas. C'est l'identifiant de connexion et la cible des codes de
 * sécurité — le changer demande un parcours de vérification qui n'existe pas
 * côté backend. Mieux vaut ne pas afficher un champ qui serait ignoré.
 */
export default function EditProfilePage() {
  const router = useRouter();
  const { user, reloadUser } = useSession();

  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [lastName, setLastName] = useState(user?.lastName ?? "");
  const [phone, setPhone] = useState(user?.phoneNumber ?? "");
  const [avatar, setAvatar] = useState<string | null>(user?.avatar ?? null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [touched, setTouched] = useState(false);

  if (!user) {
    return (
      <>
        <PageHeader title="Modifier mes informations" backHref="/profil" backLabel="Profil" />
        <Card className="space-y-3">
          <Skeleton className="h-11 w-full" />
          <Skeleton className="h-11 w-full" />
        </Card>
      </>
    );
  }

  const phoneError =
    touched && !isValidPhone(phone)
      ? "Numéro attendu au format international, ex : +22890000000"
      : null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!firstName.trim() || !lastName.trim() || !isValidPhone(phone)) return;

    setError(null);
    setSubmitting(true);
    try {
      await userApi.updateProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phoneNumber: normalizePhone(phone),
        avatar,
      });
      /* On relit le serveur plutôt que de recopier la réponse dans l'état : le
         backend a le dernier mot sur ce qui a été enregistré (il rogne les
         espaces, par exemple). */
      await reloadUser();
      router.push("/profil");
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Modifier mes informations"
        backHref="/profil"
        backLabel="Profil"
      />

      <Card>
        <form onSubmit={handleSubmit} className="space-y-5" noValidate>
          {error ? <Alert tone="danger">{error}</Alert> : null}

          <fieldset>
            <legend className="mb-3 text-sm font-medium text-ink-800">Avatar</legend>
            <div className="flex flex-wrap gap-3">
              {AVATAR_IDS.map((id) => {
                const selected = avatar === id;
                const color = AVATAR_COLOR[id];
                return (
                  <label
                    key={id}
                    className={cn(
                      "flex size-14 cursor-pointer items-center justify-center rounded-full text-2xl transition-all",
                      selected
                        ? "ring-2 ring-kola-600 ring-offset-2"
                        : "opacity-70 hover:opacity-100"
                    )}
                    style={{ backgroundColor: `${color}26` }}
                  >
                    <input
                      type="radio"
                      name="avatar"
                      className="sr-only"
                      checked={selected}
                      onChange={() => setAvatar(id)}
                    />
                    <span aria-hidden>{AVATAR_EMOJI[id]}</span>
                    <span className="sr-only">Avatar {id.replace("avatar_", "")}</span>
                  </label>
                );
              })}
            </div>
          </fieldset>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Prénom" htmlFor="firstName">
              <Input
                id="firstName"
                required
                value={firstName}
                onChange={(event) => setFirstName(event.target.value)}
              />
            </Field>
            <Field label="Nom" htmlFor="lastName">
              <Input
                id="lastName"
                required
                value={lastName}
                onChange={(event) => setLastName(event.target.value)}
              />
            </Field>
          </div>

          <Field label="Téléphone" htmlFor="phone" error={phoneError}>
            <Input
              id="phone"
              type="tel"
              inputMode="tel"
              required
              invalid={Boolean(phoneError)}
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
            />
          </Field>

          <Field
            label="Adresse e-mail"
            htmlFor="email"
            hint="L'e-mail sert d'identifiant de connexion et ne peut pas être modifié ici."
          >
            <Input id="email" value={user.email ?? ""} disabled />
          </Field>

          <div className="flex gap-2">
            <Button type="submit" loading={submitting} full>
              Enregistrer
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
