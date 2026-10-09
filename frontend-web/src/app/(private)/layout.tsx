import type { Metadata } from "next";
import { SessionProvider } from "@/lib/session";

/**
 * Racine de tout ce qui est privé : espace client ET écrans d'authentification.
 *
 * ═══ POURQUOI CE GROUPE EXISTE ═══
 *
 * Deux raisons, et chacune suffirait.
 *
 * 1. LA SESSION. `(app)` et `(auth)` ont tous deux besoin du contexte de
 *    session — l'un pour garder ses écrans, l'autre pour connecter puis
 *    rediriger. Le placer à la racine du site le ferait monter sur la page
 *    d'accueil publique, qui interrogerait alors `/users/me` à chaque visite
 *    d'un visiteur porteur d'un vieux jeton. Ici, il ne couvre que ce qui en a
 *    l'usage.
 *
 * 2. L'INDEXATION. Depuis la fusion, le site public et l'espace client
 *    partagent un domaine. Sans cette déclaration, `/mon-compte`, `/coffres` ou
 *    `/connexion` hériteraient des métadonnées marketing — donc de leur
 *    indexation. Un écran de compte qui remonte dans un moteur de recherche
 *    n'est pas une visite, c'est une surface exposée.
 *
 * Ce layout est un composant SERVEUR : c'est ce qui lui permet d'exporter
 * `metadata`. Le fournisseur de session, lui, est un composant client — il est
 * rendu ici comme n'importe quel enfant, sans forcer tout le sous-arbre à
 * basculer côté client.
 */
export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function PrivateLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <SessionProvider>{children}</SessionProvider>;
}
