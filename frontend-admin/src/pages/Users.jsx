import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { count, date, options, userState } from '../lib/format';
import { describePeriod } from '../lib/period';
import { useFilters } from '../lib/useFilters';
import { useResource } from '../lib/useResource';
import { Card, ErrorNotice, PageHead, Pager, Person, SearchInput, Skeleton, Status, Table } from '../components/ui';
import { IconClose } from '../components/icons';

const DEFAULTS = { q: '', kyc: '', du: '', au: '', page: 0, size: 15 };

/**
 * Utilisateurs : quel client dois-je retrouver ?
 *
 * Une recherche (nom ou téléphone) et un seul filtre, le niveau KYC. La période d'inscription
 * n'a pas de contrôle propre : elle arrive par un lien (« Nouveaux utilisateurs » de l'accueil)
 * et s'affiche alors en puce que l'on retire d'un clic. Quatre colonnes suffisent à reconnaître
 * quelqu'un ; tout le reste est dans sa fiche. Le serveur ne trie pas : les plus récents d'abord.
 */
export default function Users() {
  const navigate = useNavigate();
  const [f, update] = useFilters(DEFAULTS);

  const { data, error, reload } = useResource(
    (signal) => api.get('/console/users', {
      params: { q: f.q, kycTier: f.kyc, from: f.du, to: f.au, page: f.page, size: f.size },
      signal,
    }),
    JSON.stringify(f),
  );

  const period = f.du || f.au;
  const filtered = Boolean(f.q || f.kyc || period);

  return (
    <>
      <PageHead
        title="Utilisateurs"
        subtitle={data ? `${count(data.total)} utilisateur${data.total > 1 ? 's' : ''}` : 'Retrouvez un client pour consulter son compte.'}
      />
      <ErrorNotice message={error} onRetry={reload} />
      <Card
        flush
        toolbar={(
          <>
            <SearchInput value={f.q} onChange={(q) => update({ q })}
              placeholder="Nom ou téléphone" label="Rechercher un utilisateur" />
            <select className="select" aria-label="Niveau KYC" value={f.kyc} onChange={(e) => update({ kyc: e.target.value })}>
              <option value="">Tous les niveaux</option>
              {options('kycTier').map(([code, text]) => <option key={code} value={code}>{text}</option>)}
            </select>
            {period && (
              <button type="button" className="chip" onClick={() => update({ du: '', au: '' })}
                aria-label={`Retirer le filtre : inscrits ${describePeriod(f.du, f.au)}`}>
                Inscrits {describePeriod(f.du, f.au)} <IconClose size={14} />
              </button>
            )}
          </>
        )}
      >
        {!data && !error ? (
          <div className="card-body"><Skeleton rows={6} /></div>
        ) : (
          <Table
            rows={data?.items}
            rowKey={(u) => u.id}
            onRowClick={(u) => navigate(`/utilisateurs/${u.id}`)}
            empty={filtered ? 'Aucun utilisateur ne correspond à cette recherche.' : 'Aucun utilisateur pour l\'instant.'}
            columns={[
              {
                key: 'name',
                header: 'Utilisateur',
                render: (u) => (
                  <Person name={u.fullName}
                    sub={u.pendingDocuments > 0 ? `${u.pendingDocuments} pièce${u.pendingDocuments > 1 ? 's' : ''} KYC à vérifier` : null} />
                ),
              },
              { key: 'phone', header: 'Téléphone', render: (u) => <span className="num">{u.phone}</span> },
              { key: 'status', header: 'Statut', render: (u) => <Status kind="userStatus" code={userState(u)} /> },
              { key: 'created', header: 'Inscription', render: (u) => <span className="num">{date(u.createdAt)}</span> },
            ]}
          />
        )}
        <Pager page={data} onPage={(page) => update({ page })} onSize={(size) => update({ size })} />
      </Card>
    </>
  );
}
