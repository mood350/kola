import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { initials, label, tone } from '../lib/format';
import { activePreset, PRESETS } from '../lib/period';
import DatePicker from './DatePicker';
import { IconClose, IconFilter, IconSearch } from './icons';

export function PageHead({ title, subtitle, back, actions }) {
  return (
    <div className="page-head">
      <div>
        {back && <Link className="back" to={back.to}>← {back.label}</Link>}
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="btn-row">{actions}</div>}
    </div>
  );
}

export function Card({ title, actions, toolbar, children, flush = false }) {
  return (
    <section className="card">
      {title && (
        <div className="card-head">
          <h2>{title}</h2>
          {actions}
        </div>
      )}
      {toolbar && <div className="toolbar">{toolbar}</div>}
      {flush ? children : <div className="card-body">{children}</div>}
    </section>
  );
}

/** Nom + ligne secondaire précédés des initiales : la cellule « client » des listes. */
export function Person({ name, sub }) {
  return (
    <span className="person">
      <span className="avatar">{initials(name)}</span>
      <span>{name || '—'}{sub && <span className="cell-sub num">{sub}</span>}</span>
    </span>
  );
}

/** État d'une ligne : un point de couleur et un mot. La couleur n'est jamais seule à parler. */
export function Status({ kind, code }) {
  return <span className={`status ${tone(kind, code)}`}>{label(kind, code)}</span>;
}

/** `title` dit ce qui a échoué : par défaut un chargement, mais une action (décision, téléchargement) le précise. */
export function ErrorNotice({ message, onRetry, title = 'Nous n\'avons pas pu charger ces informations.' }) {
  if (!message) return null;
  return (
    <div className="notice error" role="alert">
      <strong>{title}</strong>
      <span className="notice-detail">{message}</span>
      {onRetry && <button type="button" className="linklike" onClick={onRetry}>Réessayer</button>}
    </div>
  );
}

/** Lignes grises en attendant les données : la page garde sa forme, rien ne saute à l'arrivée. */
export function Skeleton({ rows = 3 }) {
  return (
    <div className="skeleton" aria-busy="true" aria-label="Chargement en cours">
      {Array.from({ length: rows }, (_, i) => <span key={i} />)}
    </div>
  );
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>;
}

/**
 * Tableau générique. `columns` : { key, header, render, align, wrap, lead }. `lead` (la première colonne par défaut)
 * dit quelle cellule titre le bloc sur mobile, sans libellé ; les autres gardent le leur.
 * `onRowClick` rend les lignes cliquables — et atteignables au clavier (Entrée).
 */
