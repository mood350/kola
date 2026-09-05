import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useAudit } from '../../hooks/useAudit';

export default function Audit() {
  const { loading, error, actionError, actionPending, reload, log, reports, exportLog, exportReport } = useAudit();
  // { id, status } — 'done' once a file was actually handed over, 'unavailable' when the
  // server accepted the request but generates no file yet (it answers `url: null`). Claiming
  // "exporté" on a button that produced nothing is how an auditor ends up with no evidence.
  const [exported, setExported] = useState(null);

  if (loading) return <LoadingState label="Chargement du journal d'audit…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  const runExport = async (id, request) => {
    setExported(null);
    try {
      const result = await request();
      if (result?.url) {
        window.open(result.url, '_blank', 'noopener,noreferrer');
        setExported({ id, status: 'done' });
      } else {
        setExported({ id, status: 'unavailable' });
      }
    } catch {
      // `actionError` already renders the failure at the top of the page.
    }
  };

  const exportLabel = (id, idle) => {
    if (exported?.id !== id) return idle;
    return exported.status === 'done' ? '✓ Fichier ouvert' : 'Indisponible';
  };

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}
      {exported?.status === 'unavailable' && (
        <div style={s('background:#FFF7DB; border:1px solid #FFCB05; border-radius:14px; padding:11px 15px; font-size:12px; font-weight:700; color:#7A5B00; line-height:1.5')}>
          La demande a bien été enregistrée, mais la génération du fichier n'est pas encore
          implémentée côté serveur : aucun document n'a été produit.
        </div>
      )}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div>
            <div style={s('font-size:14.5px; font-weight:800')}>Journal d'audit</div>
            <div style={s('font-size:11px; font-weight:700; color:#596171; margin-top:3px')}>Immuable · filtrable par admin, action ou période</div>
          </div>
          <Hoverable as="button" disabled={actionPending} onClick={() => runExport('log', exportLog)}
            style={s('border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px; cursor:pointer')}
            hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
          >{exportLabel('log', 'Exporter le journal')}</Hoverable>
        </div>
        <div style={s('display:grid; grid-template-columns:1fr 1.4fr 1fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#596171; border-bottom:1px solid #E2E8F0')}>
          <div>ADMIN</div><div>ACTION</div><div>AVANT → APRÈS</div><div>HORODATAGE</div>
        </div>
        {log.map((a) => (
          <div key={a.id} style={s('display:grid; grid-template-columns:1fr 1.4fr 1fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #E2E8F0')}>
            <div style={s('font-size:12.5px; font-weight:700')}>{a.admin}</div>
            <div style={s('font-size:12.5px; font-weight:600; color:#596171')}>{a.action}</div>
            <div style={s("font-family:'JetBrains Mono',monospace; font-size:11px; color:#596171")}>{a.diff}</div>
            <div style={s('font-size:11.5px; font-weight:600; color:#596171')}>{a.time}</div>
          </div>
        ))}
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Rapports de conformité KYC/AML</div>
        <div style={s('font-size:11.5px; color:#596171; font-weight:600; margin-top:4px')}>Conformes aux exigences UEMOA / BCEAO</div>
        <div style={s('display:flex; flex-direction:column; margin-top:12px')}>
          {reports.map((r) => (
            <div key={r.id} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #E2E8F0')}>
              <span style={s('flex:1; font-size:13px; font-weight:700')}>{r.name}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#596171')}>{r.period}</span>
              <Hoverable as="button" disabled={actionPending} onClick={() => runExport(r.id, () => exportReport(r.id))}
                style={s('border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
              >{exportLabel(r.id, 'Exporter PDF')}</Hoverable>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
