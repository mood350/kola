import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { date, label, options } from '../lib/format';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import {
  Card, DateRange, ErrorNotice, FilterField, FiltersPanel, FiltersToggle, PageHead, Pager, Person, SearchInput, Status, Table,
} from '../components/ui';

const DEFAULTS = { q: '', kyc: '', attente: '', du: '', au: '', page: 0, size: 15 };

/**
 * Clients. La barre ne garde que la recherche et « Pièce à vérifier » — le
 * filtre dont on se sert tous les jours ; niveau KYC et période d'inscription
 * sont derrière « Filtres ». Le niveau KYC et les pièces en attente restent
 * visibles dans la liste même.
 */
export default function Users() {
  const navigate = useNavigate();
  const [f, update, reset, dirty] = useFilters(DEFAULTS);
  const [open, setOpen] = useState(false);

  const { data, error, loading, reload } = useResource(
    (signal) => api.get('/console/users', {
      params: { q: f.q, kycTier: f.kyc, kycPending: f.attente ? 'true' : '', from: f.du, to: f.au, page: f.page, size: f.size },
      signal,
    }),
    JSON.stringify(f),
  );

  const hidden = [f.kyc, f.du || f.au].filter(Boolean).length;

  return (
    <>
      <PageHead title="Utilisateurs" subtitle={data ? `${data.total} compte${data.total > 1 ? 's' : ''}` : null} />
      <ErrorNotice message={error} onRetry={reload} />
      <Card
        flush
        toolbar={(
          <>
            <SearchInput value={f.q} onChange={(q) => update({ q })}
              placeholder="Nom ou téléphone" label="Rechercher un utilisateur" />
            <label className="check">
              <input type="checkbox" checked={Boolean(f.attente)} onChange={(e) => update({ attente: e.target.checked ? '1' : '' })} />
              Pièce à vérifier
            </label>
            <FiltersToggle open={open} count={hidden} onToggle={() => setOpen((v) => !v)} />
          </>
        )}
      >
        {open && (
          <FiltersPanel canReset={dirty} onReset={() => { reset(); setOpen(false); }}>
            <FilterField label="Niveau KYC">
              <select className="select" value={f.kyc} onChange={(e) => update({ kyc: e.target.value })}>
                <option value="">Tous</option>
                {options('kycTier').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
              </select>
            </FilterField>
            <DateRange from={f.du} to={f.au} onChange={({ from, to }) => update({ du: from, au: to })} label="Inscrits entre" />
          </FiltersPanel>
        )}
        <Table
          rows={data?.items}
          rowKey={(u) => u.id}
          onRowClick={(u) => navigate(`/utilisateurs/${u.id}`)}
          empty={loading ? 'Chargement…' : 'Aucun utilisateur ne correspond.'}
          columns={[
            { key: 'name', header: 'Client', render: (u) => <Person name={u.fullName} sub={u.phone} /> },
            {
              key: 'kyc',
              header: 'KYC',
              render: (u) => (
                <>
                  <span className="badge info">{label('kycTier', u.kycTier)}</span>
                  {u.pendingDocuments > 0 && (
                    <>
                      {' '}
                      <span className="badge warn">
                        {u.pendingDocuments} pièce{u.pendingDocuments > 1 ? 's' : ''} à vérifier
                      </span>
                    </>
                  )}
                </>
              ),
            },
            {
              key: 'status',
              header: 'État',
              render: (u) => (u.locked && u.status === 'ACTIVE'
                ? <span className="badge warn">Verrouillé</span>
                : <Status kind="userStatus" code={u.status} />),
            },
            { key: 'created', header: 'Inscrit le', render: (u) => date(u.createdAt) },
            { key: 'last', header: 'Dernière connexion', render: (u) => date(u.lastLoginAt) },
          ]}
        />
        <Pager page={data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
      </Card>
    </>
  );
}
