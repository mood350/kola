import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import { count, date, loanState, money, relativeDay } from '../lib/format';
import { activePreset, describePeriod } from '../lib/period';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import {
  Card, DateRange, ErrorNotice, FiltersPanel, PageHead, Pager, PeriodSelect, Person, SidePanel, Skeleton, Status, Table, Tabs,
} from '../components/ui';

/** Les segments : un statut du serveur chacun, sauf « en cours », qui réunit les deux états d'un prêt qui court. */
const SEGMENTS = {
  tous: { label: 'Tous', statuses: [null], empty: 'Aucun prêt sur cette période.' },
  encours: { label: 'En cours', statuses: ['ACTIVE', 'OVERDUE'], empty: 'Aucun prêt en cours.' },
  rembourses: { label: 'Remboursés', statuses: ['REPAID'], empty: 'Aucun prêt remboursé sur cette période.' },
  defaut: { label: 'En défaut', statuses: ['DEFAULTED'], empty: 'Aucun prêt en défaut. C\'est une bonne nouvelle.' },
};
/** D'anciens liens portaient un statut du serveur : ils mènent au bon segment. */
const LEGACY = { ACTIVE: 'encours', OVERDUE: 'encours', REPAID: 'rembourses', DEFAULTED: 'defaut' };
/** « En cours » réunit deux requêtes : au-delà, on ne montre que les plus récentes et on le dit. */
const MERGE_LIMIT = 100;

const DEFAULTS = { statut: '', du: '', au: '', page: 0, size: 15 };

/**
 * Crédits : quels prêts dois-je suivre ?
 *
 * Quatre segments et une période de versement. Un prêt dont l'échéance est passée se dit « en retard »,
 * quel que soit le statut stocké : le serveur ne le pose que lorsqu'on touche au prêt, il sous-estime. Le
 * segment « en cours » trie par échéance, la plus proche d'abord. Un clic ouvre le détail (`?pret=…`) ;
 * il n'y a pas de route de détail ni d'échéancier — le prêt n'a qu'une échéance — donc le panneau montre
 * ce que la ligne porte. Lecture seule : tout le cycle du prêt est automatique.
 */
export default function Loans() {
  const [f, update, reset, dirty] = useFilters(DEFAULTS);
  const [params, setParams] = useSearchParams();
  const [open, setOpen] = useState(false);

  const segment = SEGMENTS[f.statut] ? f.statut : (LEGACY[f.statut] ?? 'tous');
  const config = SEGMENTS[segment];
  const merged = config.statuses.length > 1;
  const custom = Boolean(f.du || f.au) && activePreset(f.du, f.au) === null;
  const selectedId = params.get('pret');

  const period = { from: f.du, to: f.au };
  const key = `${segment}|${f.du}|${f.au}`;

  const list = useResource(async (signal) => {
    if (merged) {
      const pages = await Promise.all(config.statuses.map((status) => api.get('/console/loans', {
        params: { status, ...period, page: 0, size: MERGE_LIMIT }, signal,
      })));
      const items = pages.flatMap((p) => p.items).sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt));
      return { items, truncated: pages.some((p) => p.total > p.items.length) };
    }
    return api.get('/console/loans', { params: { status: config.statuses[0], ...period, page: f.page, size: f.size }, signal });
  }, `${key}|${f.page}|${f.size}`);

  const summary = useResource(async (signal) => {
    const parts = await Promise.all(config.statuses.map((status) => api.get('/console/loans/summary', { params: { status, ...period }, signal })));
    return parts.reduce((sum, p) => ({
      count: sum.count + p.count,
      principal: sum.principal + Number(p.principal),
      outstanding: sum.outstanding + Number(p.outstanding),
    }), { count: 0, principal: 0, outstanding: 0 });
  }, key);

  const items = list.data?.items ?? [];
  const selected = items.find((l) => l.id === selectedId) ?? null;
  const select = (id) => setParams((previous) => {
    const next = new URLSearchParams(previous);
    if (id) next.set('pret', id); else next.delete('pret');
    return next;
  });

  const s = summary.data;
  const tabs = Object.entries(SEGMENTS).map(([k, v]) => ({ key: k, label: v.label }));

  return (
    <>
      <PageHead
        title="Crédits"
        subtitle={s
          ? <><span className="num">{count(s.count)}</span> prêt{s.count > 1 ? 's' : ''} · <span className="num">{money(s.principal)}</span> prêtés{segment !== 'rembourses' && <> · <span className="num">{money(s.outstanding)}</span> à rembourser</>} — {describePeriod(f.du, f.au)}</>
          : describePeriod(f.du, f.au)}
      />

      <Tabs tabs={tabs} value={segment} label="Prêts par état" onChange={(next) => update({ statut: next === 'tous' ? '' : next })}>
        <Card
          flush
          toolbar={(
            <PeriodSelect from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })}
              onCustom={() => setOpen(true)} label="Période de versement" />
          )}
        >
          {(open || custom) && (
            <FiltersPanel canReset={dirty} onReset={() => { reset(); setOpen(false); }}>
              <DateRange from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} label="Versés entre" />
            </FiltersPanel>
          )}
          {list.error && <div className="card-body"><ErrorNotice message={list.error} onRetry={list.reload} /></div>}
          {!list.data && !list.error ? (
            <div className="card-body"><Skeleton rows={6} /></div>
          ) : (
            <Table
              rows={items}
              rowKey={(l) => l.id}
              onRowClick={(l) => select(l.id)}
              empty={config.empty}
              columns={[
                { key: 'who', header: 'Emprunteur', render: (l) => <Person name={l.userName} /> },
                { key: 'principal', header: 'Montant prêté', align: 'right', render: (l) => <span className="num">{money(l.principal, l.currency)}</span> },
                { key: 'owed', header: 'Reste dû', align: 'right', render: (l) => <span className="num">{money(l.outstanding, l.currency)}</span> },
                { key: 'due', header: 'Échéance', render: (l) => <span className="num">{date(l.dueAt)}</span> },
                { key: 'status', header: 'Statut', render: (l) => <Status kind="loanStatus" code={loanState(l)} /> },
              ]}
            />
          )}
          {merged ? (
            list.data?.truncated && <div className="card-body"><p className="muted">Seuls les {MERGE_LIMIT} prêts les plus récents de chaque état sont listés : affinez par période de versement.</p></div>
          ) : (
            <Pager page={list.data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
          )}
        </Card>
      </Tabs>

      {selectedId && (
        <LoanPanel loan={selected} onClose={() => select(null)} />
      )}
    </>
  );
}

