import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, download } from '../lib/api';
import { count, date, dateTime, elapsed, label, since } from '../lib/format';
import { useResource } from '../lib/useResource';
import { useSession } from '../lib/session';
import {
  Card, ConfirmDialog, ErrorNotice, PageHead, Pager, Person, Skeleton, Status, Table, Tabs,
} from '../components/ui';
import { DocumentPreview } from '../components/DocumentPreview';
import { IconCheck, IconChevron, IconDownload } from '../components/icons';

const VIEWS = {
  attente: { status: 'PENDING', defaultOrder: 'anciens' },
  acceptes: { status: 'APPROVED', defaultOrder: 'recents' },
  refuses: { status: 'REJECTED', defaultOrder: 'recents' },
};

/**
 * KYC : quel dossier dois-je trancher ?
 *
 * Trois vues dans l'adresse (`?statut=refuses&tri=anciens`). En attente, c'est un poste de travail :
 * la file à gauche, le dossier ouvert à droite, la décision dessous ; la file se trie ici (le serveur
 * la livre en entier). Acceptés et refusés sont de la consultation : un tableau paginé, trié par le
 * serveur sur la date de la décision, dont chaque ligne mène à la fiche du client. Accepter recalcule
 * le niveau du client ; refuser exige un motif, qui lui est envoyé — « refusé » tout seul ne lui dirait
 * pas quoi renvoyer.
 */