export function Table({ columns, rows, rowKey, onRowClick, empty = 'Aucun élément.' }) {
  if (!rows || rows.length === 0) return <Empty>{empty}</Empty>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key} className={c.align === 'right' ? 'right' : undefined}>{c.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              className={onRowClick ? 'clickable' : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              onKeyDown={onRowClick ? (e) => { if (e.key === 'Enter') onRowClick(row); } : undefined}
              tabIndex={onRowClick ? 0 : undefined}
            >
              {columns.map((c, index) => (
                <td key={c.key} data-label={c.header || undefined} data-lead={(c.lead ?? index === 0) ? '' : undefined} className={[c.align === 'right' ? 'right' : '', c.wrap ? 'wrap' : ''].join(' ').trim() || undefined}>
                  {c.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/**
 * Pages numérotées « 1 … 4 5 6 … 20 » et taille de page. Seule la page affichée
 * est demandée au serveur : 15 lignes, pas l'historique entier.
 */
export function Pager({ page, onPage, onSize, sizes = [10, 15, 25] }) {
  if (!page || page.total === 0) return null;
  const current = page.page;
  const last = page.totalPages - 1;

  const numbers = [];
  for (let i = 0; i <= last; i++) {
    if (i === 0 || i === last || Math.abs(i - current) <= 1) numbers.push(i);
    else if (numbers[numbers.length - 1] !== 'gap') numbers.push('gap');
  }
  const first = current * page.size + 1;
  const lastRow = Math.min((current + 1) * page.size, page.total);

  return (
    <div className="pager">
      <span className="num">{first}–{lastRow} sur {page.total}</span>
      {last > 0 && (
        <nav className="pages" aria-label="Pagination">
          <button type="button" disabled={current === 0} onClick={() => onPage(current - 1)} aria-label="Page précédente">‹</button>
          {numbers.map((n, i) => (n === 'gap'
            ? <span key={`gap-${i}`} className="gap">…</span>
            : (
              <button key={n} type="button" aria-current={n === current ? 'page' : undefined} onClick={() => onPage(n)}>
                {n + 1}
              </button>
            )))}
          <button type="button" disabled={current === last} onClick={() => onPage(current + 1)} aria-label="Page suivante">›</button>
        </nav>
      )}
      {onSize && (
        <label className="page-size">
          Lignes
          <select className="select" value={page.size} onChange={(e) => onSize(Number(e.target.value))}>
            {sizes.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
      )}
    </div>
  );
}

/**
 * Période en un seul menu : les préréglages (Aujourd'hui, Hier, 7 jours…) et
 * « Dates précises… », qui ouvre le panneau des filtres sur les deux dates.
 * Une période libre déjà choisie s'affiche comme telle.
 */
export function PeriodSelect({ from, to, onChange, onCustom, label: text = 'Période' }) {
  const active = activePreset(from, to) ?? 'custom';
  const choose = (key) => {
    if (key === 'custom') onCustom();
    else onChange(PRESETS.find((p) => p.key === key).range());
  };
  return (
    <select className="select" aria-label={text} value={active} onChange={(e) => choose(e.target.value)}>
      {PRESETS.map((p) => <option key={p.key} value={p.key}>{p.key === 'all' ? 'Toutes les dates' : p.label}</option>)}
      <option value="custom">{active === 'custom' ? 'Dates précises' : 'Dates précises…'}</option>
    </select>
  );
}

/** Les deux dates d'une période libre, pour le panneau des filtres : « du » ne dépasse jamais « au ». */
export function DateRange({ from, to, onChange, label: text = 'Période' }) {
  return (
    <div className="filter-field">
      <span className="filter-label">{text}</span>
      <span className="range">
        <DatePicker label="Du" value={from || ''} max={to || undefined}
          onChange={(next) => onChange({ from: next, to })} />
        <span aria-hidden="true">→</span>
        <DatePicker label="Au" value={to || ''} min={from || undefined}
          onChange={(next) => onChange({ from, to: next })} />
      </span>
    </div>
  );
}

/** Bouton « Filtres », avec le nombre de filtres actifs cachés derrière. */
export function FiltersToggle({ open, count: active, onToggle }) {
  return (
    <button type="button" className={`btn secondary${active ? ' has-filters' : ''}`} aria-expanded={open} onClick={onToggle}>
      <IconFilter size={16} />
      Filtres
      {active > 0 && <span className="filter-count">{active}</span>}
    </button>
  );
}

/** Panneau des filtres secondaires, sous la barre d'outils. */
export function FiltersPanel({ children, onReset, canReset }) {
  return (
    <div className="filters-panel">
      {children}
      {canReset && <button type="button" className="reset" onClick={onReset}>Effacer les filtres</button>}
    </div>
  );
}

/** Champ nommé du panneau des filtres. */
export function FilterField({ label: text, children }) {
  return (
    <label className="filter-field">
      <span className="filter-label">{text}</span>
      {children}
    </label>
  );
}

/**
 * Champ de recherche à validation différée : la requête part 300 ms après la
 * dernière frappe, pas à chaque caractère.
 */
export function SearchInput({ value, onChange, placeholder, label: text }) {
  const [draft, setDraft] = useState(value);
  const first = useRef(true);

  useEffect(() => {
    if (first.current) {
      first.current = false;
      return undefined;
    }
    // Rien à signaler tant que la saisie n'a pas changé la valeur : sinon la recherche réécrirait l'adresse
    // à son premier rendu (et effacerait au passage ce qu'elle ne connaît pas).
    if (draft.trim() === value) return undefined;
    const timer = setTimeout(() => onChange(draft.trim()), 300);
    return () => clearTimeout(timer);
    // `onChange` change d'identité à chaque rendu du parent ; seule la saisie compte.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft]);

  return (
    <div className="search">
      <IconSearch size={16} />
      <label className="sr-only" htmlFor="search">{text}</label>
      <input
        id="search"
        className="input"
        type="search"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        placeholder={placeholder}
      />
    </div>
  );
}

/**
 * Boîte de confirmation. Avec `reasonLabel`, elle exige un texte non vide —
 * c'est le cas du rejet KYC, dont le motif est envoyé au client.
 */
/**
 * Le focus d'une fenêtre ouverte : il entre dans la fenêtre (le champ de saisie s'il y en a un, sinon
 * le premier bouton), y reste tant qu'elle est ouverte (Tab et Maj+Tab bouclent), Échap la ferme, et il
 * retourne à l'élément qui l'avait ouverte. Partagé par la confirmation et le panneau latéral.
 */
function useDialogFocus(ref, onClose) {
  const [opener] = useState(() => document.activeElement);

  useEffect(() => {
    ref.current?.querySelector('textarea, button')?.focus();
    return () => { if (opener?.isConnected) opener.focus(); };
  }, [ref, opener]);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') {
        onClose();
        return;
      }
      if (e.key !== 'Tab' || !ref.current) return;
      const focusable = [...ref.current.querySelectorAll('button:not(:disabled), textarea, a[href]')];
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [ref, onClose]);
}

export function ConfirmDialog({ title, message, confirmLabel, reasonLabel, danger, pending, error, onConfirm, onCancel, children }) {
  const [reason, setReason] = useState('');
  const dialog = useRef(null);
  useDialogFocus(dialog, onCancel);

  const blocked = pending || (reasonLabel && !reason.trim());

  return (
    <div className="overlay" onClick={onCancel}>
      <div className="dialog" ref={dialog} role="dialog" aria-modal="true" aria-labelledby="dialog-title"
        aria-describedby={message ? 'dialog-message' : undefined} onClick={(e) => e.stopPropagation()}>
        <h2 id="dialog-title">{title}</h2>
        {message && <p id="dialog-message">{message}</p>}
        {children}
        {reasonLabel && (
          <div className="field">
            <label htmlFor="reason">{reasonLabel}</label>
            <textarea id="reason" maxLength={300} value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
        )}
        <ErrorNotice message={error} title="L'opération n'a pas abouti." />
        <div className="btn-row">
          <button type="button" className="btn secondary" onClick={onCancel}>Annuler</button>
          <button
            type="button"
            className={danger ? 'btn danger' : 'btn'}
            disabled={blocked}
            onClick={() => onConfirm(reason.trim())}
          >
            {pending ? 'Un instant…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * Panneau latéral : le détail d'une ligne sans quitter sa liste. À droite sur grand écran, plein écran
 * sur mobile ; cliquer à côté, Échap ou « Fermer » le referme.
 */
export function SidePanel({ title, onClose, children }) {
  const panel = useRef(null);
  useDialogFocus(panel, onClose);

  return (
    <div className="panel-overlay" onClick={onClose}>
      <aside className="side-panel" ref={panel} role="dialog" aria-modal="true" aria-labelledby="panel-title"
        onClick={(e) => e.stopPropagation()}>
        <div className="side-panel-head">
          <h2 id="panel-title">{title}</h2>
          <button type="button" className="icon-btn" aria-label="Fermer" onClick={onClose}><IconClose /></button>
        </div>
        <div className="side-panel-body">{children}</div>
      </aside>
    </div>
  );
}

/**
 * Onglets d'une même fiche : des groupes d'informations réellement distincts, pas un décor.
 * Flèches gauche et droite, Début et Fin déplacent la sélection ; le panneau est atteignable au clavier.
 */
export function Tabs({ tabs, value, onChange, label: text, children }) {
  const onKeyDown = (event) => {
    const index = tabs.findIndex((tab) => tab.key === value);
    const next = {
      ArrowRight: (index + 1) % tabs.length,
      ArrowLeft: (index - 1 + tabs.length) % tabs.length,
      Home: 0,
      End: tabs.length - 1,
    }[event.key];
    if (next === undefined) return;
    event.preventDefault();
    onChange(tabs[next].key);
    document.getElementById(`tab-${tabs[next].key}`)?.focus();
  };

  return (
    <>
      <div className="tabs" role="tablist" aria-label={text} onKeyDown={onKeyDown}>
        {tabs.map((tab) => (
          <button key={tab.key} id={`tab-${tab.key}`} type="button" role="tab" aria-selected={tab.key === value}
            aria-controls={`panel-${tab.key}`} tabIndex={tab.key === value ? 0 : -1} onClick={() => onChange(tab.key)}>
            {tab.label}
            {tab.badge ? <span className="tab-badge num">{tab.badge}</span> : null}
          </button>
        ))}
      </div>
      <div className="tabpanel" role="tabpanel" id={`panel-${value}`} aria-labelledby={`tab-${value}`} tabIndex={0}>
        {children}
      </div>
    </>
  );
}
