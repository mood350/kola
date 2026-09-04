import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { audit, reports } from '../data';

export default function Audit() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div>
            <div style={s('font-size:14.5px; font-weight:800')}>Journal d'audit</div>
            <div style={s('font-size:11px; font-weight:700; color:#8894B2; margin-top:3px')}>Immuable · filtrable par admin, action ou période</div>
          </div>
          <Hoverable as="button"
            style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px; cursor:pointer')}
            hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
          >Exporter le journal</Hoverable>
        </div>
        <div style={s('display:grid; grid-template-columns:1fr 1.4fr 1fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
          <div>ADMIN</div><div>ACTION</div><div>AVANT → APRÈS</div><div>HORODATAGE</div>
        </div>
        {audit.map((a, i) => (
          <div key={i} style={s('display:grid; grid-template-columns:1fr 1.4fr 1fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #F4F7FC')}>
            <div style={s('font-size:12.5px; font-weight:700')}>{a.admin}</div>
            <div style={s('font-size:12.5px; font-weight:600; color:#5C6B8E')}>{a.action}</div>
            <div style={s("font-family:'JetBrains Mono',monospace; font-size:11px; color:#8894B2")}>{a.diff}</div>
            <div style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{a.time}</div>
          </div>
        ))}
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Rapports de conformité KYC/AML</div>
        <div style={s('font-size:11.5px; color:#7C8AAB; font-weight:600; margin-top:4px')}>Conformes aux exigences UEMOA / BCEAO</div>
        <div style={s('display:flex; flex-direction:column; margin-top:12px')}>
          {reports.map((r) => (
            <div key={r.name} style={s('display:flex; align-items:center; gap:12px; padding:13px 4px; border-bottom:1px solid #F4F7FC')}>
              <span style={s('flex:1; font-size:13px; font-weight:700')}>{r.name}</span>
              <span style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{r.period}</span>
              <Hoverable as="button"
                style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
              >Exporter PDF</Hoverable>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
