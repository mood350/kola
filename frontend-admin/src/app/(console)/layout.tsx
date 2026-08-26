"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSidebar } from "@/context/SidebarContext";
import AppHeader from "@/layout/AppHeader";
import AppSidebar from "@/layout/AppSidebar";
import Backdrop from "@/layout/Backdrop";
import { useSession } from "@/lib/session";

/**
 * Enveloppe de la console : rail + en-tête (TailAdmin) et garde de session.
 *
 * LA GARDE N'EST PAS UNE PROTECTION, ET IL FAUT LE SAVOIR. Elle décide de ce
 * qu'on AFFICHE, pas de ce qu'on autorise : tout ce qui est ici s'exécute dans
 * le navigateur et peut être contourné. La sécurité réelle est côté serveur —
 * `SecurityConfig` exige l'autorité ADMIN sur `/api/admin/**`, vérifiée à
 * chaque requête. Quelqu'un qui forcerait l'affichage de ces écrans n'obtiendrait
 * que des 403 en série.
 *
 * Ce qu'elle apporte : ne pas montrer une console vide à qui n'y a pas droit, et
 * rediriger proprement vers la connexion plutôt que d'afficher des tableaux en
 * erreur.
 *
 * La marge gauche suit l'état du rail, exactement comme dans le template : 290 px
 * déployé, 90 px replié, aucune sous `lg` où le rail devient un tiroir.
 */
export default function ConsoleLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isExpanded, isHovered, isMobileOpen } = useSidebar();
  const { status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === "anonymous") router.replace("/connexion");
  }, [status, router]);

  /**
   * Tant que la session n'est pas tranchée, on n'affiche NI la console NI une
   * redirection. Rendre les écrans pendant la vérification déclencherait leurs
   * requêtes sans jeton confirmé ; rediriger tout de suite éjecterait un
   * utilisateur légitime dont les jetons sont encore en cours de contrôle.
   */
  if (status !== "authenticated") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {status === "loading"
            ? "Vérification de la session…"
            : "Redirection…"}
        </p>
      </div>
    );
  }

  const mainContentMargin = isMobileOpen
    ? "ml-0"
    : isExpanded || isHovered
      ? "lg:ml-[290px]"
      : "lg:ml-[90px]";

  return (
    <div className="min-h-screen xl:flex">
      <AppSidebar />
      <Backdrop />
      <div
        className={`flex-1 transition-all duration-300 ease-in-out ${mainContentMargin}`}
      >
        <AppHeader />
        <main
          id="contenu"
          className="mx-auto max-w-(--breakpoint-2xl) p-4 md:p-6"
        >
          {children}
        </main>
      </div>
    </div>
  );
}
