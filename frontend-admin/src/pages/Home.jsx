import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { addDays, displayIso, todayIso } from '../lib/calendar';
import { count, date, dateTime, label, money, partiesText, since } from '../lib/format';
import { useResource } from '../lib/useResource';
import { useSession } from '../lib/session';
import { Tile } from '../components/bento';
import { ErrorNotice, Empty, PageHead, Skeleton, Status } from '../components/ui';
import { IconChevron } from '../components/icons';

const KYC_PREVIEW = 5;
const RECENT_COUNT = 8;
const weekday = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', timeZone: 'Africa/Lome' });

/**
 * Accueil : quelle est la situation aujourd'hui ?
 *
 * En haut, quelques repères sur l'activité (le bento) ; puis trois chiffres du jour, ce qui attend
 * une décision (les dossiers KYC) et les dernières opérations.
 * « Du jour » veut dire depuis minuit, heure de Lomé — le serveur découpe ses jours en UTC, qui est
 * l'heure de Lomé toute l'année. Un rôle ne voit que les blocs de ses modules.
 */
export default function Home() {
  const { can } = useSession();
  const overview = useResource((signal) => api.get('/console/overview', { signal }), 'home-overview');
  const queue = useResource(
    (signal) => (can('users') ? api.get('/console/kyc', { signal }) : Promise.resolve(null)),
    `home-kyc|${can('users')}`,
  );
  const recent = useResource(
    (signal) => (can('users')
      ? api.get('/console/transactions', { params: { size: RECENT_COUNT }, signal })
      : Promise.resolve(null)),
    `home-recent|${can('users')}`,
  );

  const now = new Date();
  const subtitle = `${weekday.format(now)} ${date(now)} · opérations réussies depuis minuit, heure de Lomé.`;
  const { data, error, reload } = overview;

  return (
    <>
      <PageHead title="Accueil" subtitle={subtitle} />
      {!data && (error ? <ErrorNotice message={error} onRetry={reload} /> : <Skeleton rows={3} />)}
      {data && (
        <>
          <Bento data={data} can={can} />
          <Indicators data={data} can={can} />
        </>
      )}
      {can('users') && (
        <>
          <KycQueue queue={queue} />
          <RecentTransactions recent={recent} />
        </>
      )}
    </>
  );
}

/**
 * Les repères de fond : le volume des 30 jours (la grande tuile, avec ses 14 derniers jours), la base
 * de clients, ce que les prêts ont encore à rembourser, et les frais et remboursements du jour.
 * Chiffres bruts du serveur, sans calcul : aucune tuile n'invente une valeur.
 */
function Bento({ data, can }) {
  const today = todayIso();
  const since30 = addDays(today, -29);
  const days = data.volumeByDay || [];
  const tallest = Math.max(...days.map((d) => Number(d.amount)), 0);

  return (
    <div className="bento" role="group" aria-label="Repères sur l'activité">
      <Tile big label="Volume des 30 derniers jours" value={money(data.volume30d)}
        sub={`${count(data.transactions30d)} transaction${data.transactions30d > 1 ? 's' : ''} réussie${data.transactions30d > 1 ? 's' : ''}`}
        to={can('users') ? `/transactions?du=${since30}&au=${today}` : null}>
        <div className="mini-bars" role="img"
          aria-label={`Volume par jour sur les ${days.length} derniers jours, du ${displayIso(days[0]?.day?.slice(0, 10))} à aujourd'hui`}>
          {days.map((d, i) => (
            <span key={d.day} className={i === days.length - 1 ? 'today' : undefined}
              title={`${displayIso(d.day.slice(0, 10))} · ${money(d.amount)}`}
              style={{ height: `${tallest > 0 ? Math.max((Number(d.amount) / tallest) * 100, 2) : 2}%` }} />
          ))}
        </div>
        <div className="mini-axis" aria-hidden="true">
          <span>{days.length > 0 ? displayIso(days[0].day.slice(0, 10)).slice(0, 5) : ''}</span>
          <span>Aujourd&apos;hui</span>
        </div>
      </Tile>
      <Tile label="Clients" value={count(data.users)} sub={`+${count(data.newUsers30d)} sur 30 jours`}
        to={can('users') ? '/utilisateurs' : null} />
      <Tile label="Encours de prêts" value={money(data.outstanding)}
        sub={`${count(data.activeLoans)} prêt${data.activeLoans > 1 ? 's' : ''} en cours`}
        to={can('credit') ? '/credits?statut=encours' : null} />
      <Tile label="Frais perçus aujourd'hui" value={money(data.today.fees)} sub={`hier : ${money(data.yesterday.fees)}`} />
      <Tile label="Remboursements reçus aujourd'hui" value={money(data.today.repayments)}
        sub={`hier : ${money(data.yesterday.repayments)}`}
        to={can('users') ? `/transactions?du=${todayIso()}&au=${todayIso()}&type=LOAN_REPAYMENT` : null} />
    </div>
  );
}

