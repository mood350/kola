import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, download } from '../lib/api';
import { dateTime, label } from '../lib/format';
import { useResource } from '../lib/useResource';
import { Card, ConfirmDialog, ErrorNotice, PageHead, Person, Table } from '../components/ui';
import { IconDownload } from '../components/icons';

/**
 * Pièces d'identité à vérifier, plus anciennes d'abord. Approuver recalcule le
 * niveau du client ; rejeter exige un motif, qui lui est envoyé — « rejeté »
 * tout seul ne lui dirait pas quoi renvoyer.
 */
export default function Kyc() {
  const { data, error, reload, replace } = useResource((signal) => api.get('/console/kyc', { signal }), 'kyc');
  const [decision, setDecision] = useState(null); // { doc, action }
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [fileError, setFileError] = useState('');

  const close = () => { setDecision(null); setActionError(''); };

  const confirm = async (reason) => {
    const { doc, action } = decision;
    setPending(true);
    setActionError('');
    try {
      await api.post(`/console/kyc/${doc.id}/${action}`, action === 'reject' ? { reason } : {});
      replace(data.filter((d) => d.id !== doc.id));
      setNotice(`${label('docType', doc.type)} de ${doc.userName} ${action === 'approve' ? 'approuvée' : 'rejetée'}.`);
      close();
    } catch (e) {
      setActionError(e.message);
    } finally {
      setPending(false);
    }
  };

  const fetchFile = async (doc) => {
    setNotice('');
    setFileError('');
    try {
      await download(`/kyc/documents/${doc.id}/file`, doc.fileName);
    } catch (e) {
      setFileError(e.message);
    }
  };

  return (
    <>
      <PageHead title="Vérifications KYC" subtitle={data ? `${data.length} pièce${data.length > 1 ? 's' : ''} en attente` : null} />
      <ErrorNotice message={error} onRetry={reload} />
      <ErrorNotice message={fileError} />
      {notice && <div className="notice success" role="status">{notice}</div>}
      <Card flush>
        <Table
          rows={data}
          rowKey={(d) => d.id}
          empty="Aucune pièce en attente."
          columns={[
            {
              key: 'who',
              header: 'Client',
              render: (d) => <Link className="plain" to={`/utilisateurs/${d.userId}`}><Person name={d.userName} sub={d.phone} /></Link>,
            },
            { key: 'type', header: 'Pièce', render: (d) => label('docType', d.type) },
            { key: 'tier', header: 'Niveau actuel', render: (d) => label('kycTier', d.currentTier) },
            { key: 'date', header: 'Envoyée le', render: (d) => dateTime(d.submittedAt) },
            {
              key: 'actions',
              header: '',
              align: 'right',
              render: (d) => (
                <div className="btn-row end">
                  <button type="button" className="btn secondary sm" onClick={() => fetchFile(d)}><IconDownload size={16} />Pièce</button>
                  <button type="button" className="btn danger sm" onClick={() => setDecision({ doc: d, action: 'reject' })}>Rejeter</button>
                  <button type="button" className="btn sm" onClick={() => setDecision({ doc: d, action: 'approve' })}>Approuver</button>
                </div>
              ),
            },
          ]}
        />
      </Card>

      {decision && (
        <ConfirmDialog
          title={decision.action === 'approve' ? 'Approuver la pièce' : 'Rejeter la pièce'}
          message={decision.action === 'approve'
            ? `${label('docType', decision.doc.type)} de ${decision.doc.userName}. Son niveau KYC sera recalculé.`
            : `${label('docType', decision.doc.type)} de ${decision.doc.userName}.`}
          reasonLabel={decision.action === 'reject' ? 'Motif, envoyé au client (300 caractères max.)' : undefined}
          confirmLabel={decision.action === 'approve' ? 'Approuver' : 'Rejeter'}
          danger={decision.action === 'reject'}
          pending={pending}
          error={actionError}
          onConfirm={confirm}
          onCancel={close}
        />
      )}
    </>
  );
}
