import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import {
  addDays, addMonths, clampIso, displayIso, monthWeeks, parseIso, todayIso, toIso, weekday,
} from '../lib/calendar';
import { IconCalendar, IconChevron, IconPrev } from './icons';

const monthName = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric', timeZone: 'UTC' });
const dayName = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' });
// Un lundi quelconque (le 1er janvier 2024), pour lire les noms des jours dans l'ordre de la semaine.
const WEEK = Array.from({ length: 7 }, (_, i) => new Date(Date.UTC(2024, 0, 1 + i)));
const short = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', timeZone: 'UTC' });
const long = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', timeZone: 'UTC' });

const capitalise = (text) => text.charAt(0).toUpperCase() + text.slice(1);
const asDate = (iso) => { const { y, m, d } = parseIso(iso); return new Date(Date.UTC(y, m - 1, d)); };

/**
 * Calendrier maison, à la place du sélecteur du navigateur (dont l'allure et la langue changent
 * d'un navigateur à l'autre). Valeur et bornes sont des chaînes AAAA-MM-JJ ; l'affichage est JJ/MM/AAAA.
 *
 * Clavier : flèches (jour, semaine), Début/Fin (semaine), Page précédente/suivante (mois, +Maj : année),
 * Entrée ou Espace pour choisir, Échap pour fermer. Un jour hors de [min, max] n'est pas atteignable.
 */
export default function DatePicker({ value, onChange, label, min, max }) {
  const [open, setOpen] = useState(false);
  const [focused, setFocused] = useState(null);
  const root = useRef(null);
  const trigger = useRef(null);
  const grid = useRef(null);
  // Le focus ne suit le jour que lorsqu'il vient du clavier ou de l'ouverture : cliquer sur les
  // flèches de mois ne doit pas l'arracher au bouton que l'on est en train d'utiliser.
  const pull = useRef(false);

  const today = todayIso();
  const selected = parseIso(value) ? value : '';
  const cursor = parseIso(focused) ? focused : clampIso(selected || today, min, max);
  const { y, m } = parseIso(cursor);

  // useLayoutEffect : le jour focalisé disparaît avec son mois ; le focus doit être reposé avant
  // que le navigateur ne signale sa perte, sans quoi la fenêtre se croirait quittée.
  useLayoutEffect(() => {
    if (open && pull.current) {
      pull.current = false;
      grid.current?.querySelector('button[tabindex="0"]')?.focus();
    }
  }, [open, focused]);

  useEffect(() => {
    if (!open) return undefined;
    const onPointerDown = (event) => { if (!root.current?.contains(event.target)) setOpen(false); };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  const close = () => {
    setOpen(false);
    trigger.current?.focus();
  };
  const choose = (iso) => {
    onChange(iso);
    close();
  };
  // Changement de mois par les flèches. Si la borne est atteinte, la flèche utilisée se grise : le focus
  // passe alors à la grille plutôt que de se perdre avec elle.
  const goMonth = (target, direction) => {
    const next = clampIso(target, min, max);
    const { y: ny, m: nm } = parseIso(next);
    const start = toIso(ny, nm, 1);
    const end = addDays(addMonths(start, 1), -1);
    if ((direction < 0 && min && start <= min) || (direction > 0 && max && end >= max)) pull.current = true;
    setFocused(next);
  };
  const move = (iso) => {
    if (min && iso < min) return;
    if (max && iso > max) return;
    pull.current = true;
    setFocused(iso);
  };

  const onKeyDown = (event) => {
    const step = {
      ArrowLeft: () => move(addDays(cursor, -1)),
      ArrowRight: () => move(addDays(cursor, 1)),
      ArrowUp: () => move(addDays(cursor, -7)),
      ArrowDown: () => move(addDays(cursor, 7)),
      Home: () => move(addDays(cursor, -weekday(cursor))),
      End: () => move(addDays(cursor, 6 - weekday(cursor))),
      PageUp: () => move(addMonths(cursor, event.shiftKey ? -12 : -1)),
      PageDown: () => move(addMonths(cursor, event.shiftKey ? 12 : 1)),
      Escape: close,
    }[event.key];
    if (!step) return;
    event.preventDefault();
    step();
  };

  const monthStart = `${y}-${String(m).padStart(2, '0')}-01`;
  const previousMonth = addMonths(monthStart, -1);
  const nextMonth = addMonths(monthStart, 1);
  const monthEnd = addDays(nextMonth, -1);
  const canGoBack = !min || monthStart > min;
  const canGoForward = !max || monthEnd < max;

  return (
    <div className="datepick" ref={root}
      onBlur={() => {
        setTimeout(() => { if (root.current && !root.current.contains(document.activeElement)) setOpen(false); }, 0);
      }}>
      <button type="button" ref={trigger} className={`datepick-trigger${selected ? '' : ' empty'}`}
        aria-haspopup="dialog" aria-expanded={open}
        aria-label={selected ? `${label} : ${displayIso(selected)}` : `${label} : aucune date`}
        onClick={() => { if (open) setOpen(false); else { pull.current = true; setFocused(cursor); setOpen(true); } }}>
        <span>{selected ? displayIso(selected) : 'JJ/MM/AAAA'}</span>
        <IconCalendar size={16} />
      </button>

      {open && (
        <div className="datepick-pop" role="dialog" aria-label={label} tabIndex={-1} onKeyDown={onKeyDown}>
          <div className="datepick-head">
            <button type="button" className="icon-btn" aria-label="Mois précédent" disabled={!canGoBack}
              onClick={() => goMonth(previousMonth, -1)}><IconPrev /></button>
            <strong aria-live="polite">{capitalise(monthName.format(asDate(cursor)))}</strong>
            <button type="button" className="icon-btn" aria-label="Mois suivant" disabled={!canGoForward}
              onClick={() => goMonth(nextMonth, 1)}><IconChevron /></button>
          </div>

          <table className="datepick-grid" role="grid" ref={grid}>
            <thead>
              <tr>
                {WEEK.map((day) => <th key={day.getTime()} scope="col" abbr={long.format(day)}>{short.format(day)}</th>)}
              </tr>
            </thead>
            <tbody>
              {monthWeeks(y, m).map((week, row) => (
                <tr key={row}>
                  {week.map((iso, col) => (
                    <td key={col} role="gridcell" aria-selected={iso ? iso === selected : undefined}>
                      {iso && (
                        <button type="button" className={iso === selected ? 'selected' : undefined}
                          tabIndex={iso === cursor ? 0 : -1}
                          aria-label={dayName.format(asDate(iso))}
                          aria-current={iso === today ? 'date' : undefined}
                          disabled={(min && iso < min) || (max && iso > max)}
                          onClick={() => choose(iso)}>
                          {Number(iso.slice(8))}
                        </button>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>

          <div className="datepick-foot">
            <button type="button" className="linklike" onClick={() => choose(clampIso(today, min, max))}
              disabled={(min && today < min) || (max && today > max)}>Aujourd&apos;hui</button>
            {selected && <button type="button" className="linklike" onClick={() => choose('')}>Effacer</button>}
          </div>
        </div>
      )}
    </div>
  );
}
