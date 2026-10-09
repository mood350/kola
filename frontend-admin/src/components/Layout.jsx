import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useSession } from '../lib/session';
import { useResource } from '../lib/useResource';
import { initials } from '../lib/format';
import { PageHead } from './ui';
import { IconArrows, IconCoins, IconHome, IconLogout, IconShield, IconUsers } from './icons';

/**
 * Sections de la console — uniquement ce que l'application mobile propose.
 * `module` est celui que le serveur exige pour les données de la section.
 */
const NAV = [
  { to: '/', label: 'Tableau de bord', module: 'dashboard', end: true, Icon: IconHome },
  { to: '/utilisateurs', label: 'Utilisateurs', module: 'users', Icon: IconUsers },
  { to: '/kyc', label: 'Vérifications KYC', module: 'users', Icon: IconShield, pending: true },
  { to: '/transactions', label: 'Transactions', module: 'users', Icon: IconArrows },
  { to: '/prets', label: 'Prêts', module: 'credit', Icon: IconCoins },
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

  if (status === 'loading') return <div className="empty">Vérification de la session…</div>;
  if (status !== 'authenticated') return <Navigate to="/connexion" replace state={{ from: pathname }} />;

  const handleLogout = async () => {
    await logout();
    navigate('/connexion', { replace: true });
  };

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><img src="/kola-logo-transparent.png" alt="" /></div>
          <div>
            <div className="brand-name">KOLA</div>
            <div className="brand-sub">Administration</div>
          </div>
        </div>

        <nav className="nav" aria-label="Sections">
          {NAV.filter((item) => can(item.module)).map(({ to, label, end, Icon, pending }) => (
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
          <NavLink to="/compte" className="me-text" title="Mon compte">
            <div className="me-name">{user?.name}</div>
            <div className="me-role">{user?.role}</div>
          </NavLink>
          <button type="button" className="icon-btn" onClick={handleLogout} title="Se déconnecter" aria-label="Se déconnecter">
            <IconLogout />
          </button>
        </div>
      </aside>

      <main className="main" id="contenu">
        <div className="main-inner">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

/** Garde de section : un lien masqué ne suffit pas quand l'URL est tapée à la main. */
export function RequireModule({ module, children }) {
  const { can } = useSession();
  if (!can(module)) {
    return <PageHead title="Accès refusé" subtitle="Votre rôle ne donne pas accès à cette section." />;
  }
  return children;
}
