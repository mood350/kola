import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { s } from '../lib/style';
import { BRAND_NAME, BRAND_INITIAL } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import FocusableInput from '../components/FocusableInput';
import { useAuth } from './AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from?.pathname || '/admin/dashboard';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setPending(true);
    try {
      await login(email, password);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err?.message || 'Connexion impossible. Essayez l’accès rapide ci-dessous.');
    } finally {
      setPending(false);
    }
  };

  const quickLogin = async () => {
    setError('');
    setPending(true);
    try {
      await login('sena.ametepe@dogaa.io', 'demo');
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err?.message || 'Connexion impossible.');
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
          <div style={s('font-size:21px; font-weight:800; letter-spacing:-.01em')}>Connexion</div>
          <div style={s('font-size:12.5px; color:#596171; font-weight:500; margin-top:4px')}>Accédez au back-office {BRAND_NAME}.</div>
        </div>

        <form onSubmit={handleSubmit} style={s('display:flex; flex-direction:column; gap:12px; margin-top:22px')}>
          <label style={{ display: 'block' }}>
            <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171; margin-bottom:6px')}>E-MAIL ADMIN</span>
            <FocusableInput
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="sena.ametepe@dogaa.io"
              style={s('width:100%; border:1px solid #E2E8F0; border-radius:14px; padding:13px 15px; font-family:Manrope,sans-serif; font-size:14px; font-weight:700; color:#131B2E; outline:0; background:#F2F3FF')}
              focusStyle={{ borderColor: '#FFCB05', background: '#fff' }}
            />
          </label>
          <label style={{ display: 'block' }}>
            <span style={s('display:flex; justify-content:space-between; align-items:center; font-size:11px; font-weight:800; letter-spacing:.1em; color:#596171; margin-bottom:6px')}>
              MOT DE PASSE
              <Link to="/forgot-password" style={s('font-size:10.5px; font-weight:700; letter-spacing:0; color:#131B2E; text-decoration:none')}>Oublié ?</Link>
            </span>
            <FocusableInput
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
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
          >{pending ? 'Connexion…' : 'Se connecter'}</Hoverable>
        </form>

        <div style={s('display:flex; align-items:center; gap:10px; margin-top:26px')}>
          <div style={s('flex:1; height:1px; background:#E2E8F0')} />
          <span style={s('font-size:11px; font-weight:700; color:#596171')}>accès rapide de démonstration</span>
          <div style={s('flex:1; height:1px; background:#E2E8F0')} />
        </div>

        <Hoverable as="button" type="button" disabled={pending} onClick={quickLogin}
          style={s('display:flex; align-items:center; gap:12px; width:100%; text-align:left; border:1px solid #E2E8F0; background:#F2F3FF; border-radius:14px; padding:12px 14px; cursor:pointer; font-family:Manrope,sans-serif; margin-top:16px')}
          hoverStyle={{ borderColor: '#FFCB05', background: '#fff' }}
        >
          <span style={s('width:34px; height:34px; flex:0 0 34px; border-radius:11px; background:linear-gradient(145deg,#0F3875,#002353); color:#FFCB05; font-size:13px; font-weight:800; display:flex; align-items:center; justify-content:center')}>SA</span>
          <span>
            <span style={s('display:block; font-size:13px; font-weight:800; color:#131B2E')}>Sena Amétépé</span>
            <span style={s('display:block; font-size:11px; color:#596171; font-weight:600')}>Super-admin</span>
          </span>
        </Hoverable>

        <div style={s('font-size:10.5px; color:#596171; font-weight:600; text-align:center; margin-top:22px; line-height:1.5')}>Connexion chiffrée · sessions et appareils actifs journalisés</div>
      </div>
    </div>
  );
}
