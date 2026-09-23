import { NextResponse } from "next/server";

/**
 * Réception des messages du formulaire de contact.
 *
 * CE QUE FAIT CE HANDLER, ET CE QU'IL NE FAIT PAS
 *
 * Il valide la saisie puis relaie le message vers `CONTACT_WEBHOOK_URL`
 * (Formspree, Resend, un webhook Slack, l'API du backend Spring — n'importe
 * quel point d'entrée HTTP).
 *
 * Si cette variable n'est PAS définie, il répond 503 au lieu de renvoyer un
 * faux succès. C'est délibéré : un formulaire qui affiche « message envoyé »
 * alors que rien n'a été transmis est pire que pas de formulaire du tout — le
 * visiteur pense avoir été entendu et n'essaie pas d'autre canal. L'interface
 * bascule alors sur l'adresse e-mail directe.
 */

const MAX = { name: 80, email: 160, subject: 120, message: 4000 };

type Payload = {
  name?: unknown;
  email?: unknown;
  subject?: unknown;
  message?: unknown;
  /** Champ piège, invisible pour un humain : rempli, c'est un robot. */
  website?: unknown;
};

function asText(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

export async function POST(request: Request) {
  let payload: Payload;
  try {
    payload = await request.json();
  } catch {
    return NextResponse.json(
      { error: "Requête illisible." },
      { status: 400 }
    );
  }

  // Pot de miel : on répond 200 sans rien transmettre. Renvoyer une erreur
  // apprendrait au robot que le champ est piégé.
  if (asText(payload.website)) {
    return NextResponse.json({ ok: true });
  }

  const name = asText(payload.name);
  const email = asText(payload.email);
  const subject = asText(payload.subject);
  const message = asText(payload.message);

  const errors: Record<string, string> = {};
  if (!name) errors.name = "Le nom est obligatoire.";
  else if (name.length > MAX.name) errors.name = "Le nom est trop long.";

  if (!email) errors.email = "L'e-mail est obligatoire.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email) || email.length > MAX.email)
    errors.email = "Cette adresse e-mail n'est pas valide.";

  if (!message) errors.message = "Le message est obligatoire.";
  else if (message.length > MAX.message) errors.message = "Le message est trop long.";

  if (subject.length > MAX.subject) errors.subject = "Le sujet est trop long.";

  if (Object.keys(errors).length > 0) {
    return NextResponse.json({ errors }, { status: 422 });
  }

  const endpoint = process.env.CONTACT_WEBHOOK_URL;
  if (!endpoint) {
    return NextResponse.json(
      {
        error:
          "L'envoi de messages n'est pas encore configuré sur ce site.",
        unconfigured: true,
      },
      { status: 503 }
    );
  }

  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name,
        email,
        subject: subject || "(sans sujet)",
        message,
        receivedAt: new Date().toISOString(),
      }),
    });

    if (!response.ok) {
      // On ne remonte pas le détail de la réponse amont : il peut contenir des
      // identifiants ou la structure interne du fournisseur.
      console.error("[contact] relais en échec, statut", response.status);
      return NextResponse.json(
        { error: "L'envoi a échoué. Réessayez dans un instant." },
        { status: 502 }
      );
    }
  } catch (error) {
    console.error("[contact] relais injoignable", error);
    return NextResponse.json(
      { error: "L'envoi a échoué. Réessayez dans un instant." },
      { status: 502 }
    );
  }

  return NextResponse.json({ ok: true });
}
