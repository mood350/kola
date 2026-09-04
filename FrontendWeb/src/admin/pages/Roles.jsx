import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { admins } from '../data';

export default function Roles() {
  return (
    <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
      <div style={s('font-size:14.5px; font-weight:800')}>Comptes admin & permissions</div>
      <div style={s('display:grid; grid-template-columns:1.4fr 1fr 2fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
        <div>ADMIN</div><div>RÔLE</div><div>PERMISSIONS PAR MODULE</div><div></div>
      </div>
      {admins.map((a) => (
        <div key={a.name} style={s('display:grid; grid-template-columns:1.4fr 1fr 2fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #F4F7FC')}>
          <div style={s('display:flex; align-items:center; gap:10px')}>
            <span style={s('width:32px; height:32px; flex:0 0 32px; border-radius:10px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:11px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{a.initials}</span>
            <span style={s('font-size:13px; font-weight:700')}>{a.name}</span>
          </div>
          <div><span style={a.roleStyle}>{a.role}</span></div>
          <div style={s('font-size:11.5px; font-weight:600; color:#8894B2')}>{a.scope}</div>
          <Hoverable as="button"
            style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
            hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
          >Modifier</Hoverable>
        </div>
      ))}
    </section>
  );
}
