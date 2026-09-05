// Small helpers that let page code read almost like the original design mockups:
// CSS declaration strings ("display:flex; gap:8px") convert straight into React
// style objects, so markup can be ported near verbatim.

const kebabToCamel = (str) => str.replace(/-([a-z])/g, (_, c) => c.toUpperCase());

export function s(css) {
  if (!css) return undefined;
  const out = {};
  for (const decl of css.split(';')) {
    const i = decl.indexOf(':');
    if (i < 0) continue;
    const prop = decl.slice(0, i).trim();
    if (!prop) continue;
    const val = decl.slice(i + 1).trim();
    out[prop.startsWith('--') ? prop : kebabToCamel(prop)] = val;
  }
  return out;
}

export const badge = (bg, fg) => ({
  display: 'inline-block',
  fontSize: 11,
  fontWeight: 800,
  padding: '5px 10px',
  borderRadius: 20,
  background: bg,
  color: fg,
});

export const OK = badge('rgba(16,185,129,.12)', '#005236');
export const PEND = badge('rgba(255,203,5,.2)', '#745B00');
export const KO = badge('rgba(186,26,26,.1)', '#BA1A1A');
export const NEUT = badge('#E2E7FF', '#596171');

export const delta = (up) => ({
  fontSize: 11,
  fontWeight: 700,
  marginTop: 6,
  color: up ? '#005236' : '#BA1A1A',
});

export const fmt = (n) => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');

// React warns when a style transitions between a `border` shorthand and a
// longhand like `borderColor` across renders (as happens on hover/focus when
// only the color changes). Expanding the shorthand up front keeps both
// states on the same longhand keys so that transition never occurs.
export function expandBorder(style) {
  const border = style && style.border;
  if (typeof border !== 'string') return style;
  const m = border.match(/^(\S+)\s+(\S+)\s+(\S+)$/);
  if (!m) return style;
  const { border: _drop, ...rest } = style;
  return { borderWidth: m[1], borderStyle: m[2], borderColor: m[3], ...rest };
}
