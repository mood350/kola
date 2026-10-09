import { useState } from 'react';
import { api } from '../../lib/api';
import { money } from '../../lib/format';
import { useResource } from '../../lib/useResource';
import { ConfirmDialog, ErrorNotice, Skeleton, Table } from '../../components/ui';
import { ChangeList } from './FeesEditor';
import {
  NO_CEILING, ceilingFromServer, percentFromServer, percentInput, percentLabel,
  readCeiling, readPercent, readScore, tierName,
} from './shared';

const ceilingLabel = (value) => (value === null ? NO_CEILING : money(value));
const grouped = (value) => (value === null ? '' : value.toLocaleString('fr-FR'));

/**
 * Les paliers de prêt : score minimal pour y accéder, plafond du montant, taux mensuel.
 * En lecture seule pour qui n'a pas le droit de modifier (l'analyste crédit), modifiable pour le
 * Super-admin — avec le même récapitulatif avant/après que les frais. Un plafond vide veut dire
 * « aucun plafond » ; 0 est refusé (il bloquerait tous les prêts).
 */
export default function LadderEditor({ editable }) {
  const ladder = useResource((signal) => api.get('/credit/tier-config', { signal }), 'ladder');
  const [edits, setEdits] = useState({});
  const [confirming, setConfirming] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  if (!ladder.data) {
    return ladder.error ? <ErrorNotice message={ladder.error} onRetry={ladder.reload} /> : <Skeleton rows={4} />;
  }

  const FIELDS = [
    {
      key: 'minScore', header: 'Score minimal (sur 100)', read: readScore, align: 'right',
      original: (row) => row.minScore, input: (v) => String(v), text: (v) => `${v} / 100`,
      write: (v) => v, unit: '/ 100', hint: 'un entier de 0 à 100',
    },
    {
      key: 'maxAmount', header: 'Montant maximum', read: readCeiling, align: 'right',
      original: (row) => ceilingFromServer(row.maxAmount), input: grouped, text: ceilingLabel,
      write: (v) => (v === null ? NO_CEILING : String(v)), placeholder: NO_CEILING, unit: 'FCFA', wide: true,
      hint: 'un montant en FCFA, ou vide pour aucun plafond',
    },
    {
      key: 'monthlyRate', header: 'Taux mensuel', read: readPercent, align: 'right',
      original: (row) => percentFromServer(row.monthlyRate), input: percentInput, text: percentLabel,
      write: (v) => `${percentInput(v)} %/mois`, unit: '% / mois', hint: 'de 0 à 100 %, une décimale au plus',
    },
  ];

  const cell = (row, field) => `${row.name}.${field.key}`;
  const shown = (row, field) => edits[cell(row, field)] ?? field.input(field.original(row));

  const changes = [];
  let invalid = 0;
  for (const row of ladder.data) {
    for (const field of FIELDS) {
      const text = edits[cell(row, field)];
      if (text === undefined) continue;
      const next = field.read(text);
      if (Number.isNaN(next)) invalid += 1;
      else if (next !== field.original(row)) {
        changes.push({
          id: cell(row, field),
          what: `${tierName(row.name)} · ${field.header.replace(' (sur 100)', '')}`,
          from: field.text(field.original(row)),
          to: field.text(next),
        });
      }
    }
  }

  const save = async () => {
    setPending(true);
    setError('');
    try {
      const tiers = ladder.data.map((row) => {
        const body = { name: row.name, minScore: row.minScore, maxAmount: row.maxAmount, monthlyRate: row.monthlyRate };
        for (const field of FIELDS) {
          const text = edits[cell(row, field)];
          if (text !== undefined) body[field.key] = field.write(field.read(text));
        }
        return body;
      });
      ladder.replace(await api.put('/credit/tier-config', { tiers }));
      setEdits({});
      setConfirming(false);
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setPending(false);
    }
  };

  const columns = [
    { key: 'name', header: 'Palier', render: (row) => tierName(row.name) },
    ...FIELDS.map((field) => ({
      key: field.key,
      header: field.header,
      align: field.align,
      render: (row) => {
        if (!editable) return <span className="num">{field.text(field.original(row))}</span>;
        const text = edits[cell(row, field)];
        const bad = text !== undefined && Number.isNaN(field.read(text));
        return (
          <span className="rate-field">
            <input
              className={`input compact num${field.wide ? ' wide' : ''}`}
              inputMode={field.key === 'monthlyRate' ? 'decimal' : 'numeric'}
              autoComplete="off"
              value={shown(row, field)}
              placeholder={field.placeholder}
              onChange={(event) => { setEdits({ ...edits, [cell(row, field)]: event.target.value }); setDone(false); }}
              aria-label={`${field.header}, ${tierName(row.name)} : ${field.hint}`}
              aria-invalid={bad}
            />
            <span className="unit" aria-hidden="true">{field.unit}</span>
          </span>
        );
      },
    })),
  ];

  return (
    <>
      <p className="muted">
        Les conditions de prêt par palier : plus le score est haut, plus le palier est avantageux.
        {editable
          ? ' Les nouvelles conditions s\'appliquent dès l\'enregistrement.'
          : ' Seul un Super-admin peut les modifier.'}
      </p>
      {done && <div className="notice success" role="status">Barème de crédit enregistré.</div>}
      <ErrorNotice message={ladder.error} onRetry={ladder.reload} />
      <Table rows={ladder.data} rowKey={(row) => row.name} columns={columns} />

      {editable && (
        <>
          {invalid > 0 && (
            <p className="field-error" role="alert">
              Score : un entier de 0 à 100. Montant : un montant en FCFA supérieur à 0, ou vide pour aucun plafond.
              Taux : de 0 à 100 %, une décimale au plus.
            </p>
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
        </>
      )}

      {confirming && (
        <ConfirmDialog
          title="Modifier le barème de crédit ?"
          message="Ces conditions seront appliquées aux prochaines demandes de prêt."
          confirmLabel="Appliquer le nouveau barème"
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
