import { Link } from 'react-router-dom';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import { useScoreCountUp, gaugeDashoffset } from '../useScoreCountUp';
import { wallets, actions, txns, upcoming } from '../data';

export default function Dashboard() {
  const score = useScoreCountUp(78);

  return (
    <div style={s('display:flex; flex-direction:column; gap:22px')}>
      <div style={s('display:grid; grid-template-columns:1.15fr .95fr .9fr; gap:18px')}>
        <section style={{ ...s('background:linear-gradient(150deg,#0F2A6B,#0A1F5C 70%); border-radius:26px; padding:24px; color:#fff; position:relative; overflow:hidden; box-shadow:0 18px 40px -18px rgba(10,31,92,.5)'), animation: 'kUp .5s ease both' }}>
          <div style={s('position:absolute; width:230px; height:230px; border-radius:50%; background:radial-gradient(circle,rgba(245,179,1,.2),transparent 68%); right:-80px; bottom:-110px')} />
          <div style={s('font-size:11.5px; font-weight:700; letter-spacing:.13em; color:rgba(255,255,255,.55)')}>SOLDE TOTAL CONSOLIDÉ</div>
          <div style={s('display:flex; align-items:flex-end; gap:8px; margin-top:14px')}>
            <div style={s('font-size:32px; font-weight:800; letter-spacing:-.02em; line-height:1')}>1 744 400</div>
            <div style={s('font-size:15px; font-weight:700; color:#F5B301; padding-bottom:4px')}>XOF</div>
          </div>
          <div style={s('display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; margin-top:20px; position:relative')}>
            {wallets.map((w) => (
              <div key={w.code} style={s('min-width:0; background:rgba(255,255,255,.09); border:1px solid rgba(255,255,255,.12); border-radius:14px; padding:10px 11px; overflow:hidden')}>
                <div style={s('font-size:10px; font-weight:800; color:#F5B301; letter-spacing:.08em')}>{w.code}</div>
                <div style={s('font-size:12.5px; font-weight:700; margin-top:5px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis')}>{w.short}</div>
              </div>
            ))}
          </div>
        </section>

        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px; display:flex; align-items:center; gap:18px; box-shadow:0 10px 30px -18px rgba(10,31,92,.28)'), animation: 'kUp .5s .07s ease both' }}>
          <div style={s('position:relative; width:126px; height:126px; flex:0 0 126px')}>
            <svg viewBox="0 0 128 128" style={{ width: 126, height: 126, transform: 'rotate(-90deg)' }}>
              <circle cx="64" cy="64" r="54" fill="none" stroke="#EDF1F8" strokeWidth="13" />
              <circle cx="64" cy="64" r="54" fill="none" stroke="#F5B301" strokeWidth="13" strokeLinecap="round" strokeDasharray="339.3"
                style={{ strokeDashoffset: gaugeDashoffset(score), transition: 'stroke-dashoffset .1s linear' }} />
            </svg>
            <div style={s('position:absolute; inset:0; display:flex; flex-direction:column; align-items:center; justify-content:center')}>
              <div style={s('font-size:29px; font-weight:800; letter-spacing:-.02em')}>{score}</div>
              <div style={s('font-size:10px; font-weight:700; color:#8894B2; letter-spacing:.1em')}>/ 100</div>
            </div>
          </div>
          <div>
            <div style={s('font-size:11.5px; font-weight:700; letter-spacing:.11em; color:#8894B2')}>SCORE ALTERNATIF</div>
            <div style={s('font-size:17px; font-weight:800; margin-top:7px')}>Profil solide</div>
            <div style={s('font-size:12.5px; color:#7C8AAB; line-height:1.55; margin-top:6px')}>Ligne pré-approuvée de <strong style={{ color: '#0A1F5C' }}>300 000 XOF</strong> à 8 %/mois.</div>
            <Link to="/app/credit">
              <Hoverable as="button"
                style={s('margin-top:13px; border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:12.5px; padding:10px 16px; border-radius:12px; transition:transform .18s ease, box-shadow .18s ease')}
                hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 10px 20px -8px rgba(245,179,1,.7)' }}
              >Voir mon crédit</Hoverable>
            </Link>
          </div>
        </section>

        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px; box-shadow:0 10px 30px -18px rgba(10,31,92,.28)'), animation: 'kUp .5s .14s ease both' }}>
          <div style={s('font-size:11.5px; font-weight:700; letter-spacing:.11em; color:#8894B2')}>ÉPARGNE BLOQUÉE</div>
          <div style={s('font-size:28px; font-weight:800; letter-spacing:-.02em; margin-top:12px')}>485 000 <span style={{ fontSize: 14, color: '#F5B301' }}>XOF</span></div>
          <div style={s('height:8px; border-radius:8px; background:#EDF1F8; margin-top:14px; overflow:hidden')}>
            <div style={s('width:72%; height:100%; border-radius:8px; background:linear-gradient(90deg,#0F2A6B,#F5B301)')} />
          </div>
          <div style={s('display:flex; justify-content:space-between; font-size:11.5px; font-weight:600; color:#7C8AAB; margin-top:9px')}>
            <span>3 coffres actifs</span><span>72 % des objectifs</span>
          </div>
          <div style={s('border-top:1px solid #EDF1F8; margin-top:15px; padding-top:13px; display:flex; justify-content:space-between; align-items:center')}>
            <div>
              <div style={s('font-size:11px; font-weight:700; color:#8894B2')}>DISPONIBLE</div>
              <div style={s('font-size:16px; font-weight:800; margin-top:3px')}>1 259 400 XOF</div>
            </div>
            <Link to="/app/vaults">
              <Hoverable as="button"
                style={s('border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:12px; padding:9px 13px; border-radius:12px; cursor:pointer')}
                hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
              >Coffres →</Hoverable>
            </Link>
          </div>
        </section>
      </div>

      <section style={{ ...s('border-radius:26px; padding:30px 34px; background:linear-gradient(110deg,#0A1F5C 0%,#0F2A6B 58%,#143487 100%); color:#fff; position:relative; overflow:hidden; display:flex; align-items:center; gap:28px'), animation: 'kUp .55s .18s ease both' }}>
        <div style={s('position:absolute; inset:0; background:repeating-linear-gradient(115deg,rgba(255,255,255,.05) 0 2px,transparent 2px 26px)')} />
        <div style={{ ...s('position:absolute; width:190px; height:190px; border-radius:50%; border:26px solid rgba(245,179,1,.14); right:180px; top:-70px'), animation: 'kFloat 7s ease-in-out infinite' }} />
        <div style={s('position:relative; flex:1')}>
          <div style={s('font-size:11px; font-weight:800; letter-spacing:.2em; color:#F5B301')}>ÉPARGNE PROGRAMMÉE</div>
          <div style={s('font-size:31px; font-weight:800; line-height:1.15; margin-top:11px; letter-spacing:-.02em')}>Épargnez chaque mois,<br /><span style={{ color: '#F5B301' }}>votre score grimpe tout seul.</span></div>
          <div style={s('font-size:13.5px; color:rgba(255,255,255,.65); margin-top:10px; max-width:520px; line-height:1.6')}>Un virement automatique récurrent prouve votre discipline à l'algorithme Dogaa — et débloque des paliers de prêt supérieurs.</div>
          <div style={s('display:flex; gap:10px; margin-top:20px')}>
            <Link to="/app/vaults">
              <Hoverable as="button"
                style={s("border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:13px; padding:12px 20px; border-radius:14px; transition:transform .18s ease")}
                hoverStyle={{ transform: 'translateY(-2px)' }}
              >Créer un coffre-fort</Hoverable>
            </Link>
            <Hoverable as="button"
              style={s('border:1px solid rgba(255,255,255,.28); cursor:pointer; background:transparent; color:#fff; font-family:Manrope,sans-serif; font-weight:700; font-size:13px; padding:12px 20px; border-radius:14px')}
              hoverStyle={{ background: 'rgba(255,255,255,.1)' }}
            >Comment ça marche</Hoverable>
          </div>
        </div>
        <div style={s("position:relative; width:196px; height:150px; flex:0 0 196px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.16); border-radius:20px; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:8px")}>
          <div style={s("font-size:10.5px; font-family:'JetBrains Mono',monospace; color:rgba(255,255,255,.5); letter-spacing:.08em")}>visuel campagne</div>
          <div style={s('width:130px; height:56px; border-radius:12px; background:repeating-linear-gradient(45deg,rgba(255,255,255,.14) 0 6px,transparent 6px 12px)')} />
        </div>
      </section>

      <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px')}>
        {actions.map((a) => (
          <Link key={a.label} to={`/app/${a.go}`} style={{ textDecoration: 'none' }}>
            <Hoverable as="button"
              style={s('min-width:0; width:100%; background:#fff; border:1px solid rgba(10,31,92,.07); border-radius:24px; padding:20px 18px; display:flex; align-items:center; gap:14px; cursor:pointer; font-family:Manrope,sans-serif; text-align:left; transition:transform .2s ease, box-shadow .2s ease; box-shadow:0 8px 24px -18px rgba(10,31,92,.4)')}
              hoverStyle={{ transform: 'translateY(-4px)', boxShadow: '0 18px 34px -20px rgba(10,31,92,.5)' }}
            >
              <span style={s('width:50px; height:50px; flex:0 0 50px; border-radius:17px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:20px; display:flex; align-items:center; justify-content:center')}>{a.icon}</span>
              <span style={{ minWidth: 0 }}>
                <span style={s('display:block; font-size:14px; font-weight:800; color:#0A1F5C')}>{a.label}</span>
                <span style={s('display:block; font-size:11.5px; font-weight:600; color:#8894B2; margin-top:3px')}>{a.sub}</span>
              </span>
            </Hoverable>
          </Link>
        ))}
      </div>

      <div style={s('display:grid; grid-template-columns:1.5fr 1fr; gap:18px')}>
        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .1s ease both' }}>
          <div style={s('display:flex; align-items:center; justify-content:space-between')}>
            <div style={s('font-size:15.5px; font-weight:800')}>Dernières transactions</div>
            <Link to="/app/wallet" style={{ textDecoration: 'none' }}>
              <Hoverable as="button"
                style={s('border:0; background:transparent; cursor:pointer; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; color:#0F2A6B')}
                hoverStyle={{ color: '#F5B301' }}
              >Tout voir →</Hoverable>
            </Link>
          </div>
          <div style={s('display:flex; flex-direction:column; margin-top:6px')}>
            {txns.map((t, i) => (
              <Hoverable key={i}
                style={s('display:flex; align-items:center; gap:14px; padding:13px 6px; border-bottom:1px solid #F1F4FA; border-radius:12px; transition:background .16s ease')}
                hoverStyle={{ background: '#F8FAFE' }}
              >
                <span style={s('width:38px; height:38px; flex:0 0 38px; border-radius:13px; background:#F2F5FC; color:#0F2A6B; display:flex; align-items:center; justify-content:center; font-size:15px')}>{t.icon}</span>
                <span style={{ flex: 1, minWidth: 0 }}>
                  <span style={s('display:block; font-size:13.5px; font-weight:700')}>{t.label}</span>
                  <span style={s('display:block; font-size:11.5px; color:#8894B2; font-weight:600; margin-top:2px')}>{t.meta}</span>
                </span>
                <span style={t.amtStyle}>{t.amount}</span>
              </Hoverable>
            ))}
          </div>
        </section>

        <section style={{ ...s('background:#fff; border-radius:26px; padding:22px 24px; box-shadow:0 10px 30px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .16s ease both' }}>
          <div style={s('font-size:15.5px; font-weight:800')}>Prochaines échéances</div>
          <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:16px')}>
            {upcoming.map((u, i) => (
              <div key={i} style={s('background:#F7F9FD; border:1px solid rgba(10,31,92,.06); border-radius:18px; padding:14px')}>
                <div style={s('display:flex; justify-content:space-between; align-items:center')}>
                  <div style={s('font-size:13px; font-weight:800')}>{u.label}</div>
                  <div style={s('font-size:13px; font-weight:800; color:#0F2A6B')}>{u.amount}</div>
                </div>
                <div style={s('font-size:11.5px; color:#8894B2; font-weight:600; margin-top:5px')}>{u.date}</div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div style={s('display:grid; grid-template-columns:1fr 1fr; gap:18px')}>
        <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(10,31,92,.35)'), animation: 'kUp .5s .18s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>En cas de retard de paiement</div>
          <div style={s('display:flex; flex-direction:column; gap:10px; margin-top:14px')}>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#B3262F; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>Délai de grâce de 3 jours, puis pénalité de 1 % par semaine de retard et baisse immédiate du score.</span></div>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#B3262F; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>Au-delà de 15 jours : blocage progressif des nouvelles demandes de crédit, puis des transferts sortants.</span></div>
            <div style={s('display:flex; gap:11px')}><span style={s('width:6px; height:6px; border-radius:50%; background:#5C6B8E; margin-top:7px; flex:0 0 6px')} /><span style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; line-height:1.6')}>Score recalculé chaque nuit ; après l'octroi d'un premier prêt, un recalcul intermédiaire a lieu à J+7.</span></div>
          </div>
          <Hoverable as="button"
            style={s('margin-top:14px; border:1px solid rgba(10,31,92,.12); background:#F7F9FD; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:12.5px; padding:10px 16px; border-radius:12px; cursor:pointer')}
            hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
          >Contester mon score</Hoverable>
        </section>
        <section style={{ ...s('background:#F7F9FD; border:1px dashed rgba(10,31,92,.15); border-radius:24px; padding:20px 22px'), animation: 'kUp .5s .22s ease both' }}>
          <div style={s('font-size:14.5px; font-weight:800')}>Conformité BCEAO</div>
          <div style={s('font-size:12.5px; color:#5C6B8E; font-weight:600; margin-top:8px; line-height:1.65')}>Tous les taux d'intérêt appliqués par Dogaa respectent le plafond d'usure fixé par la BCEAO pour la zone UEMOA. Le taux affiché avant validation d'un prêt est toujours le taux effectif global.</div>
        </section>
      </div>
    </div>
  );
}
