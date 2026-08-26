"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useRouter } from "next/navigation";
import { apiFetch, clearTokens, readTokens, writeTokens } from "@/lib/api";
import { authApi, userApi, walletApi } from "@/lib/services";
import type { AuthTokens, CurrentUser, Wallet } from "@/lib/types";

/**
 * Session de l'application.
 *
 * Elle porte deux choses, et pas une de plus : QUI est connecté, et SUR QUEL
 * WALLET il travaille.
 *
 * Le wallet est ici plutôt que dans chaque écran parce qu'il est transverse :
 * l'accueil affiche son solde, un dépôt le crédite, un virement le débite, un
 * coffre y puise. Si chaque page tenait sa propre copie, un dépôt réussi
 * laisserait l'accueil afficher l'ancien solde jusqu'au prochain rechargement
 * complet — le défaut le plus visible qu'une application d'argent puisse avoir.
 * Une opération appelle `reloadWallets()`, et tout ce qui affiche un solde se
 * met à jour.
 *
 * Le reste (transactions, coffres, prêts) est chargé par les écrans concernés
 * avec `useResource` : ces données ne sont utiles qu'à un endroit, les hisser
 * ici ne ferait que les charger pour rien à l'ouverture.
 */

type Status = "loading" | "authenticated" | "anonymous";

type SessionValue = {
  status: Status;
  user: CurrentUser | null;
  wallets: Wallet[];
  /** Wallet courant : celui que les écrans d'opération créditent ou débitent. */
  activeWallet: Wallet | null;
  selectWallet: (id: number) => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Relit le profil (après modification) — l'en-tête doit refléter le nouveau nom. */
  reloadUser: () => Promise<void>;
  /** Relit les soldes — À APPELER APRÈS TOUTE OPÉRATION D'ARGENT. */
  reloadWallets: () => Promise<void>;
  /**
   * Vrai quand la session vient d'être fermée par l'utilisateur lui-même.
   *
   * La garde du layout connecté s'en sert pour NE PAS mémoriser la page
   * quittée : une session expirée doit y ramener après reconnexion, une
   * déconnexion volontaire non. Sans cette distinction, se déconnecter depuis
   * le profil renvoie sur `/connexion?suite=/profil`, et la connexion suivante
   * atterrit sur le profil au lieu de l'accueil.
   */
  signedOut: boolean;
};

const SessionContext = createContext<SessionValue>({
  status: "loading",
  user: null,
  wallets: [],
  activeWallet: null,
  selectWallet: () => {},
  login: async () => {},
  logout: async () => {},
  reloadUser: async () => {},
  reloadWallets: async () => {},
  signedOut: false,
});

export function useSession() {
  return useContext(SessionContext);
}

/* Le wallet choisi survit au rechargement : quelqu'un qui a plusieurs devises
   ne veut pas re-sélectionner la bonne à chaque visite. Confort local, jamais
   une donnée dont dépend une opération — le serveur reçoit toujours un
   identifiant explicite. */
const ACTIVE_WALLET_KEY = "kola_web_wallet";

