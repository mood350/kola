import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { s } from '../lib/style';
import { BRAND_NAME, BRAND_INITIAL } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import FocusableInput from '../components/FocusableInput';
import { findAccountByPhone } from './directory';

export default function Login() {
  const navigate = useNavigate();
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    const account = findAccountByPhone(phone);
    if (!account) {
      setError('Compte de démonstration introuvable pour ce numéro. Essayez l’un des accès rapides ci-dessous.');
      return;
    }
    setError('');
    navigate(account.dest);
  };

  const quickLogin = (dest) => navigate(dest);

  return (
    <div style={s('min-height:100vh; display:flex; align-items:center; justify-content:center; padding:32px; background:linear-gradient(160deg,#E8ECF4 0%,#F5F7FB 55%,#EEF1F8 100%); font-family:Manrope,Helvetica,sans-serif; color:#0A1F5C')}>
      <div style={{ ...s('width:100%; max-width:420px; background:#fff; border-radius:26px; padding:34px 32px; box-shadow:0 24px 60px -30px rgba(10,31,92,.45)'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; gap:11px')}>
          <div style={s('width:42px; height:42px; border-radius:14px; background:linear-gradient(145deg,#FDB813,#F5B301); display:flex; align-items:center; justify-content:center; font-weight:800; font-size:21px; color:#0A1F5C; box-shadow:0 8px 20px rgba(245,179,1,.34)')}>{BRAND_INITIAL}</div>
          <div>
            <div style={s('font-weight:800; font-size:19px; letter-spacing:.02em; line-height:1; color:#0A1F5C')}>{BRAND_NAME}</div>
            <div style={s('color:#8894B2; font-size:10px; font-weight:700; letter-spacing:.16em; margin-top:3px')}>UEMOA · FINTECH</div>
          </div>
        </div>

        <div style={s('margin-top:28px')}>
          <div style={s('font-size:21px; font-weight:800; letter-spacing:-.01em')}>Connexion</div>
          <div style={s('font-size:12.5px; color:#7C8AAB; font-weight:500; margin-top:4px')}>Accédez à votre espace {BRAND_NAME}.</div>
        </div>

        <form onSubmit={handleSubmit} style={s('display:flex; flex-direction:column; gap:12px; margin-top:22px')}>
          <label style={{ display: 'block' }}>
            <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2; margin-bottom:6px')}>NUMÉRO DE TÉLÉPHONE</span>
            <FocusableInput
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+228 90 12 34 56"
              style={s('width:100%; border:1px solid rgba(10,31,92,.1); border-radius:14px; padding:13px 15px; font-family:Manrope,sans-serif; font-size:14px; font-weight:700; color:#0A1F5C; outline:0; background:#F9FBFE')}
              focusStyle={{ borderColor: '#F5B301', background: '#fff' }}
            />
          </label>
          <label style={{ display: 'block' }}>
            <span style={s('display:block; font-size:11px; font-weight:800; letter-spacing:.1em; color:#8894B2; margin-bottom:6px')}>MOT DE PASSE</span>
            <FocusableInput
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={s('width:100%; border:1px solid rgba(10,31,92,.1); border-radius:14px; padding:13px 15px; font-family:Manrope,sans-serif; font-size:14px; font-weight:700; color:#0A1F5C; outline:0; background:#F9FBFE')}
              focusStyle={{ borderColor: '#F5B301', background: '#fff' }}
            />
          </label>

          {error && (
            <div style={s('font-size:12px; font-weight:600; color:#B3262F; background:#FDEBEC; border-radius:12px; padding:10px 13px; line-height:1.5')}>{error}</div>
          )}

          <Hoverable as="button" type="submit"
            style={s('border:0; cursor:pointer; background:#F5B301; color:#0A1F5C; font-family:Manrope,sans-serif; font-weight:800; font-size:14px; padding:14px; border-radius:14px; margin-top:4px; transition:transform .18s ease, box-shadow .18s ease')}
            hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 14px 26px -12px rgba(245,179,1,.7)' }}
          >Se connecter</Hoverable>
        </form>

        <div style={s('display:flex; align-items:center; gap:10px; margin-top:26px')}>
          <div style={s('flex:1; height:1px; background:#EDF1F8')} />
          <span style={s('font-size:11px; font-weight:700; color:#8894B2')}>accès rapide de démonstration</span>
          <div style={s('flex:1; height:1px; background:#EDF1F8')} />
        </div>

        <div style={s('display:flex; flex-direction:column; gap:9px; margin-top:16px')}>
          <Hoverable as="button" type="button" onClick={() => quickLogin('/app/dashboard')}
            style={s('display:flex; align-items:center; gap:12px; width:100%; text-align:left; border:1px solid rgba(10,31,92,.1); background:#F7F9FD; border-radius:14px; padding:12px 14px; cursor:pointer; font-family:Manrope,sans-serif')}
            hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
          >
            <span style={s('width:34px; height:34px; flex:0 0 34px; border-radius:11px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:13px; font-weight:800; display:flex; align-items:center; justify-content:center')}>AK</span>
            <span>
              <span style={s('display:block; font-size:13px; font-weight:800; color:#0A1F5C')}>Espace client</span>
              <span style={s('display:block; font-size:11px; color:#8894B2; font-weight:600')}>Aïcha Kodjo · TIER_2</span>
            </span>
          </Hoverable>
          <Hoverable as="button" type="button" onClick={() => quickLogin('/admin/dashboard')}
            style={s('display:flex; align-items:center; gap:12px; width:100%; text-align:left; border:1px solid rgba(10,31,92,.1); background:#F7F9FD; border-radius:14px; padding:12px 14px; cursor:pointer; font-family:Manrope,sans-serif')}
            hoverStyle={{ borderColor: '#F5B301', background: '#fff' }}
          >
            <span style={s('width:34px; height:34px; flex:0 0 34px; border-radius:11px; background:linear-gradient(145deg,#0F2A6B,#0A1F5C); color:#F5B301; font-size:13px; font-weight:800; display:flex; align-items:center; justify-content:center')}>SA</span>
            <span>
              <span style={s('display:block; font-size:13px; font-weight:800; color:#0A1F5C')}>Back-office admin</span>
              <span style={s('display:block; font-size:11px; color:#8894B2; font-weight:600')}>Sena Amétépé · Super-admin</span>
            </span>
          </Hoverable>
        </div>

        <div style={s('font-size:10.5px; color:#8894B2; font-weight:600; text-align:center; margin-top:22px; line-height:1.5')}>Connexion chiffrée · sessions et appareils actifs journalisés</div>
      </div>
    </div>
  );
}