/** Le détail d'un prêt : ce que la ligne porte, plus l'échéance dite en jours. */
function LoanPanel({ loan, onClose }) {
  const [now] = useState(() => Date.now());
  if (!loan) {
    return (
      <SidePanel title="Détail du prêt" onClose={onClose}>
        <p className="muted">Ce prêt n&apos;est pas dans la liste affichée : changez de segment ou de période.</p>
      </SidePanel>
    );
  }

  const state = loanState(loan, now);
  const running = loan.status === 'ACTIVE' || loan.status === 'OVERDUE';
  const headline = running ? { kicker: 'Reste dû', value: loan.outstanding } : { kicker: 'Montant prêté', value: loan.principal };

  return (
    <SidePanel title="Détail du prêt" onClose={onClose}>
      <p className="panel-kicker">{headline.kicker}</p>
      <p className="panel-amount num">{money(headline.value, loan.currency)}</p>
      <p className="panel-status">
        <Status kind="loanStatus" code={state} />
        {running && <span className="row-sub"> · échéance {relativeDay(loan.dueAt, now)}</span>}
      </p>

      <dl className="fields panel-fields">
        <div className="panel-wide">
          <dt>Emprunteur</dt>
          <dd><Link to={`/utilisateurs/${loan.userId}`}>{loan.userName || 'Client'}</Link></dd>
        </div>
        <div><dt>Montant prêté</dt><dd className="num">{money(loan.principal, loan.currency)}</dd></div>
        <div><dt>Intérêts</dt><dd className="num">{money(loan.interest, loan.currency)}</dd></div>
        <div><dt>Déjà remboursé</dt><dd className="num">{money(loan.repaid, loan.currency)}</dd></div>
        <div><dt>Pénalité</dt><dd className="num">{money(loan.penalty, loan.currency)}</dd></div>
        <div><dt>Versé le</dt><dd className="num">{date(loan.disbursedAt)}</dd></div>
        <div><dt>Échéance</dt><dd className="num">{date(loan.dueAt)}</dd></div>
        {loan.settledAt && <div><dt>Soldé le</dt><dd className="num">{date(loan.settledAt)}</dd></div>}
        {loan.status === 'DEFAULTED' && <div className="panel-wide"><dt>Garantie</dt><dd>Saisie pour couvrir le prêt.</dd></div>}
      </dl>

      <p className="panel-links">
        <Link className="card-link" to={`/utilisateurs/${loan.userId}?onglet=credits`}>Voir ses crédits</Link>
        <Link className="card-link" to={`/utilisateurs/${loan.userId}?onglet=transactions`}>Voir ses transactions</Link>
      </p>
    </SidePanel>
  );
}
