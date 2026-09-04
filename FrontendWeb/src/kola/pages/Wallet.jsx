import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import FocusableInput from '../../components/FocusableInput';
import { wallets, quickAmounts, fxRates, tierLimits, operators, allTxns, filterNames } from '../data';

export default function Wallet() {
  const [filter, setFilter] = useState('Tous');
  const rows = allTxns(filter);

  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px')}>
        {wallets.map((w) => (
          <div key={w.code} style={{ ...s('background:#fff; border:1px solid rgba(10,31,92,.07); border-radius:22px; padding:18px 20px; box-shadow:0 10px 28px -22px rgba(10,31,92,.4)'), animation: 'kPop .45s ease both' }}>
            <div style={s('display:flex; align-items:center; justify-content:space-between')}>
              <span style={s('font-size:11px; font-weight:800; letter-spacing:.12em; color:#8894B2')}>{w.code}</span>
              <span style={s('width:26px; height:26px; border-radius:9px; background:#0A1F5C; color:#F5B301; font-size:11px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{w.sym}</span>
            </div>
            <div style={s('font-size:22px; font-weight:800; letter-spacing:-.02em; margin-top:12px')}>{w.short}</div>
            <div style={s('font-size:11.5px; font-weight:600; color:#7C8AAB; margin-top:4px')}>{w.note}</div>
          </div>
        ))}
      </div>

      <div style={s('display:grid; grid-template-columns:1fr 1fr; gap:18px')}>
        <section style={{ ...s('background:#fff; border-radius:26px; padding:24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Transfert instantané (P2P)</div>
          <div style={s('font-size:12.5px; color:#7C8AAB; font-weight:600; margin-top:4px')}>Vers un compte Dogaa ou un numéro Mobile Money externe.</div>
          <div style={s('display:flex; flex-direction:column; gap:12px; margin-top:18px')}>
            <label style={{ display: 'block' }}>
              <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2; margin-bottom:6px')}>DESTINATAIRE</span>
              <FocusableInput defaultValue="+228 90 55 21 08"
                style={s('width:100%; border:1px solid rgba(10,31,92,.1); border-radius:14px; padding:13px 15px; font-family:Manrope,sans-serif; font-size:14px; font-weight:700; color:#0A1F5C; outline:0; background:#F9FBFE')}
                focusStyle={{ borderColor: '#F5B301', background: '#fff' }} />
            </label>
            <label style={{ display: 'block' }}>
              <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2; margin-bottom:6px')}>MONTANT (XOF)</span>
              <FocusableInput defaultValue="100 000"
                style={s('width:100%; border:1px solid rgba(10,31,92,.1); border-radius:14px; padding:13px 15px; font-family:Manrope,sans-serif; font-size:20px; font-weight:800; color:#0A1F5C; outline:0; background:#F9FBFE')}
                focusStyle={{ borderColor: '#F5B301', background: '#fff' }} />
            </label>
            <div style={s('display:flex; gap:8px')}>
              {quickAmounts.map((q) => (
                <Hoverable key={q} as="button"
                  style={s('flex:1; border:1px solid rgba(10,31,92,.1); background:#F7F9FD; border-radius:12px; padding:9px 0; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; color:#0F2A6B; cursor:pointer')}
                  hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
                >{q}</Hoverable>
              ))}
            </div>
            <div style={s('background:#F7F9FD; border:1px dashed rgba(10,31,92,.14); border-radius:16px; padding:14px 16px; display:flex; flex-direction:column; gap:7px')}>
              <div style={s('display:flex; justify-content:space-between; font-size:12.5px; font-weight:600; color:#7C8AAB')}><span>Frais (1,5 % · TIER_2)</span><span style={{ fontWeight: 800, color: '#0A1F5C' }}>1 500 XOF</span></div>
              <div style={s('display:flex; justify-content:space-between; font-size:12.5px; font-weight:600; color:#7C8AAB')}><span>Total débité</span><span style={{ fontWeight: 800, color: '#0A1F5C' }}>101 500 XOF</span></div>
            </div>
            <Hoverable as="button"
              style={s('border:0; cursor:pointer; background:linear-gradient(100deg,#0F2A6B,#0A1F5C); color:#fff; font-family:Manrope,sans-serif; font-weight:800; font-size:14px; padding:15px; border-radius:16px; transition:transform .18s ease, box-shadow .18s ease')}
              hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 16px 30px -14px rgba(10,31,92,.65)' }}
            >Envoyer 100 000 XOF</Hoverable>
          </div>
        </section>

        <section style={{ ...s('background:linear-gradient(155deg,#0F2A6B,#0A1F5C 75%); border-radius:26px; padding:24px; color:#fff; position:relative; overflow:hidden'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('position:absolute; width:240px; height:240px; border-radius:50%; background:radial-gradient(circle,rgba(245,179,1,.18),transparent 70%); left:-110px; bottom:-120px')} />
          <div style={s('position:relative')}>
            <div style={s('font-size:15.5px; font-weight:800')}>Paiement marchand</div>
            <div style={s('font-size:12.5px; color:rgba(255,255,255,.6); font-weight:600; margin-top:4px')}>Scannez le QR d'un commerçant partenaire ou partagez le vôtre.</div>
            <div style={s('display:flex; align-items:center; gap:20px; margin-top:22px')}>
              <div style={s('width:158px; height:158px; flex:0 0 158px; border-radius:22px; background:#fff; padding:14px; display:flex; align-items:center; justify-content:center')}>
                <div style={s('width:100%; height:100%; border-radius:10px; background:repeating-conic-gradient(#0A1F5C 0% 25%, #fff 0% 50%) 50%/22px 22px; position:relative')}>
                  <div style={s('position:absolute; inset:38%; background:#F5B301; border-radius:6px')} />
                </div>
              </div>
              <div style={s('display:flex; flex-direction:column; gap:11px')}>
                <div>
                  <div style={s('font-size:10.5px; font-weight:800; letter-spacing:.12em; color:#F5B301')}>MON CODE MARCHAND</div>
                  <div style={s("font-family:'JetBrains Mono',monospace; font-size:15px; font-weight:600; margin-top:5px")}>DOGAA-TG-88427</div>
                </div>
                <Hoverable as="button"
                  style={s('border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:12.5px; padding:11px 16px; border-radius:13px')}
                  hoverStyle={{ filter: 'brightness(1.07)' }}
                >Scanner un QR</Hoverable>
                <Hoverable as="button"
                  style={s('border:1px solid rgba(255,255,255,.28); cursor:pointer; background:transparent; color:#fff; font-family:Manrope,sans-serif; font-weight:700; font-size:12.5px; padding:11px 16px; border-radius:13px')}
                  hoverStyle={{ background: 'rgba(255,255,255,.1)' }}
                >Partager mon code</Hoverable>
              </div>
            </div>
            <div style={s('display:flex; gap:10px; margin-top:22px')}>
              <div style={s('flex:1; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:16px; padding:13px')}>
                <div style={s('font-size:10.5px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.1em')}>CE MOIS</div>
                <div style={s('font-size:17px; font-weight:800; margin-top:6px')}>42 paiements</div>
              </div>
              <div style={s('flex:1; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:16px; padding:13px')}>
                <div style={s('font-size:10.5px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.1em')}>DÉPÔTS</div>
                <div style={s('font-size:17px; font-weight:800; margin-top:6px; color:#F5B301')}>0 % de frais</div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div style={s('display:grid; grid-template-columns:1.1fr 1fr; gap:18px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .08s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div style={s('font-size:14.5px; font-weight:800')}>Taux de change</div>
            <span style={s('font-size:10.5px; font-weight:700; color:#8894B2')}>MAJ toutes les 4 h</span>
          </div>
          <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
            {fxRates.map((fx) => (
              <div key={fx.code} style={s('display:flex; align-items:center; gap:10px; padding:11px 4px; border-bottom:1px solid #F4F7FC')}>
                <span style={s('font-size:12.5px; font-weight:800; width:82px')}>1 XOF =</span>
                <span style={{ flex: 1, fontSize: 13, fontWeight: 700 }}>{fx.value}</span>
                <span style={s('font-size:11px; font-weight:700; color:#8894B2')}>{fx.code}</span>
              </div>
            ))}
          </div>
          <div style={s('font-size:11px; color:#8894B2; font-weight:600; margin-top:8px; line-height:1.5')}>Taux fixés via un flux agrégateur de marché, avec une marge de conversion de 1 %.</div>
        </section>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .1s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Limites par palier KYC</div>
          <div style={s('display:flex; flex-direction:column; margin-top:10px')}>
            {tierLimits.map((tl) => (
              <div key={tl.tier} style={s('display:flex; align-items:center; gap:10px; padding:11px 4px; border-bottom:1px solid #F4F7FC')}>
                <span style={s('font-size:12.5px; font-weight:800; width:64px')}>{tl.tier}</span>
                <span style={{ flex: 1, fontSize: '12.5px', fontWeight: 600, color: '#5C6B8E' }}>{tl.limit}</span>
              </div>
            ))}
          </div>
          <div style={s('font-size:11px; color:#8894B2; font-weight:600; margin-top:8px; line-height:1.5')}>Transaction non confirmée par l'opérateur : fonds retenus 15 min puis annulés et recrédités automatiquement.</div>
        </section>
      </div>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .12s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Opérateurs Mobile Money intégrés</div>
        <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:12px; margin-top:14px')}>
          {operators.map((op) => (
            <div key={op.name} style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.07); border-radius:16px; padding:13px 14px')}>
              <div style={s('font-size:12.5px; font-weight:800')}>{op.name}</div>
              <div style={s('font-size:11px; color:#8894B2; font-weight:600; margin-top:4px')}>{op.mode}</div>
            </div>
          ))}
        </div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .12s ease both' }}>
        <div style={s('display:flex; align-items:center; gap:14px; flex-wrap:wrap')}>
          <div style={s('font-size:15.5px; font-weight:800; margin-right:auto')}>Historique complet</div>
          {filterNames.map((n) => (
            <button key={n} onClick={() => setFilter(n)} style={{
              border: '1px solid ' + (n === filter ? 'transparent' : 'rgba(10,31,92,.1)'),
              background: n === filter ? '#0A1F5C' : '#F7F9FD', color: n === filter ? '#F5B301' : '#5C6B8E',
              fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 800, padding: '9px 14px', borderRadius: 12,
              cursor: 'pointer', transition: 'all .18s ease',
            }}>{n}</button>
          ))}
        </div>
        <div style={s('display:grid; grid-template-columns:2.2fr 1fr 1fr 1fr; gap:8px; padding:16px 8px 10px; font-size:10.5px; font-weight:800; letter-spacing:.1em; color:#8894B2; border-bottom:1px solid #EDF1F8')}>
          <div>OPÉRATION</div><div>TYPE</div><div>STATUT</div><div style={{ textAlign: 'right' }}>MONTANT</div>
        </div>
        {rows.map((t, i) => (
          <Hoverable key={i}
            style={s('display:grid; grid-template-columns:2.2fr 1fr 1fr 1fr; gap:8px; align-items:center; padding:14px 8px; border-bottom:1px solid #F4F7FC; border-radius:12px; transition:background .16s ease')}
            hoverStyle={{ background: '#F8FAFE' }}
          >
            <div style={s('display:flex; align-items:center; gap:12px; min-width:0')}>
              <span style={s('width:36px; height:36px; flex:0 0 36px; border-radius:12px; background:#F2F5FC; color:#0F2A6B; display:flex; align-items:center; justify-content:center; font-size:14px')}>{t.icon}</span>
              <span style={{ minWidth: 0 }}>
                <span style={s('display:block; font-size:13.5px; font-weight:700')}>{t.label}</span>
                <span style={s('display:block; font-size:11.5px; color:#8894B2; font-weight:600; margin-top:2px')}>{t.meta}</span>
              </span>
            </div>
            <div style={s('font-size:12.5px; font-weight:700; color:#5C6B8E')}>{t.type}</div>
            <div><span style={t.statusStyle}>{t.status}</span></div>
            <div style={{ ...t.amtStyle, textAlign: 'right' }}>{t.amount}</div>
          </Hoverable>
        ))}
      </section>
    </div>
  );
}
