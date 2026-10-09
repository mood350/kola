import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { count, date, money, options } from '../lib/format';
import { describePeriod } from '../lib/period';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import {
  Card, DateRange, ErrorNotice, FilterField, FiltersPanel, FiltersToggle, PageHead, Pager, PeriodSelect, Person, Status, Table,
} from '../components/ui';

const DEFAULTS = { statut: '', du: '', au: '', page: 0, size: 15 };

/**
 * Prêts, par jour de versement : « combien aujourd'hui, hier, sur telle
 * période » se lit sur la ligne de totaux sous le titre. État et dates précises
 * sont derrière « Filtres ». Lecture seule — tout le cycle du prêt est automatique.
 */
export default function Loans() {
  const [f, update, reset, dirty] = useFilters(DEFAULTS);
  const [open, setOpen] = useState(false);
  const filters = { status: f.statut, from: f.du, to: f.au };
  const key = JSON.stringify(filters);

  const list = useResource(
    (signal) => api.get('/console/loans', { params: { ...filters, page: f.page, size: f.size }, signal }),
    `${key}|${f.page}|${f.size}`,
  );
  const summary = useResource((signal) => api.get('/console/loans/summary', { params: filters, signal }), key);
  const s = summary.data;

  return (
    <>
      <PageHead
        title="Prêts"
        subtitle={s
          ? <><span className="num">{count(s.count)}</span> prêt{s.count > 1 ? 's' : ''} accordé{s.count > 1 ? 's' : ''} · <span className="num">{money(s.principal)}</span> prêtés · <span className="num">{money(s.outstanding)}</span> à rembourser{s.overdue > 0 && <> · <span className="badge warn">{count(s.overdue)} en retard</span></>} — {describePeriod(f.du, f.au)}</>
          : describePeriod(f.du, f.au)}
      />
      <ErrorNotice message={list.error} onRetry={list.reload} />

      <Card
        flush
        toolbar={(
          <>
            <PeriodSelect from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })}
              onCustom={() => setOpen(true)} label="Période de versement" />
            <FiltersToggle open={open} count={f.statut ? 1 : 0} onToggle={() => setOpen((v) => !v)} />
          </>
        )}
      >
        {open && (
          <FiltersPanel canReset={dirty} onReset={() => { reset(); setOpen(false); }}>
            <FilterField label="État">
              <select className="select" value={f.statut} onChange={(e) => update({ statut: e.target.value })}>
                <option value="">Tous</option>
                {options('loanStatus').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
              </select>
            </FilterField>
            <DateRange from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} label="Versés entre" />
          </FiltersPanel>
        )}
        <Table
          rows={list.data?.items}
          rowKey={(l) => l.id}
          empty={list.loading ? 'Chargement…' : 'Aucun prêt sur cette période.'}
          columns={[
            { key: 'who', header: 'Emprunteur', render: (l) => <Link to={`/utilisateurs/${l.userId}`} className="plain"><Person name={l.userName} /></Link> },
            { key: 'principal', header: 'Montant prêté', align: 'right', render: (l) => <span className="num">{money(l.principal, l.currency)}</span> },
            {
              key: 'owed',
              header: 'Reste dû',
              align: 'right',
              render: (l) => (
                <span className="num">
                  {money(l.outstanding, l.currency)}
                  {Number(l.penalty) > 0 && <span className="cell-sub">dont pénalité {money(l.penalty, l.currency)}</span>}
                </span>
              ),
            },
            { key: 'status', header: 'État', render: (l) => <Status kind="loanStatus" code={l.status} /> },
            { key: 'disbursed', header: 'Versé le', render: (l) => date(l.disbursedAt) },
            { key: 'due', header: 'Échéance', render: (l) => date(l.dueAt) },
          ]}
        />
        <Pager page={list.data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
      </Card>
    </>
  );
}
