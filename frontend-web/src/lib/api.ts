import type { ApiErrorBody, AuthTokens } from "@/lib/types";

/**
 * Client HTTP de l'application web Kola.
 *
 * Il porte les trois responsabilités qui, laissées à chaque écran, finiraient
 * dupliquées et divergentes : joindre le jeton, renouveler une session expirée
 * une fois et une seule, et traduire l'`ErrorResponse` du backend en une
 * exception que l'interface sait présenter.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081/api";

/* Clés distinctes de celles de la console d'administration (`kola_admin_*`) :
   les deux applications tournent sur `localhost` en développement, où le
   `localStorage` est partagé entre les ports. Des clés communes feraient qu'une
   connexion ici déconnecterait l'autre. */
const ACCESS_KEY = "kola_web_access";
const REFRESH_KEY = "kola_web_refresh";

/* ---------------------------------------------------------------------------
   Stockage des jetons
   ------------------------------------------------------------------------ */

/**
 * `localStorage` — le choix qu'impose le backend, avec sa contrepartie.
 *
 * `AuthController.logout` documente que « le frontend doit supprimer les tokens
 * de son côté (localStorage) » : l'API ne pose aucun cookie et n'expose aucun
 * point d'entrée qui en poserait. Un cookie `HttpOnly`, hors de portée d'un
 * script injecté, supposerait de modifier le backend.
 *
 * CE QUE ÇA COÛTE : tout script exécuté dans cette page peut lire les jetons.
 * L'atténuation appliquée ici est la même que dans la console : aucun script
 * tiers, aucune balise de mesure, aucune police distante — les polices sont
 * auto-hébergées par `next/font`.
 *
 * Les accès sont gardés par `typeof window` : ce module est importé par des
 * composants que Next évalue aussi côté serveur au build.
 */
export function readTokens(): AuthTokens | null {
  if (typeof window === "undefined") return null;
  const access = window.localStorage.getItem(ACCESS_KEY);
  const refresh = window.localStorage.getItem(REFRESH_KEY);
  return access && refresh
    ? { access_token: access, refresh_token: refresh }
    : null;
}

export function writeTokens(tokens: AuthTokens) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(ACCESS_KEY, tokens.access_token);
  window.localStorage.setItem(REFRESH_KEY, tokens.refresh_token);
}

export function clearTokens() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(ACCESS_KEY);
  window.localStorage.removeItem(REFRESH_KEY);
}

/* ---------------------------------------------------------------------------
   Erreurs
   ------------------------------------------------------------------------ */

/**
 * Erreur d'API portant le `code` métier du backend.
 *
 * Le code compte autant que le message : c'est lui qui permet à un écran de
 * réagir à la cause (`INSUFFICIENT_FUNDS`, `VAULT_LOCKED`,
 * `KYC_LIMIT_EXCEEDED`) sans comparer des chaînes traduites, qui changent à la
 * première reformulation.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: Record<string, string>;

  constructor(status: number, body: Partial<ApiErrorBody> | null) {
    super(body?.message ?? "Une erreur est survenue.");
    this.name = "ApiError";
    this.status = status;
    this.code = body?.code ?? "UNKNOWN";
    this.details = body?.details ?? {};
  }
}

/** Session irrécupérable : le renouvellement a été REFUSÉ, il faut se reconnecter. */
export class SessionExpiredError extends Error {
  constructor() {
    super("Votre session a expiré. Reconnectez-vous.");
    this.name = "SessionExpiredError";
  }
}

/**
 * Le serveur n'a pas répondu.
 *
 * Distinguée d'un refus, et c'est tout l'intérêt : sur un renouvellement de
 * jeton, un refus doit purger la session, une coupure réseau surtout pas —
 * sinon perdre le réseau trois secondes déconnecte quelqu'un dont les jetons
 * sont parfaitement valides. Le mobile fait la même distinction
 * (`AuthTokenResult.networkFailure`).
 */
export class NetworkError extends Error {
  constructor() {
    super("Impossible de joindre le serveur. Vérifiez votre connexion.");
    this.name = "NetworkError";
  }
}

/* ---------------------------------------------------------------------------
   Renouvellement du jeton
   ------------------------------------------------------------------------ */

type RefreshOutcome = "renewed" | "rejected" | "unreachable";

type RefreshResult = { outcome: RefreshOutcome; tokens: AuthTokens | null };

/**
 * Renouvellement en cours, partagé.
 *
 * Un écran lance souvent plusieurs requêtes de front. Si le jeton vient
 * d'expirer, elles échouent TOUTES en 401 au même instant. Sans mutualisation,
 * chacune lancerait son propre renouvellement — et comme chaque renouvellement
 * émet une nouvelle paire, la dernière écraserait les précédentes, invalidant
 * les jetons que les requêtes concurrentes viennent d'obtenir. La session se
 * coupait alors sans raison apparente.
 */
let refreshInFlight: Promise<RefreshResult> | null = null;

