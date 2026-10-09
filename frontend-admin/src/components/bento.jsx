import { Link } from 'react-router-dom';

/**
 * Une tuile du bento : un libellé, un chiffre, une précision. Cliquable quand elle mène quelque part ;
 * `big` en fait la grande tuile de l'accueil, qui peut porter un petit graphique en `children`.
 * Les chiffres arrivent déjà mis en forme : la tuile ne calcule rien.
 */
export function Tile({ label, value, sub, to, big = false, children }) {
  const body = (
    <>
      <span className="tile-label">{label}</span>
      <span className="tile-value num">{value}</span>
      <span className="tile-sub num">{sub}</span>
      {children}
    </>
  );
  const className = `tile${big ? ' big' : ''}`;
  return to ? <Link className={`${className} link`} to={to}>{body}</Link> : <div className={className}>{body}</div>;
}
