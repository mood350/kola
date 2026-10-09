import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, download } from '../lib/api';
import { date, dateTime, initials, label, loanState, money, userState } from '../lib/format';
import { useResource } from '../lib/useResource';
import { useSession } from '../lib/session';
import {
  Card, ConfirmDialog, ErrorNotice, PageHead, Pager, Skeleton, Status, Table, Tabs,
} from '../components/ui';
import { transactionColumns } from './Transactions';
import { IconDownload } from '../components/icons';

const TAB_KEYS = ['apercu', 'transactions', 'credits', 'epargne', 'kyc'];

/**
 * Fiche utilisateur : qui est ce client, et où en est son compte ?
 *
 * Un en-tête (identité, statut, et l'unique action : lever un blocage), puis cinq onglets qui
 * regroupent des informations réellement distinctes. L'onglet vit dans l'adresse (`?onglet=kyc`) :
 * un lien mène droit à la bonne section. Le rôle Support consulte, il ne décide pas : les boutons
 * de décision lui sont masqués (le serveur reste seul juge).
 */
export default function UserDetail() {
  const { id } = useParams();
  const { canAct } = useSession();
  const [params, setParams] = useSearchParams();
  const tab = TAB_KEYS.includes(params.get('onglet')) ? params.get('onglet') : 'apercu';

  const { data, error, reload, replace } = useResource((signal) => api.get(`/console/users/${id}`, { signal }), id);
  const [dialog, setDialog] = useState(null); // { kind: 'unblock' } | { kind: 'approve'|'reject', doc }
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  const close = () => { setDialog(null); setActionError(''); };
  const selectTab = (next) => setParams(next === 'apercu' ? {} : { onglet: next }, { replace: true });

  const confirm = async (reason) => {
    setPending(true);
    setActionError('');
    try {
      if (dialog.kind === 'unblock') {
        replace(await api.post(`/console/users/${id}/unblock`));
        setNotice('Le compte est débloqué : le client peut de nouveau se connecter.');
      } else {
        await api.post(`/console/kyc/${dialog.doc.id}/${dialog.kind}`, dialog.kind === 'reject' ? { reason } : {});
        setNotice(dialog.kind === 'approve'
          ? 'Pièce acceptée : le niveau de vérification du client est recalculé.'
          : 'Pièce refusée : le motif a été envoyé au client.');
        reload();
        window.dispatchEvent(new Event('kola:kyc-changed'));
      }
      close();
    } catch (e) {
      setActionError(e.message);
    } finally {
      setPending(false);
    }
  };

  const back = { to: '/utilisateurs', label: 'Utilisateurs' };
  if (!data) {
    return (
      <>
        <PageHead title="Utilisateur" back={back} />
        {error ? <ErrorNotice message={error} onRetry={reload} /> : <Skeleton rows={4} />}
      </>
    );
  }

  const { profile: p } = data;
  const blocked = p.status !== 'ACTIVE' || p.locked;
  const pendingDocs = data.documents.filter((d) => d.status === 'PENDING').length;
  const tabs = [
    { key: 'apercu', label: 'Aperçu' },
    { key: 'transactions', label: 'Transactions' },
    { key: 'credits', label: 'Crédits', badge: data.loans.length || null },
    { key: 'epargne', label: 'Épargne' },
    { key: 'kyc', label: 'KYC', badge: pendingDocs || null },
  ];

  return (
    <>
      <PageHead
        back={back}
        title={(
          <span className="hero-person">
            <span className="avatar lg">{initials(p.fullName)}</span>
            <span>
              {p.fullName}
              <span className="cell-sub num">{p.phone}</span>
            </span>
          </span>
        )}
        actions={blocked && canAct && (
          <button type="button" className="btn" onClick={() => setDialog({ kind: 'unblock' })}>Débloquer le compte</button>
        )}
      />
      <p className="profile-meta">
        <Status kind="userStatus" code={userState(p)} />
        <span>{label('kycTier', p.kycTier)}</span>
        <span>Inscrit le <span className="num">{date(p.createdAt)}</span></span>
        <span>Dernière connexion : <span className="num">{dateTime(p.lastLoginAt)}</span></span>
      </p>
      {p.locked && p.status === 'ACTIVE' && (
        <div className="notice" role="status">
          Compte verrouillé après plusieurs PIN erronés, jusqu&apos;au <span className="num">{dateTime(p.lockedUntil)}</span>.
        </div>
      )}
      <ErrorNotice message={error} onRetry={reload} />
      {notice && <div className="notice success" role="status">{notice}</div>}

      <Tabs tabs={tabs} value={tab} onChange={selectTab} label="Sections de la fiche">
        {tab === 'apercu' && <Overview data={data} />}
        {tab === 'transactions' && <History userId={p.id} phone={p.phone} />}
        {tab === 'credits' && <Loans loans={data.loans} />}
        {tab === 'epargne' && <Savings wallets={data.wallets} vaults={data.vaults} />}
        {tab === 'kyc' && <Documents documents={data.documents} canAct={canAct} onDecide={(doc, kind) => setDialog({ kind, doc })} />}
      </Tabs>

      {dialog && (
        <ConfirmDialog
          title={{ unblock: 'Débloquer le compte', approve: 'Accepter la pièce', reject: 'Refuser la pièce' }[dialog.kind]}
          message={dialog.kind === 'unblock'
            ? `${p.fullName} pourra de nouveau se connecter. Le compteur de PIN erronés est remis à zéro.`
            : `${label('docType', dialog.doc.type)} de ${p.fullName}.${dialog.kind === 'approve' ? ' Le niveau de vérification sera recalculé.' : ''}`}
          reasonLabel={dialog.kind === 'reject' ? 'Motif, envoyé au client (300 caractères max.)' : undefined}
          confirmLabel={{ unblock: 'Débloquer', approve: 'Accepter', reject: 'Refuser' }[dialog.kind]}
          danger={dialog.kind === 'reject'}
          pending={pending}
          error={actionError}
          onConfirm={confirm}
          onCancel={close}
        />
      )}
    </>
  );
}

