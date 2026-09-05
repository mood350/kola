import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { s, badge } from '../lib/style';
import { BRAND_NAME, BRAND_LOGO } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import { useAuth } from '../auth/useAuth';
import { useNotifications } from '../hooks/useNotifications';
import { alertDotColor } from './presentation';
import { NAV_ITEMS } from './nav';
import { canAccessNav } from './permissions';
import { domainUsesMockData } from '../repositories';

const PROFILE_NAV = { label: 'Mon profil', sub: 'Identité et sécurité du compte' };

const initialsOf = (name) =>
  (name || '')
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { alerts } = useNotifications();
  const activeId = location.pathname.split('/')[2] || 'dashboard';
  const current = activeId === 'profile' ? PROFILE_NAV : NAV_ITEMS.find((i) => i.id === activeId) || NAV_ITEMS[0];
  const visibleNavItems = NAV_ITEMS.filter((item) => canAccessNav(user?.role, item.id));
  const allowed = canAccessNav(user?.role, activeId);

  const [search, setSearch] = useState('');
  const [notifOpen, setNotifOpen] = useState(false);
  const notifRef = useRef(null);

  useEffect(() => {
    if (!notifOpen) return;
    const onPointerDown = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) setNotifOpen(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [notifOpen]);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (!search.trim()) return;
    navigate(`/admin/users?q=${encodeURIComponent(search.trim())}`);
  };

  return (
    <div style={s('display:flex; min-height:100vh; background:#FAF8FF; font-family:Manrope,Helvetica,sans-serif; color:#131B2E')}>
      <aside style={s('width:252px; flex:0 0 252px; background:linear-gradient(185deg,#0F3875 0%,#002353 62%,#002353 100%); position:sticky; top:0; height:100vh; display:flex; flex-direction:column; padding:24px 16px 20px; overflow-y:auto')}>
        <div style={s('display:flex; align-items:center; gap:11px; padding:4px 6px 0')}>
          {/* White tile: the mark is mostly navy and would sink into the navy rail without it. */}
          <div style={s('width:38px; height:38px; border-radius:12px; background:#fff; display:flex; align-items:center; justify-content:center; box-shadow:0 8px 20px rgba(0,0,0,.22)')}>
            <img src={BRAND_LOGO} alt="" style={s('width:28px; height:28px; object-fit:contain; display:block')} />
          </div>
          <div>
            <div style={s('color:#fff; font-weight:800; font-size:16.5px; letter-spacing:.02em; line-height:1')}>{BRAND_NAME}</div>
            <div style={s('color:rgba(255,255,255,.5); font-size:9.5px; font-weight:700; letter-spacing:.14em; margin-top:3px')}>BACK-OFFICE ADMIN</div>
          </div>
        </div>

        <nav style={s('display:flex; flex-direction:column; gap:3px; margin-top:26px')}>
          {visibleNavItems.map((item) => {
            const on = item.id === activeId;
            return (
              <NavLink key={item.id} to={`/admin/${item.id}`} style={{ textDecoration: 'none' }}>
                <button style={{
                  display: 'flex', alignItems: 'center', gap: 11, width: '100%', border: 0, cursor: 'pointer',
                  fontFamily: 'Manrope,sans-serif', fontSize: '12.5px', fontWeight: on ? 800 : 600,
                  padding: '10px 11px', borderRadius: 13, transition: 'background .18s ease, color .18s ease',
                  color: on ? '#002353' : 'rgba(255,255,255,.72)',
                  background: on ? 'linear-gradient(100deg,#FFCB05,#FFCB05)' : 'transparent',
                }}>
                  <span style={{
                    width: 23, height: 23, borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '11.5px', lineHeight: 1, background: on ? 'rgba(0,35,83,.12)' : 'rgba(255,255,255,.08)',
                    color: on ? '#002353' : '#FFCB05',
                  }}>{item.icon}</span>
                  <span style={{ flex: 1, textAlign: 'left' }}>{item.label}</span>
                  {item.badge && (
                    <span style={item.id === 'disputes' ? badge('#BA1A1A', '#fff') : badge('#FFCB05', '#002353')}>{item.badge}</span>
                  )}
                </button>
              </NavLink>
            );
          })}
        </nav>

        <div style={{ flex: 1, minHeight: 16 }} />
        <div style={s('background:rgba(255,255,255,.07); border:1px solid rgba(255,255,255,.1); border-radius:18px; padding:14px')}>
          <div style={s('display:flex; align-items:center; gap:8px')}>
            <span style={{ ...s('width:7px; height:7px; border-radius:50%; background:#10B981; box-shadow:0 0 0 3px rgba(16,185,129,.25)'), animation: 'kPulse 2.4s infinite' }} />
            <div style={s('color:#fff; font-size:11.5px; font-weight:700')}>Session tracée</div>
          </div>
          <div style={s('color:rgba(255,255,255,.52); font-size:10.5px; line-height:1.5; margin-top:6px')}>Toute action sensible est journalisée avec horodatage et identité admin.</div>
        </div>
      </aside>

      <main style={s('flex:1; min-width:0; display:flex; flex-direction:column')}>
        <header style={s('position:sticky; top:0; z-index:20; display:flex; align-items:center; gap:16px; padding:16px 30px; background:rgba(250,248,255,.85); backdrop-filter:blur(14px); border-bottom:1px solid #E2E8F0')}>
          <div>
            <div style={s('font-size:19px; font-weight:800; letter-spacing:-.01em')}>{current.label}</div>
            <div style={s('font-size:12px; color:#596171; font-weight:500; margin-top:2px')}>{current.sub}</div>
          </div>
          <form onSubmit={handleSearch} style={s('flex:1; max-width:320px; margin-left:auto; display:flex; align-items:center; gap:9px; background:#fff; border:1px solid #E2E8F0; border-radius:13px; padding:9px 13px; box-shadow:0 2px 10px rgba(15,56,117,.04)')}>
            <span style={s('color:#596171; font-size:13px')}>⌕</span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Rechercher un utilisateur, une transaction…"
              style={s('border:0; outline:0; font-family:Manrope,sans-serif; font-size:12.5px; color:#131B2E; width:100%; background:transparent')}
            />
          </form>

          <div ref={notifRef} style={{ position: 'relative' }}>
            <Hoverable as="button"
              onClick={() => setNotifOpen((v) => !v)}
              style={s('position:relative; width:40px; height:40px; border-radius:13px; border:1px solid #E2E8F0; background:#fff; cursor:pointer; font-size:14px; color:#0F3875')}
              hoverStyle={{ borderColor: '#FFCB05' }}
            >
              🔔
              {alerts.length > 0 && (
                <span style={s('position:absolute; top:8px; right:9px; width:7px; height:7px; border-radius:50%; background:#BA1A1A; box-shadow:0 0 0 2.5px #fff')} />
              )}
            </Hoverable>
            {notifOpen && (
              <div style={s('position:absolute; top:48px; right:0; width:320px; background:#fff; border:1px solid #E2E8F0; border-radius:18px; box-shadow:0 20px 44px -20px rgba(15,56,117,.4); padding:8px; z-index:30')}>
                <div style={s('font-size:11px; font-weight:800; letter-spacing:.08em; color:#596171; padding:9px 10px 6px')}>ALERTES PRIORITAIRES</div>
                {alerts.length === 0 && (
                  <div style={s('font-size:12px; color:#596171; font-weight:600; padding:10px')}>Aucune alerte pour le moment.</div>
                )}
                {alerts.map((a) => (
                  <Link key={a.title} to="/admin/dashboard" onClick={() => setNotifOpen(false)} style={{ textDecoration: 'none' }}>
                    <div style={s('display:flex; align-items:flex-start; gap:9px; padding:9px 10px; border-radius:12px')}>
                      <span style={{ width: 8, height: 8, borderRadius: '50%', background: alertDotColor(a.severity), marginTop: 4, flex: '0 0 8px' }} />
                      <div>
                        <div style={s('font-size:12px; font-weight:800; color:#131B2E')}>{a.title}</div>
                        <div style={s('font-size:10.5px; color:#596171; font-weight:600; margin-top:2px; line-height:1.5')}>{a.detail}</div>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>

          <Hoverable
            as={Link}
            to="/admin/profile"
            title="Mon profil"
            style={s('display:flex; align-items:center; gap:10px; background:#fff; border:1px solid #E2E8F0; border-radius:15px; padding:5px 13px 5px 6px; cursor:pointer; font-family:Manrope,sans-serif; text-align:left; text-decoration:none')}
            hoverStyle={{ borderColor: '#FFCB05' }}
          >
            <div style={s('width:30px; height:30px; border-radius:10px; background:linear-gradient(145deg,#0F3875,#002353); color:#FFCB05; font-weight:800; font-size:11px; display:flex; align-items:center; justify-content:center')}>{initialsOf(user?.name) || '··'}</div>
            <div>
              <div style={s('font-size:12.5px; font-weight:700; line-height:1.1; color:#131B2E')}>{user?.name || '—'}</div>
              <div style={s('font-size:10px; color:#596171; font-weight:600')}>{user?.role || ''}</div>
            </div>
          </Hoverable>

          <Hoverable as="button"
            onClick={handleLogout}
            title="Se déconnecter"
            style={s('width:40px; height:40px; border-radius:13px; border:1px solid #E2E8F0; background:#fff; cursor:pointer; font-size:14px; color:#0F3875')}
            hoverStyle={{ borderColor: '#FFCB05' }}
          >⏻</Hoverable>
        </header>

        <div style={s('padding:24px 30px 50px; display:flex; flex-direction:column; gap:20px')}>
          {allowed && domainUsesMockData(activeId) && (
            <div style={s('background:#FFF7DB; border:1px solid #FFCB05; border-radius:14px; padding:11px 15px; font-size:12px; font-weight:700; color:#7A5B00')}>
              Données de démonstration — le backend de cette section n'est pas encore branché.
            </div>
          )}
          {allowed ? (
            <Outlet />
          ) : (
            <div style={s('background:#fff; border-radius:24px; padding:40px 32px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35); text-align:center')}>
              <div style={s('font-size:17px; font-weight:800')}>Accès refusé</div>
              <div style={s('font-size:12.5px; color:#596171; font-weight:600; margin-top:8px; line-height:1.6; max-width:420px; margin-left:auto; margin-right:auto')}>
                Votre rôle ({user?.role || '—'}) n'a pas accès à cette section. Contactez un super-admin si vous pensez qu'il s'agit d'une erreur.
              </div>
              <Hoverable as={Link} to="/admin/dashboard"
                style={s('display:inline-block; margin-top:20px; border:0; cursor:pointer; background:#FFCB05; color:#002353; font-family:Manrope,sans-serif; font-weight:800; font-size:13px; padding:11px 20px; border-radius:12px; text-decoration:none')}
                hoverStyle={{ filter: 'brightness(1.05)' }}
              >Retour au tableau de bord</Hoverable>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
