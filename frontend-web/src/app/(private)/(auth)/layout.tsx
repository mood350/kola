"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useSession } from "@/lib/session";
import { BrandMark } from "@/components/layout/app-shell";
import { Spinner } from "@/components/ui/primitives";

/**
 * Coque des écrans d'authentification.
 *
 * Deux colonnes à partir de `lg` : le formulaire à gauche, un panneau de marque
 * à droite. Ce panneau n'est pas décoratif — il porte ce que le produit promet
 * (épargne, score, crédit) et occupe l'espace que, sinon, un formulaire de deux
 * champs laisserait vide au milieu d'un écran de 1 400 pixels.
 *
 * La redirection inverse de celle du layout connecté : quelqu'un de déjà
 * authentifié qui revient sur `/connexion` (signet, bouton retour) est renvoyé
 * dans l'application plutôt que de reconnecter une session ouverte.
 */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  const { status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === "authenticated") router.replace("/mon-compte");
  }, [status, router]);

  if (status === "loading") {
    return (
      <div className="flex min-h-dvh items-center justify-center text-kola-600">
        <Spinner className="size-8" />
      </div>
    );
  }

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[1fr_minmax(0,28rem)]">
      <div className="flex min-h-dvh flex-col px-5 py-8 sm:px-8 lg:px-16">
        <Link href="/connexion" className="mb-10 inline-flex items-center gap-2.5 self-start">
          <BrandMark />
          <span className="font-display text-xl font-semibold tracking-tight text-ink-950">
            Kola
          </span>
        </Link>

        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center">
          {children}
        </div>

        <p className="mx-auto mt-10 w-full max-w-md text-xs text-ink-400">
          Kola — mobile money, épargne et micro-crédit en Afrique de l&apos;Ouest.
        </p>
      </div>

      <aside className="hidden bg-kola-700 p-12 text-white lg:flex lg:flex-col lg:justify-center">
        <p className="font-display text-3xl leading-tight font-semibold">
          Votre argent, votre score, vos possibilités.
        </p>
        <p className="mt-4 max-w-sm text-kola-100">
          Le même compte que sur votre téléphone : soldes, transferts, coffres
          d&apos;épargne bloqués, score de confiance et prêts.
        </p>

        <ul className="mt-10 space-y-4 text-sm text-kola-50">
          {[
            ["Transférez", "Vers vos bénéficiaires Mobile Money, en quelques secondes."],
            ["Épargnez", "Des coffres bloqués jusqu'à la date que vous fixez."],
            ["Empruntez", "Un score calculé sur vos habitudes, pas sur votre carnet d'adresses."],
          ].map(([title, description]) => (
            <li key={title} className="border-l-2 border-kola-400 pl-4">
              <p className="font-semibold text-white">{title}</p>
              <p className="text-kola-200">{description}</p>
            </li>
          ))}
        </ul>
      </aside>
    </div>
  );
}
