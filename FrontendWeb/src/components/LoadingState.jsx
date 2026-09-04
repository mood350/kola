import { s } from '../lib/style';

export default function LoadingState({ label = 'Chargement…' }) {
  return (
    <div style={s('display:flex; align-items:center; justify-content:center; padding:60px 20px; color:#596171; font-size:13px; font-weight:600; font-family:Manrope,sans-serif')}>
      {label}
    </div>
  );
}
