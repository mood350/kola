import { useState } from 'react';
import { api } from '../lib/api';
import { useSession } from '../lib/session';
import { Card, ErrorNotice, PageHead } from '../components/ui';

/** Minimum imposé par `ChangePasswordRequest` (`@Size(min = 10)`). */
const MIN_LENGTH = 10;

/** Le compte de l'administrateur connecté : identité en lecture, mot de passe modifiable. */
export default function Account() {
  const { user } = useSession();
  const [form, setForm] = useState({ current: '', next: '', confirm: '' });
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const set = (key) => (e) => { setForm({ ...form, [key]: e.target.value }); setDone(false); };
  const mismatch = form.confirm && form.next !== form.confirm;
  const ready = form.current && form.next.length >= MIN_LENGTH && form.next === form.confirm;

  const submit = async (e) => {
    e.preventDefault();
    setPending(true);
    setError('');
    try {
      await api.post('/auth/change-password', { currentPassword: form.current, newPassword: form.next });
      setForm({ current: '', next: '', confirm: '' });
      setDone(true);
    } catch (err) {
      setError(Object.values(err.fieldErrors || {})[0] || err.message);
    } finally {
      setPending(false);
    }
  };

  return (
    <>
      <PageHead title="Mon compte" />
      <div className="stack">
        <Card title="Identité">
          <dl className="fields">
            <div><dt>Nom</dt><dd>{user?.name}</dd></div>
            <div><dt>E-mail</dt><dd>{user?.email}</dd></div>
            <div><dt>Rôle</dt><dd>{user?.role}</dd></div>
          </dl>
        </Card>

        <Card title="Mot de passe">
          <form onSubmit={submit} className="narrow" noValidate>
            {done && <div className="notice success" role="status">Mot de passe modifié.</div>}
            <ErrorNotice message={error} />
            <div className="field">
              <label htmlFor="current">Mot de passe actuel</label>
              <input id="current" className="input" type="password" autoComplete="current-password"
                value={form.current} onChange={set('current')} />
            </div>
            <div className="field">
              <label htmlFor="next">Nouveau mot de passe ({MIN_LENGTH} caractères minimum)</label>
              <input id="next" className="input" type="password" autoComplete="new-password"
                value={form.next} onChange={set('next')} />
            </div>
            <div className="field">
              <label htmlFor="confirm">Confirmation</label>
              <input id="confirm" className="input" type="password" autoComplete="new-password"
                value={form.confirm} onChange={set('confirm')} aria-invalid={Boolean(mismatch)} />
              {mismatch && <span className="muted">Les deux saisies diffèrent.</span>}
            </div>
            <button className="btn" type="submit" disabled={pending || !ready}>
              {pending ? 'Modification…' : 'Changer le mot de passe'}
            </button>
            <p className="muted">
              Les sessions déjà ouvertes ailleurs restent valables jusqu&apos;à leur expiration, au plus 8 heures.
            </p>
          </form>
        </Card>
      </div>
    </>
  );
}
