"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ThemeToggleButton } from "@/components/common/ThemeToggleButton";
import { useSidebar } from "@/context/SidebarContext";
import { useSession } from "@/lib/session";
import { displayName } from "@/lib/format";
import { KolaMark } from "@/components/kola/brand";

/**
 * Barre supérieure.
 *
 * DIFFÉRENCES ASSUMÉES AVEC LE TEMPLATE, et pourquoi :
 *
 * • PAS DE CHAMP DE RECHERCHE GLOBALE. Celui de TailAdmin ne cherche rien — il
 *   attend un moteur qui n'existe pas ici. Un champ qui ne répond pas se
 *   remarque tout de suite et fait douter du reste de l'interface. La recherche
 *   réelle, filtrée côté serveur, vit sur l'écran Utilisateurs.
 *
 * • PAS DE CLOCHE DE NOTIFICATIONS. Aucun endpoint ne les alimente. Une cloche
 *   perpétuellement vide — ou pire, garnie d'exemples — sur une console qui
 *   traite des alertes de conformité serait un contresens : l'opérateur
 *   apprendrait à ignorer un signal qui, un jour, devra compter.
 *
 * • LE MENU UTILISATEUR AFFICHE LE VRAI COMPTE connecté, lu depuis la session,
 *   et déconnecte réellement.
 */
const AppHeader: React.FC = () => {
  const { isMobileOpen, toggleSidebar, toggleMobileSidebar } = useSidebar();
  const { user, logout } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleToggle = () => {
    if (window.innerWidth >= 1024) {
      toggleSidebar();
    } else {
      toggleMobileSidebar();
    }
  };

  return (
    <header className="sticky top-0 z-40 flex w-full border-gray-200 bg-white lg:border-b dark:border-gray-800 dark:bg-gray-900">
      <div className="flex grow flex-col items-center justify-between lg:flex-row lg:px-6">
        <div className="flex w-full items-center justify-between gap-2 border-b border-gray-200 px-3 py-3 sm:gap-4 lg:justify-normal lg:border-b-0 lg:px-0 lg:py-4 dark:border-gray-800">
          <button
            className="h-10 w-10 items-center justify-center rounded-lg border-gray-200 text-gray-500 lg:flex lg:h-11 lg:w-11 lg:border dark:border-gray-800 dark:text-gray-400"
            onClick={handleToggle}
            aria-expanded={isMobileOpen}
            aria-label={
              isMobileOpen ? "Fermer la navigation" : "Ouvrir la navigation"
            }
          >
            {isMobileOpen ? (
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M6 6l12 12M18 6L6 18"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
              </svg>
            ) : (
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M4 6h16M4 12h16M4 18h10"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
              </svg>
            )}
          </button>

          {/* Le rail est masqué sous `lg` : la marque revient ici pour que la
              page ne se retrouve jamais sans point de retour à l'accueil. */}
          <Link href="/" className="flex items-center gap-2 lg:hidden">
            <KolaMark className="h-7 w-7 text-brand-500" />
            <span className="text-sm font-semibold text-gray-900 dark:text-white/90">
              Kola
            </span>
          </Link>
        </div>

        <div className="flex w-full items-center justify-end gap-3 px-5 py-3 lg:px-0 lg:py-0">
          <ThemeToggleButton />

          <div className="relative">
            <button
              type="button"
              onClick={() => setMenuOpen((open) => !open)}
              aria-expanded={menuOpen}
              aria-haspopup="menu"
              className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-left transition-colors hover:bg-gray-100 dark:hover:bg-white/5"
            >
              {/* Initiale en pastille : l'API ne sert pas de photo de profil,
                  seulement une référence d'avatar prédéfinie côté mobile. */}
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-50 text-sm font-semibold text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">
                {(user?.firstName ?? user?.email ?? "?").charAt(0).toUpperCase()}
              </span>
              <span className="hidden sm:block">
                <span className="block text-theme-sm font-medium text-gray-800 dark:text-white/90">
                  {user
                    ? displayName(user.firstName, user.lastName, user.email)
                    : "—"}
                </span>
                <span className="block text-theme-xs text-gray-500 dark:text-gray-400">
                  Administrateur
                </span>
              </span>
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
                className={`text-gray-500 transition-transform ${menuOpen ? "rotate-180" : ""}`}
              >
                <path
                  d="M6 9l6 6 6-6"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </button>

            {menuOpen ? (
              <>
                {/* Voile transparent : un clic n'importe où referme le menu.
                    Sans lui, il resterait ouvert derrière la page une fois
                    l'attention partie ailleurs. */}
                <button
                  type="button"
                  aria-label="Fermer le menu"
                  className="fixed inset-0 z-40 cursor-default"
                  onClick={() => setMenuOpen(false)}
                />
                <div
                  role="menu"
                  className="absolute right-0 z-50 mt-2 w-64 rounded-2xl border border-gray-200 bg-white p-3 shadow-theme-lg dark:border-gray-800 dark:bg-gray-900"
                >
                  <p className="px-2 pb-2 text-theme-xs break-all text-gray-500 dark:text-gray-400">
                    {user?.email}
                  </p>
                  {/* Doublon assumé avec l'entrée du rail : le menu utilisateur
                      est l'endroit où l'on cherche instinctivement ses propres
                      réglages, et il reste atteignable quand le rail est replié
                      ou masqué sous `lg`. */}
                  <Link
                    href="/parametres"
                    role="menuitem"
                    onClick={() => setMenuOpen(false)}
                    className="flex w-full items-center gap-2 rounded-lg px-2 py-2 text-theme-sm font-medium text-gray-700 transition-colors hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5"
                  >
                    Paramètres
                  </Link>
                  <button
                    type="button"
                    role="menuitem"
                    onClick={logout}
                    className="flex w-full items-center gap-2 rounded-lg px-2 py-2 text-theme-sm font-medium text-gray-700 transition-colors hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5"
                  >
                    Se déconnecter
                  </button>
                </div>
              </>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
};

export default AppHeader;
