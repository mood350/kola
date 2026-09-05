import { s } from '../../lib/style';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useFinance } from '../../hooks/useFinance';
import { operatorStatusView } from '../presentation';

export default function Finance() {
  const { loading, error, reload, liquidity, revenue, operators } = useFinance();

  if (loading) return <LoadingState label="Chargement du suivi financier…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:linear-gradient(140deg,#0F3875,#002353 72%); border-radius:24px; padding:24px; color:#fff'), animation: 'kUp .5s ease both' }}>
        <div style={s('font-size:11px; font-weight:800; letter-spacing:.16em; color:rgba(255,255,255,.55)')}>LIQUIDITÉ EN TEMPS RÉEL</div>
        <div style={s('display:flex; gap:14px; margin-top:16px')}>
          {liquidity.map((l) => (
            <div key={l.label} style={s('flex:1; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:18px; padding:16px')}>
              <div style={s('font-size:10.5px; font-weight:800; letter-spacing:.08em; color:#FFCB05')}>{l.label}</div>
              <div style={s('font-size:20px; font-weight:800; margin-top:9px')}>{l.value}</div>
              <div style={s('font-size:11px; color:rgba(255,255,255,.55); font-weight:600; margin-top:5px')}>{l.note}</div>
            </div>
          ))}
        </div>
        <div style={s('height:9px; border-radius:9px; background:rgba(255,255,255,.14); margin-top:16px; overflow:hidden; display:flex')}>
          {liquidity.map((l) => (
            <div key={l.label} style={{ width: l.pct + '%', background: l.label.startsWith('DISPONIBLE') ? 'rgba(255,255,255,.3)' : l.label.startsWith('BLOQUÉ') ? '#FFCB05' : '#0047BA' }} />
          ))}
        </div>
      </section>

      <div style={s('display:grid; grid-template-columns:1.3fr 1fr; gap:16px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Revenus par source · ce mois</div>
          <div style={s('display:flex; flex-direction:column; gap:12px; margin-top:16px')}>
            {revenue?.lines.map((r) => (
              <div key={r.label}>
                <div style={s('display:flex; justify-content:space-between; font-size:12.5px; font-weight:700')}><span>{r.label}</span><span>{r.value}</span></div>
                <div style={s('height:8px; border-radius:8px; background:#E2E8F0; margin-top:7px; overflow:hidden')}><div style={{ width: r.pct + '%', height: '100%', borderRadius: 8, background: '#0F3875' }} /></div>
              </div>
            ))}
          </div>
          {revenue && (
            <div style={s('border-top:1px solid #E2E8F0; margin-top:16px; padding-top:13px; display:flex; justify-content:space-between; font-size:14px; font-weight:800')}><span>Total</span><span style={{ color: '#0F3875' }}>{revenue.total}</span></div>
          )}
        </section>

        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .14s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Réconciliation opérateurs</div>
          <div style={s('display:flex; flex-direction:column; gap:10px; margin-top:14px')}>
            {operators.map((o) => {
              const view = operatorStatusView(o.status);
              return (
                <div key={o.name} style={s('display:flex; align-items:center; gap:11px; background:#F2F3FF; border-radius:14px; padding:12px 14px')}>
                  <span style={{ flex: 1, fontSize: '12.5px', fontWeight: 700 }}>{o.name}</span>
                  <span style={view.style}>{view.label}</span>
                </div>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}
