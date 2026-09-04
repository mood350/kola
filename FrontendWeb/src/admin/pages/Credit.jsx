import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { tierConfig, defaults } from '../data';

export default function Credit() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <div style={s('display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px')}>
        <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(10,31,92,.35)')}><div style={s('font-size:11px; font-weight:800; color:#8894B2; letter-spacing:.1em')}>ENCOURS TOTAL</div><div style={s('font-size:22px; font-weight:800; margin-top:8px')}>1,86 Md XOF</div></div>
        <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(10,31,92,.35)')}><div style={s('font-size:11px; font-weight:800; color:#8894B2; letter-spacing:.1em')}>TAUX DE DÉFAUT</div><div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#B3262F')}>2,8 %</div></div>
        <div style={s('background:#fff; border-radius:22px; padding:18px 20px; box-shadow:0 10px 26px -22px rgba(10,31,92,.35)')}><div style={s('font-size:11px; font-weight:800; color:#8894B2; letter-spacing:.1em')}>PRÊTS EN RETARD</div><div style={s('font-size:22px; font-weight:800; margin-top:8px; color:#96690A')}>27</div></div>
      </div>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Paramètres de scoring & paliers de prêt</div>
        <div style={s('font-size:11.5px; color:#7C8AAB; font-weight:600; margin-top:4px')}>Réservé au rôle super-admin · version historisée</div>
        <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
          <div>PALIER</div><div>SCORE MIN</div><div>MONTANT MAX</div><div>TAUX MENSUEL</div>
        </div>
        {tierConfig.map((t) => (
          <div key={t.name} style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #F4F7FC')}>
            <div style={s('font-size:13px; font-weight:800')}>{t.name}</div>
            <div style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.1); border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{t.min}</div>
            <div style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.1); border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{t.max}</div>
            <div style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.1); border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{t.rate}</div>
          </div>
        ))}
        <div style={s('display:flex; align-items:center; gap:10px; margin-top:14px')}>
          <Hoverable as="button"
            style={s('border:0; cursor:pointer; background:#0A1F5C; color:#fff; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 16px; border-radius:12px')}
            hoverStyle={{ background: '#0F2A6B' }}
          >Enregistrer les paramètres</Hoverable>
          <span style={s('font-size:11px; font-weight:800; color:#96690A; background:#FFF4DA; padding:8px 12px; border-radius:20px')}>⭑ action tracée · historique conservé</span>
        </div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Prêts en défaut</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {defaults.map((d) => (
            <div key={d.name} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #F4F7FC')}>
              <span style={s('flex:1; font-size:13px; font-weight:700')}>{d.name}</span>
              <span style={s('font-size:12px; font-weight:700; color:#5C6B8E')}>{d.amount}</span>
              <span style={s('font-size:11.5px; font-weight:700; color:#B3262F')}>{d.late} j de retard</span>
              <Hoverable as="button"
                style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
              >Relancer</Hoverable>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
