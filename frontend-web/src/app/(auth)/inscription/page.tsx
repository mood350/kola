"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import {
  COUNTRIES,
  DEFAULT_COUNTRY,
  PASSWORD_RULES,
  dialCodeFor,
  isValidPassword,
  isValidPhone,
  normalizePhone,
} from "@/lib/countries";
import { Alert, Button, Field, Input, Select } from "@/components/ui/primitives";
import { PasswordInput } from "@/components/ui/password-input";
import { CheckIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";

/**
 * Inscription.
 *
 * CE QUE CET ÉCRAN NE FAIT PAS : connecter l'utilisateur. `POST /auth/register`
 * répond 202 sans corps et n'ouvre aucune session — le compte reste désactivé
 * jusqu'à la saisie du code à 6 chiffres envoyé par e-mail. La redirection va
 * donc vers la confirmation, et l'écran le dit avant l'envoi plutôt que de
 * laisser l'utilisateur chercher pourquoi sa connexion échoue.
 */
export default function RegisterPage() {
  const router = useRouter();

  const [firstname, setFirstname] = useState("");
  const [lastname, setLastname] = useState("");
  const [email, setEmail] = useState("");
  const [countryCode, setCountryCode] = useState(DEFAULT_COUNTRY);
  const [phone, setPhone] = useState(dialCodeFor(DEFAULT_COUNTRY));
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  /* Erreurs affichées seulement après une tentative d'envoi : souligner en
     rouge un champ qu'on n'a pas fini de remplir est une réprimande, pas une
     aide. */
  const [touched, setTouched] = useState(false);

  const phoneError =
    touched && !isValidPhone(phone)
      ? "Numéro attendu au format international, ex : +22890000000"
      : null;
  const passwordError =
    touched && !isValidPassword(password)
      ? "Le mot de passe ne respecte pas les règles ci-dessous."
      : null;

  const handleCountryChange = (code: string) => {
    setCountryCode(code);
    /* L'indicatif suit le pays tant que l'utilisateur n'a pas commencé à
       composer un numéro : le remplacer après coup effacerait sa saisie. */
    const previous = dialCodeFor(countryCode);
    if (phone === previous || phone === "") setPhone(dialCodeFor(code));
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setTouched(true);
    setError(null);

    if (!isValidPhone(phone) || !isValidPassword(password)) return;

    setSubmitting(true);
    try {
      await authApi.register({
        firstname: firstname.trim(),
        lastname: lastname.trim(),
        email: email.trim(),
        phoneNumber: normalizePhone(phone),
        countryCode,
        password,
      });
      router.push(`/confirmation?email=${encodeURIComponent(email.trim())}`);
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  return (
    <div className="rise">
      <h1 className="font-display text-3xl font-semibold tracking-tight text-ink-950">
        Créer un compte
      </h1>
      <p className="mt-2 text-ink-500">
        Quelques informations, puis un code de confirmation par e-mail.
      </p>

      <form onSubmit={handleSubmit} className="mt-8 space-y-4" noValidate>
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Prénom" htmlFor="firstname">
            <Input
              id="firstname"
              autoComplete="given-name"
              required
              value={firstname}
              onChange={(event) => setFirstname(event.target.value)}
            />
          </Field>
          <Field label="Nom" htmlFor="lastname">
            <Input
              id="lastname"
              autoComplete="family-name"
              required
              value={lastname}
              onChange={(event) => setLastname(event.target.value)}
            />
          </Field>
        </div>

        <Field label="Adresse e-mail" htmlFor="email" hint="Le code de confirmation y sera envoyé.">
          <Input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-[minmax(0,10rem)_1fr]">
          <Field label="Pays" htmlFor="country">
            <Select
              id="country"
              value={countryCode}
              onChange={(event) => handleCountryChange(event.target.value)}
            >
              {COUNTRIES.map((country) => (
                <option key={country.code} value={country.code}>
                  {country.name}
                </option>
              ))}
            </Select>
          </Field>

          <Field label="Téléphone" htmlFor="phone" error={phoneError}>
            <Input
              id="phone"
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              required
              invalid={Boolean(phoneError)}
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="+22890000000"
            />
          </Field>
        </div>

        <Field label="Mot de passe" htmlFor="new-password" error={passwordError}>
          <PasswordInput
            id="new-password"
            autoComplete="new-password"
            required
            invalid={Boolean(passwordError)}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Field>

        {/* Les règles sont affichées et cochées en direct : les énoncer après
            un refus du serveur oblige à recommencer une saisie déjà faite. */}
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

        <Button type="submit" size="lg" full loading={submitting}>
          Créer mon compte
        </Button>
      </form>

      <p className="mt-8 border-t border-line pt-6 text-sm text-ink-600">
        Vous avez déjà un compte ?{" "}
        <Link href="/connexion" className="font-semibold text-kola-600 hover:underline">
          Se connecter
        </Link>
      </p>
    </div>
  );
}
