"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSidebar } from "../context/SidebarContext";
/* `LockIcon` pour la conformité : la console LAB-FT est la seule section dont
   le contenu est frappé du secret professionnel — le cadenas dit la bonne
   chose. Toutes les icônes viennent du jeu du template, aucune n'est ajoutée. */
import {
  GridIcon,
  GroupIcon,
  DollarLineIcon,
  LockIcon,
  UserCircleIcon,
} from "../icons/index";
import { KolaBrand } from "@/components/kola/brand";

/**
 * Rail de navigation.
 *
 * Reprend la mécanique de TailAdmin — replié à 90 px, déployé à 290 px,
 * déploiement au survol quand il est replié, tiroir plein écran sous `lg` — et
 * remplace son arborescence de démonstration par les quatre sections de Kola.
 *
 * PAS DE SOUS-MENUS. Le template en propose, la console n'en a pas l'usage :
 * quatre entrées tiennent à plat, et un accordéon ajouterait un clic avant
 * chaque destination. Un menu déroulant se justifie à partir d'une dizaine
 * d'entrées ; en dessous, il ne fait que masquer la navigation.
 *
 * Le bloc promotionnel « Upgrade to Pro » du template a été supprimé.
 */

type NavItem = {
  name: string;
  icon: React.ReactNode;
  path: string;
  /** `/` ne doit s'activer que sur une correspondance exacte. */
  exact?: boolean;
};

/**
 * Deux groupes, et la séparation n'est pas cosmétique : les quatre premières
 * entrées portent sur les DONNÉES DE LA PLATEFORME — ce qu'on vient consulter
 * ou traiter. « Paramètres » porte sur le COMPTE DE L'OPÉRATEUR lui-même. Les
 * mêler ferait chercher son mot de passe entre les prêts et les alertes.
 */
const NAV_ITEMS: NavItem[] = [
  { icon: <GridIcon />, name: "Tableau de bord", path: "/", exact: true },
  { icon: <GroupIcon />, name: "Utilisateurs", path: "/utilisateurs" },
  { icon: <DollarLineIcon />, name: "Prêts", path: "/prets" },
  { icon: <LockIcon />, name: "Conformité", path: "/conformite" },
];

const ACCOUNT_ITEMS: NavItem[] = [
  { icon: <UserCircleIcon />, name: "Paramètres", path: "/parametres" },
];

const AppSidebar: React.FC = () => {
  const { isExpanded, isMobileOpen, isHovered, setIsHovered } = useSidebar();
  const pathname = usePathname();

  /* Le rail montre ses libellés quand il est déployé, épinglé ouvert au survol,
     ou ouvert en tiroir mobile. Les trois cas donnent la même largeur utile. */
  const showLabels = isExpanded || isHovered || isMobileOpen;

  return (
    <aside
      className={`fixed top-0 left-0 z-50 flex h-screen flex-col border-r border-gray-200 bg-white px-5 text-gray-900 transition-all duration-300 ease-in-out lg:mt-0 dark:border-gray-800 dark:bg-gray-900
        ${showLabels ? "w-[290px]" : "w-[90px]"}
        ${isMobileOpen ? "translate-x-0" : "-translate-x-full"}
        lg:translate-x-0`}
      onMouseEnter={() => !isExpanded && setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div
        className={`flex py-8 ${showLabels ? "justify-start" : "justify-center"}`}
      >
        <Link href="/" aria-label="Kola, tableau de bord">
          <KolaBrand compact={!showLabels} />
        </Link>
      </div>

      <div className="no-scrollbar flex flex-col overflow-y-auto duration-300 ease-linear">
        <NavGroup
          heading="Pilotage"
          items={NAV_ITEMS}
          pathname={pathname}
          showLabels={showLabels}
        />
        <NavGroup
          heading="Compte"
          items={ACCOUNT_ITEMS}
          pathname={pathname}
          showLabels={showLabels}
        />
      </div>
    </aside>
  );
};

/**
 * Un groupe d'entrées, titre compris.
 *
 * Chaque groupe est un `<nav>` distinct porteur de son propre `aria-label` :
 * un lecteur d'écran qui liste les régions de la page annonce alors
 * « Pilotage » et « Compte » séparément, au lieu d'une seule zone de navigation
 * dont il faudrait parcourir toutes les entrées pour comprendre la structure.
 */
function NavGroup({
  heading,
  items,
  pathname,
  showLabels,
}: {
  heading: string;
  items: readonly NavItem[];
  pathname: string;
  showLabels: boolean;
}) {
  return (
    <nav aria-label={heading} className="mb-6">
      <h2
        className={`mb-4 flex text-theme-xs leading-[20px] font-medium text-gray-400 uppercase ${
          showLabels ? "justify-start" : "justify-center"
        }`}
      >
        {/* Rail replié : le titre est remplacé par trois points. Le masquer
            entièrement collerait les groupes l'un à l'autre et effacerait la
            séparation qui justifie leur existence. */}
        {showLabels ? heading : "···"}
      </h2>

      <ul className="flex flex-col gap-2">
        {items.map((item) => {
          const active = item.exact
            ? pathname === item.path
            : pathname.startsWith(item.path);

          return (
            <li key={item.path}>
              <Link
                href={item.path}
                /* `aria-current` porte l'état actif pour les lecteurs
                   d'écran : le fond coloré ne leur dit rien. */
                aria-current={active ? "page" : undefined}
                className={`menu-item group ${
                  active ? "menu-item-active" : "menu-item-inactive"
                } ${showLabels ? "lg:justify-start" : "lg:justify-center"}`}
              >
                <span
                  className={
                    active ? "menu-item-icon-active" : "menu-item-icon-inactive"
                  }
                >
                  {item.icon}
                </span>
                {/* Le libellé disparaît avec le rail replié, mais reste
                    annoncé : `sr-only` le retire de l'écran, pas de l'arbre
                    d'accessibilité. Sans ça, le rail replié n'offrirait que des
                    icônes muettes. */}
                <span className={showLabels ? "" : "sr-only"}>{item.name}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

export default AppSidebar;
