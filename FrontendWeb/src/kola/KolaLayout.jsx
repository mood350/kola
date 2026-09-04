import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { s } from '../lib/style';
import { BRAND_NAME, BRAND_INITIAL } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import { NAV_ITEMS } from './nav';

export default function KolaLayout() {
  const location = useLocation();
  const activeId = location.pathname.split('/')[2] || 'dashboard';
  const current = NAV_ITEMS.find((i) => i.id === activeId) || NAV_ITEMS[0];

  return (
    <div style={s('display:flex; min-height:100vh; background:linear-gradient(160deg,#E8ECF4 0%,#F5F7FB 55%,#EEF1F8 100%); font-family:Manrope,Helvetica,sans-serif; color:#0A1F5C')}>
      <aside style={s('width:266px; flex:0 0 266px; background:linear-gradient(185deg,#0F2A6B 0%,#0A1F5C 62%,#081847 100%); position:sticky; top:0; height:100vh; display:flex; flex-direction:column; padding:26px 18px 22px; overflow:hidden')}>
        <div style={s('position:absolute; width:280px; height:280px; border-radius:50%; background:radial-gradient(circle,rgba(245,179,1,.16),transparent 70%); top:-120px; right:-130px; pointer-events:none')} />
        <div style={{ ...s('display:flex; align-items:center; gap:11px; padding:4px 8px 0'), animation: 'kIn .5s ease both' }}>
          <div style={s('width:40px; height:40px; border-radius:13px; background:linear-gradient(145deg,#FDB813,#F5B301); display:flex; align-items:center; justify-content:center; font-weight:800; font-size:21px; color:#0A1F5C; box-shadow:0 8px 20px rgba(245,179,1,.34)')}>{BRAND_INITIAL}</div>
          <div>
            <div style={s('color:#fff; font-weight:800; font-size:19px; letter-spacing:.02em; line-height:1')}>{BRAND_NAME}</div>
            <div style={s('color:rgba(255,255,255,.5); font-size:10px; font-weight:600; letter-spacing:.16em; margin-top:3px')}>UEMOA · FINTECH</div>
          </div>
        </div>

        <nav style={s('display:flex; flex-direction:column; gap:5px; margin-top:34px')}>
          {NAV_ITEMS.map((item) => {
            const on = item.id === activeId;
            return (
              <NavLink key={item.id} to={`/app/${item.id}`} style={{ textDecoration: 'none' }}>
                <button
                  style={{
                    display: 'flex', alignItems: 'center', gap: 12, width: '100%', border: 0, cursor: 'pointer',
                    fontFamily: 'Manrope,sans-serif', fontSize: '13.5px', fontWeight: on ? 800 : 600,
                    padding: '12px 13px', borderRadius: 15, transition: 'background .18s ease, color .18s ease',
                    color: on ? '#0A1F5C' : 'rgba(255,255,255,.72)',
                    background: on ? 'linear-gradient(100deg,#FDB813,#F5B301)' : 'transparent',
                    boxShadow: on ? '0 10px 22px -12px rgba(245,179,1,.75)' : 'none',
                  }}
                >
                  <span style={{
                    width: 26, height: 26, borderRadius: 9, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 13, lineHeight: 1, background: on ? 'rgba(10,31,92,.12)' : 'rgba(255,255,255,.08)',
                    color: on ? '#0A1F5C' : '#F5B301',
                  }}>{item.icon}</span>
                  <span style={{ flex: 1, textAlign: 'left' }}>{item.label}</span>
                  {item.badge && (
                    <span style={s('background:#F5B301; color:#0A1F5C; font-size:10px; font-weight:800; padding:2px 7px; border-radius:20px')}>{item.badge}</span>
                  )}
                </button>
              </NavLink>
            );
          })}
        </nav>

        <div style={{ flex: 1, minHeight: 24 }} />
        <div style={{ ...s('margin-top:auto; background:rgba(255,255,255,.07); border:1px solid rgba(255,255,255,.1); border-radius:20px; padding:16px'), animation: 'kUp .7s .2s ease both' }}>
          <div style={s('display:flex; align-items:center; gap:8px')}>
            <span style={{ ...s('width:8px; height:8px; border-radius:50%; background:#F5B301; box-shadow:0 0 0 4px rgba(245,179,1,.2)'), animation: 'kPulse 2.4s infinite' }} />
            <div style={s('color:#fff; font-size:12.5px; font-weight:700')}>Palier KYC 2</div>
          </div>
          <div style={s('color:rgba(255,255,255,.55); font-size:11.5px; line-height:1.5; margin-top:8px')}>Validez votre identité pour un accès illimité.</div>
          <div style={s('height:5px; border-radius:5px; background:rgba(255,255,255,.15); margin-top:12px; overflow:hidden')}>
            <div style={s('width:66%; height:100%; border-radius:5px; background:linear-gradient(90deg,#F5B301,#FDB813)')} />
          </div>
        </div>
      </aside>

      <main style={s('flex:1; min-width:0; display:flex; flex-direction:column')}>
        <header style={s('position:sticky; top:0; z-index:20; display:flex; align-items:center; gap:18px; padding:18px 34px; background:rgba(245,247,251,.82); backdrop-filter:blur(14px); border-bottom:1px solid rgba(10,31,92,.07)')}>
          <div>
            <div style={s('font-size:20px; font-weight:800; letter-spacing:-.01em')}>{current.title}</div>
            <div style={s('font-size:12.5px; color:#7C8AAB; font-weight:500; margin-top:2px')}>{current.sub}</div>
          </div>
          <div style={s('flex:1; max-width:340px; margin-left:auto; display:flex; align-items:center; gap:10px; background:#fff; border:1px solid rgba(10,31,92,.08); border-radius:14px; padding:10px 14px; box-shadow:0 2px 10px rgba(10,31,92,.04)')}>
            <span style={s('color:#9AA6C2; font-size:14px')}>⌕</span>
            <input placeholder="Rechercher une transaction, un coffre…" style={s('border:0; outline:0; font-family:Manrope,sans-serif; font-size:13px; color:#0A1F5C; width:100%; background:transparent')} />
          </div>
          <Hoverable
            as="button"
            style={s('position:relative; width:42px; height:42px; border-radius:14px; border:1px solid rgba(10,31,92,.08); background:#fff; cursor:pointer; font-size:15px; color:#0F2A6B; box-shadow:0 2px 10px rgba(10,31,92,.04)')}
            hoverStyle={{ borderColor: '#F5B301' }}
          >
            🔔
            <span style={s('position:absolute; top:9px; right:10px; width:7px; height:7px; border-radius:50%; background:#F5B301; box-shadow:0 0 0 2.5px #fff')} />
          </Hoverable>
          <div style={s('display:flex; align-items:center; gap:11px; background:#fff; border:1px solid rgba(10,31,92,.08); border-radius:16px; padding:6px 14px 6px 7px; box-shadow:0 2px 10px rgba(10,31,92,.04)')}>
            <div style={s('width:32px; height:32px; border-radius:11px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-weight:800; font-size:12px; display:flex; align-items:center; justify-content:center')}>AK</div>
            <div>
              <div style={s('font-size:13px; font-weight:700; line-height:1.1')}>Aïcha Kodjo</div>
              <div style={s('font-size:10.5px; color:#8894B2; font-weight:600')}>+228 90 12 34 56</div>
            </div>
          </div>
        </header>

        <div style={s('padding:26px 34px 54px; display:flex; flex-direction:column; gap:22px')}>
          <Outlet />
        </div>
      </main>
    </div>
  );
}
