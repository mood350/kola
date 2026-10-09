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
  /* Bouton « verre » : deux reflets intérieurs et une lueur indigo dessous
     (`shadow-gloss`). L'ombre est posée une fois pour toutes et n'est jamais
     transitionnée — animer un `box-shadow` flouté repeint une large zone à
     chaque frame. Le survol ne change que la teinte et la position. */
  primary:
    "bg-kola-600 text-white shadow-gloss hover:bg-kola-500 active:bg-kola-700",
  /* Blanc cerné d'un filet : l'action secondaire du site public, lisible sur
     le fond gris bleuté sans rivaliser avec le bouton plein. */
  secondary:
    "border border-hairline bg-surface text-ink-900 shadow-card hover:border-ink-300 active:bg-mist",
  ghost: "bg-transparent text-ink-700 hover:bg-ink-100 active:bg-ink-200",
  /** Sur fond nocturne : inversion complète. */
  inverse:
    "bg-white text-ink-950 shadow-[inset_0_-2px_0_rgb(16_17_56/0.08)] hover:bg-kola-50 active:bg-kola-100",
  /** Sur fond nocturne, action secondaire. */
  outlineInverse:
    "border border-white/15 bg-white/5 text-white backdrop-blur-sm hover:bg-white/10 active:bg-white/15",
};

const SIZES: Record<Size, string> = {
  // 44px de haut minimum : la cible tactile recommandée. En dessous, le taux
  // d'erreur au pouce grimpe nettement sur mobile.
  md: "h-11 px-5 text-sm",
  lg: "h-13 px-7 text-[0.9375rem]",
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
        "transition-[background-color,border-color,transform] duration-200 ease-[var(--ease-editorial)]",
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
