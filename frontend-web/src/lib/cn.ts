/**
 * Concaténation conditionnelle de classes.
 *
 * Écrite à la main plutôt que d'ajouter clsx + tailwind-merge : cette
 * application ne compose jamais de variantes conflictuelles au runtime — chaque
 * variante de composant définit son jeu complet de couleurs — la résolution de
 * conflits Tailwind serait donc du poids mort. Même choix que dans
 * `frontend-admin/`.
 */
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}
