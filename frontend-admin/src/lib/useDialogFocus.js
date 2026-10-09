import { useEffect, useState } from 'react';

/**
 * Le focus d'une fenêtre ouverte : il entre dans la fenêtre (le champ de saisie s'il y en a un, sinon
 * le premier bouton), y reste tant qu'elle est ouverte (Tab et Maj+Tab bouclent), Échap la ferme, et il
 * retourne à l'élément qui l'avait ouverte. Partagé par la confirmation, le panneau latéral et le visualiseur.
 */
export function useDialogFocus(ref, onClose) {
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
