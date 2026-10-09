import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, download } from '../lib/api';
import { count, dateTime, label, money, options, partiesText } from '../lib/format';
import { activePreset, describePeriod } from '../lib/period';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import {
  Card, DateRange, ErrorNotice, FiltersPanel, PageHead, Pager, PeriodSelect, SearchInput, SidePanel,
  Skeleton, Status, Table,
} from '../components/ui';
import { IconDownload } from '../components/icons';

/**
 * Colonnes d'une ligne de transaction, partagées avec la fiche client.
 * `perspective` : l'utilisateur du point de vue duquel le sens (+) se lit.
 * La référence vient en premier : sur mobile, c'est elle qui titre le bloc. La liste ne porte que de
 * quoi reconnaître une opération ; frais, motif d'échec et description sont dans son détail.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function transactionColumns(perspective) {
  return [
    { key: 'reference', header: 'Référence', render: (t) => <span className="mono">{t.reference}</span> },
    { key: 'parties', header: 'Client', wrap: true, render: (t) => partiesText(t) },
    { key: 'type', header: 'Type', render: (t) => label('txType', t.type) },
    {
      key: 'amount',
      header: 'Montant',
      align: 'right',
      render: (t) => {
        const incoming = perspective && t.recipientId === perspective && t.senderId !== perspective;
        return <span className={`num ${incoming ? 'amount-in' : ''}`}>{incoming ? '+ ' : ''}{money(t.amount, t.currency)}</span>;
      },
    },
    {
      key: 'status',
      header: 'Statut',
      render: (t) => <Status kind="txStatus" code={t.status} />,
    },
    { key: 'date', header: 'Date', render: (t) => <span className="num">{dateTime(t.createdAt)}</span> },
  ];
}

const DEFAULTS = { q: '', type: '', statut: '', du: '', au: '', page: 0, size: 15 };

/**
 * Transactions : quelle opération dois-je examiner ?
 *
 * Une recherche (référence ou téléphone), trois filtres visibles — type, statut, période — et deux
 * exports en retrait. Les dates précises (calendrier) s'ouvrent sous la barre quand on les choisit.
 * Un clic sur une ligne ouvre son détail dans un panneau (`?ref=TXN-…`) : l'adresse mène donc droit
 * à une opération, depuis l'accueil comme depuis la fiche d'un client. Le serveur ne trie pas : les
 * plus récentes d'abord. Les totaux de la période tiennent sur une ligne ; la ventilation par type
 * est dans l'export Excel (feuille « Synthèse »).
 */
