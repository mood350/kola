import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { kyc, kycLimits, kycDocs } from '../data';

export default function Kyc() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <section style={{ ...s('background:#fff; border-radius:26px; padding:26px 28px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:flex-end; justify-content:space-between; gap:20px')}>
          <div>
            <div style={s('font-size:11px; font-weight:800; letter-spacing:.16em; color:#8894B2')}>VÉRIFICATION PROGRESSIVE</div>
            <div style={s('font-size:24px; font-weight:800; letter-spacing:-.02em; margin-top:8px')}>Vous êtes au palier <span style={{ color: '#F5B301' }}>TIER_2</span></div>
            <div style={s('font-size:13px; color:#7C8AAB; font-weight:600; margin-top:5px')}>Une étape restante pour un accès total et illimité.</div>
          </div>
          <Hoverable as="button"
            style={s("border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:13px; padding:13px 20px; border-radius:14px; transition:transform .18s ease")}
            hoverStyle={{ transform: 'translateY(-2px)' }}
          >Finaliser TIER_3</Hoverable>
        </div>
        <div style={s('position:relative; margin-top:32px; padding:0 4px')}>
          <div style={s('position:absolute; left:26px; right:26px; top:19px; height:5px; border-radius:5px; background:#EDF1F8')} />
          <div style={s('position:absolute; left:26px; top:19px; width:56%; height:5px; border-radius:5px; background:linear-gradient(90deg,#0F2A6B,#F5B301)')} />
          <div style={s('position:relative; display:grid; grid-template-columns:repeat(4,minmax(0,1fr))')}>
            {kyc.map((k) => (
              <div key={k.name} style={s('display:flex; flex-direction:column; align-items:center; text-align:center; padding:0 10px')}>
                <span style={k.dotStyle}>{k.mark}</span>
                <div style={s('font-size:13px; font-weight:800; margin-top:13px')}>{k.name}</div>
                <div style={s('font-size:11.5px; font-weight:600; color:#8894B2; margin-top:4px; line-height:1.5')}>{k.req}</div>
                <span style={k.badgeStyle}>{k.state}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div style={s('display:grid; grid-template-columns:1fr 1fr; gap:18px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Limites par palier</div>
          <div style={s('display:grid; grid-template-columns:1.2fr 1fr 1fr; gap:8px; padding:16px 6px 10px; font-size:10.5px; font-weight:800; letter-spacing:.1em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
            <div>PALIER</div><div>ENVOI / JOUR</div><div>CRÉDIT</div>
          </div>
          {kycLimits.map((l) => (
            <div key={l.name} style={l.rowStyle}>
              <div style={s('font-size:13px; font-weight:800')}>{l.name}</div>
              <div style={s('font-size:12.5px; font-weight:700; color:#5C6B8E')}>{l.send}</div>
              <div style={s('font-size:12.5px; font-weight:700; color:#5C6B8E')}>{l.credit}</div>
            </div>
          ))}
        </section>

        <section style={{ ...s('background:linear-gradient(155deg,#0F2A6B,#0A1F5C 78%); border-radius:26px; padding:24px; color:#fff; position:relative; overflow:hidden'), animation: 'kUp .5s .14s ease both' }}>
          <div style={s('position:absolute; width:230px; height:230px; border-radius:50%; background:radial-gradient(circle,rgba(245,179,1,.18),transparent 70%); right:-90px; bottom:-110px')} />
          <div style={s('position:relative')}>
            <div style={s('font-size:15.5px; font-weight:800')}>Documents à fournir</div>
            <div style={s('font-size:12.5px; color:rgba(255,255,255,.6); font-weight:600; margin-top:4px')}>Traitement sous 24 h ouvrées.</div>
            <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:20px')}>
              {kycDocs.map((d) => (
                <div key={d.name} style={s('display:flex; align-items:center; gap:13px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:18px; padding:14px 16px')}>
                  <span style={d.iconStyle}>{d.mark}</span>
                  <span style={{ flex: 1 }}>
                    <span style={s('display:block; font-size:13.5px; font-weight:700')}>{d.name}</span>
                    <span style={s('display:block; font-size:11.5px; color:rgba(255,255,255,.55); font-weight:600; margin-top:2px')}>{d.note}</span>
                  </span>
                  <span style={s('font-size:11.5px; font-weight:800; color:#F5B301')}>{d.action}</span>
                </div>
              ))}
            </div>
            <div style={s("border:1px dashed rgba(255,255,255,.25); border-radius:18px; padding:18px; margin-top:14px; text-align:center")}>
              <div style={s("font-family:'JetBrains Mono',monospace; font-size:11px; color:rgba(255,255,255,.5); letter-spacing:.06em")}>glisser une photo de la pièce d'identité</div>
            </div>
          </div>
        </section>
      </div>

      <div style={s('display:grid; grid-template-columns:1fr 1fr; gap:18px')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .2s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Sécurité de la vérification</div>
          <div style={s('display:flex; flex-direction:column; gap:10px; margin-top:14px')}>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#F5B301; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>TIER_2 accepte CNI, passeport ou carte consulaire, avec vérification biométrique (selfie + détection de vivacité) pour limiter l'usurpation d'identité.</span></div>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#F5B301; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>Dépistage automatique des personnes politiquement exposées (PEP) à chaque changement de palier, en conformité AML.</span></div>
          </div>
        </section>
        <section style={{ ...s('background:#F7F9FD; border:1px dashed rgba(10,31,92,.15); border-radius:24px; padding:20px 22px'), animation: 'kUp .5s .24s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Renouvellement du KYC</div>
          <div style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; margin-top:8px; line-height:1.65')}>Vos documents d'identité expirent après 24 mois. Un rappel est envoyé 30 jours avant échéance ; à défaut de renouvellement, vos limites reviennent au palier TIER_1.</div>
        </section>
      </div>
    </div>
  );
}
