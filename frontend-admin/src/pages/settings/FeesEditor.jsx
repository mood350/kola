import { useState } from 'react';
import { api } from '../../lib/api';
import { label } from '../../lib/format';
import { useResource } from '../../lib/useResource';
import { ConfirmDialog, ErrorNotice, Skeleton, Table } from '../../components/ui';
import { percentFromServer, percentInput, percentLabel, readPercent } from './shared';

const FIELDS = [
  { key: 'p2p', header: 'Transfert entre particuliers' },
  { key: 'merchant', header: 'Paiement chez un marchand' },
  { key: 'cashout', header: 'Retrait' },
];

/**
 * Commission que paie chaque niveau de vérification (KYC), par type d'opération.
 *
 * Rien n'est envoyé tant qu'on n'a pas confirmé : « Enregistrer » ouvre un récapitulatif avant/après
 * des seules cases modifiées. Le serveur applique la grille dès l'enregistrement et la consigne
 * dans son journal d'audit. Les taux ont une décimale, comme ce que le serveur affiche.
 */
export default function FeesEditor() {
  const fees = useResource((signal) => api.get('/config/fees', { signal }), 'fees');
  const [edits, setEdits] = useState({});
  const [confirming, setConfirming] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  if (!fees.data) {
    return fees.error ? <ErrorNotice message={fees.error} onRetry={fees.reload} /> : <Skeleton rows={4} />;
  }

  const cell = (row, field) => `${row.tier}.${field.key}`;
  const original = (row, field) => percentFromServer(row[field.key]);
  const shown = (row, field) => edits[cell(row, field)] ?? percentInput(original(row, field));

  const changes = [];
  let invalid = 0;
  for (const row of fees.data) {
    for (const field of FIELDS) {
      const text = edits[cell(row, field)];
      if (text === undefined) continue;
      const next = readPercent(text);
      if (Number.isNaN(next)) invalid += 1;
      else if (next !== original(row, field)) {
        changes.push({
          id: cell(row, field),
          what: `${label('kycTier', row.tier)} · ${field.header}`,
          from: percentLabel(original(row, field)),
          to: percentLabel(next),
        });
      }
    }
  }

  const edit = (row, field) => (event) => {
    setEdits({ ...edits, [cell(row, field)]: event.target.value });
    setDone(false);
  };

  const save = async () => {
    setPending(true);
    setError('');
    try {
      const fees_ = fees.data.map((row) => ({
        tier: row.tier,
        ...Object.fromEntries(FIELDS.map((field) => {
          const text = edits[cell(row, field)];
          return [field.key, text === undefined ? row[field.key] : `${percentInput(readPercent(text))} %`];
        })),
      }));
      fees.replace(await api.put('/config/fees', { fees: fees_ }));
      setEdits({});
      setConfirming(false);
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setPending(false);
    }
  };

  return (
    <>
      <p className="muted">
        Commission prélevée sur chaque opération, selon le niveau de vérification du client. Les nouveaux taux
        s&apos;appliquent dès l&apos;enregistrement.
      </p>
      {done && <div className="notice success" role="status">Grille de frais enregistrée.</div>}
      <ErrorNotice message={fees.error} onRetry={fees.reload} />
      <Table
        rows={fees.data}
        rowKey={(row) => row.tier}
        columns={[
          { key: 'tier', header: 'Niveau de vérification', render: (row) => label('kycTier', row.tier) },
          ...FIELDS.map((field) => ({
            key: field.key,
            header: field.header,
            align: 'right',
            render: (row) => {
              const bad = edits[cell(row, field)] !== undefined && Number.isNaN(readPercent(edits[cell(row, field)]));
              return (
                <span className="rate-field">
                  <input
                    className="input compact num"
                    inputMode="decimal"
                    autoComplete="off"
                    value={shown(row, field)}
                    onChange={edit(row, field)}
                    aria-label={`${field.header}, ${label('kycTier', row.tier)}, en pourcentage`}
                    aria-invalid={bad}
                  />
                  <span aria-hidden="true">%</span>
                </span>
              );
            },
          })),
        ]}
      />
      {invalid > 0 && (
        <p className="field-error" role="alert">Un taux va de 0 à 100 %, avec une décimale au plus (par exemple 1,5).</p>
      )}
      <div className="btn-row save-bar">
        <button type="button" className="btn" disabled={changes.length === 0 || invalid > 0} onClick={() => setConfirming(true)}>
          Enregistrer
        </button>
        <button type="button" className="btn secondary" disabled={Object.keys(edits).length === 0}
          onClick={() => { setEdits({}); setDone(false); }}>
          Annuler les modifications
        </button>
        <span className="muted" role="status">
          {changes.length === 0 ? 'Aucune modification.' : `${changes.length} modification${changes.length > 1 ? 's' : ''} en attente.`}
        </span>
      </div>

      {confirming && (
        <ConfirmDialog
          title="Modifier la grille de frais ?"
          message="Ces taux seront appliqués aux prochaines opérations de tous les clients concernés."
          confirmLabel="Appliquer les nouveaux taux"
          pending={pending}
          error={error}
          onConfirm={save}
          onCancel={() => { setConfirming(false); setError(''); }}
        >
          <ChangeList changes={changes} />
        </ConfirmDialog>
      )}
    </>
  );
}

/** Les cases modifiées, avant → après. */
export function ChangeList({ changes }) {
  return (
    <ul className="change-list">
      {changes.map((change) => (
        <li key={change.id}>
          <span>{change.what}</span>
          <span className="num"><s>{change.from}</s> → <strong>{change.to}</strong></span>
        </li>
      ))}
    </ul>
  );
}
