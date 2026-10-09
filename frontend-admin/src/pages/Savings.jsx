import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import { count, date, dateTime, label, money, userState } from '../lib/format';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import { Tile } from '../components/bento';
import { Card, ErrorNotice, PageHead, Pager, Person, SearchInput, SidePanel, Skeleton, Status, Table } from '../components/ui';

const DEFAULTS = { q: '', page: 0, size: 15 };
/** Les opérations qui touchent l'épargne : le serveur filtre un type à la fois, on trie donc ici. */
const SAVINGS_TYPES = ['VAULT_DEPOSIT', 'VAULT_WITHDRAWAL', 'SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL'];
const HISTORY_WINDOW = 100;
const RECENT_COUNT = 8;

/**
 * Épargne : qui épargne, et combien ?
 *
 * Quatre repères sur toute la base (le total, les épargnants, les comptes épargne et ce qui y est bloqué
 * en garantie, les coffres), puis la liste des épargnants, du plus gros au plus petit. Un épargnant est un
 * client qui détient quelque chose sur son compte épargne ou dans un coffre actif : ouvrir un compte à
 * l'inscription n'est pas épargner. Un clic ouvre son détail (`?client=…`) : coffres, compte, dernières
 * opérations. Le serveur ne trie pas autrement que par montant.
 */
