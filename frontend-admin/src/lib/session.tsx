"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { useRouter } from "next/navigation";
import {
  ApiError,
  apiFetch,
  clearTokens,
  readTokens,
  writeTokens,
} from "@/lib/api";
import type { AuthTokens, CurrentUser } from "@/lib/types";

/**
 * Session de la console.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 *   COMMENT ON SAIT QU'UN UTILISATEUR EST ADMINISTRATEUR
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * On le DEMANDE au serveur — on ne le déduit de rien.
 *
 * Le JWT ne porte aucun rôle (`AuthenticationService.authenticate()` appelle
 * `generateToken(user)`, la surcharge sans claims supplémentaires) et
 * `GET /api/users/me` exclut délibérément les autorités. Il n'existe donc
 * aucune source côté client permettant de conclure quoi que ce soit.
 *
 * La console appelle donc un endpoint réservé — `GET /api/admin/loans/overview`,
 * le moins coûteux du lot, une seule requête agrégée — et lit la réponse :
 * 200 signifie ADMIN, 403 (`ACCESS_DENIED`) signifie non.
 *
 * Cette façon de faire a un mérite qu'une lecture de claim n'aurait pas :
 * l'autorisation est vérifiée PAR LE SERVEUR, à chaque démarrage de session.
 * Un rôle retiré en base prend effet au prochain chargement, là où un rôle gravé
 * dans un JWT resterait valable jusqu'à l'expiration du jeton. Et rien de ce que
 * fait cette vérification ne protège quoi que ce soit à elle seule : chaque
 * endpoint reste gardé côté serveur. Elle sert à ne pas afficher une console
 * vide et incompréhensible à quelqu'un qui n'y a pas droit — pas à tenir la
 * porte.
 */

const ADMIN_PROBE = "/admin/loans/overview";

type Status = "loading" | "authenticated" | "anonymous";

type SessionValue = {
  status: Status;
  user: CurrentUser | null;
  /** Connecte, vérifie la qualité d'administrateur, puis ouvre la console. */
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /**
   * Recharge le profil depuis le serveur.
   *
   * Appelé après une modification du profil : sans lui, l'en-tête continuerait
   * d'afficher l'ancien nom jusqu'au prochain rechargement complet de la page.
   * On relit le serveur plutôt que de recopier la réponse du formulaire dans
   * l'état — c'est le serveur qui a le dernier mot sur ce qui a été enregistré
   * (il rogne les espaces, par exemple).
   */
  refresh: () => Promise<void>;
};

const SessionContext = createContext<SessionValue>({
  status: "loading",
  user: null,
  login: async () => {},
  logout: async () => {},
  refresh: async () => {},
});

export function useSession() {
  return useContext(SessionContext);
}

/** Levée quand les identifiants sont bons mais que le compte n'est pas ADMIN. */
export class NotAnAdminError extends Error {
  constructor() {
    super(
      "Ce compte n'a pas le rôle administrateur. La console est réservée aux administrateurs."
    );
    this.name = "NotAnAdminError";
  }
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const router = useRouter();

  /**
   * Charge le profil ET confirme le rôle. Les deux appels partent ensemble :
   * ils sont indépendants, les enchaîner doublerait le délai d'ouverture.
   */
  const loadIdentity = useCallback(async (): Promise<CurrentUser> => {
    const [profile] = await Promise.all([
      apiFetch<CurrentUser>("/users/me"),
      apiFetch<unknown>(ADMIN_PROBE).catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 403) {
          throw new NotAnAdminError();
        }
        throw error;
      }),
    ]);
    return profile;
  }, []);

  /**
   * Restauration au chargement.
   *
   * Le rendu ne commence PAS à « anonyme » pour basculer ensuite : il commence
   * à « loading ». Sans cet état intermédiaire, un rechargement de page
   * afficherait brièvement l'écran de connexion à quelqu'un déjà connecté, et
   * la garde du layout le redirigerait avant même que les jetons aient été lus.
   */
  useEffect(() => {
    let cancelled = false;

    /* Tout passe par cette fonction, y compris le cas « aucun jeton » — qui
       pourrait se traiter en une ligne au-dessus, mais dans le corps même de
       l'effet. Un `setState` synchrone à cet endroit déclenche un rendu en
       cascade que React signale ; l'isoler dans une fonction appelée par
       l'effet remet la mise à jour dans un callback, là où elle doit être. */
    const restore = async () => {
      if (!readTokens()) {
        if (!cancelled) setStatus("anonymous");
        return;
      }

      try {
        const profile = await loadIdentity();
        if (cancelled) return;
        setUser(profile);
        setStatus("authenticated");
      } catch {
        if (cancelled) return;
        /* Jetons périmés, rôle retiré, backend injoignable : dans tous les cas
           la console n'est pas utilisable. On repart d'une session propre
           plutôt que de laisser un état à moitié chargé. */
        clearTokens();
        setUser(null);
        setStatus("anonymous");
      }
    };

    void restore();

    return () => {
      cancelled = true;
    };
  }, [loadIdentity]);

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await apiFetch<AuthTokens>("/auth/login", {
        method: "POST",
        body: { email, password },
        anonymous: true,
      });
      writeTokens(tokens);

      try {
        const profile = await loadIdentity();
        setUser(profile);
        setStatus("authenticated");
      } catch (error) {
        /* Identifiants valides mais compte non administrateur : on n'entre pas
           dans la console, et surtout on ne CONSERVE PAS les jetons. Les garder
           laisserait une session ouverte sur une interface entièrement vide,
           où chaque écran répondrait 403 sans que l'utilisateur comprenne
           pourquoi. */
        clearTokens();
        setUser(null);
        setStatus("anonymous");
        throw error;
      }
    },
    [loadIdentity]
  );

  const logout = useCallback(async () => {
    /* On prévient le serveur, mais son échec ne doit pas retenir l'utilisateur
       sur place : la déconnexion qui compte pour lui est la suppression des
       jetons de cet appareil, et elle est locale. */
    await apiFetch<void>("/auth/logout", { method: "POST" }).catch(() => {});
    clearTokens();
    setUser(null);
    setStatus("anonymous");
    router.push("/connexion");
  }, [router]);

  const refresh = useCallback(async () => {
    /* Un échec ici est silencieux : la donnée affichée reste celle qu'on avait,
       ce qui est préférable à vider l'en-tête. Si la session est réellement
       morte, le prochain appel d'un écran le révélera par une 401. */
    const profile = await apiFetch<CurrentUser>("/users/me").catch(() => null);
    if (profile) setUser(profile);
  }, []);

  return (
    <SessionContext.Provider value={{ status, user, login, logout, refresh }}>
      {children}
    </SessionContext.Provider>
  );
}