/** Le profil, les comptes et les paiements programmés : de quoi situer le client en un coup d'œil. */
function Overview({ data }) {
  const { profile: p } = data;
  return (
    <div className="stack">
      <Card title="Profil">
        <dl className="fields">
          <Field label="E-mail">{p.email || '—'}</Field>
          <Field label="Localisation">{[p.city, p.country].filter(Boolean).join(', ') || '—'}</Field>
          <Field label="Niveau de vérification">{label('kycTier', p.kycTier)}</Field>
          <Field label="Inscription"><span className="num">{date(p.createdAt)}</span></Field>
        </dl>
      </Card>

      <Card title="Comptes" flush>
        <Table
          rows={data.wallets}
          rowKey={(w) => `${w.type}-${w.currency}`}
          empty="Ce client n'a pas encore de compte."
          columns={[
            { key: 'type', header: 'Compte', render: (w) => label('walletType', w.type) },
            { key: 'available', header: 'Disponible', align: 'right', render: (w) => <span className="num">{money(w.available, w.currency)}</span> },
            { key: 'locked', header: 'Bloqué', align: 'right', render: (w) => <span className="num">{money(w.locked, w.currency)}</span> },
          ]}
        />
      </Card>

      <Card title="Paiements programmés" flush>
        <Table
          rows={data.scheduled}
          rowKey={(t) => t.id}
          empty="Aucun paiement programmé."
          columns={[
            { key: 'type', header: 'Type', wrap: true, render: (t) => <>{label('taskType', t.type)}{t.description && <span className="cell-sub">{t.description}</span>}</> },
            { key: 'amount', header: 'Montant', align: 'right', render: (t) => <span className="num">{money(t.amount, t.currency)}</span> },
            { key: 'frequency', header: 'Fréquence', render: (t) => label('frequency', t.frequency) },
            { key: 'next', header: 'Prochaine exécution', render: (t) => <span className="num">{dateTime(t.nextRunAt)}</span> },
            { key: 'status', header: 'Statut', render: (t) => <Status kind="taskStatus" code={t.status} /> },
          ]}
        />
      </Card>
    </div>
  );
}

/** Historique paginé : dix lignes à la fois, jamais l'historique entier. */
function History({ userId, phone }) {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const { data, error, reload } = useResource(
    (signal) => api.get('/console/transactions', { params: { userId, page, size }, signal }),
    `${userId}|${page}|${size}`,
  );

  return (
    <Card
      title="Transactions"
      actions={<Link className="card-link" to={`/transactions?q=${encodeURIComponent(phone)}`}>Filtrer et exporter</Link>}
      flush
    >
      {error && <div className="card-body"><ErrorNotice message={error} onRetry={reload} /></div>}
      {!data && !error ? (
        <div className="card-body"><Skeleton rows={5} /></div>
      ) : (
        <Table rows={data?.items} rowKey={(t) => t.reference} columns={transactionColumns(userId)}
          onRowClick={(t) => navigate(`/transactions?ref=${encodeURIComponent(t.reference)}`)}
          empty="Ce client n'a encore aucune transaction." />
      )}
      <Pager page={data} onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} sizes={[10, 15, 25]} />
    </Card>
  );
}