export default function Kyc() {
  const { canAct } = useSession();
  const [params, setParams] = useSearchParams();

  const view = VIEWS[params.get('statut')] ? params.get('statut') : 'attente';
  const config = VIEWS[view];
  const order = ['anciens', 'recents'].includes(params.get('tri')) ? params.get('tri') : config.defaultOrder;
  const selectedId = params.get('dossier');
  const pageNumber = Number(params.get('page')) || 0;
  const size = Number(params.get('size')) || 15;
  const decided = view !== 'attente';

  // Une adresse ne garde que ce qui s'écarte des valeurs par défaut de sa vue.
  const query = ({ statut = view, tri, dossier = null, page = 0, taille = size } = {}) => {
    const next = {};
    if (statut !== 'attente') next.statut = statut;
    const wanted = tri ?? (statut === view ? order : VIEWS[statut].defaultOrder);
    if (wanted !== VIEWS[statut].defaultOrder) next.tri = wanted;
    if (page > 0) next.page = String(page);
    if (taille !== 15) next.size = String(taille);
    if (dossier) next.dossier = dossier;
    return next;
  };
  const go = (changes) => setParams(query(changes), { replace: true });

  const queue = useResource((signal) => api.get('/console/kyc', { signal }), 'kyc');
  const history = useResource(
    (signal) => (decided
      ? api.get('/console/kyc/history', {
        params: { status: config.status, order: order === 'recents' ? 'desc' : 'asc', page: pageNumber, size },
        signal,
      })
      : Promise.resolve(null)),
    `${view}|${order}|${pageNumber}|${size}`,
  );

  const [decision, setDecision] = useState(null); // { doc, action }
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [fileError, setFileError] = useState('');

  // En attente, le tri est local : le serveur livre la file du plus ancien au plus récent.
  const sortedQueue = [...(queue.data ?? [])].sort(
    (a, b) => (new Date(a.submittedAt) - new Date(b.submittedAt)) * (order === 'recents' ? -1 : 1),
  );
  const selected = sortedQueue.find((d) => d.id === selectedId) ?? null;
  const closeDialog = () => { setDecision(null); setActionError(''); };

  const confirm = async (reason) => {
    const { doc, action } = decision;
    setPending(true);
    setActionError('');
    try {
      await api.post(`/console/kyc/${doc.id}/${action}`, action === 'reject' ? { reason } : {});
      const index = sortedQueue.findIndex((d) => d.id === doc.id);
      const remaining = sortedQueue.filter((d) => d.id !== doc.id);
      queue.replace(remaining);
      // Sur grand écran on enchaîne avec le dossier suivant ; sur mobile on revient à la liste.
      const wide = window.matchMedia('(min-width: 900px)').matches;
      go({ dossier: wide ? (remaining[index] ?? remaining[index - 1])?.id : null });
      setNotice(action === 'approve'
        ? `${label('docType', doc.type)} de ${doc.userName} acceptée : son niveau de vérification est recalculé.`
        : `${label('docType', doc.type)} de ${doc.userName} refusée : le motif lui est envoyé.`);
      window.dispatchEvent(new Event('kola:kyc-changed'));
      closeDialog();
    } catch (e) {
      setActionError(e.message);
    } finally {
      setPending(false);
    }
  };

  const fetchFile = async (doc) => {
    setFileError('');
    try {
      await download(`/kyc/documents/${doc.id}/file`, doc.fileName);
    } catch (e) {
      setFileError(e.message);
    }
  };

  const direction = order === 'recents' ? 'les plus récents' : 'les plus anciens';
  const total = history.data?.total ?? 0;
  const subtitle = {
    attente: queue.data ? `${sortedQueue.length} dossier${sortedQueue.length > 1 ? 's' : ''} en attente, ${direction} d'abord.` : 'Les pièces d\'identité envoyées par les clients.',
    acceptes: history.data ? `${count(total)} dossier${total > 1 ? 's' : ''} accepté${total > 1 ? 's' : ''}, ${direction} d'abord.` : 'Les pièces acceptées.',
    refuses: history.data ? `${count(total)} dossier${total > 1 ? 's' : ''} refusé${total > 1 ? 's' : ''}, ${direction} d'abord.` : 'Les pièces refusées, avec leur motif.',
  }[view];

  const tabs = [
    { key: 'attente', label: 'En attente', badge: queue.data?.length || null },
    { key: 'acceptes', label: 'Acceptés' },
    { key: 'refuses', label: 'Refusés' },
  ];

  return (
    <>
      <PageHead title="KYC" subtitle={subtitle} />
      {notice && <div className="notice success" role="status">{notice}</div>}

      <Tabs tabs={tabs} value={view} label="Statut des dossiers" onChange={(next) => { setNotice(''); go({ statut: next }); }}>
        {decided ? (
          <Decisions view={view} history={history} order={order}
            onOrder={(tri) => go({ tri })} onPage={(page) => go({ page })} onSize={(taille) => go({ taille })} />
        ) : (
          <>
            <ErrorNotice message={queue.error} onRetry={queue.reload} />
            {!queue.data && !queue.error && <Skeleton rows={5} />}

            {queue.data && sortedQueue.length === 0 && (
              <div className="all-done">
                <IconCheck size={30} />
                <strong>Tous les dossiers sont traités.</strong>
                <span>Chaque nouvelle pièce envoyée par un client apparaîtra ici.</span>
              </div>
            )}

            {sortedQueue.length > 0 && (
              <div className={`split${selectedId ? ' has-selection' : ''}`}>
                <section className="queue-pane" aria-label="Dossiers en attente">
                  <div className="queue-tools">
                    <label htmlFor="kyc-sort">Trier par date d&apos;envoi</label>
                    <select id="kyc-sort" className="select" value={order} onChange={(e) => go({ tri: e.target.value, dossier: selectedId })}>
                      <option value="anciens">Plus anciens d&apos;abord</option>
                      <option value="recents">Plus récents d&apos;abord</option>
                    </select>
                  </div>
                  <ul className="queue">
                    {sortedQueue.map((d) => (
                      <li key={d.id}>
                        <Link to={{ search: `?${new URLSearchParams(query({ dossier: d.id }))}` }} replace
                          aria-current={d.id === selectedId ? 'true' : undefined}>
                          <span className="queue-main">
                            <strong>{d.userName || 'Client sans nom'}</strong>
                            <span className="row-sub">{label('docType', d.type)} · {label('kycTier', d.currentTier)}</span>
                          </span>
                          <span className="queue-side">
                            <span className="row-sub">{since(d.submittedAt)}</span>
                            <span className="row-sub num">{date(d.submittedAt)}</span>
                          </span>
                          <IconChevron className="row-go" />
                        </Link>
                      </li>
                    ))}
                  </ul>
                </section>

                <section className="dossier-pane" aria-label="Dossier ouvert">
                  <ErrorNotice message={fileError} title="Le téléchargement n'a pas abouti." />
                  {selected ? (
                    <Dossier doc={selected} canAct={canAct} back={{ search: `?${new URLSearchParams(query())}` }}
                      onDownload={() => fetchFile(selected)} onDecide={(action) => setDecision({ doc: selected, action })} />
                  ) : (
                    <div className="dossier-empty">
                      {selectedId
                        ? 'Ce dossier n\'est plus en attente : il a déjà été traité.'
                        : 'Choisissez un dossier dans la liste pour l\'examiner.'}
                    </div>
                  )}
                </section>
              </div>
            )}
          </>
        )}
      </Tabs>

      {decision && (
        <ConfirmDialog
          title={decision.action === 'approve' ? 'Accepter la pièce' : 'Refuser la pièce'}
          message={decision.action === 'approve'
            ? `${label('docType', decision.doc.type)} de ${decision.doc.userName}. Son niveau de vérification sera recalculé.`
            : `${label('docType', decision.doc.type)} de ${decision.doc.userName}. Le client reçoit le motif : dites-lui ce qu'il doit renvoyer.`}
          reasonLabel={decision.action === 'reject' ? 'Motif, envoyé au client (300 caractères max.)' : undefined}
          confirmLabel={decision.action === 'approve' ? 'Accepter' : 'Refuser'}
          danger={decision.action === 'reject'}
          pending={pending}
          error={actionError}
          onConfirm={confirm}
          onCancel={closeDialog}
        />
      )}
    </>
  );
}

