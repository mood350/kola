import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import { useSession } from '../lib/session';
import { Card, ErrorNotice, PageHead, Tabs } from '../components/ui';
import FeesEditor from './settings/FeesEditor';
import LadderEditor from './settings/LadderEditor';

/** Minimum imposé par `ChangePasswordRequest` (`@Size(min = 10)`). */
const MIN_LENGTH = 10;

/**
 * Paramètres : « Mon compte » pour tous les rôles ; la grille de frais (module « config », Super-admin)
 * et le barème de crédit (module « credit », modifiable par le seul Super-admin) pour ceux qui y ont droit.
 * L'onglet est dans l'adresse (`?onglet=`). Les droits réels sont ceux du serveur : ici on ne fait que
 * ne pas montrer ce qui serait refusé.
 */
export default function Settings() {
  const { can } = useSession();
  const [params, setParams] = useSearchParams();

  const tabs = [
    { key: 'compte', label: 'Mon compte' },
    ...(can('config') ? [{ key: 'frais', label: 'Frais' }] : []),
    ...(can('credit') ? [{ key: 'bareme', label: 'Barème de crédit' }] : []),
  ];
  const requested = params.get('onglet');
  const current = tabs.some((tab) => tab.key === requested) ? requested : 'compte';

  const select = (key) => setParams((previous) => {
    const next = new URLSearchParams(previous);
    next.set('onglet', key);
    return next;
  });

  const content = {
    compte: <Account />,
    frais: <FeesEditor />,
    bareme: <LadderEditor editable={can('config')} />,
  }[current];

  return (
    <>
      <PageHead title="Paramètres" subtitle={tabs.length > 1 ? 'Votre compte et la configuration du produit.' : 'Votre compte.'} />
      {tabs.length > 1 ? (
        <Tabs tabs={tabs} value={current} onChange={select} label="Rubriques des paramètres">{content}</Tabs>
      ) : content}
    </>
  );
}

/** Le compte de l'administrateur connecté : identité en lecture, mot de passe modifiable. */
function Account() {
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
          <ErrorNotice message={error} title="Le mot de passe n'a pas été modifié." />
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
  );
}