/** Les prêts du client ; un prêt dont l'échéance est passée se dit « en retard », quel que soit son statut stocké. */
function Loans({ loans }) {
  return (
    <Card title="Prêts" flush>
      <Table
        rows={loans}
        rowKey={(l) => l.id}
        empty="Ce client n'a jamais emprunté."
        columns={[
          { key: 'principal', header: 'Montant prêté', align: 'right', render: (l) => <span className="num">{money(l.principal, l.currency)}</span> },
          { key: 'outstanding', header: 'Reste dû', align: 'right', render: (l) => <span className="num">{money(l.outstanding, l.currency)}</span> },
          { key: 'status', header: 'Statut', render: (l) => <Status kind="loanStatus" code={loanState(l)} /> },
          { key: 'disbursed', header: 'Versé le', render: (l) => <span className="num">{date(l.disbursedAt)}</span> },
          { key: 'due', header: 'Échéance', render: (l) => <span className="num">{date(l.dueAt)}</span> },
        ]}
      />
    </Card>
  );
}

/** Le compte épargne et les coffres. Il n'existe pas de liste globale d'épargne : tout passe par la fiche. */
function Savings({ wallets, vaults }) {
  const savings = wallets.filter((w) => w.type === 'SAVINGS');
  return (
    <div className="stack">
      <Card title="Compte épargne" flush>
        <Table
          rows={savings}
          rowKey={(w) => `${w.type}-${w.currency}`}
          empty="Ce client n'a pas de compte épargne."
          columns={[
            { key: 'currency', header: 'Devise', render: (w) => (w.currency === 'XOF' ? 'FCFA' : w.currency) },
            { key: 'available', header: 'Disponible', align: 'right', render: (w) => <span className="num">{money(w.available, w.currency)}</span> },
            { key: 'locked', header: 'Bloqué (garantie d\'un prêt)', align: 'right', render: (w) => <span className="num">{money(w.locked, w.currency)}</span> },
          ]}
        />
      </Card>

      <Card title="Coffres" flush>
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
              render: (v) => (v.targetAmount
                ? <span className="num">{money(v.targetAmount, v.currency)}{v.targetDate && <span className="cell-sub">avant le {date(v.targetDate)}</span>}</span>
                : '—'),
            },
            { key: 'status', header: 'Statut', render: (v) => label('vaultStatus', v.status) },
          ]}
        />
      </Card>
    </div>
  );
}

/** Les pièces du client ; celles en attente se tranchent ici, sauf pour un rôle en consultation. */
function Documents({ documents, canAct, onDecide }) {
  const [fileError, setFileError] = useState('');
  const fetchFile = async (d) => {
    setFileError('');
    try {
      await download(`/kyc/documents/${d.id}/file`, d.fileName);
    } catch (e) {
      setFileError(e.message);
    }
  };

  return (
    <Card title="Vérification d'identité" flush>
      {fileError && <div className="card-body"><ErrorNotice message={fileError} title="Le téléchargement n'a pas abouti." /></div>}
      {!canAct && documents.some((d) => d.status === 'PENDING') && (
        <div className="card-body"><p className="muted">Votre rôle consulte les dossiers sans les trancher.</p></div>
      )}
      <Table
        rows={documents}
        rowKey={(d) => d.id}
        empty="Aucune pièce envoyée depuis l'application."
        columns={[
          { key: 'type', header: 'Pièce', render: (d) => label('docType', d.type) },
          { key: 'date', header: 'Envoyée le', render: (d) => <span className="num">{date(d.submittedAt)}</span> },
          {
            key: 'status',
            header: 'Statut',
            wrap: true,
            render: (d) => <><Status kind="docStatus" code={d.status} />{d.rejectionReason && <span className="cell-sub">{d.rejectionReason}</span>}</>,
          },
          {
            key: 'actions',
            header: '',
            align: 'right',
            render: (d) => (
              <div className="btn-row end">
                <button type="button" className="btn secondary sm" onClick={() => fetchFile(d)}><IconDownload size={14} /> Télécharger</button>
                {d.status === 'PENDING' && canAct && (
                  <>
                    <button type="button" className="btn danger sm" onClick={() => onDecide(d, 'reject')}>Refuser</button>
                    <button type="button" className="btn sm" onClick={() => onDecide(d, 'approve')}>Accepter</button>
                  </>
                )}
              </div>
            ),
          },
        ]}
      />
    </Card>
  );
}

function Field({ label: text, children }) {
  return (
    <div>
      <dt>{text}</dt>
      <dd>{children}</dd>
    </div>
  );
}
