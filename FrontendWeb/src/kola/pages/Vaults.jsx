import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import FocusableInput from '../../components/FocusableInput';
import { vaults } from '../data';

export default function Vaults() {
  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr; gap:16px')}>
        <div style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.4)'), animation: 'kPop .45s ease both' }}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.11em; color:#8894B2')}>SOLDE DISPONIBLE</div>
          <div style={s('font-size:26px; font-weight:800; margin-top:10px; letter-spacing:-.02em')}>1 259 400 XOF</div>
          <div style={s('font-size:11.5px; font-weight:600; color:#7C8AAB; margin-top:5px')}>Utilisable pour transferts et paiements</div>
        </div>
        <div style={{ ...s('background:linear-gradient(150deg,#0F2A6B,#0A1F5C); color:#fff; border-radius:24px; padding:20px 22px; position:relative; overflow:hidden'), animation: 'kPop .45s .06s ease both' }}>
          <div style={s('position:absolute; width:180px; height:180px; border-radius:50%; background:radial-gradient(circle,rgba(245,179,1,.2),transparent 70%); right:-70px; top:-80px')} />
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.11em; color:rgba(255,255,255,.55)')}>SOLDE BLOQUÉ (VAULTS)</div>
          <div style={s('font-size:26px; font-weight:800; margin-top:10px; letter-spacing:-.02em; color:#F5B301')}>485 000 XOF</div>
          <div style={s('font-size:11.5px; font-weight:600; color:rgba(255,255,255,.6); margin-top:5px')}>Verrouillé · non dépensable</div>
        </div>
        <div style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.4)'), animation: 'kPop .45s .12s ease both' }}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.11em; color:#8894B2')}>IMPACT SUR LE SCORE</div>
          <div style={s('font-size:26px; font-weight:800; margin-top:10px; letter-spacing:-.02em; color:#0E8A5F')}>+ 12 pts</div>
          <div style={s('font-size:11.5px; font-weight:600; color:#7C8AAB; margin-top:5px')}>Grâce à 3 virements programmés actifs</div>
        </div>
      </div>

      <div style={s('display:grid; grid-template-columns:1.55fr 1fr; gap:18px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Mes coffres-forts</div>
          <div style={s('display:flex; flex-direction:column; gap:14px; margin-top:18px')}>
            {vaults.map((v) => (
              <Hoverable key={v.name}
                style={s('border:1px solid rgba(10,31,92,.08); border-radius:22px; padding:18px 20px; transition:box-shadow .2s ease, transform .2s ease')}
                hoverStyle={{ transform: 'translateY(-3px)', boxShadow: '0 16px 32px -22px rgba(10,31,92,.45)' }}
              >
                <div style={s('display:flex; align-items:center; gap:14px')}>
                  <span style={s('width:44px; height:44px; flex:0 0 44px; border-radius:15px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:18px; display:flex; align-items:center; justify-content:center')}>{v.icon}</span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={s('font-size:14.5px; font-weight:800')}>{v.name}</div>
                    <div style={s('font-size:11.5px; font-weight:600; color:#8894B2; margin-top:3px')}>{v.rule}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={s('font-size:15px; font-weight:800')}>{v.saved}</div>
                    <div style={s('font-size:11px; font-weight:700; color:#8894B2; margin-top:2px')}>/ {v.goal}</div>
                  </div>
                </div>
                <div style={s('height:9px; border-radius:9px; background:#EDF1F8; margin-top:15px; overflow:hidden')}>
                  <div style={{ width: v.pct + '%', height: '100%', borderRadius: 9, background: 'linear-gradient(90deg,#0F2A6B,#F5B301)', transition: 'width .8s ease' }} />
                </div>
                <div style={s('display:flex; align-items:center; justify-content:space-between; margin-top:10px')}>
                  <span style={s('font-size:11.5px; font-weight:700; color:#0F2A6B')}>{v.pct} % atteint</span>
                  <span style={v.tagStyle}>{v.tag}</span>
                </div>
              </Hoverable>
            ))}
          </div>
        </section>

        <section style={{ ...s('background:#fff; border-radius:26px; padding:24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Créer un coffre-fort</div>
          <div style={s('font-size:12.5px; color:#7C8AAB; font-weight:600; margin-top:4px')}>Les fonds versés sont verrouillés jusqu'à l'objectif.</div>
          <div style={s('display:flex; flex-direction:column; gap:12px; margin-top:18px')}>
            <label style={{ display: 'block' }}>
              <span style={s("display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2; margin-bottom:6px")}>NOM DE L'OBJECTIF</span>
              <FocusableInput defaultValue="Stock de marchandises"
                style={s('width:100%; border:1px solid rgba(10,31,92,.1); border-radius:14px; padding:12px 15px; font-family:Manrope,sans-serif; font-size:13.5px; font-weight:700; color:#0A1F5C; outline:0; background:#F9FBFE')}
                focusStyle={{ borderColor: '#F5B301', background: '#fff' }} />
            </label>
            <label style={{ display: 'block' }}>
              <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2; margin-bottom:6px')}>MONTANT CIBLE (XOF)</span>
              <FocusableInput defaultValue="500 000"
                style={s('width:100%; border:1px solid rgba(10,31,92,.1); border-radius:14px; padding:12px 15px; font-family:Manrope,sans-serif; font-size:17px; font-weight:800; color:#0A1F5C; outline:0; background:#F9FBFE')}
                focusStyle={{ borderColor: '#F5B301', background: '#fff' }} />
            </label>
            <div style={s('background:#F7F9FD; border-radius:18px; padding:16px')}>
              <div style={s('display:flex; align-items:center; justify-content:space-between')}>
                <div style={s('font-size:12.5px; font-weight:800')}>Virement programmé</div>
                <span style={s('width:44px; height:25px; border-radius:20px; background:#F5B301; position:relative; display:inline-block')}><span style={s('position:absolute; top:3px; right:3px; width:19px; height:19px; border-radius:50%; background:#fff')} /></span>
              </div>
              <div style={s('display:flex; gap:9px; margin-top:14px')}>
                <div style={{ flex: 1 }}>
                  <div style={s('font-size:10.5px; font-weight:800; color:#8894B2; letter-spacing:.08em')}>MONTANT</div>
                  <div style={s('background:#fff; border:1px solid rgba(10,31,92,.1); border-radius:12px; padding:10px 12px; font-size:13px; font-weight:800; margin-top:5px')}>25 000</div>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={s('font-size:10.5px; font-weight:800; color:#8894B2; letter-spacing:.08em')}>JOUR DU MOIS</div>
                  <div style={s('background:#fff; border:1px solid rgba(10,31,92,.1); border-radius:12px; padding:10px 12px; font-size:13px; font-weight:800; margin-top:5px')}>le 5</div>
                </div>
              </div>
              <div style={s('font-size:11.5px; color:#7C8AAB; font-weight:600; line-height:1.55; margin-top:12px')}>Le système vérifie les fonds à minuit, exécute le transfert puis reprogramme le mois suivant.</div>
            </div>
            <Hoverable as="button"
              style={s('border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:14px; padding:15px; border-radius:16px; transition:transform .18s ease, box-shadow .18s ease')}
              hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 14px 26px -12px rgba(245,179,1,.7)' }}
            >Verrouiller mon épargne</Hoverable>
          </div>
        </section>
      </div>

      <div style={s('display:grid; grid-template-columns:1fr 1fr; gap:18px')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .1s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Règles des virements et retraits</div>
          <div style={s('display:flex; flex-direction:column; gap:10px; margin-top:14px')}>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#F5B301; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>Échec d'un virement programmé : 2 nouvelles tentatives dans la journée, notification immédiate, puis suspension de l'objectif après 3 échecs.</span></div>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#F5B301; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>Retrait anticipé autorisé avant l'objectif, avec une pénalité de 2 % sur le montant retiré (dissuasive, non punitive).</span></div>
          </div>
        </section>
        <section style={{ ...s('background:linear-gradient(150deg,#0F2A6B,#0A1F5C); color:#fff; border-radius:24px; padding:20px 22px'), animation: 'kUp .5s .16s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div style={s('font-size:14.5px; font-weight:800')}>Coffre collectif (tontine)</div>
            <span style={s('font-size:10px; font-weight:800; color:#0A1F5C; background:#F5B301; padding:5px 10px; border-radius:20px')}>Nouveau</span>
          </div>
          <div style={s('font-size:12px; color:rgba(255,255,255,.62); font-weight:600; margin-top:8px; line-height:1.6')}>Plusieurs utilisateurs cotisent vers un objectif commun (ex : achat groupé de stock). Chaque membre voit sa contribution et le total collectif en temps réel.</div>
          <Hoverable as="button"
            style={s('margin-top:13px; border:1px solid rgba(255,255,255,.28); cursor:pointer; background:transparent; color:#fff; font-family:Manrope,sans-serif; font-weight:700; font-size:12.5px; padding:10px 16px; border-radius:12px')}
            hoverStyle={{ background: 'rgba(255,255,255,.1)' }}
          >Créer un coffre collectif</Hoverable>
        </section>
      </div>
    </div>
  );
}
