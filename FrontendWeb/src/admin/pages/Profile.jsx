import { useState } from 'react';
import { s } from '../../lib/style';
import Hoverable from '../../components/Hoverable';
import FocusableInput from '../../components/FocusableInput';
import { useAuth } from '../../auth/useAuth';
import { authService } from '../../services/authService';
import { roleBadgeStyle } from '../presentation';

const inputStyle = s('width:100%; border:1px solid #E2E8F0; border-radius:14px; padding:12px 15px; font-family:Manrope,sans-serif; font-size:13.5px; font-weight:700; color:#131B2E; outline:0; background:#F2F3FF');
const focusStyle = { borderColor: '#FFCB05', background: '#fff' };

const initialsOf = (name) =>
  (name || '')
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

export default function Profile() {
  const { user } = useAuth();
  const [form, setForm] = useState({ current: '', next: '', confirm: '' });
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    if (form.next !== form.confirm) {
      setError('La confirmation ne correspond pas au nouveau mot de passe.');
      return;
    }
    setPending(true);
    try {
      await authService.changePassword(form.current, form.next);
      setSuccess(true);
      setForm({ current: '', next: '', confirm: '' });
    } catch (err) {
      setError(err?.message || 'Changement de mot de passe impossible.');
    } finally {
      setPending(false);
    }
  };

  return (
    <div style={s('display:flex; flex-direction:column; gap:18px')}>
      <section style={{ ...s('background:linear-gradient(155deg,#0F3875,#002353 78%); border-radius:24px; padding:24px; color:#fff; display:flex; align-items:center; gap:18px'), animation: 'kUp .5s ease both' }}>
        <span style={s('width:60px; height:60px; flex:0 0 60px; border-radius:18px; background:rgba(255,255,255,.12); color:#FFCB05; font-size:19px; font-weight:800; display:flex; align-items:center; justify-content:center')}>{initialsOf(user?.name) || '··'}</span>
        <div>
          <div style={s('font-size:18px; font-weight:800')}>{user?.name || '—'}</div>
          <div style={s('font-size:12.5px; color:rgba(255,255,255,.65); font-weight:600; margin-top:3px')}>{user?.email}</div>
          <div style={{ marginTop: 9 }}><span style={roleBadgeStyle(user?.role)}>{user?.role}</span></div>
        </div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .08s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Périmètre d'accès</div>
        <div style={s('font-size:12.5px; color:#596171; font-weight:600; margin-top:8px; line-height:1.6')}>{user?.scope}</div>
      </section>

      <section style={{ ...s('background:#fff; border-radius:24px; padding:20px 22px; box-shadow:0 10px 28px -22px rgba(15,56,117,.35)'), animation: 'kUp .5s .12s ease both' }}>
        <div style={s('font-size:14.5px; font-weight:800')}>Changer le mot de passe</div>
        <form onSubmit={handleSubmit} style={s('display:flex; flex-direction:column; gap:12px; margin-top:16px; max-width:360px')}>
          <label style={{ display: 'block' }}>
            <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171; margin-bottom:6px')}>MOT DE PASSE ACTUEL</span>
            <FocusableInput type="password" required value={form.current}
              onChange={(e) => setForm((f) => ({ ...f, current: e.target.value }))}
              style={inputStyle} focusStyle={focusStyle} />
          </label>
          <label style={{ display: 'block' }}>
            <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171; margin-bottom:6px')}>NOUVEAU MOT DE PASSE</span>
            <FocusableInput type="password" required value={form.next}
              onChange={(e) => setForm((f) => ({ ...f, next: e.target.value }))}
              style={inputStyle} focusStyle={focusStyle} />
          </label>
          <label style={{ display: 'block' }}>
            <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171; margin-bottom:6px')}>CONFIRMER</span>
            <FocusableInput type="password" required value={form.confirm}
              onChange={(e) => setForm((f) => ({ ...f, confirm: e.target.value }))}
              style={inputStyle} focusStyle={focusStyle} />
          </label>

          {error && (
            <div style={s('font-size:12px; font-weight:600; color:#BA1A1A; background:rgba(186,26,26,.1); border-radius:12px; padding:10px 13px; line-height:1.5')}>{error}</div>
          )}
          {success && (
            <div style={s('font-size:12px; font-weight:600; color:#005236; background:rgba(16,185,129,.12); border-radius:12px; padding:10px 13px; line-height:1.5')}>Mot de passe mis à jour.</div>
          )}

          <Hoverable as="button" type="submit" disabled={pending}
            style={s('border:0; cursor:pointer; background:#002353; color:#fff; font-family:Manrope,sans-serif; font-weight:800; font-size:13px; padding:13px; border-radius:14px; margin-top:4px')}
            hoverStyle={{ background: '#0F3875' }}
          >{pending ? 'Enregistrement…' : 'Enregistrer le nouveau mot de passe'}</Hoverable>
        </form>
      </section>
    </div>
  );
}
