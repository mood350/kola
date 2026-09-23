"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useSession } from "@/lib/session";
import { AppShell } from "@/components/layout/app-shell";
import { Spinner } from "@/components/ui/primitives";

/**
 * Garde des écrans connectés.
 *
 * CE QU'ELLE EST : un confort de navigation. CE QU'ELLE N'EST PAS : une
 * protection. Rien ici ne défend une donnée — chaque endpoint est gardé côté
 * serveur, et un client qui contournerait cette redirection n'obtiendrait que
 * des 401. Son rôle est d'éviter d'afficher une application vide, dont chaque
 * panneau échouerait, à quelqu'un dont la session est terminée.
 *
 * L'état « loading » est ce qui empêche un clignotement : sans lui, un
 * rechargement de page enverrait vers l'écran de connexion quelqu'un de
 * parfaitement connecté, le temps que les jetons soient lus et le profil
 * rechargé.
 */
export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { status, signedOut } = useSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status !== "anonymous") return;
    /* La destination voulue est transmise à l'écran de connexion, qui y renvoie
       après authentification : quelqu'un qui ouvre un lien vers un coffre
       précis doit retrouver ce coffre, pas l'accueil.
       SAUF après une déconnexion volontaire — on ne ramène pas quelqu'un sur
       la page qu'il vient délibérément de quitter. */
    const keepDestination =
      !signedOut && pathname && pathname !== "/mon-compte";
    const next = keepDestination ? `?suite=${encodeURIComponent(pathname)}` : "";
    router.replace(`/connexion${next}`);
  }, [status, router, pathname, signedOut]);

  if (status !== "authenticated") {
    return (
      <div className="flex min-h-dvh items-center justify-center text-kola-600">
        <Spinner className="size-8" />
      </div>
    );
  }

  return <AppShell>{children}</AppShell>;
}