export default function Savings() {
  const [f, update] = useFilters(DEFAULTS);
  const [params, setParams] = useSearchParams();
  const clientId = params.get('client');

  const list = useResource(
    (signal) => api.get('/console/savings', { params: { q: f.q, page: f.page, size: f.size }, signal }),
    JSON.stringify(f),
  );
  const summary = useResource((signal) => api.get('/console/savings/summary', { signal }), 'savings-summary');

  const open = (id) => setParams((previous) => {
    const next = new URLSearchParams(previous);
    if (id) next.set('client', id); else next.delete('client');
    return next;
  });

  const s = summary.data;
  const average = s && s.savers > 0 ? Number(s.total) / s.savers : 0;

  return (
    <>
      <PageHead title="Épargne" subtitle="Qui épargne, et combien." />

      {summary.error && <ErrorNotice message={summary.error} onRetry={summary.reload} />}
      {s && (
        <div className="bento compact" role="group" aria-label="Repères sur l'épargne">
          <Tile label="Total épargné" value={money(s.total)} sub="comptes épargne et coffres" />
          <Tile label="Épargnants" value={count(s.savers)} sub={`${money(Math.round(average))} en moyenne`} />
          <Tile label="Comptes épargne" value={money(s.savings)} sub={`dont ${money(s.collateral)} en garantie`} />
          <Tile label="Coffres" value={money(s.vaultsBalance)} sub={`${count(s.vaults)} coffre${s.vaults > 1 ? 's' : ''} ouvert${s.vaults > 1 ? 's' : ''}`} />
        </div>
      )}

      <ErrorNotice message={list.error} onRetry={list.reload} />
      <Card
        flush
        toolbar={<SearchInput value={f.q} onChange={(q) => update({ q })} placeholder="Nom ou téléphone" label="Rechercher un épargnant" />}
      >
        {!list.data && !list.error ? (
          <div className="card-body"><Skeleton rows={6} /></div>
        ) : (
          <Table
            rows={list.data?.items}
            rowKey={(r) => r.userId}
            onRowClick={(r) => open(r.userId)}
            empty={f.q
              ? 'Aucun épargnant ne correspond à cette recherche.'
              : 'Personne n\'épargne encore. Dès qu\'un client alimente son compte épargne ou ouvre un coffre, il apparaît ici.'}
            columns={[
              { key: 'who', header: 'Épargnant', render: (r) => <Person name={r.fullName} sub={r.phone} /> },
              { key: 'savings', header: 'Compte épargne', align: 'right', render: (r) => <span className="num">{money(r.savings)}</span> },
              { key: 'vaults', header: 'Coffres', align: 'right', render: (r) => <span className="num">{money(r.vaultsBalance)}</span> },
              { key: 'total', header: 'Total', align: 'right', render: (r) => <strong className="num">{money(r.total)}</strong> },
            ]}
          />
        )}
        <Pager page={list.data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
      </Card>

      {clientId && (
        <SidePanel title="Épargne du client" onClose={() => open(null)}>
          <ClientSavings id={clientId} />
        </SidePanel>
      )}
    </>
  );
}

/** L'épargne d'un client : le total, le compte épargne, les coffres, puis les dernières opérations. */
function ClientSavings({ id }) {
  const navigate = useNavigate();
  const detail = useResource((signal) => api.get(`/console/users/${id}`, { signal }), id);
  const history = useResource(
    (signal) => api.get('/console/transactions', { params: { userId: id, size: HISTORY_WINDOW }, signal }),
    `history|${id}`,
  );

  if (!detail.data) {
    return detail.error
      ? <ErrorNotice message={detail.error} onRetry={detail.reload} />
      : <Skeleton rows={4} />;
  }

  const { profile: p, wallets, vaults } = detail.data;
  const savings = wallets.filter((w) => w.type === 'SAVINGS');
  // Le coffre se loge dans le compte courant (somme verrouillée) ; le compte épargne est un compte à part : pas de double compte.
  const total = savings.reduce((sum, w) => sum + Number(w.available) + Number(w.locked), 0)
    + vaults.filter((v) => v.status === 'ACTIVE').reduce((sum, v) => sum + Number(v.balance), 0);
  const operations = (history.data?.items ?? []).filter((t) => SAVINGS_TYPES.includes(t.type)).slice(0, RECENT_COUNT);

  return (
    <div className="panel-sections">
      <div>
        <div className="dossier-head">
          <Person name={p.fullName} sub={p.phone} />
          <Link className="card-link" to={`/utilisateurs/${id}?onglet=epargne`}>Voir la fiche</Link>
        </div>
        <p className="panel-kicker">Total épargné</p>
        <p className="panel-amount num">{money(total)}</p>
        <p className="panel-status"><Status kind="userStatus" code={userState(p)} /></p>
      </div>

      <section>
        <h3>Compte épargne</h3>
        <Table
          rows={savings}
          rowKey={(w) => `${w.type}-${w.currency}`}
          empty="Ce client n'a pas de compte épargne."
          columns={[
            { key: 'available', header: 'Disponible', align: 'right', lead: false, render: (w) => <span className="num">{money(w.available, w.currency)}</span> },
            { key: 'locked', header: 'En garantie', align: 'right', render: (w) => <span className="num">{money(w.locked, w.currency)}</span> },
          ]}
        />
      </section>

      <section>
        <h3>Coffres</h3>
        <Table
          rows={vaults}
          rowKey={(v) => v.id}
          empty="Ce client n'a ouvert aucun coffre."
          columns={[
            { key: 'name', header: 'Coffre', render: (v) => v.name },
            { key: 'balance', header: 'Solde', align: 'right', render: (v) => <span className="num">{money(v.balance, v.currency)}</span> },
            {
              key: 'target',
              header: 'Objectif',
              align: 'right',
              render: (v) => (Number(v.targetAmount) > 0
                ? <span className="num">{money(v.targetAmount, v.currency)}<span className="cell-sub">{Math.round((Number(v.balance) / Number(v.targetAmount)) * 100)} % atteint{v.targetDate ? ` · avant le ${date(v.targetDate)}` : ''}</span></span>
                : '—'),
            },
          ]}
        />
      </section>

      <section>
        <h3>Dernières opérations d&apos;épargne</h3>
        {history.error && <ErrorNotice message={history.error} onRetry={history.reload} />}
        {!history.data && !history.error ? (
          <Skeleton rows={3} />
        ) : (
          <Table
            rows={operations}
            rowKey={(t) => t.reference}
            onRowClick={(t) => navigate(`/transactions?ref=${encodeURIComponent(t.reference)}`)}
            empty="Aucune opération d'épargne parmi ses 100 dernières opérations."
            columns={[
              { key: 'type', header: 'Opération', render: (t) => label('txType', t.type) },
              { key: 'amount', header: 'Montant', align: 'right', render: (t) => <span className="num">{money(t.amount, t.currency)}</span> },
              { key: 'date', header: 'Date', render: (t) => <span className="num">{dateTime(t.createdAt)}</span> },
            ]}
          />
        )}
        <p className="panel-links">
          <Link className="card-link" to={`/utilisateurs/${id}?onglet=transactions`}>Toutes ses transactions</Link>
        </p>
      </section>
    </div>
  );
}
