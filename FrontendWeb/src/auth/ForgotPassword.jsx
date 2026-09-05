import { useState } from 'react';
import { Link } from 'react-router-dom';
import { s } from '../lib/style';
import { BRAND_NAME, BRAND_INITIAL } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import FocusableInput from '../components/FocusableInput';
import { authService } from '../services/authService';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [pending, setPending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setPending(true);
    try {
      await authService.requestPasswordReset(email);
      setSent(true);
    } catch (err) {
      setError(err?.message || 'Envoi impossible pour le moment.');
    } finally {
      setPending(false);
    }
  };

  return (
    <div style={s('min-height:100vh; display:flex; align-items:center; justify-content:center; padding:32px; background:#FAF8FF; font-family:Manrope,Helvetica,sans-serif; color:#131B2E')}>
      <div style={{ ...s('width:100%; max-width:420px; background:#fff; border-radius:26px; padding:34px 32px; box-shadow:0 24px 60px -30px rgba(15,56,117,.45)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; gap:11px')}>
          <div style={s('width:42px; height:42px; border-radius:14px; background:linear-gradient(145deg,#FFCB05,#FFCB05); display:flex; align-items:center; justify-content:center; font-weight:800; font-size:21px; color:#131B2E; box-shadow:0 8px 20px rgba(255,203,5,.34)')}>{BRAND_INITIAL}</div>
          <div>
            <div style={s('font-weight:800; font-size:19px; letter-spacing:.02em; line-height:1; color:#131B2E')}>{BRAND_NAME}</div>
            <div style={s('color:#596171; font-size:9.5px; font-weight:700; letter-spacing:.14em; margin-top:3px')}>BACK-OFFICE ADMIN</div>
          </div>
        </div>

        <div style={s('margin-top:28px')}>
          <div style={s('font-size:21px; font-weight:800; letter-spacing:-.01em')}>Mot de passe oublié</div>
          <div style={s('font-size:12.5px; color:#596171; font-weight:500; margin-top:4px')}>Recevez un lien de réinitialisation par e-mail.</div>
        </div>

        {sent ? (
          <div style={s('font-size:12.5px; font-weight:600; color:#005236; background:rgba(16,185,129,.12); border-radius:12px; padding:14px 15px; line-height:1.6; margin-top:22px')}>
            Si un compte admin correspond à {email || 'cette adresse'}, un lien de réinitialisation vient d'être envoyé.
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={s('display:flex; flex-direction:column; gap:12px; margin-top:22px')}>
            <label style={{ display: 'block' }}>
              <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171; margin-bottom:6px')}>E-MAIL ADMIN</span>
              <FocusableInput
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="sena.ametepe@dogaa.io"
                style={s('width:100%; border:1px solid #E2E8F0; border-radius:14px; padding:13px 15px; font-family:Manrope,sans-serif; font-size:14px; font-weight:700; color:#131B2E; outline:0; background:#F2F3FF')}
                focusStyle={{ borderColor: '#FFCB05', background: '#fff' }}
              />
            </label>

            {error && (
              <div style={s('font-size:12px; font-weight:600; color:#BA1A1A; background:rgba(186,26,26,.1); border-radius:12px; padding:10px 13px; line-height:1.5')}>{error}</div>
            )}

            <Hoverable as="button" type="submit" disabled={pending}
              style={s('border:0; cursor:pointer; background:#FFCB05; color:#002353; font-family:Manrope,sans-serif; font-weight:800; font-size:14px; padding:14px; border-radius:14px; margin-top:4px; transition:transform .18s ease, box-shadow .18s ease')}
              hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 14px 26px -12px rgba(255,203,5,.7)' }}
            >{pending ? 'Envoi…' : 'Envoyer le lien'}</Hoverable>
          </form>
        )}

        <Link to="/login" style={{ textDecoration: 'none' }}>
          <div style={s('font-size:12px; font-weight:700; color:#131B2E; text-align:center; margin-top:22px')}>← Retour à la connexion</div>
        </Link>
      </div>
    </div>
  );
}
