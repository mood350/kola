import { s } from '../../lib/style';
import { tickets, manualActions } from '../data';

export default function Support() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>File de tickets support</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {tickets.map((t) => (
            <div key={t.ref} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #F4F7FC')}>
              <span style={s("font-family:'JetBrains Mono',monospace; font-size:11px; color:#5C6B8E; background:#F2F5FC; padding:5px 9px; border-radius:9px")}>{t.ref}</span>
              <span style={s('flex:1; font-size:13px; font-weight:700')}>{t.subject}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{t.user}</span>
              <span style={t.stateStyle}>{t.state}</span>
            </div>
          ))}
        </div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Historique des actions manuelles — Fatou Adé</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {manualActions.map((m, i) => (
            <div key={i} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #F4F7FC')}>
              <span style={s('width:8px; height:8px; border-radius:50%; background:#0F2A6B; flex:0 0 8px')} />
              <span style={s('flex:1; font-size:12.5px; font-weight:700')}>{m.action}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{m.by}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{m.time}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
