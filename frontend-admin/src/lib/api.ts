import type { ApiErrorBody, AuthTokens } from "@/lib/types";

/**
 * Client HTTP de la console.
 *
 * Il concentre trois responsabilités que l'on retrouverait sinon dupliquées, et
 * divergentes, dans chaque écran : porter le jeton, renouveler une session
 * expirée, et traduire les erreurs de l'API en une exception exploitable.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081/api";

const ACCESS_KEY = "kola_admin_access";
const REFRESH_KEY = "kola_admin_refresh";

/* ---------------------------------------------------------------------------
   Stockage des jetons
   ------------------------------------------------------------------------ */

/**
 * `localStorage` — choix assumé, avec sa contrepartie.
 *
 * C'est ce que le backend prévoit explicitement : « Le frontend doit supprimer
 * les tokens de son côté (localStorage) » (`AuthController.logout`). L'API ne
 * pose aucun cookie et n'expose aucun point d'entrée qui en poserait ; un
 * stockage en cookie `HttpOnly` — plus sûr face au XSS — supposerait de
 * modifier le backend, ce qui dépasse le périmètre de cette console.
 *
 * CE QUE ÇA COÛTE : tout script injecté dans cette page peut lire les jetons.
 * La console n'atténue ce risque que d'une façon — elle ne charge aucun script
 * tiers, aucune police distante, aucune balise de mesure. C'est une atténuation
 * réelle mais partielle. Le passage aux cookies `HttpOnly` reste le chantier de
 * sécurité à ouvrir si cette console sort d'un usage interne.
 *
 * Les accès sont gardés par `typeof window` : ces fonctions sont importées par
 * des modules qui peuvent être évalués côté serveur au build.
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
 * réagir différemment selon la cause (`ACCESS_DENIED` renvoie à l'accueil,
 * une erreur de validation s'affiche sous le champ concerné) sans avoir à
 * comparer des chaînes de texte traduites, qui changent au premier ajustement
 * de formulation.
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

/** Session irrécupérable : le renouvellement a échoué, il faut se reconnecter. */
export class SessionExpiredError extends Error {
  constructor() {
    super("Votre session a expiré. Reconnectez-vous.");
    this.name = "SessionExpiredError";
  }
}

/* ---------------------------------------------------------------------------
   Renouvellement du jeton
   ------------------------------------------------------------------------ */

/**
 * Renouvellement en cours, partagé.
 *
 * POURQUOI CETTE VARIABLE EXISTE : un écran déclenche souvent plusieurs appels
 * en parallèle. Si le jeton vient d'expirer, ils échouent TOUS en 401 en même
 * temps. Sans mutualisation, chacun lancerait son propre renouvellement — et
 * comme chaque renouvellement émet une nouvelle paire de jetons, le dernier
 * écrirait par-dessus les autres, invalidant les jetons que les requêtes
 * concurrentes viennent tout juste d'obtenir. La session se coupait alors de
 * façon aléatoire, en apparence sans raison.
 *
 * Ici, le premier 401 crée la promesse ; les suivants s'y raccrochent.
 */
let refreshInFlight: Promise<AuthTokens | null> | null = null;

async function refreshTokens(): Promise<AuthTokens | null> {
  const current = readTokens();
  if (!current) return null;

  const response = await fetch(`${API_URL}/auth/refresh-token`, {
    method: "POST",
    /* Le refresh token voyage dans l'en-tête Authorization, et non dans le
       corps : c'est ce qu'attend `AuthController.refreshToken`, qui lit le
       header et en retire le préfixe « Bearer ». */
    headers: { Authorization: `Bearer ${current.refresh_token}` },
  });

  if (!response.ok) return null;

  const tokens = (await response.json()) as AuthTokens;
  writeTokens(tokens);
  return tokens;
}

function refreshOnce(): Promise<AuthTokens | null> {
  refreshInFlight ??= refreshTokens()
    .catch(() => null)
    .finally(() => {
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
  /** Coupe l'en-tête d'autorisation — utilisé pour la connexion. */
  anonymous?: boolean;
  signal?: AbortSignal;
};

/**
 * Un appel à l'API, jeton et renouvellement compris.
 *
 * Sur 401, le jeton est renouvelé UNE fois et la requête rejouée. Une seule
 * tentative, jamais de boucle : si la requête rejouée échoue encore, le refresh
 * token est lui-même mort, et réessayer ne ferait que marteler le serveur avec
 * des identifiants périmés.
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

    return fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  };

  let response = await send(anonymous ? null : (readTokens()?.access_token ?? null));

  if (response.status === 401 && !anonymous) {
    const renewed = await refreshOnce();
    if (!renewed) {
      clearTokens();
      throw new SessionExpiredError();
    }
    response = await send(renewed.access_token);

    if (response.status === 401) {
      clearTokens();
      throw new SessionExpiredError();
    }
  }

  if (!response.ok) {
    /* Le corps d'erreur peut manquer (502 d'un proxy, coupure réseau) : on ne
       laisse pas l'échec d'analyse masquer le vrai statut HTTP, seule
       information fiable dont on dispose à ce stade. */
    const errorBody = await response
      .json()
      .catch(() => null) as Partial<ApiErrorBody> | null;
    throw new ApiError(response.status, errorBody);
  }

  /* 204 No Content — la déconnexion, notamment. `response.json()` échouerait
     sur un corps vide. */
  if (response.status === 204) return undefined as T;

  return (await response.json()) as T;
}

/**
 * Construit une chaîne de requête en ignorant les filtres vides.
 *
 * Un paramètre présent mais vide (`?status=`) n'est PAS équivalent à un
 * paramètre absent : Spring tenterait de convertir la chaîne vide en enum et
 * répondrait 400. Les valeurs nulles, indéfinies et vides sont donc écartées
 * plutôt que sérialisées.
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
