import { useEffect, useState } from 'react';
import { s } from '../lib/style';
import Hoverable from './Hoverable';

/**
 * Shows the KYC document a reviewer is about to accept or refuse.
 *
 * <p>The file needs a bearer token, so it cannot be an `<img src>` pointing at the API: it is
 * fetched as a Blob and rendered from an object URL, which is revoked when the panel closes.
 *
 * <p>Images are drawn inline — they cannot execute anything. A PDF is offered as a download
 * instead: the server deliberately sends every document as an attachment so that a file supplied
 * by a stranger never renders inside the console's own origin, and embedding it here would undo
 * exactly that.
 */
export default function DocumentViewer({ open, submission, load, onClose }) {
  const [state, setState] = useState({ status: 'idle', url: null, error: null });

  useEffect(() => {
    if (!open || !submission) return undefined;

    let objectUrl = null;
    let cancelled = false;
    setState({ status: 'loading', url: null, error: null });

    load(submission.id)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setState({ status: 'ready', url: objectUrl, error: null });
      })
      .catch((err) => {
        if (!cancelled) setState({ status: 'error', url: null, error: err.message });
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [open, submission, load]);

  useEffect(() => {
    if (!open) return undefined;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  if (!open || !submission) return null;

  const isImage = (submission.contentType || '').startsWith('image/');

  return (
    <div
      onClick={onClose}
      style={s('position:fixed; inset:0; background:rgba(19,27,46,.55); display:flex; align-items:center; justify-content:center; z-index:110; padding:24px')}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Pièce de ${submission.name}`}
        onClick={(e) => e.stopPropagation()}
        style={{ ...s('width:100%; max-width:620px; max-height:90vh; overflow:auto; background:#fff; border-radius:22px; padding:22px; box-shadow:0 30px 70px -30px rgba(15,56,117,.5)'), animation: 'kPop .2s ease both' }}
      >
        <div style={s('display:flex; align-items:flex-start; gap:14px')}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={s('font-size:16px; font-weight:800; color:#131B2E')}>{submission.documentType || 'Pièce justificative'}</div>
            <div style={s('font-size:12px; color:#596171; font-weight:600; margin-top:5px; line-height:1.5')}>
              {submission.name} · {submission.fromTier} → {submission.toTier} · reçue {submission.receivedAt}
              {submission.fileName ? ` · ${submission.fileName}` : ''}
            </div>
          </div>
          <Hoverable as="button" onClick={onClose}
            style={s('border:0; background:#F2F3FF; color:#596171; width:34px; height:34px; border-radius:11px; cursor:pointer; font-size:13px; flex:0 0 34px')}
            hoverStyle={{ background: '#E2E8F0' }}
          >✕</Hoverable>
        </div>

        <div style={s('margin-top:18px; border:1px solid #E2E8F0; border-radius:16px; background:#F2F3FF; min-height:220px; display:flex; align-items:center; justify-content:center; padding:14px')}>
          {state.status === 'loading' && (
            <span style={s('font-size:12.5px; font-weight:700; color:#596171')}>Chargement de la pièce…</span>
          )}
          {state.status === 'error' && (
            <span style={s('font-size:12.5px; font-weight:700; color:#BA1A1A; text-align:center; line-height:1.6')}>{state.error}</span>
          )}
          {state.status === 'ready' && isImage && (
            <img src={state.url} alt={`Pièce de ${submission.name}`}
              style={s('max-width:100%; max-height:60vh; border-radius:10px; display:block')} />
          )}
          {state.status === 'ready' && !isImage && (
            <div style={s('text-align:center')}>
              <div style={s('font-size:12.5px; font-weight:700; color:#596171; line-height:1.6; max-width:340px')}>
                Ce document est un {submission.contentType || 'fichier'}. Il s'ouvre en téléchargement :
                un fichier fourni par un tiers n'est jamais affiché dans la console.
              </div>
              <Hoverable as="a" href={state.url} download={submission.fileName || 'document'}
                style={s('display:inline-block; margin-top:14px; border:0; cursor:pointer; background:#002353; color:#fff; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px 18px; border-radius:12px; text-decoration:none')}
                hoverStyle={{ background: '#0F3875' }}
              >Télécharger la pièce</Hoverable>
            </div>
          )}
        </div>

        <div style={s('font-size:11px; font-weight:700; color:#745B00; background:rgba(255,203,5,.2); padding:8px 12px; border-radius:20px; margin-top:14px; display:inline-block')}>
          ⭑ consultation tracée
        </div>
      </div>
    </div>
  );
}
