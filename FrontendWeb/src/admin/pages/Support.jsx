import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useSupport } from '../../hooks/useSupport';
import { ticketStatusView } from '../presentation';

export default function Support() {
  const { loading, error, actionError, actionPending, reload, tickets, manualActions, takeCharge, resolve } = useSupport();

  if (loading) return <LoadingState label="Chargement du support…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>File de tickets support</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {tickets.map((t) => {
            const view = ticketStatusView(t.status);
            return (
              <div key={t.ref} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #E2E8F0')}>
                <span style={s("font-family:'JetBrains Mono',monospace; font-size:11px; color:#596171; background:#F2F3FF; padding:5px 9px; border-radius:9px")}>{t.ref}</span>
                <span style={s('flex:1; font-size:13px; font-weight:700')}>{t.subject}</span>
                <span style={s('font-size:11.5px; font-weight:600; color:#596171')}>{t.userName}</span>
                <span style={view.style}>{view.label}</span>
                {t.status === 'open' && (
                  <Hoverable as="button" disabled={actionPending} onClick={() => takeCharge(t.ref)}
                    style={s('border:0; cursor:pointer; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px')}
                    hoverStyle={{ background: '#E2E8F0' }}
                  >Prendre en charge</Hoverable>
                )}
                {t.status !== 'resolved' && (
                  <Hoverable as="button" disabled={actionPending} onClick={() => resolve(t.ref)}
                    style={s('border:0; cursor:pointer; background:rgba(16,185,129,.12); color:#005236; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px')}
                    hoverStyle={{ background: 'rgba(16,185,129,.18)' }}
                  >Marquer résolu</Hoverable>
                )}
              </div>
            );
          })}
        </div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Historique des actions manuelles</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {manualActions.map((m) => (
            <div key={m.id} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #E2E8F0')}>
              <span style={s('width:8px; height:8px; border-radius:50%; background:#0F3875; flex:0 0 8px')} />
              <span style={s('flex:1; font-size:12.5px; font-weight:700')}>{m.action}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#596171')}>{m.by}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#596171')}>{m.time}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
