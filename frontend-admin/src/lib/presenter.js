import { project, rubberband, spring } from './spring';

const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * Dernière pression du pointeur : un dialogue naît de l'endroit où l'on a appuyé, pas du centre de
 * l'écran. Au clavier, il n'y en a pas de récente et il naît du centre.
 */
let lastPress = null;
if (typeof document !== 'undefined') {
  document.addEventListener('pointerdown', (e) => { lastPress = { x: e.clientX, y: e.clientY, at: performance.now() }; }, true);
}

/**
 * Le mouvement d'une surface qui se présente : panneau latéral (`x`), feuille du bas (`y`) ou dialogue
 * (`scale`). Un objet plutôt que des hooks : il tient un état qui change à chaque image, hors du rendu.
 *
 * Position `pos` : distance restant à parcourir avant d'être entièrement ouvert (0 = ouvert, `size` =
 * fermé). Pour un dialogue, `size` vaut 100 et `pos` se lit en pourcentage.
 */
export function createPresenter({ kind, travel = 0 }) {
  let sheet = null;
  let overlay = null;
  let onClosed = null;
  let size = 1;
  let pos = 0;
  let anim = null;
  let closing = false;
  let reduced = false;
  let drag = null;
  let suppressClick = false;

  const apply = (next) => {
    if (!sheet) return;
    pos = next;
    const progress = 1 - Math.min(Math.max(pos / size, 0), 1);
    overlay?.style.setProperty('--p', progress.toFixed(4));
    if (reduced) {
      sheet.style.opacity = progress.toFixed(4);
    } else if (kind === 'scale') {
      sheet.style.transform = `scale(${0.94 + 0.06 * progress})`;
      sheet.style.opacity = progress.toFixed(4);
    } else {
      sheet.style.transform = kind === 'x' ? `translate3d(${pos}px,0,0)` : `translate3d(0,${pos}px,0)`;
    }
  };

  const animateTo = (target, velocity = 0) => {
    anim?.stop();
    // Un rebond n'a de sens que si le geste portait de l'élan.
    const momentum = Math.abs(velocity) > 300 && !reduced;
    anim = spring({
      from: pos,
      to: target,
      velocity,
      response: reduced ? 0.16 : 0.32,
      damping: momentum ? 0.8 : 1,
      // Visuellement arrivé bien avant les 0,1 px : en sortie, on n'attend pas la queue de l'exponentielle.
      precision: target === size ? 2 : 0.1,
      onUpdate: apply,
      onSettle: () => {
        if (sheet) sheet.style.willChange = '';
        if (target === size) onClosed?.();
      },
    });
  };

  const along = (e) => (kind === 'x' ? e.clientX : e.clientY);
  const across = (e) => (kind === 'x' ? e.clientY : e.clientX);

  const release = (e, cancelled) => {
    const d = drag;
    if (!d || d.id !== e.pointerId) return;
    drag = null;
    if (!d.active) return;
    suppressClick = true;
    setTimeout(() => { suppressClick = false; }, 0);
    const first = d.samples[0];
    const last = d.samples[d.samples.length - 1];
    // Un doigt resté immobile avant de se lever n'a plus de vitesse.
    const velocity = cancelled || e.timeStamp - last.t > 80 ? 0 : (last.v - first.v) / (Math.max(last.t - first.t, 1) / 1000);
    const goingOut = Math.abs(velocity) > 300 ? velocity > 0 : pos + project(velocity) > size / 2;
    closing = goingOut;
    animateTo(goingOut ? size : 0, velocity);
  };

  return {
    setOnClosed(fn) { onClosed = fn; },

    mount(sheetEl, overlayEl) {
      sheet = sheetEl;
      overlay = overlayEl;
      reduced = reducedMotion();
      closing = false;
      size = kind === 'scale' ? 100 : (kind === 'x' ? sheet.offsetWidth : sheet.offsetHeight) + travel;
      if (kind === 'scale') {
        const box = sheet.getBoundingClientRect();
        const fresh = lastPress && performance.now() - lastPress.at < 1500;
        sheet.style.transformOrigin = fresh ? `${lastPress.x - box.left}px ${lastPress.y - box.top}px` : '50% 50%';
      }
      sheet.style.willChange = reduced ? 'opacity' : 'transform, opacity';
      apply(size);
      animateTo(0);
    },

    unmount() {
      anim?.stop();
      sheet = null;
      overlay = null;
    },

    /** Joue la sortie, puis appelle `onClosed`. Sans effet si elle est déjà en cours. */
    close: () => {
      if (closing) return;
      closing = true;
      animateTo(size, 0);
    },

    /** Poignées de pointeur à poser sur la zone de saisie (rien pour un dialogue). */
    drag: kind === 'scale' ? {} : {
      onPointerDown(e) {
        if (reduced || e.button !== 0 || e.target.closest?.('input, textarea, select')) return;
        drag = { id: e.pointerId, x0: along(e), c0: across(e), active: false, base: 0, startPos: 0, samples: [{ t: e.timeStamp, v: along(e) }] };
      },
      onPointerMove(e) {
        const d = drag;
        if (!d || d.id !== e.pointerId) return;
        if (!d.active) {
          const moved = along(e) - d.x0;
          const sideways = across(e) - d.c0;
          if (Math.hypot(moved, sideways) < 10) return;
          // Un geste surtout transversal est un défilement : il n'est pas à nous.
          if (Math.abs(sideways) > Math.abs(moved)) { drag = null; return; }
          d.active = true;
          anim?.stop();
          closing = false;
          d.startPos = pos;
          d.base = along(e);
          e.currentTarget.setPointerCapture(e.pointerId);
        }
        let next = d.startPos + (along(e) - d.base);
        if (next < 0) next = -rubberband(-next, size);
        apply(next);
        d.samples.push({ t: e.timeStamp, v: along(e) });
        while (d.samples.length > 2 && e.timeStamp - d.samples[0].t > 100) d.samples.shift();
      },
      onPointerUp: (e) => release(e, false),
      onPointerCancel: (e) => release(e, true),
      // Le clic qui suit un glissement n'est pas une intention.
      onClickCapture(e) {
        if (suppressClick) { e.preventDefault(); e.stopPropagation(); }
      },
    },
  };
}
