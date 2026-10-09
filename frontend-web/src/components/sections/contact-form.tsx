"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { gsap, useGSAP, prefersReducedMotion } from "@/lib/gsap";
import { cn } from "@/lib/cn";
import { CONTACT_EMAIL } from "@/lib/content";
import { RESPONSE_PROMISE } from "@/lib/business";

/**
 * Le succès mène à `/merci` plutôt qu'à un bandeau affiché sous le formulaire.
 *
 * Trois raisons, dans l'ordre d'importance :
 *
 *   1. le succès devient MESURABLE — une visite de `/merci` correspond
 *      exactement à un message envoyé, sans instrumenter le moindre clic ;
 *   2. la page de remerciement a la place de répondre à la question qui suit
 *      immédiatement l'envoi (« quand aurai-je une réponse ? »), là où un
 *      bandeau ne tient qu'une phrase ;
 *   3. l'état survit à un rafraîchissement, contrairement à un message
 *      d'interface qui disparaît au premier rechargement.
 *
 * L'ÉCHEC, LUI, RESTE SUR PLACE : emmener quelqu'un sur une autre page pour lui
 * annoncer que son message n'est pas parti lui ferait perdre son texte. Le
 * message d'erreur s'affiche sous le bouton, formulaire intact, avec l'adresse
 * e-mail directe comme voie de repli.
 */
type Status = "idle" | "sending" | "error";
type FieldErrors = Partial<Record<"name" | "email" | "subject" | "message", string>>;

const FIELD =
  "w-full rounded-2xl bg-canvas px-4 py-3 text-[0.9375rem] text-ink-900 " +
  "outline-none transition-[background-color,box-shadow] duration-200 " +
  "placeholder:text-ink-400 hover:bg-ink-100 " +
  "focus:bg-surface focus:shadow-[0_0_0_2px_var(--color-kola-500)]";

