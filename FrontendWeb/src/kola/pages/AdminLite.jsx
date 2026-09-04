import { s, delta } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { metrics, chart, disputes, accounts } from '../data';

export default function AdminLite() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px')}>
        {metrics.map((m) => (
          <div key={m.label} style={{ ...s('background:#fff; border:1px solid rgba(10,31,92,.07); border-radius:22px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.4)'), animation: 'kPop .45s ease both' }}>
            <div style={s('font-size:10.5px; font-weight:800; letter-spacing:.12em; color:#8894B2')}>{m.label}</div>
            <div style={s('font-size:25px; font-weight:800; letter-spacing:-.02em; margin-top:11px')}>{m.value}</div>
            <div style={delta(m.up)}>{m.delta}</div>
          </div>
        ))}
      </div>

      <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div>
            <div style={s('font-size:15.5px; font-weight:800')}>Volume de transactions · 14 jours</div>
            <div style={s('font-size:12.5px; color:#7C8AAB; font-weight:600; margin-top:3px')}>Millions XOF traités par jour</div>
          </div>
          <div style={s('display:flex; gap:8px')}>
            <span style={s('font-size:11px; font-weight:800; padding:7px 12px; border-radius:12px; background:#0A1F5C; color:#F5B301')}>14 j</span>
            <span style={s('font-size:11px; font-weight:800; padding:7px 12px; border-radius:12px; background:#F7F9FD; color:#5C6B8E')}>30 j</span>
          </div>
        </div>
        <div style={s('display:flex; align-items:flex-end; gap:10px; height:170px; margin-top:22px')}>
          {chart.map((c) => (
            <div key={c.day + c.h} style={s('flex:1; display:flex; flex-direction:column; align-items:center; gap:8px; height:100%; justify-content:flex-end')}>
              <div style={{
                width: '100%', borderRadius: '9px 9px 4px 4px', height: c.h + '%',
                background: c.last ? 'linear-gradient(180deg,#F5B301,#FDB813)' : 'linear-gradient(180deg,#1B3E92,#0A1F5C)',
                transition: 'height .6s ease',
              }} />
              <div style={s('font-size:10px; font-weight:700; color:#8894B2')}>{c.day}</div>
            </div>
          ))}
        </div>
      </section>

      <div style={s('display:grid; grid-template-columns:1.35fr 1fr; gap:18px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .1s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div style={s('font-size:15.5px; font-weight:800')}>Litiges & fraude</div>
            <span style={s('font-size:11px; font-weight:800; padding:6px 11px; border-radius:20px; background:#FDEBEC; color:#B3262F')}>3 en attente</span>
          </div>
          <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:16px')}>
            {disputes.map((d) => (
              <Hoverable key={d.ref}
                style={s('border:1px solid rgba(10,31,92,.08); border-radius:20px; padding:16px 18px; transition:box-shadow .2s ease')}
                hoverStyle={{ boxShadow: '0 14px 30px -24px rgba(10,31,92,.5)' }}
              >
                <div style={s('display:flex; align-items:center; gap:12px')}>
                  <span style={s("font-family:'JetBrains Mono',monospace; font-size:11.5px; font-weight:600; color:#5C6B8E; background:#F2F5FC; padding:5px 9px; border-radius:9px")}>{d.ref}</span>
                  <span style={d.tagStyle}>{d.tag}</span>
                  <span style={s('margin-left:auto; font-size:14px; font-weight:800')}>{d.amount}</span>
                </div>
                <div style={s('font-size:13px; font-weight:700; margin-top:11px')}>{d.title}</div>
                <div style={s('font-size:11.5px; color:#8894B2; font-weight:600; margin-top:4px')}>{d.meta}</div>
                <div style={s('display:flex; gap:8px; margin-top:14px')}>
                  <Hoverable as="button"
                    style={s('border:0; cursor:pointer; background:#0A1F5C; color:#fff; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                    hoverStyle={{ background: '#0F2A6B' }}
                  >Chargeback</Hoverable>
                  <Hoverable as="button"
                    style={s('border:1px solid rgba(10,31,92,.12); cursor:pointer; background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                    hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
                  >Rejeter</Hoverable>
                  <Hoverable as="button"
                    style={s('border:0; cursor:pointer; background:transparent; color:#5C6B8E; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 8px; border-radius:11px')}
                    hoverStyle={{ color: '#F5B301' }}
                  >Dossier complet →</Hoverable>
                </div>
              </Hoverable>
            ))}
          </div>
        </section>

        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .16s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Comptes & coffres</div>
          <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
            {accounts.map((a) => (
              <div key={a.name} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #F4F7FC')}>
                <span style={s('width:36px; height:36px; flex:0 0 36px; border-radius:12px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:11.5px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{a.initials}</span>
                <span style={{ flex: 1, minWidth: 0 }}>
                  <span style={s('display:block; font-size:13px; font-weight:700')}>{a.name}</span>
                  <span style={s('display:block; font-size:11px; color:#8894B2; font-weight:600; margin-top:2px')}>{a.meta}</span>
                </span>
                <span style={a.stateStyle}>{a.state}</span>
                <Hoverable as="button"
                  style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:7px 11px; border-radius:10px; cursor:pointer')}
                  hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
                >{a.action}</Hoverable>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