export default function Transactions() {
  const [f, update, reset, dirty] = useFilters(DEFAULTS);
  const [params, setParams] = useSearchParams();
  const [open, setOpen] = useState(false);
  const [exporting, setExporting] = useState('');
  const [exportError, setExportError] = useState('');

  const filters = { q: f.q, type: f.type, status: f.statut, from: f.du, to: f.au };
  const key = JSON.stringify(filters);
  const custom = Boolean(f.du || f.au) && activePreset(f.du, f.au) === null;
  const reference = params.get('ref');

  const list = useResource(
    (signal) => api.get('/console/transactions', { params: { ...filters, page: f.page, size: f.size }, signal }),
    `${key}|${f.page}|${f.size}`,
  );
  const summary = useResource((signal) => api.get('/console/transactions/summary', { params: filters, signal }), key);

  const openDetail = (ref) => setParams((previous) => {
    const next = new URLSearchParams(previous);
    if (ref) next.set('ref', ref); else next.delete('ref');
    return next;
  });

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

  return (
    <>
      <PageHead
        title="Transactions"
        subtitle={s
          ? <><span className="num">{count(s.count)}</span> transaction{s.count > 1 ? 's' : ''} · <span className="num">{money(s.volume)}</span> · frais perçus <span className="num">{money(s.fees)}</span> — {describePeriod(f.du, f.au)}</>
          : describePeriod(f.du, f.au)}
      />
      <ErrorNotice message={exportError} title="L'export n'a pas abouti." />
      <ErrorNotice message={list.error} onRetry={list.reload} />

      <Card
        flush
        toolbar={(
          <>
            <SearchInput value={f.q} onChange={(q) => update({ q })}
              placeholder="Référence ou téléphone" label="Rechercher une transaction" />
            <select className="select" aria-label="Type d'opération" value={f.type} onChange={(e) => update({ type: e.target.value })}>
              <option value="">Tous les types</option>
              {options('txType').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
            </select>
            <select className="select" aria-label="Statut" value={f.statut} onChange={(e) => update({ statut: e.target.value })}>
              <option value="">Tous les statuts</option>
              {options('txStatus').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
            </select>
            <PeriodSelect from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} onCustom={() => setOpen(true)} />
            <span className="spacer" />
            <button type="button" className="btn secondary" disabled={Boolean(exporting)} onClick={() => exportAs('csv')}>
              <IconDownload size={16} />{exporting === 'csv' ? 'Export…' : 'CSV'}
            </button>
            <button type="button" className="btn secondary" disabled={Boolean(exporting)} onClick={() => exportAs('xlsx')}>
              <IconDownload size={16} />{exporting === 'xlsx' ? 'Export…' : 'Excel'}
            </button>
          </>
        )}
      >
        {(open || custom) && (
          <FiltersPanel canReset={dirty} onReset={() => { reset(); setOpen(false); }}>
            <DateRange from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} label="Dates précises" />
          </FiltersPanel>
        )}

        {!list.data && !list.error ? (
          <div className="card-body"><Skeleton rows={6} /></div>
        ) : (
          <Table rows={list.data?.items} rowKey={(t) => t.reference} columns={transactionColumns(null)}
            onRowClick={(t) => openDetail(t.reference)}
            empty={dirty ? 'Aucune transaction ne correspond à ces filtres.' : 'Aucune transaction pour l\'instant. Dès qu\'un client enverra de l\'argent, elle apparaîtra ici.'} />
        )}
        <Pager page={list.data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
      </Card>

      {reference && (
        <TransactionPanel reference={reference} fromList={list.data?.items?.find((t) => t.reference === reference)}
          onClose={() => openDetail(null)} />
      )}
    </>
  );
}

/**
 * Le détail d'une opération. La ligne de la liste suffit quand elle est là ; sur un lien direct, on la
 * cherche par sa référence (recherche exacte du serveur). Il n'existe ni historique d'états ni
 * événements : on montre exactement ce que le serveur sait de l'opération.
 */
function TransactionPanel({ reference, fromList, onClose }) {
  const found = useResource(
    (signal) => (fromList
      ? Promise.resolve(null)
      : api.get('/console/transactions', { params: { q: reference, size: 1 }, signal })),
    `${reference}|${Boolean(fromList)}`,
  );
  const t = fromList ?? found.data?.items?.find((item) => item.reference === reference);

  const raw = t?.counterpartyName || t?.counterparty;
  const outside = raw === 'EXTERNAL' ? 'Extérieur' : raw;
  const client = (id, name) => (id
    ? <Link to={`/utilisateurs/${id}`}>{name || 'Compte Kola'}</Link>
    : null);

  return (
    <SidePanel title="Détail de la transaction" onClose={onClose}>
      {!t && !found.error && <Skeleton rows={4} />}
      {found.error && <ErrorNotice message={found.error} onRetry={found.reload} />}
      {!t && found.data && <p className="muted">Cette transaction est introuvable : vérifiez sa référence.</p>}
      {t && (
        <>
          <p className="panel-amount num">{money(t.amount, t.currency)}</p>
          <p className="panel-status"><Status kind="txStatus" code={t.status} /></p>
          {t.failureReason && <div className="notice error" role="status">{t.failureReason}</div>}

          <dl className="fields panel-fields">
            <div><dt>Référence</dt><dd className="mono wrap-any">{t.reference}</dd></div>
            <div><dt>Type</dt><dd>{label('txType', t.type)}</dd></div>
            <div><dt>Date</dt><dd className="num">{dateTime(t.createdAt)}</dd></div>
            <div><dt>Frais</dt><dd className="num">{money(t.fee, t.currency)}</dd></div>
            <div><dt>De</dt><dd>{client(t.senderId, t.senderName) || outside || '—'}</dd></div>
            <div><dt>Vers</dt><dd>{client(t.recipientId, t.recipientName) || outside || '—'}</dd></div>
            {t.senderId && t.senderId === t.recipientId && (
              <div className="panel-wide"><dt>Nature</dt><dd>Mouvement entre deux comptes du même client</dd></div>
            )}
            {t.description && <div className="panel-wide"><dt>Description</dt><dd>{t.description}</dd></div>}
          </dl>
        </>
      )}
    </SidePanel>
  );
}