function readStoredWalletId(): number | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(ACTIVE_WALLET_KEY);
  const parsed = raw ? Number(raw) : NaN;
  return Number.isFinite(parsed) ? parsed : null;
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [activeWalletId, setActiveWalletId] = useState<number | null>(null);
  const [signedOut, setSignedOut] = useState(false);
  const router = useRouter();

  /**
   * Profil et wallets partent ENSEMBLE : ils sont indépendants, les enchaîner
   * doublerait le délai avant l'affichage de l'accueil.
   */
  const loadIdentity = useCallback(async () => {
    const [profile, list] = await Promise.all([userApi.me(), walletApi.list()]);
    return { profile, list };
  }, []);

  /**
   * Restauration au chargement.
   *
   * Le rendu ne commence PAS à « anonyme » pour basculer ensuite : il commence
   * à « loading ». Sans cet état intermédiaire, un rechargement afficherait
   * brièvement l'écran de connexion à quelqu'un déjà connecté, et la garde du
   * layout le redirigerait avant même que les jetons aient été lus.
   */
  useEffect(() => {
    let cancelled = false;

    /* Tout passe par cette fonction, y compris le cas « aucun jeton » — qui
       tiendrait en une ligne au-dessus, mais dans le corps même de l'effet. Un
       `setState` synchrone à cet endroit déclenche le rendu en cascade que
       React signale ; l'isoler dans une fonction appelée par l'effet remet la
       mise à jour dans un callback. */
    const restore = async () => {
      if (!readTokens()) {
        if (!cancelled) setStatus("anonymous");
        return;
      }

      try {
        const { profile, list } = await loadIdentity();
        if (cancelled) return;
        setUser(profile);
        setWallets(list);
        setActiveWalletId(readStoredWalletId());
        setStatus("authenticated");
      } catch {
        if (cancelled) return;
        /* Jetons périmés ou backend injoignable : dans les deux cas
           l'application n'est pas utilisable. On repart d'une session propre
           plutôt que d'un état à moitié chargé. */
        clearTokens();
        setUser(null);
        setWallets([]);
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
      setSignedOut(false);
      const tokens = await apiFetch<AuthTokens>("/auth/login", {
        method: "POST",
        body: { email, password },
        anonymous: true,
      });
      writeTokens(tokens);

      try {
        const { profile, list } = await loadIdentity();
        setUser(profile);
        setWallets(list);
        setActiveWalletId(readStoredWalletId());
        setStatus("authenticated");
      } catch (error) {
        /* Les identifiants étaient bons mais la session n'a pas pu s'ouvrir :
           on ne CONSERVE PAS les jetons. Les garder laisserait l'application
           dans un état où chaque écran échoue sans que l'on comprenne
           pourquoi. */
        clearTokens();
        setUser(null);
        setWallets([]);
        setStatus("anonymous");
        throw error;
      }
    },
    [loadIdentity]
  );

  const logout = useCallback(async () => {
    /* On prévient le serveur, mais son échec ne doit pas retenir l'utilisateur
       sur place : la déconnexion qui compte pour lui est le retrait des jetons
       de cet appareil, et elle est locale. */
    await authApi.logout().catch(() => {});
    clearTokens();
    setUser(null);
    setWallets([]);
    setActiveWalletId(null);
    /* Posé AVANT le passage à « anonymous » : c'est ce changement de statut qui
       réveille la garde du layout, et elle doit déjà savoir que le départ est
       volontaire. */
    setSignedOut(true);
    setStatus("anonymous");
    router.replace("/connexion");
  }, [router]);

  const reloadUser = useCallback(async () => {
    /* Échec silencieux : garder la valeur affichée vaut mieux que vider
       l'en-tête. Si la session est réellement morte, le prochain appel d'un
       écran le révélera par une 401. */
    const profile = await userApi.me().catch(() => null);
    if (profile) setUser(profile);
  }, []);

  const reloadWallets = useCallback(async () => {
    const list = await walletApi.list().catch(() => null);
    if (list) setWallets(list);
  }, []);

  const selectWallet = useCallback((id: number) => {
    setActiveWalletId(id);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(ACTIVE_WALLET_KEY, String(id));
    }
  }, []);

  /**
   * Wallet courant, avec deux niveaux de repli.
   *
   * L'identifiant mémorisé peut désigner un wallet fermé, ou appartenant au
   * compte précédemment connecté sur ce navigateur. On ne le fait donc jamais
   * confiance aveuglément : on retombe sur le premier wallet actif, et à défaut
   * sur le premier tout court. Un écran d'opération sans wallet ne s'affiche
   * pas — il propose d'en créer un.
   */
  const activeWallet = useMemo(() => {
    if (wallets.length === 0) return null;
    return (
      wallets.find((wallet) => wallet.id === activeWalletId) ??
      wallets.find((wallet) => wallet.active) ??
      wallets[0]
    );
  }, [wallets, activeWalletId]);

  const value = useMemo<SessionValue>(
    () => ({
      status,
      user,
      wallets,
      activeWallet,
      selectWallet,
      login,
      logout,
      reloadUser,
      reloadWallets,
      signedOut,
    }),
    [
      status,
      user,
      wallets,
      activeWallet,
      selectWallet,
      login,
      logout,
      reloadUser,
      reloadWallets,
      signedOut,
    ]
  );

  return (
    <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
  );
}
