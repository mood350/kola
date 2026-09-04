import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { feeConfig, merchants } from '../data';

export default function Config() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div style={s('font-size:14.5px; font-weight:800')}>Frais & commissions par tier KYC</div>
          <span style={s('font-size:10.5px; font-weight:800; color:#96690A; background:#FFF4DA; padding:7px 11px; border-radius:20px')}>super-admin uniquement</span>
        </div>
        <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
          <div>TIER</div><div>TRANSFERT P2P</div><div>MARCHAND</div><div>CASH-OUT</div>
        </div>
        {feeConfig.map((f) => (
          <div key={f.tier} style={s('display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #F4F7FC')}>
            <div style={s('font-size:13px; font-weight:800')}>{f.tier}</div>
            <div style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.1); border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{f.p2p}</div>
            <div style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.1); border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{f.merchant}</div>
            <div style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.1); border-radius:10px; padding:8px 11px; font-size:12.5px; font-weight:700')}>{f.cashout}</div>
          </div>
        ))}
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Marchands partenaires</div>
        <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
          {merchants.map((m) => (
            <div key={m.name} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #F4F7FC')}>
              <span style={s('flex:1; font-size:13px; font-weight:700')}>{m.name}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{m.cat}</span>
              <span style={m.stateStyle}>{m.state}</span>
              <Hoverable as="button"
                style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
              >{m.action}</Hoverable>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
