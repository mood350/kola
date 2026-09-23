"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback } from "react";
import { cn } from "@/lib/cn";
import { initials } from "@/lib/format";
import { KYC_LABEL, KYC_TONE } from "@/lib/labels";
import { notificationApi } from "@/lib/services";
import { useSession } from "@/lib/session";
import { useResource } from "@/lib/use-resource";
import { Avatar } from "@/components/ui/avatar";
import { DotBadge, IconButton } from "@/components/ui/primitives";
import {
  BellIcon,
  ClockIcon,
  CreditIcon,
  HomeIcon,
  ListIcon,
  LogoutIcon,
  UserIcon,
  UsersIcon,
  VaultIcon,
  WalletIcon,
} from "@/components/ui/icons";

/**
 * Coque de l'application connectée.
 *
 * Deux navigations pour un seul jeu de destinations : une colonne latérale à
 * partir de `lg`, une barre inférieure en dessous. Ce n'est pas une duplication
 * gratuite — sur téléphone, une barre basse tombe sous le pouce là où un menu
 * latéral impose un aller-retour ; sur grand écran, une colonne permanente
 * évite de masquer le contenu à chaque navigation. Le mobile Flutter fait le
 * même choix (`KolaBottomNavBar`), et les intitulés sont les mêmes des deux
 * côtés.
 */

type NavItem = {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  /** Présent dans la barre inférieure du téléphone (5 entrées maximum). */
  primary?: boolean;
};

const NAV: NavItem[] = [
  { href: "/mon-compte", label: "Accueil", icon: HomeIcon, primary: true },
  { href: "/transactions", label: "Transactions", icon: ListIcon, primary: true },
  { href: "/coffres", label: "Coffres", icon: VaultIcon, primary: true },
  { href: "/credit", label: "Crédit", icon: CreditIcon, primary: true },
  { href: "/virements-programmes", label: "Programmés", icon: ClockIcon },
  { href: "/beneficiaires", label: "Bénéficiaires", icon: UsersIcon },
  { href: "/comptes", label: "Mes comptes", icon: WalletIcon },
  { href: "/profil", label: "Profil", icon: UserIcon, primary: true },
];

