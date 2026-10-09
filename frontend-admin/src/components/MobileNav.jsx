import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { usePresentation } from '../lib/usePresentation';
import { IconLogout, IconMore } from './icons';

/** Au-delà de ce nombre de rubriques, les dernières passent sous « Plus » : une barre du bas lisible en tient cinq au plus. */
const MAX_VISIBLE = 5;

/**
 * Navigation du bas, sur tablette et mobile. Les quatre premières rubriques sont directes ; le reste
 * (et la déconnexion) s'ouvre dans « Plus ». Échap referme la feuille et rend le focus au bouton.
 */
export default function MobileNav({ items, kycPending, user, onLogout }) {
  const { pathname } = useLocation();
  const [open, setOpen] = useState(false);
  const [closing, setClosing] = useState(false);
  const button = useRef(null);

  const overflowing = items.length > MAX_VISIBLE;
  const direct = overflowing ? items.slice(0, MAX_VISIBLE - 1) : items;
  const more = overflowing ? items.slice(MAX_VISIBLE - 1) : [];
  const moreCurrent = more.some((item) => (item.end ? pathname === item.to : pathname.startsWith(item.to)));

  // La sortie est jouée par la feuille elle-même ; elle nous rend la main quand elle a fini.
  const onClosed = () => {
    setOpen(false);
    setClosing(false);
    button.current?.focus();
  };

  return (
    <>
      {open && (
        <MoreSheet closing={closing} onClosed={onClosed}>
          {more.map(({ to, label, Icon }) => (
            <NavLink key={to} to={to} onClick={() => setOpen(false)}><Icon /> {label}</NavLink>
          ))}
          <hr />
          <div className="muted" style={{ padding: '6px 14px 2px', fontSize: 12 }}>{user?.name} · {user?.role}</div>
          <button type="button" onClick={onLogout}><IconLogout /> Se déconnecter</button>
        </MoreSheet>
      )}

      <nav className="bottomnav" aria-label="Navigation principale">
        {direct.map(({ to, label, end, Icon, pending }) => (
          <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)}>
            <Icon size={22} />
            {label}
            {pending && kycPending > 0 && <span className="pill" aria-label={`${kycPending} en attente`}>{kycPending}</span>}
          </NavLink>
        ))}
        {overflowing && (
          <button type="button" ref={button} className={moreCurrent ? 'current' : undefined}
            aria-expanded={open} aria-haspopup="dialog"
            onClick={() => (open ? setClosing(true) : setOpen(true))}>
            <IconMore size={22} />
            Plus
          </button>
        )}
      </nav>
    </>
  );
}

/** Distance en plus à parcourir pour disparaître derrière la barre du bas (64 px + marge de sécurité). */
const TRAVEL = 80;

/**
 * La feuille « Plus » : elle monte de la barre dont elle est issue et y redescend. Pas de voile, car
 * elle ne bloque rien : une surface parallèle se sépare par la matière, pas en assombrissant la page.
 * Échap la referme ; la glisser vers le bas aussi.
 */
function MoreSheet({ closing, onClosed, children }) {
  const { sheet, close, drag } = usePresentation({ kind: 'y', travel: TRAVEL, onClosed });

  useEffect(() => { sheet.current?.querySelector('a, button')?.focus(); }, [sheet]);
  useEffect(() => { if (closing) close(); }, [closing, close]);

  return (
    <div className="more-sheet" ref={sheet} role="dialog" aria-label="Autres rubriques" {...drag}
      onKeyDown={(event) => { if (event.key === 'Escape') close(); }}>
      {children}
    </div>
  );
}
