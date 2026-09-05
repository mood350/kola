// Thin fetch wrapper used by every Http*Repository once a real backend
// exists. Nothing above this layer (services, hooks, pages) ever imports
// `fetch` directly — this is the one place that knows about HTTP.

const TOKEN_STORAGE_KEY = 'dogaa_admin_token';

/** Fired whenever the backend rejects the current token; AuthContext listens
 * for this to clear the session and let ProtectedRoute redirect to /login. */
export const UNAUTHORIZED_EVENT = 'dogaa:unauthorized';

export class ApiError extends Error {
  constructor(message, { status, payload, cause } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
    /** The original failure when the request never reached the server. */
    this.cause = cause;
  }
}

export function getAuthToken() {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setAuthToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_STORAGE_KEY, token);
    else localStorage.removeItem(TOKEN_STORAGE_KEY);
  } catch {
    // storage unavailable (private mode, etc.) — session just won't persist
  }
}

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

export const isBackendConfigured = () => Boolean(BASE_URL);

async function request(method, path, { body, params, signal } = {}) {
  if (!BASE_URL) {
    throw new ApiError(
      `No backend configured (set VITE_API_BASE_URL) — cannot call ${method} ${path}`,
      { status: 0 }
    );
  }

  const url = new URL(BASE_URL + path);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, value);
      }
    }
  }

  const headers = { 'Content-Type': 'application/json' };
  const token = getAuthToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (cause) {
    // The browser never got a response: API down, wrong VITE_API_BASE_URL, or this
    // origin missing from the server's CORS allow-list. Its own message is "Failed to
    // fetch", which on a login screen reads as "wrong password" — say what it is instead.
    if (cause?.name === 'AbortError') throw cause;
    throw new ApiError(
      `Impossible de joindre l'API (${method} ${BASE_URL}${path}). Vérifiez que le backend tourne `
        + `et qu'il autorise l'origine ${window.location.origin} (CORS).`,
      { status: 0, cause }
    );
  }

  const text = await res.text();
  const payload = text ? safeJsonParse(text) : null;

  if (!res.ok) {
    if (res.status === 401) {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    throw new ApiError(payload?.message || `${method} ${path} failed with HTTP ${res.status}`, {
      status: res.status,
      payload,
    });
  }
  return payload;
}

function safeJsonParse(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

/**
 * Fetches a binary file with the session token attached.
 *
 * <p>A KYC document cannot be shown with a plain `<img src>`: the endpoint requires an
 * Authorization header, which a browser never sends on an image request. So it is pulled here and
 * handed to the page as a Blob.
 */
async function requestBlob(path, { signal } = {}) {
  if (!BASE_URL) {
    throw new ApiError(`No backend configured (set VITE_API_BASE_URL) — cannot fetch ${path}`, { status: 0 });
  }

  const headers = {};
  const token = getAuthToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(BASE_URL + path, { method: 'GET', headers, signal });
  } catch (cause) {
    if (cause?.name === 'AbortError') throw cause;
    throw new ApiError(`Impossible de joindre l'API (GET ${BASE_URL}${path}).`, { status: 0, cause });
  }

  if (!res.ok) {
    if (res.status === 401) window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    const text = await res.text();
    const payload = text ? safeJsonParse(text) : null;
    throw new ApiError(payload?.message || `GET ${path} failed with HTTP ${res.status}`, {
      status: res.status,
      payload,
    });
  }
  return res.blob();
}

export const httpClient = {
  get: (path, opts) => request('GET', path, opts),
  blob: (path, opts) => requestBlob(path, opts),
  post: (path, body, opts) => request('POST', path, { ...opts, body }),
  patch: (path, body, opts) => request('PATCH', path, { ...opts, body }),
  put: (path, body, opts) => request('PUT', path, { ...opts, body }),
  delete: (path, opts) => request('DELETE', path, opts),
};
