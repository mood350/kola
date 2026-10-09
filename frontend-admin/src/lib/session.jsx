import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, getToken, setToken, UNAUTHORIZED_EVENT } from './api';

/**
 * Session administrateur.
 *
 * Au chargement, l'identité est relue sur `/auth/me` plutôt que décodée du
 * jeton : c'est le serveur qui dit si la session vaut encore.
 */

/**
 * Sections accessibles par rôle — miroir de `AdminRole.java`, restreint aux
 * modules que la console expose. NE PROTÈGE RIEN : cela masque des liens. Le
 * serveur applique la même matrice et répond 403 au reste.
 */
const ROLE_MODULES = {
  'Super-admin': ['dashboard', 'users', 'credit', 'config'],
  'Agent conformité': ['dashboard', 'users'],
  'Analyste crédit': ['dashboard', 'credit'],
  Support: ['dashboard', 'users'],
};

const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  // `loading` tant que le jeton éventuel n'a pas été vérifié : sans cet état,
  // un rechargement montrerait un instant l'écran de connexion.
  const [state, setState] = useState(() => ({ status: getToken() ? 'loading' : 'anonymous', user: null }));

  useEffect(() => {
    if (state.status !== 'loading') return undefined;
    const controller = new AbortController();
    api.get('/auth/me', { signal: controller.signal })
      .then((user) => setState({ status: 'authenticated', user }))
      .catch((error) => {
        if (error?.name === 'AbortError') return;
        setToken(null);
        setState({ status: 'anonymous', user: null });
      });
    return () => controller.abort();
  }, [state.status]);

  useEffect(() => {
    const onUnauthorized = () => setState({ status: 'anonymous', user: null });
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const login = useCallback(async (email, password) => {
    const { token, user } = await api.post('/auth/login', { email, password });
    setToken(token);
    setState({ status: 'authenticated', user });
  }, []);

  const logout = useCallback(async () => {
    // Le serveur est prévenu, mais son échec ne retient personne : ce qui
    // compte est l'effacement du jeton sur ce poste.
    await api.post('/auth/logout').catch(() => {});
    setToken(null);
    setState({ status: 'anonymous', user: null });
  }, []);

  const value = useMemo(() => {
    const modules = ROLE_MODULES[state.user?.role] || [];
    // Le rôle Support consulte sans décider : le serveur laisse pourtant passer ces actions (elles ne
    // demandent que le module « utilisateurs »). La console les masque donc pour lui — une intention,
    // pas une protection : le contrôle réel est celui du serveur.
    const canAct = state.user?.role !== 'Support';
    return { ...state, login, logout, can: (module) => modules.includes(module), canAct };
  }, [state, login, logout]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSession() {
  return useContext(SessionContext);
}
