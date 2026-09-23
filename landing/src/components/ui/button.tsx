import type { ComponentPropsWithoutRef } from "react";
import Link from "next/link";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "inverse" | "outlineInverse";
type Size = "md" | "lg";

/**
 * Chaque combinaison fond/texte est un variant à part entière ; on ne surcharge
 * jamais les couleurs depuis `className`.
 *
 * Deux utilitaires Tailwind concurrents (`text-white` du variant et un
 * `text-kola-700` passé par l'appelant) ont la même spécificité : c'est leur
 * ordre dans la feuille générée qui tranche, pas leur ordre dans l'attribut
 * class. Le bouton du CTA final est ainsi resté blanc sur blanc. Passer par des
 * variants supprime le conflit à la racine, sans embarquer tailwind-merge pour
 * arbitrer au runtime.
 */
const VARIANTS: Record<Variant, string> = {
  /* L'ombre au survol est portée par un pseudo-élément dont on n'anime que
     l'opacité. Transitionner `box-shadow` directement repeint à chaque frame
     une large zone floutée — c'est la cause classique d'un survol qui accroche
     sur une page chargée. Ici l'ombre est rasterisée une fois, et le survol ne
     fait plus qu'un fondu géré par le compositeur. */
  primary:
    "bg-kola-600 text-white shadow-soft hover:bg-kola-700 active:bg-kola-800 " +
    "after:pointer-events-none after:absolute after:inset-0 after:rounded-full " +
    "after:opacity-0 after:shadow-lifted after:transition-opacity after:duration-200 " +
    "hover:after:opacity-100",
  /* Pilule lavande sur fond lavande : le contraste vient de la saturation,
     pas d'une bordure. C'est le bouton secondaire du référentiel. */
  secondary:
    "bg-kola-100 text-kola-700 hover:bg-kola-200 active:bg-kola-200/80",
  ghost: "bg-transparent text-ink-700 hover:bg-ink-100 active:bg-ink-200",
  /** Sur aplat de marque saturé : inversion complète. */
  inverse:
    "bg-white text-kola-700 shadow-soft hover:bg-kola-50 active:bg-kola-100",
  /** Sur aplat de marque, action secondaire. */
  outlineInverse:
    "border border-white/30 bg-transparent text-white hover:bg-white/10 active:bg-white/15",
};

const SIZES: Record<Size, string> = {
  // 44px de haut minimum : la cible tactile recommandée. En dessous, le taux
  // d'erreur au pouce grimpe nettement sur mobile.
  md: "h-11 px-5 text-sm",
  lg: "h-14 px-7 text-base",
};

type ButtonProps = {
  variant?: Variant;
  size?: Size;
} & ComponentPropsWithoutRef<"a">;

/**
 * Bouton d'action, rendu en `<a>` — toutes les actions de cette page sont des
 * liens (ancres ou téléchargement de l'app). Un `<button>` ici tromperait les
 * lecteurs d'écran sur la nature de l'action.
 *
 * Le survol est traité en CSS et non en GSAP : c'est une transition d'état à
 * une seule propriété, la piloter en JS coûterait un listener par bouton pour
 * un résultat identique.
 */
export function Button({
  variant = "primary",
  size = "md",
  className,
  children,
  href,
  ...props
}: ButtonProps) {
  const classes = cn(
        "group relative inline-flex cursor-pointer items-center justify-center gap-2 rounded-full font-medium",
        // `transform` et `background-color` uniquement : `box-shadow` est animé
        // via l'opacité d'un pseudo-élément (cf. variant primary).
        "transition-[background-color,transform] duration-200 ease-[var(--ease-editorial)]",
        "hover:-translate-y-0.5 active:translate-y-0",
        VARIANTS[variant],
    SIZES[size],
    className
  );

  /* Navigation interne via next/link : un <a> nu provoque un rechargement
     complet, ce qui remonte tout le JavaScript et supprime la transition de
     route. On ne garde le <a> natif que pour les liens externes et les
     protocoles (mailto:, tel:). */
  const isInternal = href?.startsWith("/") ?? false;

  if (isInternal) {
    return (
      <Link href={href!} className={classes} {...props}>
        {children}
      </Link>
    );
  }

  return (
    <a href={href} className={classes} {...props}>
      {children}
    </a>
  );
}

/** Flèche décorative qui glisse au survol du bouton parent. */
export function ArrowRight({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
      className={cn(
        "h-4 w-4 transition-transform duration-200 ease-[var(--ease-editorial)] group-hover:translate-x-1",
        className
      )}
    >
      <path
        d="M2 8h11M9 4l4 4-4 4"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
