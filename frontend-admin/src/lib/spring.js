/**
 * Ressort à deux paramètres, ceux que les designers Apple manipulent :
 *  - `response` : en combien de secondes la valeur rejoint sa cible (ce n'est pas une durée, le ressort
 *    n'en a pas : il s'arrête quand il a fini) ;
 *  - `damping`  : 1 = amorti critique, sans rebond ; en dessous de 1, la valeur dépasse puis revient.
 *
 * Un ressort part toujours de la valeur affichée et de la vitesse courante : on peut donc le
 * saisir en plein vol et le renvoyer ailleurs sans à-coup, ce qu'une transition CSS ne sait pas faire.
 * `precision` est l'écart sous lequel on considère la valeur arrivée (dans l'unité de `from` et `to`).
 */
export function spring({ from, to, velocity = 0, response = 0.4, damping = 1, precision = 0.1, onUpdate, onSettle }) {
  const omega = (2 * Math.PI) / response;
  const stiffness = omega * omega;
  const friction = 2 * damping * omega;
  let x = from;
  let v = velocity;
  let last = 0;
  let frame = 0;

  const step = (now) => {
    let dt = Math.min((now - last) / 1000, 0.032);
    last = now;
    // Sous-pas fixes de 4 ms : le résultat ne dépend pas d'un rendu qui traîne.
    while (dt > 0) {
      const h = Math.min(dt, 0.004);
      v += (-stiffness * (x - to) - friction * v) * h;
      x += v * h;
      dt -= h;
    }
    if (Math.abs(x - to) < precision && Math.abs(v) < precision * 10) {
      onUpdate(to);
      onSettle?.();
      return;
    }
    onUpdate(x);
    frame = requestAnimationFrame(step);
  };

  frame = requestAnimationFrame((now) => {
    last = now;
    step(now);
  });

  return { stop: () => cancelAnimationFrame(frame) };
}

/** Où le geste va s'arrêter s'il est lâché maintenant : la décélération d'un défilement, pas `v²/2a`. */
export const project = (velocity, deceleration = 0.998) => ((velocity / 1000) * deceleration) / (1 - deceleration);

/** Résistance croissante au-delà d'une butée : on ralentit avant de s'arrêter, on ne se cogne pas. */
export const rubberband = (overshoot, dimension, constant = 0.55) =>
  (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
