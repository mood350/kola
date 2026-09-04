import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { users, userFilterNames, kycQueue } from '../data';

export default function Users() {
  const [filter, setFilter] = useState('Tous');
  const [selectedId, setSelectedId] = useState(null);

  const visible = users.filter((u) => filter === 'Tous' || u.tier === filter || u.state === filter);
  const selected = users.find((u) => u.id === selectedId) || null;

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; gap:12px; flex-wrap:wrap')}>
          <div style={s('font-size:14.5px; font-weight:800; margin-right:auto')}>Comptes utilisateurs</div>
          {userFilterNames.map((n) => (
            <button key={n} onClick={() => setFilter(n)} style={{
              border: '1px solid ' + (n === filter ? 'transparent' : 'rgba(10,31,92,.1)'),
              background: n === filter ? '#0A1F5C' : '#F7F9FD', color: n === filter ? '#F5B301' : '#5C6B8E',
              fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 800, padding: '9px 14px', borderRadius: 12, cursor: 'pointer',
            }}>{n}</button>
          ))}
        </div>
        <div style={s('display:grid; grid-template-columns:1.6fr .9fr .9fr .8fr 1fr; gap:8px; padding:15px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
          <div>UTILISATEUR</div><div>PALIER KYC</div><div>SCORE</div><div>ANCIENNETÉ</div><div>STATUT</div>
        </div>
        {visible.map((u) => (
          <Hoverable key={u.id} as="button" onClick={() => setSelectedId(u.id)}
            style={s('display:grid; grid-template-columns:1.6fr .9fr .9fr .8fr 1fr; gap:8px; align-items:center; width:100%; text-align:left; border:0; cursor:pointer; background:transparent; padding:13px 8px; border-bottom:1px solid #F4F7FC; font-family:Manrope,sans-serif; transition:background .16s ease')}
            hoverStyle={{ background: '#F8FAFE' }}
          >
            <div style={s('display:flex; align-items:center; gap:11px; min-width:0')}>
              <span style={s('width:34px; height:34px; flex:0 0 34px; border-radius:11px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:11px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{u.initials}</span>
              <span style={{ minWidth: 0 }}><span style={s('display:block; font-size:13px; font-weight:700; color:#0A1F5C')}>{u.name}</span><span style={s('display:block; font-size:11px; color:#8894B2; font-weight:600; margin-top:2px')}>{u.phone}</span></span>
            </div>
            <div><span style={u.tierStyle}>{u.tier}</span></div>
            <div style={s('font-size:13px; font-weight:800')}>{u.score}</div>
            <div style={s('font-size:12px; font-weight:600; color:#5C6B8E')}>{u.age}</div>
            <div><span style={u.stateStyle}>{u.state}</span></div>
          </Hoverable>
        ))}
      </section>

      {selected && (
        <section style={{ ...s('background:#fff; border-radius:24px; padding:22px 24px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .45s ease both' }}>
          <div style={s('display:flex; align-items:center; gap:16px')}>
            <span style={s('width:52px; height:52px; flex:0 0 52px; border-radius:16px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:16px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{selected.initials}</span>
            <div style={{ flex: 1 }}>
              <div style={s('font-size:16.5px; font-weight:800')}>{selected.name}</div>
              <div style={s('font-size:12px; color:#8894B2; font-weight:600; margin-top:3px')}>{selected.phone} · {selected.tier} · membre depuis {selected.age}</div>
            </div>
            <Hoverable as="button" onClick={() => setSelectedId(null)}
              style={s('border:0; background:#F7F9FD; color:#5C6B8E; width:34px; height:34px; border-radius:11px; cursor:pointer; font-size:13px')}
              hoverStyle={{ background: '#EDF1F8' }}
            >✕</Hoverable>
          </div>
          <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:14px; margin-top:20px')}>
            <div style={s('background:#F7F9FD; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#8894B2; letter-spacing:.09em')}>SCORE ACTUEL</div><div style={s('font-size:19px; font-weight:800; margin-top:7px')}>{selected.score} / 100</div></div>
            <div style={s('background:#F7F9FD; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#8894B2; letter-spacing:.09em')}>COFFRES ACTIFS</div><div style={s('font-size:19px; font-weight:800; margin-top:7px')}>{selected.vaults}</div></div>
            <div style={s('background:#F7F9FD; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#8894B2; letter-spacing:.09em')}>PRÊT EN COURS</div><div style={s('font-size:19px; font-weight:800; margin-top:7px')}>{selected.loan}</div></div>
            <div style={s('background:#F7F9FD; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#8894B2; letter-spacing:.09em')}>STATUT</div><div style={{ marginTop: 8 }}><span style={selected.stateStyle}>{selected.state}</span></div></div>
          </div>
          <div style={s('display:flex; gap:9px; margin-top:18px; flex-wrap:wrap')}>
            <Hoverable as="button"
              style={s('border:0; cursor:pointer; background:#0A1F5C; color:#fff; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 16px; border-radius:12px')}
              hoverStyle={{ background: '#0F2A6B' }}
            >Débloquer le compte</Hoverable>
            <Hoverable as="button"
              style={s('border:1px solid rgba(10,31,92,.12); cursor:pointer; background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 16px; border-radius:12px')}
              hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
            >Forcer fermeture d'un coffre</Hoverable>
            <span style={s('margin-left:auto; display:flex; align-items:center; gap:6px; font-size:11px; font-weight:800; color:#96690A; background:#FFF4DA; padding:8px 12px; border-radius:20px')}>⭑ action tracée · confirmation requise</span>
          </div>
        </section>
      )}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .1s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div style={s('font-size:14.5px; font-weight:800')}>File de validation KYC (TIER_2 / TIER_3)</div>
          <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:20px; background:#FFF4DA; color:#96690A')}>4 en attente</span>
        </div>
        <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:15px')}>
          {kycQueue.map((k) => (
            <div key={k.name} style={s('display:flex; align-items:center; gap:16px; border:1px solid rgba(10,31,92,.08); border-radius:18px; padding:14px 16px')}>
              <div style={s("width:64px; height:44px; flex:0 0 64px; border-radius:10px; background:repeating-linear-gradient(45deg,#EEF2FA 0 5px,#F7F9FD 5px 10px); display:flex; align-items:center; justify-content:center; font-family:'JetBrains Mono',monospace; font-size:8.5px; color:#8894B2")}>pièce ID</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={s('font-size:13px; font-weight:700')}>{k.name}</div>
                <div style={s('font-size:11px; color:#8894B2; font-weight:600; margin-top:3px')}>{k.meta}</div>
              </div>
              <Hoverable as="button"
                style={s('border:0; cursor:pointer; background:#E6F5EE; color:#0E8A5F; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                hoverStyle={{ background: '#d6f0e2' }}
              >Approuver</Hoverable>
              <Hoverable as="button"
                style={s('border:0; cursor:pointer; background:#FDEBEC; color:#B3262F; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                hoverStyle={{ background: '#fbdadc' }}
              >Rejeter</Hoverable>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
