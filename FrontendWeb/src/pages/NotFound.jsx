import { Link } from 'react-router-dom';
import { s } from '../lib/style';
import { BRAND_NAME, BRAND_INITIAL } from '../lib/brand';
import Hoverable from '../components/Hoverable';
import { useAuth } from '../auth/useAuth';

export default function NotFound() {
  const { isAuthenticated } = useAuth();

  return (
    <div style={s('min-height:100vh; display:flex; align-items:center; justify-content:center; padding:32px; background:#FAF8FF; font-family:Manrope,Helvetica,sans-serif; color:#131B2E')}>
      <div style={{ ...s('width:100%; max-width:420px; background:#fff; border-radius:26px; padding:34px 32px; box-shadow:0 24px 60px -30px rgba(15,56,117,.45); text-align:center'), animation: 'kUp .5s ease both' }}>
        <div style={s('display:flex; align-items:center; justify-content:center; gap:11px')}>
          <div style={s('width:42px; height:42px; border-radius:14px; background:linear-gradient(145deg,#FFCB05,#FFCB05); display:flex; align-items:center; justify-content:center; font-weight:800; font-size:21px; color:#131B2E; box-shadow:0 8px 20px rgba(255,203,5,.34)')}>{BRAND_INITIAL}</div>
          <div style={s('font-weight:800; font-size:19px; letter-spacing:.02em; color:#131B2E')}>{BRAND_NAME}</div>
        </div>

        <div style={s('font-size:52px; font-weight:800; letter-spacing:-.03em; margin-top:26px; color:#0F3875')}>404</div>
        <div style={s('font-size:16px; font-weight:800; margin-top:8px')}>Page introuvable</div>
        <div style={s('font-size:12.5px; color:#596171; font-weight:500; margin-top:6px; line-height:1.6')}>Cette page n'existe pas ou a été déplacée.</div>

        <Link to={isAuthenticated ? '/admin/dashboard' : '/login'} style={{ textDecoration: 'none' }}>
          <Hoverable as="button"
            style={s('border:0; cursor:pointer; background:#FFCB05; color:#002353; font-family:Manrope,sans-serif; font-weight:800; font-size:13.5px; padding:13px 20px; border-radius:14px; margin-top:26px; transition:transform .18s ease, box-shadow .18s ease')}
            hoverStyle={{ transform: 'translateY(-2px)', boxShadow: '0 14px 26px -12px rgba(255,203,5,.7)' }}
          >{isAuthenticated ? 'Retour au tableau de bord' : 'Retour à la connexion'}</Hoverable>
        </Link>
      </div>
    </div>
  );
}
