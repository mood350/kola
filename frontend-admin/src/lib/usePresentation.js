import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createPresenter } from './presenter';

/**
 * Entrée, sortie et geste d'une surface qui se présente : panneau latéral (`x`), feuille du bas (`y`)
 * ou dialogue (`scale`). Le mouvement vit dans `presenter.js`.
 *
 * - Un ressort amorti critique la fait entrer ; la sortie suit le même chemin à l'envers (ce qui entre
 *   par la droite repart par la droite).
 * - Tout est interruptible : saisir la surface pendant qu'elle bouge l'arrête là où elle est.
 * - `drag` se pose sur la zone de saisie. Le déplacement suit le doigt 1 pour 1 au-delà de 10 px,
 *   résiste hors de sa course, et au lâcher la vitesse passe au ressort ; la destination est celle où
 *   le geste allait s'arrêter, pas celle où le doigt s'est levé.
 * - `prefers-reduced-motion` : plus de déplacement, un fondu court.
 *
 * Retourne les refs à poser (`sheet` sur la surface, `overlay` sur son voile s'il y en a un),
 * `close()` (joue la sortie puis appelle `onClosed`) et `drag`.
 */
export function usePresentation({ kind, travel = 0, onClosed }) {
  const [presenter] = useState(() => createPresenter({ kind, travel }));
  const overlay = useRef(null);
  const sheet = useRef(null);

  useEffect(() => { presenter.setOnClosed(onClosed); });

  useLayoutEffect(() => {
    presenter.mount(sheet.current, overlay.current);
    return () => presenter.unmount();
  }, [presenter]);

  return { overlay, sheet, close: presenter.close, drag: presenter.drag };
}
