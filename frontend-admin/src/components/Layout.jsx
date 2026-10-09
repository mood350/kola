import { useEffect } from 'react';
import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useSession } from '../lib/session';
import { useResource } from '../lib/useResource';
import { initials } from '../lib/format';
import { PageHead } from './ui';
import MobileNav from './MobileNav';
import {
  IconArrows, IconCoins, IconHome, IconLogout, IconSavings, IconSettings, IconShield, IconUsers,
} from './icons';

/**
 * Les sept rubriques, et seulement elles : les sous-sections vivent dans leur rubrique.
 * `module` est celui que le serveur exige pour les données de la rubrique ; `null` = ouverte à
 * tous les rôles (Paramètres contient « Mon compte »).
 */
const NAV = [
  { to: '/', label: 'Accueil', module: 'dashboard', end: true, Icon: IconHome },
  { to: '/utilisateurs', label: 'Utilisateurs', module: 'users', Icon: IconUsers },
  { to: '/kyc', label: 'KYC', module: 'users', Icon: IconShield, pending: true },
  { to: '/transactions', label: 'Transactions', module: 'users', Icon: IconArrows },
  { to: '/credits', label: 'Crédits', module: 'credit', Icon: IconCoins },
  { to: '/epargne', label: 'Épargne', module: 'users', Icon: IconSavings },
  { to: '/parametres', label: 'Paramètres', module: null, Icon: IconSettings },
];

export default function Layout() {
  const { status, user, can, logout } = useSession();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  // Le nombre de pièces en attente, rafraîchi à chaque changement de page :
  // c'est la seule file que l'opérateur doit vider.
  const overview = useResource(
    (signal) => (status === 'authenticated' ? api.get('/console/overview', { signal }) : Promise.resolve(null)),
    `${status}|${pathname}`,
  );
  const kycPending = overview.data?.kycPending ?? 0;

  // Une décision KYC (sur sa page ou sur la fiche d'un client) change ce compteur sans changer de page.
  const { reload } = overview;
  useEffect(() => {
    window.addEventListener('kola:kyc-changed', reload);
    return () => window.removeEventListener('kola:kyc-changed', reload);
  }, [reload]);

  if (status === 'loading') return <div className="empty">Vérification de la session…</div>;
  if (status !== 'authenticated') return <Navigate to="/connexion" replace state={{ from: pathname }} />;

  const items = NAV.filter((item) => item.module === null || can(item.module));
  const handleLogout = async () => {
    await logout();
    navigate('/connexion', { replace: true });
  };

  return (
    <div className="shell">
      <a className="skip" href="#contenu">Aller au contenu</a>

      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><img src="/kola-logo-transparent.png" alt="" /></div>
          <div>
            <div className="brand-name">KOLA</div>
            <div className="brand-sub">Administration</div>
          </div>
        </div>

        <nav className="nav" aria-label="Navigation principale">
          {items.map(({ to, label, end, Icon, pending }) => (
            <NavLink key={to} to={to} end={end}>
              <Icon />
              {label}
              {pending && kycPending > 0 && (
                <span className="pill" aria-label={`${kycPending} en attente`}>{kycPending}</span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="me">
          <span className="avatar">{initials(user?.name)}</span>
          <NavLink to="/parametres" className="me-text" title="Mon compte">
            <div className="me-name">{user?.name}</div>
            <div className="me-role">{user?.role}</div>
          </NavLink>
          <button type="button" className="icon-btn" onClick={handleLogout} title="Se déconnecter" aria-label="Se déconnecter">
            <IconLogout />
          </button>
        </div>
      </aside>

      <main className="main" id="contenu" tabIndex={-1}>
        <div className="main-inner">
          <Outlet />
        </div>
      </main>

      <MobileNav items={items} kycPending={kycPending} user={user} onLogout={handleLogout} />
    </div>
  );
}

/** Garde de rubrique : un lien masqué ne suffit pas quand l'URL est tapée à la main. */
export function RequireModule({ module, children }) {
  const { can } = useSession();
  if (!can(module)) {
    return <PageHead title="Accès refusé" subtitle="Votre rôle ne donne pas accès à cette rubrique." />;
  }
  return children;
}
