import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, download } from '../lib/api';
import { date, dateTime, initials, label, money } from '../lib/format';
import { useResource } from '../lib/useResource';
import { Card, ConfirmDialog, Empty, ErrorNotice, PageHead, Pager, Status, Table } from '../components/ui';
import { transactionColumns } from './Transactions';
import { IconDownload } from '../components/icons';

/**
 * Fiche client : ce que l'application montre au client, vu du back-office.
 * Deux choses s'y font : débloquer le compte, et trancher ses pièces KYC —
 * sans repasser par la file.
 */
export default function UserDetail() {
  const { id } = useParams();
  const { data, error, reload, replace } = useResource((signal) => api.get(`/console/users/${id}`, { signal }), id);
  const [dialog, setDialog] = useState(null); // { kind: 'unblock' } | { kind: 'approve'|'reject', doc }
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  const close = () => { setDialog(null); setActionError(''); };

  const confirm = async (reason) => {
    setPending(true);
    setActionError('');
    try {
      if (dialog.kind === 'unblock') {
        replace(await api.post(`/console/users/${id}/unblock`));
        setNotice('Compte débloqué.');
      } else {
        await api.post(`/console/kyc/${dialog.doc.id}/${dialog.kind}`, dialog.kind === 'reject' ? { reason } : {});
        setNotice(dialog.kind === 'approve' ? 'Pièce approuvée, niveau KYC recalculé.' : 'Pièce rejetée, motif envoyé au client.');
        reload();
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
        <ErrorNotice message={error} onRetry={reload} />
        {!error && <Empty>Chargement…</Empty>}
      </>
    );
  }

  const { profile: p } = data;
  const blocked = p.status !== 'ACTIVE' || p.locked;

  return (
    <>
      <PageHead
        title={(
          <span className="hero-person">
            <span className="avatar lg">{initials(p.fullName)}</span>
            <span>
              {p.fullName}
              <span className="cell-sub num">{p.phone}</span>
            </span>
          </span>
        )}
        back={back}
        actions={blocked && (
          <button type="button" className="btn" onClick={() => setDialog({ kind: 'unblock' })}>Débloquer le compte</button>
        )}
      />
      <ErrorNotice message={error} onRetry={reload} />
      {notice && <div className="notice success" role="status">{notice}</div>}

      <div className="stack">
        <Card title="Profil">
          <dl className="fields">
            <Field label="État">
              {p.locked && p.status === 'ACTIVE'
                ? <span className="badge warn">Verrouillé jusqu&apos;au {dateTime(p.lockedUntil)}</span>
                : <Status kind="userStatus" code={p.status} />}
            </Field>
            <Field label="Niveau KYC"><span className="badge info">{label('kycTier', p.kycTier)}</span></Field>
            <Field label="E-mail">{p.email || '—'}</Field>
            <Field label="Ville">{[p.city, p.country].filter(Boolean).join(', ') || '—'}</Field>
            <Field label="Inscrit le">{date(p.createdAt)}</Field>
            <Field label="Dernière connexion">{dateTime(p.lastLoginAt)}</Field>
          </dl>
        </Card>

        <Documents documents={data.documents} onDecide={(doc, kind) => setDialog({ kind, doc })} />

        <div className="grid-2">
          <Card title="Comptes" flush>
            <Table
              rows={data.wallets}
              rowKey={(w) => `${w.type}-${w.currency}`}
              empty="Aucun compte."
              columns={[
                { key: 'type', header: 'Compte', render: (w) => label('walletType', w.type) },
                { key: 'available', header: 'Disponible', align: 'right', render: (w) => <span className="num">{money(w.available, w.currency)}</span> },
                { key: 'locked', header: 'Bloqué', align: 'right', render: (w) => <span className="num">{money(w.locked, w.currency)}</span> },
              ]}
            />
          </Card>

          <Card title="Coffres" flush>
            <Table
              rows={data.vaults}
              rowKey={(v) => v.id}
              empty="Aucun coffre."
              columns={[
                { key: 'name', header: 'Nom', render: (v) => v.name },
                {
                  key: 'balance',
                  header: 'Solde',
                  align: 'right',
                  render: (v) => (
                    <span className="num">
                      {money(v.balance, v.currency)}
                      {v.targetAmount && <span className="cell-sub">sur {money(v.targetAmount, v.currency)}</span>}
                    </span>
                  ),
                },
                { key: 'status', header: 'État', render: (v) => label('vaultStatus', v.status) },
              ]}
            />
          </Card>
        </div>

        <Card title="Prêts" flush>
          <Table
            rows={data.loans}
            rowKey={(l) => l.id}
            empty="Aucun prêt."
            columns={[
              { key: 'principal', header: 'Montant prêté', align: 'right', render: (l) => <span className="num">{money(l.principal, l.currency)}</span> },
              { key: 'outstanding', header: 'Reste dû', align: 'right', render: (l) => <span className="num">{money(l.outstanding, l.currency)}</span> },
              { key: 'status', header: 'État', render: (l) => <Status kind="loanStatus" code={l.status} /> },
              { key: 'disbursed', header: 'Versé le', render: (l) => date(l.disbursedAt) },
              { key: 'due', header: 'Échéance', render: (l) => date(l.dueAt) },
            ]}
          />
        </Card>

        <Card title="Paiements programmés" flush>
          <Table
            rows={data.scheduled}
            rowKey={(t) => t.id}
            empty="Aucun paiement programmé."
            columns={[
              { key: 'type', header: 'Type', render: (t) => <>{label('taskType', t.type)}{t.description && <span className="cell-sub">{t.description}</span>}</> },
              { key: 'amount', header: 'Montant', align: 'right', render: (t) => <span className="num">{money(t.amount, t.currency)}</span> },
              { key: 'frequency', header: 'Fréquence', render: (t) => label('frequency', t.frequency) },
              { key: 'next', header: 'Prochaine exécution', render: (t) => dateTime(t.nextRunAt) },
              { key: 'status', header: 'État', render: (t) => <Status kind="taskStatus" code={t.status} /> },
            ]}
          />
        </Card>

        <History userId={p.id} phone={p.phone} />
      </div>

      {dialog && (
        <ConfirmDialog
          title={{ unblock: 'Débloquer le compte', approve: 'Approuver la pièce', reject: 'Rejeter la pièce' }[dialog.kind]}
          message={dialog.kind === 'unblock'
            ? `${p.fullName} pourra de nouveau se connecter. Le compteur de PIN erronés est remis à zéro.`
            : `${label('docType', dialog.doc.type)} de ${p.fullName}.${dialog.kind === 'approve' ? ' Le niveau KYC sera recalculé.' : ''}`}
          reasonLabel={dialog.kind === 'reject' ? 'Motif, envoyé au client (300 caractères max.)' : undefined}
          confirmLabel={{ unblock: 'Débloquer', approve: 'Approuver', reject: 'Rejeter' }[dialog.kind]}
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

/** Pièces du client ; celles en attente se tranchent ici même. */
function Documents({ documents, onDecide }) {
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
    <Card title="Vérification d'identité (KYC)" flush>
      {fileError && <div className="card-body"><ErrorNotice message={fileError} /></div>}
      <Table
        rows={documents}
        rowKey={(d) => d.id}
        empty="Aucune pièce envoyée depuis l'application."
        columns={[
          { key: 'type', header: 'Pièce', render: (d) => label('docType', d.type) },
          { key: 'date', header: 'Envoyée le', render: (d) => date(d.submittedAt) },
          {
            key: 'status',
            header: 'État',
            wrap: true,
            render: (d) => <><Status kind="docStatus" code={d.status} />{d.rejectionReason && <span className="cell-sub">{d.rejectionReason}</span>}</>,
          },
          {
            key: 'actions',
            header: '',
            align: 'right',
            render: (d) => (
              <div className="btn-row end">
                <button type="button" className="btn secondary sm" onClick={() => fetchFile(d)}><IconDownload size={14} />Pièce</button>
                {d.status === 'PENDING' && (
                  <>
                    <button type="button" className="btn danger sm" onClick={() => onDecide(d, 'reject')}>Rejeter</button>
                    <button type="button" className="btn sm" onClick={() => onDecide(d, 'approve')}>Approuver</button>
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

/** Historique paginé : dix lignes à la fois, jamais l'historique entier. */
function History({ userId, phone }) {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const { data, loading, error, reload } = useResource(
    (signal) => api.get('/console/transactions', { params: { userId, page, size }, signal }),
    `${userId}|${page}|${size}`,
  );

  return (
    <Card
      title="Transactions"
      actions={<Link className="linklike" to={`/transactions?q=${encodeURIComponent(phone)}`}>Filtrer et exporter →</Link>}
      flush
    >
      {error && <div className="card-body"><ErrorNotice message={error} onRetry={reload} /></div>}
      <Table rows={data?.items} rowKey={(t) => t.reference} columns={transactionColumns(userId)}
        empty={loading ? 'Chargement…' : 'Aucune transaction.'} />
      <Pager page={data} onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} sizes={[10, 15, 25]} />
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
