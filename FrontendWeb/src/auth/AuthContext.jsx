import { useCallback, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { getAuthToken, setAuthToken, UNAUTHORIZED_EVENT } from '../api/httpClient';
import { AuthContext } from './authContextObject';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // No token on boot means there's nothing to restore, so the loading flag
  // can start false instead of being flipped by a synchronous setState in
  // the effect below.
  const [loading, setLoading] = useState(() => Boolean(getAuthToken()));

  useEffect(() => {
    if (!loading) return;
    let cancelled = false;
    authService.restoreSession().then((restored) => {
      if (cancelled) return;
      setUser(restored);
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [loading]);

  // A 401 from any request means the token the backend holds no longer
  // matches ours (expired, revoked, ...) — drop the session so
  // ProtectedRoute redirects to /login instead of every page showing an
  // inline error.
  useEffect(() => {
    const onUnauthorized = () => {
      setAuthToken(null);
      setUser(null);
    };
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const login = useCallback(async (email, password) => {
    const identity = await authService.login(email, password);
    setUser(identity);
    return identity;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, isAuthenticated: Boolean(user), login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
