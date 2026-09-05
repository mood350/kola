import { useEffect } from 'react';
import { s } from '../lib/style';
import Hoverable from './Hoverable';

export default function ConfirmDialog({
  open, title, message, confirmLabel = 'Confirmer', cancelLabel = 'Annuler',
  danger = false, pending = false, onConfirm, onCancel,
}) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onCancel();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onCancel]);

  if (!open) return null;

  return (
    <div
      onClick={onCancel}
      style={s('position:fixed; inset:0; background:rgba(19,27,46,.45); display:flex; align-items:center; justify-content:center; z-index:100; padding:20px')}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        onClick={(e) => e.stopPropagation()}
        style={{ ...s('width:100%; max-width:400px; background:#fff; border-radius:22px; padding:24px; box-shadow:0 30px 70px -30px rgba(15,56,117,.5)'), animation: 'kPop .2s ease both' }}
      >
        <div id="confirm-dialog-title" style={s('font-size:16px; font-weight:800; color:#131B2E')}>{title}</div>
        {message && (
          <div style={s('font-size:12.5px; color:#596171; font-weight:600; margin-top:9px; line-height:1.6')}>{message}</div>
        )}
        <div style={s('display:flex; gap:9px; margin-top:22px')}>
          <Hoverable as="button" onClick={onCancel} disabled={pending}
            style={s('flex:1; border:1px solid #E2E8F0; background:#F2F3FF; color:#131B2E; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px; border-radius:12px; cursor:pointer')}
            hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
          >{cancelLabel}</Hoverable>
          <Hoverable as="button" onClick={onConfirm} disabled={pending} autoFocus
            style={s(`flex:1; border:0; cursor:pointer; background:${danger ? '#BA1A1A' : '#002353'}; color:#fff; font-family:Manrope,sans-serif; font-size:12.5px; font-weight:800; padding:11px; border-radius:12px`)}
            hoverStyle={{ filter: 'brightness(1.1)' }}
          >{pending ? '…' : confirmLabel}</Hoverable>
        </div>
      </div>
    </div>
  );
}
