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

export const OK = badge('#E6F5EE', '#0E8A5F');
export const PEND = badge('#FFF4DA', '#96690A');
export const KO = badge('#FDEBEC', '#B3262F');
export const NEUT = badge('#EEF2FA', '#5C6B8E');

export const delta = (up) => ({
  fontSize: 11,
  fontWeight: 700,
  marginTop: 6,
  color: up ? '#0E8A5F' : '#B3262F',
});

export const fmt = (n) => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
