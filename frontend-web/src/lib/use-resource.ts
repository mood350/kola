"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, NetworkError, SessionExpiredError } from "@/lib/api";

/**
 * Chargement d'une ressource distante, avec ses états.
 *
 * Ce qu'il évite : le même quatuor `data / loading / error / reload` répété
 * dans chaque écran, avec à chaque fois une variante subtile dans la gestion de
 * l'annulation — et donc à chaque fois une occasion de laisser passer une mise
 * à jour d'état sur un composant démonté.
 *
 * ═══ `loading` EST DÉRIVÉ, PAS STOCKÉ ═══
 *
 * L'état retenu n'est pas « suis-je en train de charger ? » mais « DE QUELLE
 * REQUÊTE le résultat affiché provient-il ». Le chargement se déduit d'une
 * comparaison :
 *
 *     loading = (la requête courante) ≠ (celle qui a produit le résultat affiché)
 *
 * Trois bénéfices, tous structurels :
 *
 * 1. Aucun `setState` synchrone dans l'effet — poser `loading = true` à l'entrée
 *    provoquerait le rendu en cascade que le compilateur React signale.
 * 2. Impossible de désynchroniser : un `loading` stocké s'oublie sur un chemin
 *    d'erreur et laisse un indicateur tourner indéfiniment.
 * 3. Les données précédentes RESTENT affichées pendant un rechargement — le
 *    solde ne clignote pas à chaque retour sur l'accueil.
 *
 * ═══ `load` DOIT ÊTRE STABLE ═══
 *
 * L'appelant le construit avec `useCallback` en déclarant ses dépendances
 * réelles (page, wallet sélectionné). C'est ce qui rend le rechargement
 * explicite : la ressource se recharge quand une dépendance change, jamais à
 * chaque rendu.
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
     plus bas donne donc `loading = true` dès le premier rendu, sans l'écrire. */
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
           changement de filtre. */
        if (caught instanceof DOMException && caught.name === "AbortError") {
          return;
        }

        if (caught instanceof SessionExpiredError) {
          router.replace("/connexion");
          return;
        }

        /* `data: null` en cas d'échec : laisser les données précédentes sous
           une bannière d'erreur ferait lire un solde périmé comme s'il était à
           jour. Sur une application d'argent, c'est le pire des deux mondes. */
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
 * Chaque cas distingué est un cas où la conduite à tenir diffère : réessayer
 * (réseau), se reconnecter (403 après changement de rôle), ou renoncer (404).
 * Un « une erreur est survenue » unique laisserait l'utilisateur sans action.
 */
function describe(caught: unknown): string {
  if (caught instanceof NetworkError) return caught.message;
  if (caught instanceof ApiError) {
    if (caught.status === 403) {
      return "Accès refusé. Cette ressource ne vous appartient pas.";
    }
    if (caught.status === 404) {
      return "Cette ressource n'existe pas ou a été supprimée.";
    }
    return caught.message;
  }
  return "Une erreur inattendue est survenue.";
}

/**
 * Message affichable pour l'échec d'une ACTION (envoi de formulaire).
 *
 * Séparé de `describe` : une action échouée se raconte autrement qu'un
 * chargement raté. Le message métier du backend est ici la meilleure chose à
 * montrer — « Solde insuffisant », « Coffre encore bloqué » sont déjà rédigés
 * pour l'utilisateur final par `GlobalExceptionHandler`.
 */
export function describeActionError(caught: unknown): string {
  if (caught instanceof NetworkError) return caught.message;
  if (caught instanceof SessionExpiredError) return caught.message;
  if (caught instanceof ApiError) {
    /* Erreur de validation : le backend renvoie le détail champ par champ dans
       `details`. Le message générique (« Requête invalide ») ne dirait pas quel
       champ corriger. */
    const fieldErrors = Object.values(caught.details);
    if (fieldErrors.length > 0) return fieldErrors.join(" · ");
    return caught.message;
  }
  return "Une erreur inattendue est survenue.";
}
