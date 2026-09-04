import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { disputes } from '../data';

export default function Disputes() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div style={s('font-size:14.5px; font-weight:800')}>File de réclamations</div>
          <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:20px; background:#FDEBEC; color:#B3262F')}>3 critiques</span>
        </div>
        <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:15px')}>
          {disputes.map((d) => (
            <div key={d.ref} style={s('border:1px solid rgba(10,31,92,.08); border-radius:20px; padding:16px 18px')}>
              <div style={s('display:flex; align-items:center; gap:12px')}>
                <span style={s("font-family:'JetBrains Mono',monospace; font-size:11px; font-weight:600; color:#5C6B8E; background:#F2F5FC; padding:5px 9px; border-radius:9px")}>{d.ref}</span>
                <span style={d.tagStyle}>{d.tag}</span>
                <span style={s('margin-left:auto; font-size:14px; font-weight:800')}>{d.amount}</span>
              </div>
              <div style={s('font-size:13px; font-weight:700; margin-top:11px')}>{d.title}</div>
              <div style={s('font-size:11.5px; color:#8894B2; font-weight:600; margin-top:4px')}>{d.meta}</div>
              <div style={s('display:flex; align-items:center; gap:8px; margin-top:14px; flex-wrap:wrap')}>
                <Hoverable as="button"
                  style={s('border:0; cursor:pointer; background:#0A1F5C; color:#fff; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                  hoverStyle={{ background: '#0F2A6B' }}
                >Lancer un chargeback</Hoverable>
                <Hoverable as="button"
                  style={s('border:1px solid rgba(10,31,92,.12); cursor:pointer; background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                  hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
                >Rejeter</Hoverable>
                <span style={s('font-size:10.5px; font-weight:800; color:#96690A; background:#FFF4DA; padding:7px 11px; border-radius:20px')}>⭑ double validation requise</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section style={{ ...s('background:linear-gradient(155deg,#0F2A6B,#0A1F5C 78%); border-radius:24px; padding:22px 24px; color:#fff'), animation: 'kUp .5s .1s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Détail — Chargeback TX-99C41A</div>
        <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr; gap:12px; margin-top:16px')}>
          <div style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:14px; padding:13px')}><div style={s('font-size:10px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.08em')}>COMPTE DÉBITÉ</div><div style={s('font-size:13px; font-weight:700; margin-top:6px')}>Boutique Sika</div></div>
          <div style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:14px; padding:13px')}><div style={s('font-size:10px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.08em')}>COMPTE CRÉDITÉ</div><div style={s('font-size:13px; font-weight:700; margin-top:6px')}>Aïcha Kodjo</div></div>
          <div style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:14px; padding:13px')}><div style={s('font-size:10px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.08em')}>MONTANT</div><div style={s('font-size:13px; font-weight:700; margin-top:6px; color:#F5B301')}>450 000 XOF</div></div>
        </div>
        <div style={s('display:flex; align-items:center; gap:10px; margin-top:18px')}>
          <div style={s('flex:1; background:rgba(255,255,255,.08); border:1px dashed rgba(255,255,255,.3); border-radius:14px; padding:12px 14px; font-size:11.5px; color:rgba(255,255,255,.65)')}>1re validation : Sena A. — en attente d'un 2e admin conformité</div>
          <Hoverable as="button"
            style={s('border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:12.5px; padding:11px 18px; border-radius:12px')}
            hoverStyle={{ filter: 'brightness(1.07)' }}
          >Valider (2/2)</Hoverable>
        </div>
      </section>
    </div>
  );
}
