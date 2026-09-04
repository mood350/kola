import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useDisputes } from '../../hooks/useDisputes';
import { disputeTagStyle } from '../presentation';

export default function Disputes() {
  const { loading, error, actionError, actionPending, reload, disputes, detail, chargeback, reject, validateChargeback } = useDisputes();

  if (loading) return <LoadingState label="Chargement des litiges…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  const critical = disputes.filter((d) => d.status !== 'resolved' && d.status !== 'rejected').length;

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div style={s('font-size:14.5px; font-weight:800')}>File de réclamations</div>
          <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:20px; background:rgba(186,26,26,.1); color:#BA1A1A')}>{critical} critiques</span>
        </div>
        <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:15px')}>
          {disputes.map((d) => (
            <div key={d.ref} style={s('border:1px solid #E2E8F0; border-radius:20px; padding:16px 18px')}>
              <div style={s('display:flex; align-items:center; gap:12px')}>
                <span style={s("font-family:'JetBrains Mono',monospace; font-size:11px; font-weight:600; color:#596171; background:#F2F3FF; padding:5px 9px; border-radius:9px")}>{d.ref}</span>
                <span style={disputeTagStyle(d.tag)}>{d.tagLabel}</span>
                <span style={s('margin-left:auto; font-size:14px; font-weight:800')}>{d.amount}</span>
              </div>
              <div style={s('font-size:13px; font-weight:700; margin-top:11px')}>{d.title}</div>
              <div style={s('font-size:11.5px; color:#596171; font-weight:600; margin-top:4px')}>{d.meta}</div>
              <div style={s('display:flex; align-items:center; gap:8px; margin-top:14px; flex-wrap:wrap')}>
                <Hoverable as="button" disabled={actionPending} onClick={() => chargeback(d.ref)}
                  style={s('border:0; cursor:pointer; background:#002353; color:#fff; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                  hoverStyle={{ background: '#0F3875' }}
                >Lancer un chargeback</Hoverable>
                <Hoverable as="button" disabled={actionPending} onClick={() => reject(d.ref)}
                  style={s('border:1px solid #E2E8F0; cursor:pointer; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                  hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
                >Rejeter</Hoverable>
                <span style={s('font-size:10.5px; font-weight:800; color:#745B00; background:rgba(255,203,5,.2); padding:7px 11px; border-radius:20px')}>⭑ double validation requise</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {detail && (
        <section style={{ ...s('background:linear-gradient(155deg,#0F3875,#002353 78%); border-radius:24px; padding:22px 24px; color:#fff'), animation: 'kUp .5s .1s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Détail — Chargeback {detail.ref}</div>
          <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr; gap:12px; margin-top:16px')}>
            <div style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:14px; padding:13px')}><div style={s('font-size:10px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.08em')}>COMPTE DÉBITÉ</div><div style={s('font-size:13px; font-weight:700; margin-top:6px')}>{detail.debitedAccount}</div></div>
            <div style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:14px; padding:13px')}><div style={s('font-size:10px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.08em')}>COMPTE CRÉDITÉ</div><div style={s('font-size:13px; font-weight:700; margin-top:6px')}>{detail.creditedAccount}</div></div>
            <div style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:14px; padding:13px')}><div style={s('font-size:10px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.08em')}>MONTANT</div><div style={s('font-size:13px; font-weight:700; margin-top:6px; color:#FFCB05')}>{detail.amount}</div></div>
          </div>
          <div style={s('display:flex; align-items:center; gap:10px; margin-top:18px')}>
            <div style={s('flex:1; background:rgba(255,255,255,.08); border:1px dashed rgba(255,255,255,.3); border-radius:14px; padding:12px 14px; font-size:11.5px; color:rgba(255,255,255,.65)')}>{detail.lastValidationNote}</div>
            <Hoverable as="button" disabled={actionPending || detail.validationsDone >= detail.validationsRequired} onClick={() => validateChargeback(detail.ref)}
              style={s('border:0; cursor:pointer; background:#FFCB05; color:#002353; font-family:Manrope,sans-serif; font-weight:800; font-size:12.5px; padding:11px 18px; border-radius:12px')}
              hoverStyle={{ filter: 'brightness(1.07)' }}
            >Valider ({detail.validationsDone}/{detail.validationsRequired})</Hoverable>
          </div>
        </section>
      )}
    </div>
  );
}