function Indicators({ data, can }) {
  // Le serveur découpe ses jours en UTC, qui est l'heure de Lomé : le jour courant sert de filtre.
  const day = todayIso();
  const dayQuery = `?du=${day}&au=${day}`;
  const { today, yesterday } = data;

  return (
    <div className="indicators" role="group" aria-label="Chiffres du jour">
      <Indicator label="Transactions du jour" value={today.transactions} yesterday={yesterday.transactions}
        to={can('users') ? `/transactions${dayQuery}` : null} />
      <Indicator label="Nouveaux utilisateurs" value={today.newUsers} yesterday={yesterday.newUsers}
        to={can('users') ? `/utilisateurs${dayQuery}` : null} />
      <Indicator label="Prêts accordés" value={today.loans} yesterday={yesterday.loans}
        to={can('credit') ? `/credits${dayQuery}` : null} />
    </div>
  );
}

function Indicator({ label: text, value, yesterday, to }) {
  const body = (
    <>
      <span className="ind-label">{text}</span>
      <span className="ind-value num">{count(value)}</span>
      <span className="ind-prev num">hier : {count(yesterday)}</span>
    </>
  );
  return to ? <Link className="ind link" to={to}>{body}</Link> : <div className="ind">{body}</div>;
}

function KycQueue({ queue }) {
  const { data, error, reload } = queue;
  const total = data?.length ?? 0;

  return (
    <section className="home-section" aria-labelledby="home-kyc">
      <div className="section-head">
        <h2 id="home-kyc">Dossiers KYC à traiter{total > 0 && <span className="section-count num">{count(total)}</span>}</h2>
        {total > 0 && <Link className="btn" to="/kyc">Traiter les dossiers</Link>}
      </div>

      {!data && (error ? <ErrorNotice message={error} onRetry={reload} /> : <Skeleton />)}
      {data && total === 0 && (
        <Empty>Aucun dossier en attente. Les prochaines pièces envoyées par les clients apparaîtront ici.</Empty>
      )}
      {data && total > 0 && (
        <>
          <ul className="rows">
            {data.slice(0, KYC_PREVIEW).map((doc) => (
              <li key={doc.id}>
                <Link to={`/kyc?dossier=${doc.id}`}>
                  <span className="row-main">
                    <strong>{doc.userName || 'Client sans nom'}</strong>
                    <span className="row-sub">{label('docType', doc.type)} · {label('kycTier', doc.currentTier)} actuellement</span>
                  </span>
                  <span className="row-side">
                    <span className="row-sub">{since(doc.submittedAt)}</span>
                    <span className="row-sub num">{date(doc.submittedAt)}</span>
                  </span>
                  <IconChevron className="row-go" />
                </Link>
              </li>
            ))}
          </ul>
          {total > KYC_PREVIEW && <p className="section-more"><Link to="/kyc">Voir les {count(total - KYC_PREVIEW)} autres dossiers</Link></p>}
        </>
      )}
    </section>
  );
}

function RecentTransactions({ recent }) {
  const { data, error, reload } = recent;
  const items = data?.items ?? [];

  return (
    <section className="home-section" aria-labelledby="home-recent">
      <div className="section-head">
        <h2 id="home-recent">Dernières transactions</h2>
        <Link className="section-link" to="/transactions">Tout voir</Link>
      </div>

      {!data && (error ? <ErrorNotice message={error} onRetry={reload} /> : <Skeleton rows={5} />)}
      {data && items.length === 0 && (
        <Empty>Aucune opération pour l&apos;instant. Dès qu&apos;un client enverra de l&apos;argent, elle apparaîtra ici.</Empty>
      )}
      {data && items.length > 0 && (
        <ul className="rows">
          {items.map((t) => (
            <li key={t.reference}>
              <Link to={`/transactions?ref=${encodeURIComponent(t.reference)}`}>
                <span className="row-main">
                  <strong>{label('txType', t.type)}</strong>
                  <span className="row-sub">{partiesText(t)}</span>
                  <Status kind="txStatus" code={t.status} />
                </span>
                <span className="row-side">
                  <span className="row-amount num">{money(t.amount, t.currency)}</span>
                  <span className="row-sub num">{dateTime(t.createdAt)}</span>
                </span>
                <IconChevron className="row-go" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
