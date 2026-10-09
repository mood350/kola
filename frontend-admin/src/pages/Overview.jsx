import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { count, money } from '../lib/format';
import { useResource } from '../lib/useResource';
import { useSession } from '../lib/session';
import { Delta, ErrorNotice, PageHead } from '../components/ui';

const dayFormat = new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' });

/**
 * Tableau de bord en bento :
 *  - le volume du jour, comparé à hier, avec les 14 derniers jours (la grande case) ;
 *  - nouveaux clients, prêts accordés, frais perçus et remboursements reçus du
 *    jour, chacun comparé à hier ;
 *  - en bandeau, ce qui attend une action.
 * Chaque case mène à la liste filtrée correspondante.
 */
export default function Overview() {
  const { can } = useSession();
  const { data, error, reload } = useResource((signal) => api.get('/console/overview', { signal }), 'overview');

  if (!data) {
    return (
      <>
        <PageHead title="Tableau de bord" />
        <ErrorNotice message={error} onRetry={reload} />
      </>
    );
  }

  const { today, yesterday } = data;
  const day = isoToday();
  const dayQuery = `?du=${day}&au=${day}`;

  return (
    <>
      <PageHead title="Tableau de bord" subtitle="Aujourd'hui, comparé à hier (heure de Lomé)." />
      <ErrorNotice message={error} onRetry={reload} />

      <div className="bento">
        <Tile className="big" to={can('users') ? `/transactions${dayQuery}` : null} label="Volume du jour">
          <div className="tile-value num">{money(today.volume)}</div>
          <div className="tile-sub">
            <Delta current={today.volume} previous={yesterday.volume} />
            <span className="num">· {count(today.transactions)} transaction{today.transactions > 1 ? 's' : ''}</span>
          </div>
          <Chart days={data.volumeByDay || []} />
        </Tile>

        <Tile to={can('users') ? `/utilisateurs${dayQuery}` : null} label="Nouveaux clients">
          <div className="tile-value num">{count(today.newUsers)}</div>
          <div className="tile-sub"><Delta current={today.newUsers} previous={yesterday.newUsers} /></div>
        </Tile>

        <Tile to={can('credit') ? `/prets${dayQuery}` : null} label="Prêts accordés">
          <div className="tile-value num">{count(today.loans)}</div>
          <div className="tile-sub">
            <Delta current={today.loans} previous={yesterday.loans} />
            <span className="num">· {money(today.loanAmount)}</span>
          </div>
        </Tile>

        <Tile to={can('users') ? `/transactions${dayQuery}` : null} label="Frais perçus">
          <div className="tile-value num">{money(today.fees)}</div>
          <div className="tile-sub"><Delta current={today.fees} previous={yesterday.fees} /></div>
        </Tile>

        <Tile to={can('users') ? `/transactions${dayQuery}&type=LOAN_REPAYMENT` : null} label="Remboursements reçus">
          <div className="tile-value num">{money(today.repayments)}</div>
          <div className="tile-sub"><Delta current={today.repayments} previous={yesterday.repayments} /></div>
        </Tile>

        <div className="tile full">
          <div className="tile-label">À traiter</div>
          <ul className="todo-list inline">
            <Todo to={can('users') ? '/kyc' : null} label="Pièces KYC à vérifier" value={data.kycPending} />
            <Todo to={can('credit') ? '/prets?statut=OVERDUE' : null} label="Prêts en retard" value={data.overdueLoans} />
            <Todo to={can('credit') ? '/prets?statut=ACTIVE' : null} label="Encours à rembourser" value={money(data.outstanding)} neutral />
          </ul>
        </div>
      </div>
    </>
  );
}

function isoToday() {
  return new Date().toISOString().slice(0, 10);
}

function Tile({ label, to, className = '', children }) {
  const body = (
    <>
      <div className="tile-label">{label}</div>
      {children}
    </>
  );
  const cls = `tile ${className}`.trim();
  return to ? <Link className={cls} to={to}>{body}</Link> : <div className={cls}>{body}</div>;
}

function Todo({ label, value, to, neutral }) {
  const warn = !neutral && Number(value) > 0;
  const body = (
    <>
      <span>{label}</span>
      <span className={`badge ${warn ? 'warn' : ''} num`}>{typeof value === 'number' ? count(value) : value}</span>
    </>
  );
  return <li>{to ? <Link to={to}>{body}</Link> : <span className="todo-row">{body}</span>}</li>;
}

/** Barres du volume journalier ; aujourd'hui en bleu vif. Le détail est dans l'infobulle. */
function Chart({ days }) {
  const tallest = Math.max(...days.map((d) => Number(d.amount)), 0);
  return (
    <div className="tile-chart">
      <div className="chart" role="img" aria-label="Volume des transactions réussies, 14 derniers jours">
        {days.map((d, i) => (
          <div key={d.day} className={`chart-col${i === days.length - 1 ? ' today' : ''}`}
            title={`${dayFormat.format(new Date(d.day))} · ${money(d.amount)}`}>
            <span style={{ height: `${tallest > 0 ? (Number(d.amount) / tallest) * 100 : 0}%` }} />
          </div>
        ))}
      </div>
      <div className="chart-axis" aria-hidden="true">
        <span>{days.length > 0 ? dayFormat.format(new Date(days[0].day)) : ''}</span>
        <span>Aujourd&apos;hui</span>
      </div>
    </div>
  );
}
