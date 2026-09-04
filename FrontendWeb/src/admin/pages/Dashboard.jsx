import { Link } from 'react-router-dom';
import { s, delta } from '../../lib/style';
import { BRAND_NAME } from '../../lib/brand';
import Hoverable from '../../components/Hoverable';
import { metrics, chart, alerts, quickLinks } from '../data';

export default function Dashboard() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <div style={s('display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:14px')}>
        {metrics.map((m) => (
          <div key={m.label} style={{ ...s('background:#fff; border:1px solid rgba(10,31,92,.07); border-radius:20px; padding:17px 18px; box-shadow:0 10px 26px -22px rgba(10,31,92,.4)'), animation: 'kPop .45s ease both' }}>
            <div style={s('font-size:10px; font-weight:800; letter-spacing:.1em; color:#8894B2')}>{m.label}</div>
            <div style={s('font-size:21px; font-weight:800; letter-spacing:-.02em; margin-top:9px')}>{m.value}</div>
            <div style={delta(m.up)}>{m.delta}</div>
          </div>
        ))}
      </div>

      <div style={s('display:grid; grid-template-columns:1.5fr 1fr; gap:16px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div>
              <div style={s('font-size:14.5px; font-weight:800')}>Volume de transactions · 14 jours</div>
              <div style={s('font-size:11.5px; color:#7C8AAB; font-weight:600; margin-top:3px')}>Millions XOF traités par jour</div>
            </div>
            <div style={s('display:flex; gap:7px')}>
              <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:11px; background:#0A1F5C; color:#F5B301')}>14 j</span>
              <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:11px; background:#F7F9FD; color:#5C6B8E')}>30 j</span>
            </div>
          </div>
          <div style={s('display:flex; align-items:flex-end; gap:9px; height:150px; margin-top:20px')}>
            {chart.map((c) => (
              <div key={c.day + c.h} style={s('flex:1; display:flex; flex-direction:column; align-items:center; gap:7px; height:100%; justify-content:flex-end')}>
                <div style={{
                  width: '100%', borderRadius: '9px 9px 4px 4px', height: c.h + '%',
                  background: c.last ? 'linear-gradient(180deg,#F5B301,#FDB813)' : 'linear-gradient(180deg,#1B3E92,#0A1F5C)',
                }} />
                <div style={s('font-size:9.5px; font-weight:700; color:#8894B2')}>{c.day}</div>
              </div>
            ))}
          </div>
        </section>

        <section style={{ ...s('background:linear-gradient(155deg,#0F2A6B,#0A1F5C 78%); border-radius:24px; padding:22px; color:#fff'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Alertes prioritaires</div>
          <div style={s('display:flex; flex-direction:column; gap:10px; margin-top:16px')}>
            {alerts.map((a) => (
              <div key={a.title} style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:16px; padding:12px 14px')}>
                <div style={s('display:flex; align-items:center; gap:9px')}>
                  <span style={{ width: 9, height: 9, borderRadius: '50%', background: a.dot }} />
                  <span style={s('font-size:12.5px; font-weight:800; flex:1')}>{a.title}</span>
                </div>
                <div style={s('font-size:11px; color:rgba(255,255,255,.58); font-weight:600; margin-top:5px; line-height:1.5')}>{a.detail}</div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr; gap:16px')}>
        <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(10,31,92,.35)')}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2')}>ENCOURS DE PRÊTS</div>
          <div style={s('font-size:22px; font-weight:800; margin-top:8px')}>1,86 Md XOF</div>
          <div style={s('height:7px; border-radius:7px; background:#EDF1F8; margin-top:11px; overflow:hidden')}><div style={s('width:64%; height:100%; border-radius:7px; background:linear-gradient(90deg,#0F2A6B,#F5B301)')} /></div>
          <div style={s('font-size:11px; font-weight:600; color:#7C8AAB; margin-top:7px')}>64 % de la capacité de prêt allouée</div>
        </div>
        <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(10,31,92,.35)')}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2')}>TAUX DE DÉFAUT</div>
          <div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#B3262F')}>2,8 %</div>
          <div style={s('font-size:11px; font-weight:600; color:#7C8AAB; margin-top:11px')}>Seuil d'alerte fixé à 5 % · sous contrôle</div>
        </div>
        <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(10,31,92,.35)')}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2')}>CROISSANCE UTILISATEURS</div>
          <div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#0E8A5F')}>+ 3 940 / sem.</div>
          <div style={s('font-size:11px; font-weight:600; color:#7C8AAB; margin-top:11px')}>184 320 comptes actifs au total</div>
        </div>
      </div>

      <section style={s('background:#fff; border-radius:24px; padding:18px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35); display:flex; align-items:center; gap:20px; flex-wrap:wrap')}>
        <div style={s('flex:1; min-width:260px')}>
          <div style={s('font-size:12.5px; font-weight:800')}>Cadre réglementaire</div>
          <div style={s('font-size:11.5px; color:#7C8AAB; font-weight:600; margin-top:4px; line-height:1.55')}>{BRAND_NAME} opère en partenariat avec un établissement de monnaie électronique agréé BCEAO. Chiffrement des données au repos et en transit, authentification à deux facteurs et gestion des sessions/appareils actifs.</div>
        </div>
        <div style={s('display:flex; gap:8px; flex-wrap:wrap')}>
          {quickLinks.map((q) => (
            <Link key={q.id} to={`/admin/${q.id}`} style={{ textDecoration: 'none' }}>
              <Hoverable as="button"
                style={s('border:1px solid rgba(10,31,92,.1); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:9px 13px; border-radius:11px; cursor:pointer')}
                hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
              >{q.label}</Hoverable>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
