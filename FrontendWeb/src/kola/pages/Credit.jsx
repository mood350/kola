import { useState } from 'react';
import { s, badge } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { useScoreCountUp, gaugeDashoffset } from '../useScoreCountUp';
import { factors, tierDefs, schedule, fmt } from '../data';

const FINAL_SCORE = 78;

export default function Credit() {
  const score = useScoreCountUp(FINAL_SCORE);
  const [loan, setLoan] = useState(100000);
  const interest = Math.round(loan * 0.08);
  const total = Math.round(loan * 1.08);

  return (
    <div style={s('display:flex; flex-direction:column; gap:20px')}>
      <section style={{ ...s('display:grid; grid-template-columns:300px 1fr; gap:26px; background:linear-gradient(140deg,#0F2A6B,#0A1F5C 70%); border-radius:28px; padding:28px; color:#fff; position:relative; overflow:hidden'), animation: 'kUp .5s ease both' }}>
        <div style={s('position:absolute; inset:0; background:repeating-linear-gradient(115deg,rgba(255,255,255,.04) 0 2px,transparent 2px 24px)')} />
        <div style={s('position:relative; display:flex; flex-direction:column; align-items:center; justify-content:center')}>
          <div style={s('position:relative; width:206px; height:206px')}>
            <svg viewBox="0 0 128 128" style={{ width: 206, height: 206, transform: 'rotate(-90deg)' }}>
              <circle cx="64" cy="64" r="54" fill="none" stroke="rgba(255,255,255,.12)" strokeWidth="11" />
              <circle cx="64" cy="64" r="54" fill="none" stroke="#F5B301" strokeWidth="11" strokeLinecap="round" strokeDasharray="339.3"
                style={{ strokeDashoffset: gaugeDashoffset(score), transition: 'stroke-dashoffset .1s linear' }} />
            </svg>
            <div style={s('position:absolute; inset:0; display:flex; flex-direction:column; align-items:center; justify-content:center')}>
              <div style={s('font-size:52px; font-weight:800; letter-spacing:-.03em; line-height:1')}>{score}</div>
              <div style={s('font-size:11px; font-weight:800; color:rgba(255,255,255,.5); letter-spacing:.16em; margin-top:4px')}>SCORE / 100</div>
            </div>
          </div>
          <div style={s('font-size:12.5px; color:rgba(255,255,255,.62); font-weight:600; text-align:center; margin-top:16px; line-height:1.55')}>Recalculé chaque nuit sur<br />les 30 derniers jours d'activité.</div>
        </div>
        <div style={s('position:relative')}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.2em; color:#F5B301')}>LES 4 FACTEURS DE VOTRE SCORE</div>
          <div style={s('display:grid; grid-template-columns:1fr 1fr; gap:14px; margin-top:16px')}>
            {factors.map((f) => (
              <div key={f.name} style={s('background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.14); border-radius:20px; padding:16px 18px')}>
                <div style={s('display:flex; align-items:baseline; justify-content:space-between')}>
                  <div style={s('font-size:13px; font-weight:800')}>{f.name}</div>
                  <div style={s('font-size:15px; font-weight:800; color:#F5B301')}>{f.value}</div>
                </div>
                <div style={s('height:6px; border-radius:6px; background:rgba(255,255,255,.14); margin-top:12px; overflow:hidden')}>
                  <div style={{ width: f.pct + '%', height: '100%', borderRadius: 6, background: '#F5B301', transition: 'width .9s ease' }} />
                </div>
                <div style={s('font-size:11.5px; color:rgba(255,255,255,.6); font-weight:600; margin-top:10px; line-height:1.5')}>{f.detail}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px')}>
        {tierDefs.map((t) => {
          const on = FINAL_SCORE >= t.min;
          return (
            <div key={t.name} style={{
              background: '#fff', border: '1px solid ' + (on ? 'rgba(245,179,1,.55)' : 'rgba(10,31,92,.07)'),
              borderRadius: 22, padding: '18px 20px',
              boxShadow: on ? '0 14px 30px -22px rgba(245,179,1,.9)' : '0 8px 24px -22px rgba(10,31,92,.4)',
              opacity: on ? 1 : 0.62, animation: 'kPop .45s ease both',
            }}>
              <div style={s('display:flex; align-items:center; justify-content:space-between')}>
                <div style={s('font-size:12.5px; font-weight:800')}>{t.name}</div>
                <span style={on ? badge('#FFF4DA', '#96690A') : badge('#EEF2FA', '#8894B2')}>{on ? 'Débloqué' : 'Verrouillé'}</span>
              </div>
              <div style={s('font-size:21px; font-weight:800; letter-spacing:-.02em; margin-top:12px')}>{t.max}</div>
              <div style={s('font-size:11.5px; font-weight:600; color:#7C8AAB; margin-top:5px')}>{t.rate} · score ≥ {t.min}</div>
            </div>
          );
        })}
      </div>

      <div style={s('display:grid; grid-template-columns:1fr 1.25fr; gap:18px; align-items:start')}>
        <section style={{ ...s('background:#fff; border-radius:26px; padding:24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .1s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Simulateur de prêt</div>
          <div style={s('font-size:12.5px; color:#7C8AAB; font-weight:600; margin-top:4px')}>Déblocage instantané sur votre portefeuille.</div>
          <div style={s('display:flex; align-items:flex-end; gap:8px; margin-top:20px')}>
            <div style={s('font-size:34px; font-weight:800; letter-spacing:-.02em')}>{fmt(loan)}</div>
            <div style={s('font-size:14px; font-weight:800; color:#F5B301; padding-bottom:6px')}>XOF</div>
          </div>
          <input type="range" min="25000" max="300000" step="25000" value={loan}
            onChange={(e) => setLoan(Number(e.target.value))}
            style={{ width: '100%', marginTop: 16, accentColor: '#F5B301' }} />
          <div style={s('display:flex; justify-content:space-between; font-size:11px; font-weight:700; color:#8894B2')}>
            <span>25 000</span><span>plafond 300 000</span>
          </div>
          <div style={s('background:#F7F9FD; border:1px dashed rgba(10,31,92,.14); border-radius:18px; padding:16px; margin-top:18px; display:flex; flex-direction:column; gap:8px')}>
            <div style={s('display:flex; justify-content:space-between; font-size:12.5px; font-weight:600; color:#7C8AAB')}><span>Durée</span><span style={{ fontWeight: 800, color: '#0A1F5C' }}>30 jours</span></div>
            <div style={s('display:flex; justify-content:space-between; font-size:12.5px; font-weight:600; color:#7C8AAB')}><span>Taux (TIER_2)</span><span style={{ fontWeight: 800, color: '#0A1F5C' }}>8 % / mois</span></div>
            <div style={s('display:flex; justify-content:space-between; font-size:12.5px; font-weight:600; color:#7C8AAB')}><span>Intérêts</span><span style={{ fontWeight: 800, color: '#0A1F5C' }}>{fmt(interest)} XOF</span></div>
            <div style={s('border-top:1px solid rgba(10,31,92,.1); padding-top:9px; display:flex; justify-content:space-between; font-size:13.5px; font-weight:800')}><span>À rembourser</span><span style={{ color: '#0F2A6B' }}>{fmt(total)} XOF</span></div>
          </div>
          <Hoverable as="button"
            style={s('width:100%; border:0; cursor:pointer; background:linear-gradient(100deg,#0F2A6B,#0A1F5C); color:#fff; font-family:Manrope,sans-serif; font-weight:800; font-size:14px; padding:15px; border-radius:16px; margin-top:16px; transition:transform .18s ease, box-shadow .18s ease')}
            hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 16px 30px -14px rgba(10,31,92,.65)' }}
          >Demander ce crédit</Hoverable>
        </section>

        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .16s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div style={s('font-size:15.5px; font-weight:800')}>Échéancier de remboursement</div>
            <span style={badge('#E6F5EE', '#0E8A5F')}>Prélèvement auto</span>
          </div>
          <div style={s('display:grid; grid-template-columns:1fr 1fr 1fr; gap:12px; margin-top:18px')}>
            <div style={s('background:#F7F9FD; border-radius:16px; padding:14px')}>
              <div style={s('font-size:10.5px; font-weight:800; color:#8894B2; letter-spacing:.1em')}>CAPITAL</div>
              <div style={s('font-size:16px; font-weight:800; margin-top:6px')}>100 000</div>
            </div>
            <div style={s('background:#F7F9FD; border-radius:16px; padding:14px')}>
              <div style={s('font-size:10.5px; font-weight:800; color:#8894B2; letter-spacing:.1em')}>INTÉRÊTS</div>
              <div style={s('font-size:16px; font-weight:800; margin-top:6px')}>8 000</div>
            </div>
            <div style={s('background:#0A1F5C; border-radius:16px; padding:14px; color:#fff')}>
              <div style={s('font-size:10.5px; font-weight:800; color:rgba(255,255,255,.55); letter-spacing:.1em')}>TOTAL DÛ</div>
              <div style={s('font-size:16px; font-weight:800; margin-top:6px; color:#F5B301')}>108 000</div>
            </div>
          </div>
          <div style={s('display:flex; flex-direction:column; margin-top:8px')}>
            {schedule.map((sItem) => (
              <div key={sItem.label} style={s('display:flex; align-items:center; gap:14px; padding:14px 4px; border-bottom:1px solid #F4F7FC')}>
                <span style={{ width: 11, height: 11, borderRadius: '50%', background: sItem.dot, boxShadow: sItem.glow ? '0 0 0 4px rgba(245,179,1,.25)' : 'none' }} />
                <span style={{ flex: 1 }}>
                  <span style={s('display:block; font-size:13.5px; font-weight:700')}>{sItem.label}</span>
                  <span style={s('display:block; font-size:11.5px; color:#8894B2; font-weight:600; margin-top:2px')}>{sItem.date}</span>
                </span>
                <span style={s('font-size:13.5px; font-weight:800')}>{sItem.amount}</span>
                <span style={sItem.statusStyle}>{sItem.status}</span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