/** Onglet actif. */
function isActive(pathname: string, href: string): boolean {
  /* `startsWith` pour les sous-pages (`/coffres/12` allume « Coffres »).
     Depuis la fusion, le tableau de bord n'est plus à la racine — celle-ci
     appartient au site public — donc aucune entrée n'a besoin du cas
     particulier « égalité stricte » qui existait pour `/`. */
  return pathname.startsWith(href);
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, logout } = useSession();

  const loadUnread = useCallback(
    (signal: AbortSignal) => notificationApi.unreadCount(signal),
    []
  );
  /* L'échec est volontairement ignoré : une pastille de notifications absente
     est un désagrément, une bannière d'erreur permanente en haut de chaque
     écran serait un défaut. */
  const unread = useResource(loadUnread);
  const unreadCount = unread.data?.count ?? 0;

  const name = user ? `${user.firstName} ${user.lastName}` : "";

  return (
    <div className="min-h-dvh lg:flex">
      {/* ---- Colonne latérale (grand écran) ---- */}
      <aside className="sticky top-0 hidden h-dvh w-64 shrink-0 flex-col border-r border-line bg-surface px-4 py-6 lg:flex">
        <Link href="/mon-compte" className="mb-8 flex items-center gap-2.5 px-2">
          <BrandMark />
          <span className="font-display text-xl font-semibold tracking-tight text-ink-950">
            Kola
          </span>
        </Link>

        <nav className="flex-1 space-y-1" aria-label="Navigation principale">
          {NAV.map((item) => {
            const active = isActive(pathname, item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-full px-3.5 py-2.5 text-sm font-medium transition-colors",
                  active
                    ? "bg-kola-50 text-kola-700"
                    : "text-ink-600 hover:bg-ink-50 hover:text-ink-900"
                )}
              >
                <item.icon className="text-lg" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        {user ? (
          <div className="mt-4 border-t border-line pt-4">
            <div className="flex items-center gap-3 px-2">
              <Avatar
                avatarId={user.avatar}
                initials={initials(user.firstName, user.lastName)}
                size={36}
              />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-ink-900">{name}</p>
                <p className="truncate text-xs text-ink-500">{user.email}</p>
              </div>
              <IconButton label="Se déconnecter" onClick={() => void logout()}>
                <LogoutIcon />
              </IconButton>
            </div>
          </div>
        ) : null}
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* ---- Barre supérieure ---- */}
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-line bg-canvas/85 px-4 backdrop-blur-md sm:px-6 lg:px-8">
          <Link href="/mon-compte" className="flex items-center gap-2 lg:hidden">
            <BrandMark />
            <span className="font-display text-lg font-semibold text-ink-950">Kola</span>
          </Link>

          <div className="ml-auto flex items-center gap-2">
            {user ? (
              <span className="hidden sm:inline-flex">
                <DotBadge tone={KYC_TONE[user.kycLevel]}>
                  {KYC_LABEL[user.kycLevel]}
                </DotBadge>
              </span>
            ) : null}

            <Link
              href="/notifications"
              aria-label={
                unreadCount > 0
                  ? `Notifications, ${unreadCount} non lue${unreadCount > 1 ? "s" : ""}`
                  : "Notifications"
              }
              className="relative inline-flex size-10 items-center justify-center rounded-full text-ink-600 transition-colors hover:bg-ink-100 hover:text-ink-900"
            >
              <BellIcon className="text-xl" />
              {unreadCount > 0 ? (
                <span className="absolute top-1.5 right-1.5 min-w-4 rounded-full bg-danger-500 px-1 text-[0.625rem] leading-4 font-bold text-white">
                  {unreadCount > 9 ? "9+" : unreadCount}
                </span>
              ) : null}
            </Link>

            {user ? (
              <Link href="/profil" className="lg:hidden" aria-label="Mon profil">
                <Avatar
                  avatarId={user.avatar}
                  initials={initials(user.firstName, user.lastName)}
                  size={36}
                />
              </Link>
            ) : null}
          </div>
        </header>

        {/* `pb-24` en dessous de `lg` : la barre inférieure est fixe, sans quoi
            elle recouvrirait le dernier élément de chaque page. */}
        <main className="mx-auto w-full max-w-5xl flex-1 px-4 pt-5 pb-24 sm:px-6 lg:px-8 lg:pb-10">
          {children}
        </main>
      </div>

      {/* ---- Barre inférieure (téléphone) ---- */}
      <nav
        aria-label="Navigation principale"
        className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-surface/95 backdrop-blur-md lg:hidden"
        style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
      >
        <ul className="mx-auto flex max-w-lg">
          {NAV.filter((item) => item.primary).map((item) => {
            const active = isActive(pathname, item.href);
            return (
              <li key={item.href} className="flex-1">
                <Link
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "flex h-16 flex-col items-center justify-center gap-1 text-[0.6875rem] font-medium transition-colors",
                    active ? "text-kola-600" : "text-ink-500"
                  )}
                >
                  <item.icon className="text-xl" />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </div>
  );
}

/**
 * Marque Kola : le « K » dans une pastille indigo.
 *
 * Dessinée en SVG plutôt que posée en fichier image — elle mesure 32 pixels,
 * ne change jamais, et une requête réseau pour l'obtenir serait une requête de
 * trop sur le chemin critique de chaque page.
 */
export function BrandMark({ size = 32 }: { size?: number }) {
  return (
    <span
      className="inline-flex items-center justify-center rounded-xl bg-kola-600 font-display font-bold text-white"
      style={{ width: size, height: size, fontSize: size * 0.55 }}
      aria-hidden
    >
      K
    </span>
  );
}
