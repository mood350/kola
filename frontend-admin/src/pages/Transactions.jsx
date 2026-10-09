import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, download } from '../lib/api';
import { count, dateTime, label, money, options } from '../lib/format';
import { describePeriod } from '../lib/period';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import {
  Card, DateRange, ErrorNotice, FilterField, FiltersPanel, FiltersToggle, PageHead, Pager, PeriodSelect, SearchInput, Status, Table,
} from '../components/ui';
import { IconDownload } from '../components/icons';

/**
 * Colonnes d'une ligne de transaction, partagées avec la fiche client.
 * `perspective` : l'utilisateur du point de vue duquel le sens (+ / −) se lit.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function transactionColumns(perspective) {
  return [
    { key: 'date', header: 'Date', render: (t) => dateTime(t.createdAt) },
    {
      key: 'type',
      header: 'Opération',
      render: (t) => <>{label('txType', t.type)}<span className="cell-sub mono">{t.reference}</span></>,
    },
    { key: 'parties', header: 'De → vers', wrap: true, render: (t) => parties(t) },
    {
      key: 'amount',
      header: 'Montant',
      align: 'right',
      render: (t) => {
        const incoming = perspective && t.recipientId === perspective && t.senderId !== perspective;
        return (
          <span className={`num ${incoming ? 'amount-in' : ''}`}>
            {incoming ? '+ ' : ''}{money(t.amount, t.currency)}
            {Number(t.fee) > 0 && <span className="cell-sub">frais {money(t.fee, t.currency)}</span>}
          </span>
        );
      },
    },
    {
      key: 'status',
      header: 'État',
      render: (t) => <>{<Status kind="txStatus" code={t.status} />}{t.failureReason && <span className="cell-sub">{t.failureReason}</span>}</>,
    },
  ];
}

/**
 * Qui paie, qui reçoit : un compte Kola quand il y en a un, sinon la contrepartie
 * externe. Un mouvement entre deux comptes du même client (épargne, coffre,
 * versement de prêt) se dit tel quel — « X → X » n'apprend rien.
 */
function parties(t) {
  const who = (id, name) => (id ? <Link to={`/utilisateurs/${id}`}>{name || 'Compte Kola'}</Link> : null);
  if (t.senderId && t.senderId === t.recipientId) {
    return <>{who(t.senderId, t.senderName)} <span className="muted">· mouvement interne</span></>;
  }
  // Le backend note « EXTERNAL » l'autre bout d'un dépôt ou d'un retrait Mobile Money.
  const raw = t.counterpartyName || t.counterparty;
  const outside = raw === 'EXTERNAL' ? 'Extérieur' : raw;
  const from = who(t.senderId, t.senderName) || (t.recipientId ? outside : null) || '—';
  const to = who(t.recipientId, t.recipientName) || (t.senderId ? outside : null) || '—';
  return <>{from} → {to}</>;
}

const DEFAULTS = { q: '', type: '', statut: '', du: '', au: '', page: 0, size: 15 };

/**
 * Transactions. La barre ne garde que l'essentiel — recherche, période,
 * export ; type, état et dates précises sont derrière « Filtres ». Les totaux
 * de la période tiennent sur une ligne ; la ventilation par type est dans
 * l'export Excel (feuille « Synthèse »), pas à l'écran.
 */
export default function Transactions() {
  const [f, update, reset, dirty] = useFilters(DEFAULTS);
  const [open, setOpen] = useState(false);
  const [exporting, setExporting] = useState('');
  const [exportError, setExportError] = useState('');

  const filters = { q: f.q, type: f.type, status: f.statut, from: f.du, to: f.au };
  const key = JSON.stringify(filters);

  const list = useResource(
    (signal) => api.get('/console/transactions', { params: { ...filters, page: f.page, size: f.size }, signal }),
    `${key}|${f.page}|${f.size}`,
  );
  const summary = useResource((signal) => api.get('/console/transactions/summary', { params: filters, signal }), key);

  const exportAs = async (format) => {
    setExporting(format);
    setExportError('');
    const query = new URLSearchParams(Object.entries({ ...filters, format }).filter(([, v]) => v));
    const name = `transactions${f.du ? `_${f.du}` : ''}${f.au ? `_${f.au}` : ''}.${format}`;
    try {
      await download(`/console/transactions/export?${query}`, name);
    } catch (e) {
      setExportError(e.message);
    } finally {
      setExporting('');
    }
  };

  const s = summary.data;
  const hidden = [f.type, f.statut].filter(Boolean).length;

  return (
    <>
      <PageHead
        title="Transactions"
        subtitle={s
          ? <><span className="num">{count(s.count)}</span> transaction{s.count > 1 ? 's' : ''} · <span className="num">{money(s.volume)}</span> · frais perçus <span className="num">{money(s.fees)}</span> — {describePeriod(f.du, f.au)}</>
          : describePeriod(f.du, f.au)}
      />
      <ErrorNotice message={exportError} />
      <ErrorNotice message={list.error} onRetry={list.reload} />

      <Card
        flush
        toolbar={(
          <>
            <SearchInput value={f.q} onChange={(q) => update({ q })}
              placeholder="Référence (TXN-…) ou téléphone" label="Rechercher une transaction" />
            <PeriodSelect from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} onCustom={() => setOpen(true)} />
            <FiltersToggle open={open} count={hidden} onToggle={() => setOpen((v) => !v)} />
            <span className="spacer" />
            <button type="button" className="btn secondary" disabled={Boolean(exporting)} onClick={() => exportAs('csv')}>
              <IconDownload size={16} />{exporting === 'csv' ? '…' : 'CSV'}
            </button>
            <button type="button" className="btn" disabled={Boolean(exporting)} onClick={() => exportAs('xlsx')}>
              <IconDownload size={16} />{exporting === 'xlsx' ? '…' : 'Excel'}
            </button>
          </>
        )}
      >
        {open && (
          <FiltersPanel canReset={dirty} onReset={() => { reset(); setOpen(false); }}>
            <FilterField label="Type">
              <select className="select" value={f.type} onChange={(e) => update({ type: e.target.value })}>
                <option value="">Tous</option>
                {options('txType').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
              </select>
            </FilterField>
            <FilterField label="État">
              <select className="select" value={f.statut} onChange={(e) => update({ statut: e.target.value })}>
                <option value="">Tous</option>
                {options('txStatus').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
              </select>
            </FilterField>
            <DateRange from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} label="Dates précises" />
          </FiltersPanel>
        )}

        <Table rows={list.data?.items} rowKey={(t) => t.reference} columns={transactionColumns(null)}
          empty={list.loading ? 'Chargement…' : 'Aucune transaction sur cette période.'} />
        <Pager page={list.data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
      </Card>
    </>
  );
}
