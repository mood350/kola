"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, SessionExpiredError } from "@/lib/api";

/**
 * Chargement d'une ressource distante, avec ses états.
 *
 * Ce qu'il évite : voir répété dans chaque écran le même quatuor
 * `data / loading / error / reload`, avec à chaque fois une variante subtile
 * dans la gestion de l'annulation — et donc à chaque fois une occasion de
 * laisser passer une mise à jour d'état sur un composant démonté.
 *
 * ═══ `loading` EST DÉRIVÉ, PAS STOCKÉ ═══
 *
 * C'est le point de conception de ce hook. L'état retenu n'est pas
 * « suis-je en train de charger ? » mais « DE QUELLE REQUÊTE le résultat
 * affiché provient-il ». Le chargement se déduit d'une comparaison :
 *
 *     loading = (la requête courante) ≠ (celle qui a produit le résultat affiché)
 *
 * Trois bénéfices, tous structurels :
 *
 * 1. Aucun `setState` synchrone dans l'effet. Poser `loading = true` à l'entrée
 *    de l'effet provoquerait un rendu en cascade — un rendu pour dire qu'on
 *    charge, puis un autre pour le résultat — que React signale à juste titre.
 *    Ici le passage en chargement est immédiat et gratuit : il découle du
 *    changement de `load` dans le même rendu.
 *
 * 2. Impossible de désynchroniser. Un `loading` stocké se laisse oublier sur un
 *    chemin d'erreur et l'écran reste bloqué sur un indicateur éternel. Une
 *    valeur dérivée n'a pas de chemin où la remettre à jour.
 *
 * 3. Les données précédentes RESTENT affichées pendant un rechargement — le
 *    tableau ne clignote pas à chaque frappe dans un champ de recherche, seule
 *    la barre de progression signale l'activité.
 *
 * ═══ ANNULATION ═══
 *
 * Chaque exécution reçoit un `AbortSignal`, révoqué au démontage comme au
 * changement de `load`. Sans lui, un opérateur qui enchaîne trois filtres
 * verrait s'afficher la réponse arrivée en dernier, pas celle du filtre actif —
 * les requêtes ne reviennent pas dans l'ordre où elles partent.
 *
 * ═══ `load` DOIT ÊTRE STABLE ═══
 *
 * L'appelant le construit avec `useCallback` en déclarant ses dépendances
 * réelles (page, filtres). C'est ce qui rend le rechargement explicite : la
 * ressource se recharge exactement quand une de ces dépendances change, jamais
 * à chaque rendu.
 */
export type Resource<T> = {
  data: T | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
};

type Loader<T> = (signal: AbortSignal) => Promise<T>;

/** Résultat affiché, accompagné de l'identité de la requête qui l'a produit. */
type Snapshot<T> = {
  from: Loader<T> | null;
  attempt: number;
  data: T | null;
  error: string | null;
};

export function useResource<T>(load: Loader<T>): Resource<T> {
  /* `from: null` au départ : aucune requête n'a encore abouti, la comparaison
     ci-dessous donne donc `loading = true` dès le premier rendu, sans avoir à
     l'écrire nulle part. */
  const [snapshot, setSnapshot] = useState<Snapshot<T>>({
    from: null,
    attempt: -1,
    data: null,
    error: null,
  });

  /* Simple compteur : l'incrémenter relance l'effet sans toucher à `load`. */
  const [attempt, setAttempt] = useState(0);
  const router = useRouter();

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    load(controller.signal)
      .then((result) => {
        if (!active) return;
        setSnapshot({ from: load, attempt, data: result, error: null });
      })
      .catch((caught: unknown) => {
        if (!active) return;

        /* Une requête annulée n'est pas une erreur : c'est nous qui l'avons
           interrompue. L'afficher ferait clignoter un message d'échec à chaque
           changement de filtre. On ne touche pas au résultat affiché — l'effet
           qui vient de prendre la relève s'en chargera. */
        if (caught instanceof DOMException && caught.name === "AbortError") {
          return;
        }

        if (caught instanceof SessionExpiredError) {
          router.replace("/connexion");
          return;
        }

        /* `data: null` en cas d'échec : laisser les données précédentes sous
           une bannière d'erreur ferait lire des chiffres périmés comme s'ils
           étaient à jour. Sur une console financière, c'est le pire des deux
           mondes. */
        setSnapshot({
          from: load,
          attempt,
          data: null,
          error: describe(caught),
        });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [load, attempt, router]);

  const reload = useCallback(() => setAttempt((count) => count + 1), []);

  return {
    data: snapshot.data,
    error: snapshot.error,
    loading: snapshot.from !== load || snapshot.attempt !== attempt,
    reload,
  };
}

/**
 * Message affichable pour une erreur de chargement.
 *
 * Le 403 est distingué du reste : sur cette console, il ne signifie pas « vous
 * vous êtes trompé » mais « votre rôle a changé depuis l'ouverture de la
 * session ». C'est une information exploitable, contrairement à un « erreur
 * inattendue » générique.
 */
function describe(caught: unknown): string {
  if (caught instanceof ApiError) {
    if (caught.status === 403) {
      return "Accès refusé. Votre compte ne dispose plus du rôle administrateur.";
    }
    if (caught.status === 404) {
      return "Cette ressource n'existe pas ou a été supprimée.";
    }
    return caught.message;
  }
  return "Impossible de joindre le serveur. Vérifiez que l'API est démarrée sur http://localhost:8081.";
}
