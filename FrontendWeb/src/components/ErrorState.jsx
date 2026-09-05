import { s } from '../lib/style';
import Hoverable from './Hoverable';

export default function ErrorState({ message, onRetry }) {
  return (
    <div style={s('background:rgba(186,26,26,.1); border:1px solid rgba(186,26,26,.2); border-radius:16px; padding:18px 20px; color:#BA1A1A; font-size:12.5px; font-weight:600; font-family:Manrope,sans-serif; display:flex; align-items:center; gap:14px; flex-wrap:wrap')}>
      <span style={{ flex: 1 }}>{message || 'Une erreur est survenue.'}</span>
      {onRetry && (
        <Hoverable
          as="button"
          onClick={onRetry}
          style={s('border:1px solid rgba(179,38,47,.35); background:#fff; color:#BA1A1A; font-family:Manrope,sans-serif; font-size:12px; font-weight:800; padding:8px 14px; border-radius:10px; cursor:pointer')}
          hoverStyle={{ background: 'rgba(186,26,26,.1)' }}
        >
          Réessayer
        </Hoverable>
      )}
    </div>
  );
}