/**
 * Les dossiers déjà tranchés : un tableau, pas un poste de travail. On y lit qui, quelle pièce, le
 * niveau où en est le client, quand la décision a été prise et en combien de temps ; un refus montre
 * son motif. Une ligne mène à la fiche du client (onglet KYC), où la pièce se télécharge.
 */
function Decisions({ view, history, order, onOrder, onPage, onSize }) {
  const navigate = useNavigate();
  const accepted = view === 'acceptes';
  const { data, error, reload } = history;

  const columns = [
    { key: 'client', header: 'Client', render: (d) => <Person name={d.userName} sub={d.phone} /> },
    { key: 'type', header: 'Pièce', render: (d) => label('docType', d.type) },
    accepted
      ? { key: 'tier', header: 'Niveau du client', render: (d) => label('kycTier', d.currentTier) }
      : { key: 'reason', header: 'Motif envoyé au client', wrap: true, render: (d) => d.rejectionReason || '—' },
    { key: 'sent', header: 'Envoyée le', render: (d) => <span className="num">{date(d.submittedAt)}</span> },
    { key: 'decided', header: 'Décision', render: (d) => <span><span className="num">{dateTime(d.reviewedAt)}</span><span className="cell-sub">traitée en {elapsed(d.submittedAt, d.reviewedAt)}</span></span> },
    { key: 'status', header: 'Statut', render: (d) => <Status kind="docStatus" code={d.status} /> },
  ];

  return (
    <Card
      flush
      toolbar={(
        <>
          <label className="toolbar-label" htmlFor="kyc-sort-history">Trier par date de décision</label>
          <select id="kyc-sort-history" className="select" value={order} onChange={(e) => onOrder(e.target.value)}>
            <option value="recents">Plus récentes d&apos;abord</option>
            <option value="anciens">Plus anciennes d&apos;abord</option>
          </select>
        </>
      )}
    >
      {error && <div className="card-body"><ErrorNotice message={error} onRetry={reload} /></div>}
      {!data && !error ? (
        <div className="card-body"><Skeleton rows={5} /></div>
      ) : (
        <Table
          rows={data?.items}
          rowKey={(d) => d.id}
          onRowClick={(d) => navigate(`/utilisateurs/${d.userId}?onglet=kyc`)}
          empty={accepted
            ? 'Aucun dossier accepté pour l\'instant. Les pièces que vous acceptez apparaîtront ici.'
            : 'Aucun dossier refusé pour l\'instant. Les pièces que vous refusez apparaîtront ici, avec leur motif.'}
          columns={columns}
        />
      )}
      <Pager page={data} onPage={onPage} onSize={onSize} sizes={[10, 15, 25]} />
    </Card>
  );
}

