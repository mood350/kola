import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useSession } from '../lib/session';
import { ErrorNotice } from '../components/ui';

/**
 * Connexion. Un seul message pour « compte inconnu » et « mot de passe faux » :
 * le serveur répond pareil, en temps égal, et l'écran ne doit pas trahir quelle
 * adresse appartient au personnel.
 */
export default function Login() {
  const { status, login } = useSession();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  if (status === 'authenticated') return <Navigate to={location.state?.from || '/'} replace />;

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setPending(true);
    try {
      await login(email.trim(), password);
      navigate(location.state?.from || '/', { replace: true });
    } catch (err) {
      setError(messageFor(err));
      setPending(false);
    }
  };

  return (
    <div className="login">
      <form className="login-box" onSubmit={submit} noValidate>
        <div className="brand">
          <div className="brand-mark"><img src="/kola-logo-transparent.png" alt="" /></div>
          <div>
            <div className="brand-name">KOLA</div>
            <div className="brand-sub">Administration</div>
          </div>
        </div>
        <h1>Connexion</h1>
        <p className="lead">Accès réservé aux comptes du back-office.</p>
        <ErrorNotice message={error} />
        <div className="field">
          <label htmlFor="email">Adresse e-mail</label>
          <input id="email" className="input" type="email" autoComplete="username" required
            value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="password">Mot de passe</label>
          <input id="password" className="input" type="password" autoComplete="current-password" required
            value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn" type="submit" disabled={pending || !email || !password}>
          {pending ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
    </div>
  );
}

function messageFor(error) {
  switch (error?.status) {
    case 401:
      return 'Adresse e-mail ou mot de passe incorrect.';
    case 423:
      return 'Ce compte administrateur est désactivé.';
    case 400:
      return 'Saisissez une adresse e-mail valide et votre mot de passe.';
    default:
      return error?.message || 'Connexion impossible.';
  }
}