export function ContactForm() {
  const root = useRef<HTMLFormElement>(null);
  const router = useRouter();
  const [status, setStatus] = useState<Status>("idle");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [failure, setFailure] = useState<string | null>(null);

  /**
   * Le bloc de retour (succès ou erreur) apparaît par un fondu court.
   * `autoAlpha` plutôt qu'`opacity` : hors état, le bloc n'est pas rendu du
   * tout, mais quand il l'est il doit être immédiatement annonçable — d'où
   * `role="status"` sur le conteneur, qui fait lire le message par les
   * lecteurs d'écran sans déplacer le focus.
   */
  useGSAP(
    () => {
      if (prefersReducedMotion()) return;
      const el = root.current?.querySelector("[data-feedback]");
      if (!el) return;
      gsap.fromTo(
        el,
        { autoAlpha: 0, y: 6 },
        { autoAlpha: 1, y: 0, duration: 0.3, ease: "power2.out" }
      );
    },
    { scope: root, dependencies: [status] }
  );

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));

    setStatus("sending");
    setErrors({});
    setFailure(null);

    try {
      const response = await fetch("/api/contact", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      const body = await response.json().catch(() => ({}));

      if (response.status === 422 && body.errors) {
        setErrors(body.errors);
        setStatus("idle");
        return;
      }
      if (!response.ok) {
        setFailure(body.error ?? "L'envoi a échoué.");
        setStatus("error");
        return;
      }

      /* Le formulaire est vidé AVANT la navigation : le composant reste monté
         le temps de la transition de route, et un champ encore rempli
         réapparaîtrait une fraction de seconde si l'utilisateur revient en
         arrière. `status` reste à « sending », ce qui garde le bouton
         désactivé et empêche un second envoi pendant le changement de page. */
      form.reset();
      router.push("/merci");
    } catch {
      setFailure("Impossible de joindre le serveur.");
      setStatus("error");
    }
  }

  return (
    <form
      ref={root}
      onSubmit={handleSubmit}
      noValidate
      className="rounded-panel bg-surface p-7 border border-hairline shadow-card sm:p-9"
    >
      <h2 className="text-xl font-semibold">Écrivez-nous</h2>
      {/* La promesse de délai vient de la constante partagée : l'engagement
          affiché ici, sur la page de remerciement et dans les données
          structurées est le même texte, pas trois formulations voisines. */}
      <p className="mt-2 text-[0.9375rem] text-ink-500">
        {RESPONSE_PROMISE.headline}.
      </p>

      <div className="mt-7 flex flex-col gap-5">
        <Field
          id="name"
          label="Nom"
          error={errors.name}
          input={<input id="name" name="name" autoComplete="name" required className={FIELD} />}
        />
        <Field
          id="email"
          label="E-mail"
          error={errors.email}
          input={
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              className={FIELD}
            />
          }
        />
        <Field
          id="subject"
          label="Sujet"
          optional
          error={errors.subject}
          input={<input id="subject" name="subject" className={FIELD} />}
        />
        <Field
          id="message"
          label="Message"
          error={errors.message}
          input={
            <textarea
              id="message"
              name="message"
              rows={5}
              required
              className={cn(FIELD, "resize-y")}
            />
          }
        />

        {/* Pot de miel : masqué à l'œil ET aux lecteurs d'écran, exclu du
            parcours clavier. Un robot qui remplit tous les champs le trahit. */}
        <div aria-hidden="true" className="absolute -left-[9999px]">
          <label htmlFor="website">Ne pas remplir</label>
          <input id="website" name="website" tabIndex={-1} autoComplete="off" />
        </div>
      </div>

      <button
        type="submit"
        disabled={status === "sending"}
        className={cn(
          "mt-7 inline-flex h-13 w-full cursor-pointer items-center justify-center rounded-full",
          "bg-kola-600 px-7 font-medium text-white border border-hairline shadow-card",
          "transition-[background-color,transform] duration-200 ease-[var(--ease-editorial)]",
          "hover:-translate-y-0.5 hover:bg-kola-700 active:translate-y-0",
          "disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
        )}
      >
        {status === "sending" ? "Envoi en cours…" : "Envoyer le message"}
      </button>

      {/* `role="status"` : le retour est annoncé sans voler le focus, ce qui
          laisserait l'utilisateur perdu au milieu du formulaire. */}
      <div role="status" aria-live="polite" className="mt-5">
        {status === "error" ? (
          <p
            data-feedback
            className="rounded-2xl border border-ochre-300 bg-ochre-200/40 px-5 py-4 text-[0.9375rem] leading-relaxed text-ink-700"
          >
            {failure} Écrivez-nous directement à{" "}
            <a
              href={`mailto:${CONTACT_EMAIL}`}
              className="font-medium text-kola-700 underline underline-offset-2"
            >
              {CONTACT_EMAIL}
            </a>
            .
          </p>
        ) : null}
      </div>
    </form>
  );
}

/**
 * Champ de formulaire.
 *
 * Le libellé est toujours visible, jamais remplacé par un placeholder : un
 * placeholder disparaît à la saisie, et l'utilisateur qui revient sur un
 * formulaire à moitié rempli ne sait plus ce qu'attend le champ.
 * L'erreur est affichée sous le champ concerné, et non regroupée en haut.
 */
function Field({
  id,
  label,
  input,
  error,
  optional,
}: {
  id: string;
  label: string;
  input: React.ReactNode;
  error?: string;
  optional?: boolean;
}) {
  return (
    <div>
      <label
        htmlFor={id}
        className="mb-2 flex items-baseline gap-2 text-[0.8125rem] font-medium text-ink-800"
      >
        {label}
        {optional ? (
          <span className="text-[0.75rem] font-normal text-ink-400">
            facultatif
          </span>
        ) : null}
      </label>
      {input}
      {error ? (
        <p className="mt-2 text-[0.8125rem] text-ochre-500">{error}</p>
      ) : null}
    </div>
  );
}
