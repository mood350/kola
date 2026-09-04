import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { s, badge } from '../lib/style';
import { BRAND_NAME, BRAND_INITIAL } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import { NAV_ITEMS } from './nav';

export default function AdminLayout() {
  const location = useLocation();
  const activeId = location.pathname.split('/')[2] || 'dashboard';
  const current = NAV_ITEMS.find((i) => i.id === activeId) || NAV_ITEMS[0];

  return (
    <div style={s('display:flex; min-height:100vh; background:linear-gradient(160deg,#E8ECF4 0%,#F5F7FB 55%,#EEF1F8 100%); font-family:Manrope,Helvetica,sans-serif; color:#0A1F5C')}>
      <aside style={s('width:252px; flex:0 0 252px; background:linear-gradient(185deg,#0F2A6B 0%,#0A1F5C 62%,#081847 100%); position:sticky; top:0; height:100vh; display:flex; flex-direction:column; padding:24px 16px 20px; overflow-y:auto')}>
        <div style={s('display:flex; align-items:center; gap:11px; padding:4px 6px 0')}>
          <div style={s('width:38px; height:38px; border-radius:12px; background:linear-gradient(145deg,#FDB813,#F5B301); display:flex; align-items:center; justify-content:center; font-weight:800; font-size:19px; color:#0A1F5C; box-shadow:0 8px 20px rgba(245,179,1,.34)')}>{BRAND_INITIAL}</div>
          <div>
            <div style={s('color:#fff; font-weight:800; font-size:16.5px; letter-spacing:.02em; line-height:1')}>{BRAND_NAME}</div>
            <div style={s('color:rgba(255,255,255,.5); font-size:9.5px; font-weight:700; letter-spacing:.14em; margin-top:3px')}>BACK-OFFICE ADMIN</div>
          </div>
        </div>

        <nav style={s('display:flex; flex-direction:column; gap:3px; margin-top:26px')}>
          {NAV_ITEMS.map((item) => {
            const on = item.id === activeId;
            return (
              <NavLink key={item.id} to={`/admin/${item.id}`} style={{ textDecoration: 'none' }}>
                <button style={{
                  display: 'flex', alignItems: 'center', gap: 11, width: '100%', border: 0, cursor: 'pointer',
                  fontFamily: 'Manrope,sans-serif', fontSize: '12.5px', fontWeight: on ? 800 : 600,
                  padding: '10px 11px', borderRadius: 13, transition: 'background .18s ease, color .18s ease',
                  color: on ? '#0A1F5C' : 'rgba(255,255,255,.72)',
                  background: on ? 'linear-gradient(100deg,#FDB813,#F5B301)' : 'transparent',
                }}>
                  <span style={{
                    width: 23, height: 23, borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '11.5px', lineHeight: 1, background: on ? 'rgba(10,31,92,.12)' : 'rgba(255,255,255,.08)',
                    color: on ? '#0A1F5C' : '#F5B301',
                  }}>{item.icon}</span>
                  <span style={{ flex: 1, textAlign: 'left' }}>{item.label}</span>
                  {item.badge && (
                    <span style={item.id === 'disputes' ? badge('#B3262F', '#fff') : badge('#96690A', '#FFF4DA')}>{item.badge}</span>
                  )}
                </button>
              </NavLink>
            );
          })}
        </nav>

        <div style={{ flex: 1, minHeight: 16 }} />
        <div style={s('background:rgba(255,255,255,.07); border:1px solid rgba(255,255,255,.1); border-radius:18px; padding:14px')}>
          <div style={s('display:flex; align-items:center; gap:8px')}>
            <span style={{ ...s('width:7px; height:7px; border-radius:50%; background:#0E8A5F; box-shadow:0 0 0 3px rgba(14,138,95,.25)'), animation: 'kPulse 2.4s infinite' }} />
            <div style={s('color:#fff; font-size:11.5px; font-weight:700')}>Session tracée</div>
          </div>
          <div style={s('color:rgba(255,255,255,.52); font-size:10.5px; line-height:1.5; margin-top:6px')}>Toute action sensible est journalisée avec horodatage et identité admin.</div>
        </div>
      </aside>

      <main style={s('flex:1; min-width:0; display:flex; flex-direction:column')}>
        <header style={s('position:sticky; top:0; z-index:20; display:flex; align-items:center; gap:16px; padding:16px 30px; background:rgba(245,247,251,.85); backdrop-filter:blur(14px); border-bottom:1px solid rgba(10,31,92,.07)')}>
          <div>
            <div style={s('font-size:19px; font-weight:800; letter-spacing:-.01em')}>{current.label}</div>
            <div style={s('font-size:12px; color:#7C8AAB; font-weight:500; margin-top:2px')}>{current.sub}</div>
          </div>
          <div style={s('flex:1; max-width:320px; margin-left:auto; display:flex; align-items:center; gap:9px; background:#fff; border:1px solid rgba(10,31,92,.08); border-radius:13px; padding:9px 13px; box-shadow:0 2px 10px rgba(10,31,92,.04)')}>
            <span style={s('color:#9AA6C2; font-size:13px')}>⌕</span>
            <input placeholder="Rechercher un utilisateur, une transaction…" style={s('border:0; outline:0; font-family:Manrope,sans-serif; font-size:12.5px; color:#0A1F5C; width:100%; background:transparent')} />
          </div>
          <Hoverable as="button"
            style={s('position:relative; width:40px; height:40px; border-radius:13px; border:1px solid rgba(10,31,92,.08); background:#fff; cursor:pointer; font-size:14px; color:#0F2A6B')}
            hoverStyle={{ borderColor: '#F5B301' }}
          >
            🔔
            <span style={s('position:absolute; top:8px; right:9px; width:7px; height:7px; border-radius:50%; background:#B3262F; box-shadow:0 0 0 2.5px #fff')} />
          </Hoverable>
          <div style={s('display:flex; align-items:center; gap:10px; background:#fff; border:1px solid rgba(10,31,92,.08); border-radius:15px; padding:5px 13px 5px 6px')}>
            <div style={s('width:30px; height:30px; border-radius:10px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-weight:800; font-size:11px; display:flex; align-items:center; justify-content:center')}>SA</div>
            <div>
              <div style={s('font-size:12.5px; font-weight:700; line-height:1.1')}>Sena Amétépé</div>
              <div style={s('font-size:10px; color:#8894B2; font-weight:600')}>Super-admin</div>
            </div>
          </div>
        </header>

        <div style={s('padding:24px 30px 50px; display:flex; flex-direction:column; gap:20px')}>
          <Outlet />
        </div>
      </main>
    </div>
  );
}
