import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import FocusableInput from '../../components/FocusableInput';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import { useRoles } from '../../hooks/useRoles';
import { roleBadgeStyle } from '../presentation';

const ROLE_OPTIONS = ['Super-admin', 'Agent conformité', 'Analyste crédit', 'Support'];

const selectStyle = s('width:100%; border:1px solid #E2E8F0; border-radius:10px; padding:8px 10px; font-family:Manrope,sans-serif; font-size:12px; font-weight:700; color:#131B2E; outline:0; background:#F2F3FF');
const inputStyle = s('width:100%; border:1px solid #E2E8F0; border-radius:10px; padding:8px 10px; font-family:Manrope,sans-serif; font-size:12px; font-weight:600; color:#131B2E; outline:0; background:#F2F3FF');

export default function Roles() {
  const { loading, error, actionError, actionPending, reload, admins, updatePermissions } = useRoles();
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState({ role: '', scope: '' });

  if (loading) return <LoadingState label="Chargement des comptes admin…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  const startEdit = (a) => {
    setEditingId(a.id);
    setDraft({ role: a.role, scope: a.scope });
  };
  const cancelEdit = () => setEditingId(null);
  const save = async (id) => {
    await updatePermissions(id, draft);
    setEditingId(null);
  };

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Comptes admin & permissions</div>
        <div style={s('display:grid; grid-template-columns:1.4fr 1fr 2fr 1fr; gap:8px; padding:16px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#596171; border-bottom:1px solid #E2E8F0')}>
          <div>ADMIN</div><div>RÔLE</div><div>PERMISSIONS PAR MODULE</div><div></div>
        </div>
        {admins.map((a) => {
          const editing = editingId === a.id;
          return (
            <div key={a.id} style={s('display:grid; grid-template-columns:1.4fr 1fr 2fr 1fr; gap:8px; align-items:center; padding:13px 8px; border-bottom:1px solid #E2E8F0')}>
              <div style={s('display:flex; align-items:center; gap:10px')}>
                <span style={s('width:32px; height:32px; flex:0 0 32px; border-radius:10px; background:linear-gradient(145deg,#0F3875,#002353); color:#FFCB05; font-size:11px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{a.initials}</span>
                <span style={s('font-size:13px; font-weight:700')}>{a.name}</span>
              </div>

              {editing ? (
                <select
                  value={draft.role}
                  onChange={(e) => setDraft((d) => ({ ...d, role: e.target.value }))}
                  style={selectStyle}
                >
                  {ROLE_OPTIONS.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              ) : (
                <div><span style={roleBadgeStyle(a.role)}>{a.role}</span></div>
              )}

              {editing ? (
                <FocusableInput
                  value={draft.scope}
                  onChange={(e) => setDraft((d) => ({ ...d, scope: e.target.value }))}
                  style={inputStyle}
                  focusStyle={{ borderColor: '#FFCB05', background: '#fff' }}
                />
              ) : (
                <div style={s('font-size:11.5px; font-weight:600; color:#596171')}>{a.scope}</div>
              )}

              {editing ? (
                <div style={s('display:flex; gap:6px')}>
                  <Hoverable as="button" disabled={actionPending} onClick={() => save(a.id)}
                    style={s('border:0; cursor:pointer; background:#002353; color:#fff; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px')}
                    hoverStyle={{ background: '#0F3875' }}
                  >Enregistrer</Hoverable>
                  <Hoverable as="button" disabled={actionPending} onClick={cancelEdit}
                    style={s('border:1px solid #E2E8F0; cursor:pointer; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px')}
                    hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
                  >Annuler</Hoverable>
                </div>
              ) : (
                <Hoverable as="button" onClick={() => startEdit(a)}
                  style={s('border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:11.5px; font-weight:800; padding:8px 12px; border-radius:10px; cursor:pointer')}
                  hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
                >Modifier</Hoverable>
              )}
            </div>
          );
        })}
      </section>
    </div>
  );
}