/** Le dossier ouvert : qui, quelle pièce, depuis quand, et la décision à prendre. */
function Dossier({ doc, canAct, back, onDownload, onDecide }) {
  return (
    <Card>
      <Link className="back only-narrow" to={back} replace>← Retour à la liste</Link>
      <div className="dossier-head">
        <Person name={doc.userName} sub={doc.phone} />
        <Link className="card-link" to={`/utilisateurs/${doc.userId}?onglet=kyc`}>Voir la fiche</Link>
      </div>

      <dl className="fields dossier-fields">
        <div><dt>Pièce</dt><dd>{label('docType', doc.type)}</dd></div>
        <div><dt>Niveau actuel</dt><dd>{label('kycTier', doc.currentTier)}</dd></div>
        <div><dt>Envoyée</dt><dd><span className="num">{dateTime(doc.submittedAt)}</span> · {since(doc.submittedAt)}</dd></div>
        <div><dt>Fichier</dt><dd className="wrap-any">{doc.fileName || '—'}</dd></div>
      </dl>

      {/* `key` : changer de dossier remonte l'aperçu, qui ne montre jamais la pièce du dossier précédent. */}
      <DocumentPreview key={doc.id} doc={doc} owner={doc.userName} />

      <div className="dossier-actions">
        {canAct && (
          <>
            <button type="button" className="btn" onClick={() => onDecide('approve')}>Accepter</button>
            <button type="button" className="btn danger" onClick={() => onDecide('reject')}>Refuser</button>
          </>
        )}
        <button type="button" className="btn secondary" onClick={onDownload}><IconDownload size={16} /> Télécharger la pièce</button>
      </div>
      <p className="muted dossier-hint">
        {canAct
          ? 'L\'aperçu affiche l\'image dans la page, sans l\'ouvrir dans un onglet ; le téléchargement reste disponible.'
          : 'Votre rôle consulte les dossiers sans les trancher.'}
      </p>

      <OtherDocuments doc={doc} />
    </Card>
  );
}

/** Ce que ce client a déjà envoyé, avec les motifs de refus : de quoi décider en connaissance de cause. */
function OtherDocuments({ doc }) {
  const { data, error } = useResource((signal) => api.get(`/console/users/${doc.userId}`, { signal }), doc.userId);
  const others = (data?.documents ?? []).filter((d) => d.id !== doc.id);

  return (
    <div className="dossier-history">
      <h3>Autres pièces de ce client</h3>
      {!data && !error && <Skeleton rows={1} />}
      {error && <p className="muted">L&apos;historique de ce client n&apos;a pas pu être chargé.</p>}
      {data && others.length === 0 && <p className="muted">Aucune autre pièce envoyée par ce client.</p>}
      {others.length > 0 && (
        <ul className="history-list">
          {others.map((d) => (
            <li key={d.id}>
              <span>{label('docType', d.type)} <span className="row-sub num">· {date(d.submittedAt)}</span></span>
              <Status kind="docStatus" code={d.status} />
              {d.rejectionReason && <span className="row-sub history-reason">{d.rejectionReason}</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
