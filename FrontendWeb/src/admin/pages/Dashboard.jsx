import { Link } from 'react-router-dom';
import { s, delta } from '../../lib/style';
import { BRAND_NAME } from '../../lib/brand';
import Hoverable from '../../components/Hoverable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useDashboard } from '../../hooks/useDashboard';
import { alertDotColor } from '../presentation';

const QUICK_LINKS = [
  { id: 'users', label: 'Utilisateurs' },
  { id: 'credit', label: 'Crédit' },
  { id: 'finance', label: 'Finance' },
  { id: 'disputes', label: 'Litiges' },
  { id: 'audit', label: 'Audit' },
];

export default function Dashboard() {
  const { loading, error, reload, metrics, chart, alerts, loanBook } = useDashboard();

  if (loading) return <LoadingState label="Chargement du tableau de bord…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <div style={s('display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:14px')}>
        {metrics.map((m) => (
          <div key={m.label} style={{ ...s('background:#fff; border:1px solid #E2E8F0; border-radius:20px; padding:17px 18px; box-shadow:0 10px 26px -22px rgba(15,56,117,.4)'), animation: 'kPop .45s ease both' }}>
            <div style={s('font-size:10px; font-weight:800; letter-spacing:.1em; color:#596171')}>{m.label}</div>
            <div style={s('font-size:21px; font-weight:800; letter-spacing:-.02em; margin-top:9px')}>{m.value}</div>
            <div style={delta(m.up)}>{m.delta}</div>
          </div>
        ))}
      </div>

      <div style={s('display:grid; grid-template-columns:1.5fr 1fr; gap:16px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div>
              <div style={s('font-size:14.5px; font-weight:800')}>Volume de transactions · 14 jours</div>
              <div style={s('font-size:11.5px; color:#596171; font-weight:600; margin-top:3px')}>Millions XOF traités par jour</div>
            </div>
            <div style={s('display:flex; gap:7px')}>
              <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:11px; background:#002353; color:#FFCB05')}>14 j</span>
              <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:11px; background:#F2F3FF; color:#596171')}>30 j</span>
            </div>
          </div>
          <div style={s('display:flex; align-items:flex-end; gap:9px; height:150px; margin-top:20px')}>
            {chart.map((c) => (
              <div key={c.day + c.h} style={s('flex:1; display:flex; flex-direction:column; align-items:center; gap:7px; height:100%; justify-content:flex-end')}>
                <div style={{
                  width: '100%', borderRadius: '9px 9px 4px 4px', height: c.h + '%',
                  background: c.last ? 'linear-gradient(180deg,#FFCB05,#FFCB05)' : 'linear-gradient(180deg,#0F3875,#002353)',
                }} />
                <div style={s('font-size:9.5px; font-weight:700; color:#596171')}>{c.day}</div>
              </div>
            ))}
          </div>
        </section>

        <section style={{ ...s('background:linear-gradient(155deg,#0F3875,#002353 78%); border-radius:24px; padding:22px; color:#fff'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Alertes prioritaires</div>
          <div style={s('display:flex; flex-direction:column; gap:10px; margin-top:16px')}>
            {alerts.map((a) => (
              <div key={a.title} style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:16px; padding:12px 14px')}>
                <div style={s('display:flex; align-items:center; gap:9px')}>
                  <span style={{ width: 9, height: 9, borderRadius: '50%', background: alertDotColor(a.severity) }} />
                  <span style={s('font-size:12.5px; font-weight:800; flex:1')}>{a.title}</span>
                </div>
                <div style={s('font-size:11px; color:rgba(255,255,255,.58); font-weight:600; margin-top:5px; line-height:1.5')}>{a.detail}</div>
              </div>
            ))}
          </div>
        </section>
      </div>

      {loanBook && (
        <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr; gap:16px')}>
          <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(15,56,117,.35)')}>
            <div style={s('font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171')}>ENCOURS DE PRÊTS</div>
            <div style={s('font-size:22px; font-weight:800; margin-top:8px')}>{loanBook.outstanding}</div>
            <div style={s('height:7px; border-radius:7px; background:#E2E8F0; margin-top:11px; overflow:hidden')}><div style={{ width: loanBook.allocatedPct + '%', height: '100%', borderRadius: 7, background: 'linear-gradient(90deg,#0F3875,#FFCB05)' }} /></div>
            <div style={s('font-size:11px; font-weight:600; color:#596171; margin-top:7px')}>{loanBook.allocatedPct} % de la capacité de prêt allouée</div>
          </div>
          <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(15,56,117,.35)')}>
            <div style={s('font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171')}>TAUX DE DÉFAUT</div>
            <div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#BA1A1A')}>{loanBook.defaultRate}</div>
            <div style={s('font-size:11px; font-weight:600; color:#596171; margin-top:11px')}>{loanBook.defaultRateNote}</div>
          </div>
          <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(15,56,117,.35)')}>
            <div style={s('font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171')}>CROISSANCE UTILISATEURS</div>
            <div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#005236')}>{loanBook.userGrowth}</div>
            <div style={s('font-size:11px; font-weight:600; color:#596171; margin-top:11px')}>{loanBook.activeUsersTotal}</div>
          </div>
        </div>
      )}

      <section style={s('background:#fff; border-radius:24px; padding:18px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35); display:flex; align-items:center; gap:20px; flex-wrap:wrap')}>
        <div style={s('flex:1; min-width:260px')}>
          <div style={s('font-size:12.5px; font-weight:800')}>Cadre réglementaire</div>
          <div style={s('font-size:11.5px; color:#596171; font-weight:600; margin-top:4px; line-height:1.55')}>{BRAND_NAME} opère en partenariat avec un établissement de monnaie électronique agréé BCEAO. Chiffrement des données au repos et en transit, authentification à deux facteurs et gestion des sessions/appareils actifs.</div>
        </div>
        <div style={s('display:flex; gap:8px; flex-wrap:wrap')}>
          {QUICK_LINKS.map((q) => (
            <Link key={q.id} to={`/admin/${q.id}`} style={{ textDecoration: 'none' }}>
              <Hoverable as="button"
                style={s('border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:9px 13px; border-radius:11px; cursor:pointer')}
                hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
              >{q.label}</Hoverable>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