async function refreshTokens(): Promise<RefreshResult> {
  const current = readTokens();
  if (!current) return { outcome: "rejected", tokens: null };

  let response: Response;
  try {
    response = await fetch(`${API_URL}/auth/refresh-token`, {
      method: "POST",
      /* Le refresh token voyage dans l'en-tête Authorization, pas dans le
         corps : c'est ce que lit `AuthController.refreshToken`. */
      headers: { Authorization: `Bearer ${current.refresh_token}` },
    });
  } catch {
    return { outcome: "unreachable", tokens: null };
  }

  if (!response.ok) return { outcome: "rejected", tokens: null };

  const tokens = (await response.json()) as AuthTokens;
  writeTokens(tokens);
  return { outcome: "renewed", tokens };
}

function refreshOnce(): Promise<RefreshResult> {
  refreshInFlight ??= refreshTokens().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

/* ---------------------------------------------------------------------------
   Appel générique
   ------------------------------------------------------------------------ */

type RequestOptions = {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  body?: unknown;
  /** Coupe l'en-tête d'autorisation — connexion, inscription, mot de passe oublié. */
  anonymous?: boolean;
  signal?: AbortSignal;
};

/**
 * Un appel à l'API, jeton et renouvellement compris.
 *
 * Sur 401, le jeton est renouvelé UNE fois et la requête rejouée. Jamais de
 * boucle : si la requête rejouée échoue encore, le refresh token est lui-même
 * mort et réessayer ne ferait que marteler le serveur avec des identifiants
 * périmés.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { method = "GET", body, anonymous = false, signal } = options;

  const send = async (token: string | null) => {
    const headers: Record<string, string> = {};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (token) headers.Authorization = `Bearer ${token}`;

    try {
      return await fetch(`${API_URL}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal,
      });
    } catch (caught) {
      /* Une annulation est remontée telle quelle : `useResource` la reconnaît
         et n'affiche rien. La confondre avec une panne réseau ferait clignoter
         un message d'erreur à chaque changement de filtre. */
      if (caught instanceof DOMException && caught.name === "AbortError") {
        throw caught;
      }
      throw new NetworkError();
    }
  };

  let response = await send(
    anonymous ? null : (readTokens()?.access_token ?? null)
  );

  if (response.status === 401 && !anonymous) {
    const { outcome, tokens } = await refreshOnce();

    /* Serveur injoignable : ce n'est pas une session morte. On le dit tel quel
       et on GARDE les jetons — ils redeviendront valables au retour du réseau. */
    if (outcome === "unreachable") throw new NetworkError();

    if (outcome === "rejected" || !tokens) {
      clearTokens();
      throw new SessionExpiredError();
    }

    response = await send(tokens.access_token);
    if (response.status === 401) {
      clearTokens();
      throw new SessionExpiredError();
    }
  }

  if (!response.ok) {
    /* Le corps d'erreur peut manquer (502 d'un proxy, réponse tronquée) : on ne
       laisse pas l'échec d'analyse masquer le statut HTTP, seule information
       fiable à ce stade. */
    const errorBody = (await response
      .json()
      .catch(() => null)) as Partial<ApiErrorBody> | null;
    throw new ApiError(response.status, errorBody);
  }

  /* 204 No Content — déconnexion, suppressions. */
  if (response.status === 204) return undefined as T;

  /* ═══ TOUT SUCCÈS N'EST PAS DU JSON ═══
     `POST /auth/confirm` et `POST /auth/reset-password` sont déclarés
     `ResponseEntity<String>` côté Spring : ils répondent 200 avec une phrase en
     TEXTE BRUT (« Compte activé avec succès ! »). Un `JSON.parse` inconditionnel
     lève alors une SyntaxError sur une requête parfaitement réussie — l'écran
     d'activation affichait « une erreur inattendue » alors que le compte venait
     d'être activé, et le parcours d'inscription se terminait sur un échec
     imaginaire.
     On se fie donc au `Content-Type` annoncé : ce qui ne se présente pas comme
     du JSON est rendu tel quel. Un JSON malformé, lui, doit continuer de lever —
     c'est une vraie anomalie, pas un format alternatif. */
  const text = await response.text();
  if (!text) return undefined as T;

  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("json")) return text as T;

  return JSON.parse(text) as T;
}

/**
 * Construit une chaîne de requête en ignorant les filtres vides.
 *
 * Un paramètre présent mais vide (`?type=`) n'équivaut PAS à un paramètre
 * absent : Spring tenterait de convertir la chaîne vide en enum et répondrait
 * 400.
 */
export function queryString(
  params: Record<string, string | number | boolean | null | undefined>
): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === "") continue;
    search.set(key, String(value));
  }
  const rendered = search.toString();
  return rendered ? `?${rendered}` : "";
}

/**
 * Clé d'idempotence pour une opération d'argent.
 *
 * POURQUOI : sur le web, un double-clic, un rechargement pendant l'envoi ou un
 * réseau qui rejoue la requête peuvent la soumettre deux fois. Le backend
 * déduplique sur cette clé (`Transaction.idempotencyKey`) — à condition que les
 * deux tentatives portent LA MÊME. Elle est donc générée une fois par intention
 * (un montant, un destinataire) et non à chaque envoi ; seul un succès ou un
 * changement d'intention la renouvelle. Même raisonnement que
 * `IdempotencyKeyHolder` côté mobile.
 */
export function newIdempotencyKey(): string {
  const random =
    typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : Math.random().toString(16).slice(2);
  return `${Date.now()}-${random}`;
}
