// Client HTTP de la console : le seul endroit qui connaît `fetch`.
//
// Un seul jeton admin (JWT de 8 h, sans refresh) ; tout 401 met fin à la
// session. Un 403 — module non accordé au rôle — n'éjecte personne.

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');
const TOKEN_KEY = 'kola_admin_token';

/** Levé par la session pour renvoyer vers la connexion. */
export const UNAUTHORIZED_EVENT = 'kola:unauthorized';

export class ApiError extends Error {
  constructor(message, status, code, fieldErrors) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors || {};
  }
}

export const isConfigured = () => Boolean(BASE_URL);

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // stockage indisponible (navigation privée…) : la session ne survivra pas au rechargement
  }
}

async function send(method, path, { body, params, signal } = {}) {
  if (!BASE_URL) {
    throw new ApiError('VITE_API_BASE_URL n\'est pas défini : la console ne sait pas où joindre l\'API.', 0);
  }

  const url = new URL(BASE_URL + path);
  for (const [key, value] of Object.entries(params || {})) {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
  }

  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    if (cause?.name === 'AbortError') throw cause;
    // Le navigateur n'a reçu aucune réponse : API arrêtée, mauvaise URL, ou
    // origine absente de la liste CORS. « Failed to fetch » ne dit rien de tout ça.
    throw new ApiError(
      `Impossible de joindre l'API (${BASE_URL}). Vérifiez que le backend tourne et qu'il accepte l'origine ${window.location.origin}.`,
      0,
    );
  }

  if (response.status === 401 && path !== '/auth/login') {
    setToken(null);
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }

  return response;
}

async function toError(response) {
  const payload = await response.json().catch(() => null);
  const message = payload?.message
    || (response.status === 403 ? 'Votre rôle ne donne pas accès à cette section.' : `Erreur ${response.status}.`);
  return new ApiError(message, response.status, payload?.code, payload?.fieldErrors);
}

async function request(method, path, options) {
  const response = await send(method, path, options);
  if (!response.ok) throw await toError(response);
  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path, options) => request('GET', path, options),
  post: (path, body, options) => request('POST', path, { ...options, body: body ?? {} }),
  put: (path, body, options) => request('PUT', path, { ...options, body: body ?? {} }),
};

/**
 * Télécharge un fichier protégé (pièce KYC) et l'enregistre sur le disque.
 *
 * JAMAIS AFFICHÉ DANS UN ONGLET : l'API l'envoie en `attachment` exprès, et une
 * URL `blob:` hérite de l'origine de la console — un fichier client piégé
 * ouvert ici pourrait lire le jeton.
 */
export async function download(path, fileName) {
  const response = await send('GET', path);
  if (!response.ok) throw await toError(response);
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || 'document';
  link.click();
  URL.revokeObjectURL(url);
}
