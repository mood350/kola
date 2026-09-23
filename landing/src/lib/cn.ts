/**
 * Concaténation conditionnelle de classes.
 *
 * Volontairement écrite à la main plutôt que d'ajouter clsx + tailwind-merge :
 * ce site ne compose jamais de variantes conflictuelles au runtime, la
 * résolution de conflits Tailwind serait du poids mort dans le bundle.
 */
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}
