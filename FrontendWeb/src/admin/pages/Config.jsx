import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useConfig } from '../../hooks/useConfig';
import { merchantStatusView } from '../presentation';

export default function Config() {
  const { loading, error, actionError, actionPending, reload, fees, merchants, toggleMerchantStatus } = useConfig();

  if (loading) return <LoadingState label="Chargement de la configuration…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div style={s('font-size:14.5px; font-weight:800')}>Frais & commissions par tier KYC</div>
          <span style={s('font-size:10.5px; font-weight:800; color:#745B00; background:rgba(255,203,5,.2); padding:7px 11px; border-radius:20px')}>super-admin uniquement</span>
        </div>
        <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#596171; border-bottom:1px solid #E2E8F0')}>
          <div>TIER</div><div>TRANSFERT P2P</div><div>MARCHAND</div><div>CASH-OUT</div>
        </div>
        {fees.map((f) => (
          <div key={f.tier} style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #E2E8F0')}>
            <div style={s('font-size:13px; font-weight:800')}>{f.tier}</div>
            <div style={s('background:#F2F3FF; border:1px solid #E2E8F0; border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{f.p2p}</div>
            <div style={s('background:#F2F3FF; border:1px solid #E2E8F0; border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{f.merchant}</div>
            <div style={s('background:#F2F3FF; border:1px solid #E2E8F0; border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{f.cashout}</div>
          </div>
        ))}
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Marchands partenaires</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {merchants.map((m) => {
            const view = merchantStatusView(m.status);
            return (
              <div key={m.id} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #E2E8F0')}>
                <span style={s('flex:1; font-size:13px; font-weight:700')}>{m.name}</span>
                <span style={s('font-size:11.5px; font-weight:600; color:#596171')}>{m.category}</span>
                <span style={view.style}>{view.label}</span>
                <Hoverable as="button" disabled={actionPending} onClick={() => toggleMerchantStatus(m)}
                  style={s('border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                  hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
                >{view.actionLabel}</Hoverable>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
