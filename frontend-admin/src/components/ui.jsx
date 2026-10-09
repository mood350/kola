import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { initials, label, tone } from '../lib/format';
import { activePreset, PRESETS } from '../lib/period';
import { IconFilter, IconSearch } from './icons';

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

/** Badge d'état : libellé français, couleur uniquement si l'état le justifie. */
export function Status({ kind, code }) {
  return <span className={`badge ${tone(kind, code)}`}>{label(kind, code)}</span>;
}

/**
 * Écart avec une valeur de référence (la veille, en général).
 * Sans référence (hier à zéro), un pourcentage n'a pas de sens : on dit « nouveau ».
 */
export function Delta({ current, previous, suffix = 'vs hier' }) {
  const now = Number(current) || 0;
  const before = Number(previous) || 0;
  let text;
  let dir;
  if (before === 0) {
    text = now === 0 ? '=' : 'nouveau';
    dir = now === 0 ? '' : 'up';
  } else {
    const pct = ((now - before) / before) * 100;
    dir = pct > 0 ? 'up' : pct < 0 ? 'down' : '';
    text = `${pct > 0 ? '+' : ''}${pct.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} %`;
  }
  return (
    <>
      <span className={`delta ${dir}`}>{text}</span>
      <span>{suffix}</span>
    </>
  );
}

export function ErrorNotice({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="notice error" role="alert">
      {message}
      {onRetry && (
        <>
          {' '}
          <button type="button" className="linklike" onClick={onRetry}>Réessayer</button>
        </>
      )}
    </div>
  );
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>;
}

/**
 * Tableau générique. `columns` : { key, header, render, align, wrap }.
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
              {columns.map((c) => (
                <td key={c.key} className={[c.align === 'right' ? 'right' : '', c.wrap ? 'wrap' : ''].join(' ').trim() || undefined}>
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

/** Les deux dates d'une période libre, pour le panneau des filtres. */
export function DateRange({ from, to, onChange, label: text = 'Période' }) {
  return (
    <div className="filter-field">
      <span className="filter-label">{text}</span>
      <span className="range">
        <label className="sr-only" htmlFor="from">Du</label>
        <input id="from" className="input" type="date" value={from || ''} max={to || undefined}
          onChange={(e) => onChange({ from: e.target.value, to })} />
        <span aria-hidden="true">→</span>
        <label className="sr-only" htmlFor="to">Au</label>
        <input id="to" className="input" type="date" value={to || ''} min={from || undefined}
          onChange={(e) => onChange({ from, to: e.target.value })} />
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
export function ConfirmDialog({ title, message, confirmLabel, reasonLabel, danger, pending, error, onConfirm, onCancel }) {
  const [reason, setReason] = useState('');

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onCancel(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onCancel]);

  const blocked = pending || (reasonLabel && !reason.trim());

  return (
    <div className="overlay" onClick={onCancel}>
      <div className="dialog" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        {message && <p>{message}</p>}
        {reasonLabel && (
          <div className="field">
            <label htmlFor="reason">{reasonLabel}</label>
            <textarea id="reason" maxLength={300} value={reason} onChange={(e) => setReason(e.target.value)} autoFocus />
          </div>
        )}
        <ErrorNotice message={error} />
        <div className="btn-row">
          <button type="button" className="btn secondary" onClick={onCancel}>Annuler</button>
          <button
            type="button"
            className={danger ? 'btn danger' : 'btn'}
            disabled={blocked}
            onClick={() => onConfirm(reason.trim())}
          >
            {pending ? '…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
