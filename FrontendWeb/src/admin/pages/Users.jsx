import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import FocusableInput from '../../components/FocusableInput';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import ConfirmDialog from '../../components/ConfirmDialog';
import DocumentViewer from '../../components/DocumentViewer';
import { useUsers } from '../../hooks/useUsers';
import { useConfirmDialog } from '../../hooks/useConfirmDialog';
import { userStateStyle, tierBadgeStyle } from '../presentation';

const FILTER_NAMES = ['Tous', 'TIER_2', 'TIER_3', 'Litige'];

export default function Users() {
  const {
    loading, error, actionError, actionPending, reload,
    users, kycQueue, filter, setFilter, query, setQuery,
    selectedUser, selectUser, clearSelection,
    unblock, forceCloseVault, approveKyc, rejectKyc, getKycDocumentFile,
  } = useUsers();
  const { request, dialogProps } = useConfirmDialog();
  const [viewing, setViewing] = useState(null);

  if (loading) return <LoadingState label="Chargement des comptes utilisateurs…" />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      {actionError && <ErrorState message={actionError} />}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; gap:12px; flex-wrap:wrap')}>
          <div style={s('font-size:14.5px; font-weight:800; margin-right:auto')}>Comptes utilisateurs</div>
          <FocusableInput
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Rechercher nom ou téléphone…"
            style={s('border:1px solid #E2E8F0; border-radius:12px; padding:9px 13px; font-family:Manrope,sans-serif; font-size:12px; font-weight:600; color:#131B2E; outline:0; background:#F2F3FF; width:200px')}
            focusStyle={{ borderColor: '#FFCB05', background: '#fff' }}
          />
          {FILTER_NAMES.map((n) => (
            <button key={n} onClick={() => setFilter(n)} style={{
              border: '1px solid ' + (n === filter ? 'transparent' : '#E2E8F0'),
              background: n === filter ? '#002353' : '#F2F3FF', color: n === filter ? '#FFCB05' : '#596171',
              fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 800, padding: '9px 14px', borderRadius: 12, cursor: 'pointer',
            }}>{n}</button>
          ))}
        </div>
        {query && (
          <div style={s('font-size:11.5px; font-weight:600; color:#596171; margin-top:10px')}>
            {users.length} résultat{users.length > 1 ? 's' : ''} pour « {query} »
          </div>
        )}
        <div style={s('display:grid; grid-template-columns:1.6fr .9fr .9fr .8fr 1fr; gap:8px; padding:15px 8px 9px; font-size:10px; font-weight:800; letter-spacing:.09em; color:#596171; border-bottom:1px solid #E2E8F0')}>
          <div>UTILISATEUR</div><div>PALIER KYC</div><div>SCORE</div><div>ANCIENNETÉ</div><div>STATUT</div>
        </div>
        {users.map((u) => (
          <Hoverable key={u.id} as="button" onClick={() => selectUser(u.id)}
            style={s('display:grid; grid-template-columns:1.6fr .9fr .9fr .8fr 1fr; gap:8px; align-items:center; width:100%; text-align:left; border:0; cursor:pointer; background:transparent; padding:13px 8px; border-bottom:1px solid #E2E8F0; font-family:Manrope,sans-serif; transition:background .16s ease')}
            hoverStyle={{ background: '#F2F3FF' }}
          >
            <div style={s('display:flex; align-items:center; gap:11px; min-width:0')}>
              <span style={s('width:34px; height:34px; flex:0 0 34px; border-radius:11px; background:linear-gradient(145deg,#0F3875,#002353); color:#FFCB05; font-size:11px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{u.initials}</span>
              <span style={{ minWidth: 0 }}><span style={s('display:block; font-size:13px; font-weight:700; color:#131B2E')}>{u.name}</span><span style={s('display:block; font-size:11px; color:#596171; font-weight:600; margin-top:2px')}>{u.phone}</span></span>
            </div>
            <div><span style={tierBadgeStyle}>{u.tier}</span></div>
            <div style={s('font-size:13px; font-weight:800')}>{u.score}</div>
            <div style={s('font-size:12px; font-weight:600; color:#596171')}>{u.age}</div>
            <div><span style={userStateStyle(u.state)}>{u.state}</span></div>
          </Hoverable>
        ))}
      </section>

      {selectedUser && (
        <section style={{ ...s('background:#fff; border-radius:24px; padding:22px 24px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .45s ease both' }}>
          <div style={s('display:flex; align-items:center; gap:16px')}>
            <span style={s('width:52px; height:52px; flex:0 0 52px; border-radius:16px; background:linear-gradient(145deg,#0F3875,#002353); color:#FFCB05; font-size:16px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{selectedUser.initials}</span>
            <div style={{ flex: 1 }}>
              <div style={s('font-size:16.5px; font-weight:800')}>{selectedUser.name}</div>
              <div style={s('font-size:12px; color:#596171; font-weight:600; margin-top:3px')}>{selectedUser.phone} · {selectedUser.tier} · membre depuis {selectedUser.age}</div>
            </div>
            <Hoverable as="button" onClick={clearSelection}
              style={s('border:0; background:#F2F3FF; color:#596171; width:34px; height:34px; border-radius:11px; cursor:pointer; font-size:13px')}
              hoverStyle={{ background: '#E2E8F0' }}
            >✕</Hoverable>
          </div>
          <div style={s('display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:14px; margin-top:20px')}>
            <div style={s('background:#F2F3FF; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#596171; letter-spacing:.09em')}>SCORE ACTUEL</div><div style={s('font-size:19px; font-weight:800; margin-top:7px')}>{selectedUser.score} / 100</div></div>
            <div style={s('background:#F2F3FF; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#596171; letter-spacing:.09em')}>COFFRES ACTIFS</div><div style={s('font-size:19px; font-weight:800; margin-top:7px')}>{selectedUser.vaults}</div></div>
            <div style={s('background:#F2F3FF; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#596171; letter-spacing:.09em')}>PRÊT EN COURS</div><div style={s('font-size:19px; font-weight:800; margin-top:7px')}>{selectedUser.loan}</div></div>
            <div style={s('background:#F2F3FF; border-radius:16px; padding:14px')}><div style={s('font-size:10px; font-weight:800; color:#596171; letter-spacing:.09em')}>STATUT</div><div style={{ marginTop: 8 }}><span style={userStateStyle(selectedUser.state)}>{selectedUser.state}</span></div></div>
          </div>
          <div style={s('display:flex; gap:9px; margin-top:18px; flex-wrap:wrap')}>
            <Hoverable as="button" disabled={actionPending} onClick={() => request({
                title: 'Débloquer ce compte ?',
                message: `${selectedUser.name} retrouvera un accès immédiat à son compte et à ses coffres.`,
                confirmLabel: 'Débloquer',
              }, () => unblock(selectedUser.id))}
              style={s('border:0; cursor:pointer; background:#002353; color:#fff; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 16px; border-radius:12px')}
              hoverStyle={{ background: '#0F3875' }}
            >Débloquer le compte</Hoverable>
            <Hoverable as="button" disabled={actionPending || selectedUser.vaults === 0} onClick={() => request({
                title: 'Forcer la fermeture d\'un coffre ?',
                message: `Un coffre actif de ${selectedUser.name} sera clôturé de force. Cette action est irréversible et journalisée.`,
                confirmLabel: 'Fermer le coffre',
                danger: true,
              }, () => forceCloseVault(selectedUser.id))}
              style={s('border:1px solid #E2E8F0; cursor:pointer; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 16px; border-radius:12px')}
              hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
            >Forcer fermeture d'un coffre</Hoverable>
            <span style={s('margin-left:auto; display:flex; align-items:center; gap:6px; font-size:11px; font-weight:800; color:#745B00; background:rgba(255,203,5,.2); padding:8px 12px; border-radius:20px')}>⭑ action tracée · confirmation requise</span>
          </div>
        </section>
      )}

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .1s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:space-between')}>
          <div style={s('font-size:14.5px; font-weight:800')}>File de validation KYC (TIER_2 / TIER_3)</div>
          <span style={s('font-size:10.5px; font-weight:800; padding:6px 11px; border-radius:20px; background:rgba(255,203,5,.2); color:#745B00')}>{kycQueue.length} en attente</span>
        </div>
        <div style={s('display:flex; flex-direction:column; gap:11px; margin-top:15px')}>
          {kycQueue.length === 0 && (
            <div style={s('font-size:12.5px; color:#596171; font-weight:600; padding:8px 4px')}>Aucune demande en attente.</div>
          )}
          {kycQueue.map((k) => (
            <div key={k.id} style={s('display:flex; align-items:center; gap:16px; border:1px solid #E2E8F0; border-radius:18px; padding:14px 16px')}>
              <Hoverable as="button" onClick={() => setViewing(k)}
                style={s("width:64px; height:44px; flex:0 0 64px; border-radius:10px; border:0; cursor:pointer; background:repeating-linear-gradient(45deg,#E2E7FF 0 5px,#F2F3FF 5px 10px); display:flex; align-items:center; justify-content:center; font-family:'JetBrains Mono',monospace; font-size:8.5px; color:#596171")}
                hoverStyle={{ filter: 'brightness(.95)' }}
              >voir</Hoverable>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={s('font-size:13px; font-weight:700')}>{k.name}</div>
                <div style={s('font-size:11px; color:#596171; font-weight:600; margin-top:3px')}>{k.documentType || 'Pièce justificative'} · {k.fromTier} → {k.toTier} · reçue {k.receivedAt}</div>
              </div>
              <Hoverable as="button" onClick={() => setViewing(k)}
                style={s('border:1px solid #E2E8F0; cursor:pointer; background:#fff; color:#131B2E; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                hoverStyle={{ borderColor: '#FFCB05' }}
              >Voir la pièce</Hoverable>
              <Hoverable as="button" disabled={actionPending} onClick={() => request({
                  title: 'Approuver cette demande KYC ?',
                  message: `${k.name} passera du palier ${k.fromTier} au palier ${k.toTier}. Le client en est informé.`,
                  confirmLabel: 'Approuver',
                }, () => approveKyc(k.id))}
                style={s('border:0; cursor:pointer; background:rgba(16,185,129,.12); color:#005236; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                hoverStyle={{ background: 'rgba(16,185,129,.18)' }}
              >Approuver</Hoverable>
              <Hoverable as="button" disabled={actionPending} onClick={() => request({
                  title: 'Rejeter cette demande KYC ?',
                  message: `Le motif ci-dessous sera envoyé à ${k.name}, qui pourra renvoyer une pièce corrigée.`,
                  confirmLabel: 'Rejeter',
                  danger: true,
                  input: {
                    label: 'MOTIF DU REJET',
                    placeholder: 'Ex. : photo illisible, document expiré, nom différent du compte…',
                    required: true,
                    requiredMessage: 'Le motif est obligatoire : il est envoyé au client.',
                    maxLength: 300,
                  },
                }, (reason) => rejectKyc(k.id, reason))}
                style={s('border:0; cursor:pointer; background:rgba(186,26,26,.1); color:#BA1A1A; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:9px 14px; border-radius:11px')}
                hoverStyle={{ background: 'rgba(186,26,26,.16)' }}
              >Rejeter</Hoverable>
            </div>
          ))}
        </div>
      </section>

      <ConfirmDialog {...dialogProps} />
      <DocumentViewer
        open={Boolean(viewing)}
        submission={viewing}
        load={getKycDocumentFile}
        onClose={() => setViewing(null)}
      />
    </div>
  );
}
