import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useCredit } from '../../hooks/useCredit';

const fieldStyle = s('background:#F2F3FF; border:1px solid #E2E8F0; border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700; width:100%; font-family:Manrope,sans-serif; color:#131B2E; outline:0');

export default function Credit() {
  const { loading, error, actionError, actionPending, reload, stats, defaults, tierConfig, updateTierField, saveTierConfig, remind } = useCredit();
  const [saved, setSaved] = useState(false);

  if (loading) return <LoadingState label="Chargement du portefeuille de crédit…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  const handleSave = async () => {
    setSaved(false);
    await saveTierConfig();
    setSaved(true);
  };

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}

      {stats && (
        <div style={s('display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px')}>
          <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(15,56,117,.35)')}><div style={s('font-size:11px; font-weight:800; color:#596171; letter-spacing:.1em')}>ENCOURS TOTAL</div><div style={s('font-size:22px; font-weight:800; margin-top:8px')}>{stats.outstandingTotal}</div></div>
          <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(15,56,117,.35)')}><div style={s('font-size:11px; font-weight:800; color:#596171; letter-spacing:.1em')}>TAUX DE DÉFAUT</div><div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#BA1A1A')}>{stats.defaultRate}</div></div>
          <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(15,56,117,.35)')}><div style={s('font-size:11px; font-weight:800; color:#596171; letter-spacing:.1em')}>PRÊTS EN RETARD</div><div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#745B00')}>{stats.lateLoans}</div></div>
        </div>
      )}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Paramètres de scoring & paliers de prêt</div>
        <div style={s('font-size:11.5px; color:#596171; font-weight:600; margin-top:4px')}>Réservé au rôle super-admin · version historisée</div>
        <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#596171; border-bottom:1px solid #E2E8F0')}>
          <div>PALIER</div><div>SCORE MIN</div><div>MONTANT MAX</div><div>TAUX MENSUEL</div>
        </div>
        {tierConfig.map((t, i) => (
          <div key={t.name} style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #E2E8F0')}>
            <div style={s('font-size:13px; font-weight:800')}>{t.name}</div>
            <input style={fieldStyle} value={t.minScore} onChange={(e) => { setSaved(false); updateTierField(i, 'minScore', Number(e.target.value) || 0); }} />
            <input style={fieldStyle} value={t.maxAmount} onChange={(e) => { setSaved(false); updateTierField(i, 'maxAmount', e.target.value); }} />
            <input style={fieldStyle} value={t.monthlyRate} onChange={(e) => { setSaved(false); updateTierField(i, 'monthlyRate', e.target.value); }} />
          </div>
        ))}
        <div style={s('display:flex; align-items:center; gap:10px; margin-top:14px')}>
          <Hoverable as="button" disabled={actionPending} onClick={handleSave}
            style={s('border:0; cursor:pointer; background:#002353; color:#fff; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 16px; border-radius:12px')}
            hoverStyle={{ background: '#0F3875' }}
          >{actionPending ? 'Enregistrement…' : 'Enregistrer les paramètres'}</Hoverable>
          <span style={s('font-size:11px; font-weight:800; color:#745B00; background:rgba(255,203,5,.2); padding:8px 12px; border-radius:20px')}>
            {saved ? '✓ enregistré · historique conservé' : '⭑ action tracée · historique conservé'}
          </span>
        </div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Prêts en défaut</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {defaults.map((d) => (
            <div key={d.id} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #E2E8F0')}>
              <span style={s('flex:1; font-size:13px; font-weight:700')}>{d.borrowerName}</span>
              <span style={s('font-size:12px; font-weight:700; color:#596171')}>{d.amount}</span>
              <span style={s('font-size:11.5px; font-weight:700; color:#BA1A1A')}>{d.daysLate} j de retard</span>
              <Hoverable as="button" disabled={actionPending} onClick={() => remind(d.id)}
                style={s('border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
              >Relancer</Hoverable>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
