"use client";

import { useEffect, useId, useRef } from "react";
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/cn";
import type { Tone } from "@/lib/labels";
import { AlertIcon, CheckIcon, CloseIcon, InfoIcon } from "@/components/ui/icons";

/**
 * Base d'interface de l'application, rassemblée dans un seul module.
 *
 * Un fichier plutôt qu'un dossier de trente : ces composants sont courts, ils
 * partagent le même vocabulaire de tonalités, et les tenir côte à côte est ce
 * qui empêche un cinquième style de bouton d'apparaître discrètement. Quand un
 * écran a besoin d'une variante, elle s'ajoute ICI et devient disponible
 * partout.
 */

/* ---------------------------------------------------------------------------
   Tonalités — une seule table pour tout ce qui porte une couleur d'état
   ------------------------------------------------------------------------ */

/**
 * Un état = une couleur, partout.
 *
 * Cette table est la raison pour laquelle « Réussie » a exactement la même
 * teinte dans une liste de transactions, sur un reçu et dans une notification.
 * Sans elle, chaque écran choisit son vert, et deux verts différents se lisent
 * comme deux états différents.
 */
export const TONE_SOFT: Record<Tone, string> = {
  neutral: "bg-ink-100 text-ink-700",
  brand: "bg-kola-50 text-kola-700",
  positive: "bg-positive-50 text-positive-700",
  warning: "bg-warning-50 text-warning-700",
  danger: "bg-danger-50 text-danger-700",
  info: "bg-info-50 text-info-600",
};

export const TONE_TEXT: Record<Tone, string> = {
  neutral: "text-ink-600",
  brand: "text-kola-600",
  positive: "text-positive-600",
  warning: "text-warning-600",
  danger: "text-danger-600",
  info: "text-info-500",
};

/* ---------------------------------------------------------------------------
   Bouton
   ------------------------------------------------------------------------ */

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: "sm" | "md" | "lg";
  /** Affiche un indicateur ET désactive le bouton — les deux vont ensemble. */
  loading?: boolean;
  full?: boolean;
  icon?: ReactNode;
};

const BUTTON_VARIANT: Record<ButtonVariant, string> = {
  primary:
    "bg-kola-600 text-white hover:bg-kola-700 active:bg-kola-800 disabled:bg-kola-300",
  secondary:
    "bg-white text-ink-800 ring-1 ring-line-strong hover:bg-ink-50 disabled:text-ink-400",
  ghost: "text-ink-700 hover:bg-ink-100 disabled:text-ink-300",
  danger:
    "bg-danger-500 text-white hover:bg-danger-600 active:bg-danger-700 disabled:bg-danger-100",
};

const BUTTON_SIZE = {
  sm: "h-9 px-4 text-sm",
  md: "h-11 px-5 text-[0.95rem]",
  lg: "h-13 px-6 text-base",
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  full = false,
  icon,
  className,
  children,
  disabled,
  ...props
}: ButtonProps) {
  return (
    <button
      /* `disabled` couvre le chargement : un bouton d'envoi encore cliquable
         pendant sa requête est la première cause de double virement. La clé
         d'idempotence protège le backend, mais l'interface ne doit pas
         s'appuyer dessus pour se dispenser de bloquer le second clic. */
      disabled={disabled || loading}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-colors disabled:cursor-not-allowed",
        BUTTON_VARIANT[variant],
        BUTTON_SIZE[size],
        full && "w-full",
        className
      )}
      {...props}
    >
      {loading ? <Spinner className="size-4" /> : icon}
      {children}
    </button>
  );
}

/** Bouton carré à icône seule. `label` devient son nom accessible. */
export function IconButton({
  label,
  className,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={cn(
        "inline-flex size-10 items-center justify-center rounded-full text-ink-600 transition-colors hover:bg-ink-100 hover:text-ink-900 disabled:cursor-not-allowed disabled:text-ink-300",
        className
      )}
      {...props}
    >
      {children}
    </button>
  );
}

/* ---------------------------------------------------------------------------
   Surfaces
   ------------------------------------------------------------------------ */

/**
 * Panneau de contenu.
 *
 * ═══ POURQUOI DES VARIANTES, ET PAS UNE COULEUR PASSÉE EN `className` ═══
 *
 * Parce que ça ne marche pas. Sans `tailwind-merge`, `cn()` se contente de
 * concaténer : passer `bg-kola-600` à une carte qui porte déjà `bg-surface`
 * laisse les DEUX règles dans la feuille de style, et c'est l'ordre de
 * génération de Tailwind qui tranche — pas l'ordre d'écriture. La carte de
 * solde s'est ainsi affichée blanche, avec un montant en blanc sur blanc,
 * donc invisible.
 *
 * Chaque variante définit donc son JEU COMPLET de couleurs (fond, texte,
 * contour), et `className` ne sert plus qu'à la mise en page. C'est la règle
 * qui rend `cn()` suffisant, et elle vaut pour tout composant d'ici.
 */
type CardVariant = "default" | "brand";

const CARD_VARIANT: Record<CardVariant, string> = {
  default: "bg-surface text-ink-900 ring-line",
  brand: "bg-kola-600 text-white ring-kola-700/30",
};

export function Card({
  variant = "default",
  selected = false,
  className,
  children,
  ...props
}: React.HTMLAttributes<HTMLDivElement> & {
  variant?: CardVariant;
  /** Met la carte en avant (compte actif, choix retenu) — contour, pas fond. */
  selected?: boolean;
}) {
  return (
    <div
      className={cn(
        "rounded-card p-5 shadow-card sm:p-6",
        CARD_VARIANT[variant],
        /* `ring-2` remplace `ring-1` : les deux ne coexistent pas, la variante
           choisit l'un OU l'autre plutôt que de les superposer. */
        selected ? "ring-2 ring-kola-500" : "ring-1",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function SectionHeading({
  title,
  action,
  description,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-3 flex items-end justify-between gap-4">
      <div>
        <h2 className="font-display text-lg font-semibold text-ink-950">
          {title}
        </h2>
        {description ? (
          <p className="mt-0.5 text-sm text-ink-500">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Badges et pastilles
   ------------------------------------------------------------------------ */

export function Badge({
  tone = "neutral",
  children,
  className,
}: {
  tone?: Tone;
  children: ReactNode;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold",
        TONE_SOFT[tone],
        className
      )}
    >
      {children}
    </span>
  );
}

/** Badge à pastille colorée — la forme retenue pour le niveau KYC, comme sur mobile. */
export function DotBadge({ tone, children }: { tone: Tone; children: ReactNode }) {
  const dot: Record<Tone, string> = {
    neutral: "bg-ink-400",
    brand: "bg-kola-500",
    positive: "bg-positive-500",
    warning: "bg-warning-500",
    danger: "bg-danger-500",
    info: "bg-info-500",
  };
  return (
    <span className="inline-flex items-center gap-2 rounded-full bg-ink-100 px-3 py-1 text-xs font-semibold tracking-wide text-ink-700 uppercase">
      <span className={cn("size-2 rounded-full", dot[tone])} />
      {children}
    </span>
  );
}

/* ---------------------------------------------------------------------------
   Champs de formulaire
   ------------------------------------------------------------------------ */

/**
 * Enveloppe d'un champ : libellé, aide, erreur.
 *
 * L'erreur est reliée au champ par `aria-describedby` et non simplement posée
 * en dessous : un lecteur d'écran annonce alors la raison du refus au moment où
 * le champ prend le focus, au lieu de laisser l'utilisateur chercher.
 */
export function Field({
  label,
  hint,
  error,
  children,
  htmlFor,
}: {
  label: string;
  hint?: string;
  error?: string | null;
  htmlFor: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <label
        htmlFor={htmlFor}
        className="block text-sm font-medium text-ink-800"
      >
        {label}
      </label>
      {children}
      {error ? (
        <p id={`${htmlFor}-error`} className="text-sm text-danger-600">
          {error}
        </p>
      ) : hint ? (
        <p id={`${htmlFor}-hint`} className="text-sm text-ink-500">
          {hint}
        </p>
      ) : null}
    </div>
  );
}

const FIELD_BASE =
  "w-full rounded-field border border-line-strong bg-white px-3.5 py-2.5 text-ink-900 placeholder:text-ink-400 transition-colors focus:border-kola-500 focus:ring-2 focus:ring-kola-100 focus:outline-none disabled:bg-ink-50 disabled:text-ink-400";

export function Input({
  className,
  invalid,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }) {
  return (
    <input
      className={cn(FIELD_BASE, invalid && "border-danger-500", className)}
      aria-invalid={invalid || undefined}
      aria-describedby={invalid && props.id ? `${props.id}-error` : undefined}
      {...props}
    />
  );
}

export function Select({
  className,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cn(FIELD_BASE, "appearance-none pr-9", className)} {...props}>
      {children}
    </select>
  );
}

export function Textarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn(FIELD_BASE, "min-h-24", className)} {...props} />;
}

/* ---------------------------------------------------------------------------
   Retours d'état
   ------------------------------------------------------------------------ */

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="Chargement"
      className={cn(
        "inline-block size-5 animate-spin rounded-full border-2 border-current border-t-transparent align-[-0.125em]",
        className
      )}
    />
  );
}

export function Alert({
  tone = "danger",
  title,
  children,
  onDismiss,
}: {
  tone?: Tone;
  title?: string;
  children: ReactNode;
  onDismiss?: () => void;
}) {
  const glyph =
    tone === "positive" ? <CheckIcon /> : tone === "info" || tone === "brand" ? <InfoIcon /> : <AlertIcon />;

  return (
    <div
      /* `alert` fait annoncer le message dès son apparition : sur un échec de
         virement, l'information ne doit pas dépendre du fait qu'on regardait
         cet endroit de l'écran. */
      role="alert"
      className={cn("flex gap-3 rounded-field p-3.5 text-sm", TONE_SOFT[tone])}
    >
      <span className="mt-0.5 shrink-0 text-base">{glyph}</span>
      <div className="min-w-0 flex-1">
        {title ? <p className="font-semibold">{title}</p> : null}
        <div className={cn(title && "mt-0.5", "break-words")}>{children}</div>
      </div>
      {onDismiss ? (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Fermer"
          className="shrink-0 rounded-full p-1 hover:bg-black/5"
        >
          <CloseIcon />
        </button>
      ) : null}
    </div>
  );
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-card border border-dashed border-line-strong px-6 py-12 text-center">
      {icon ? (
        <span className="mb-3 flex size-12 items-center justify-center rounded-full bg-ink-100 text-xl text-ink-500">
          {icon}
        </span>
      ) : null}
      <p className="font-display text-base font-semibold text-ink-900">{title}</p>
      {description ? (
        <p className="mt-1 max-w-sm text-sm text-ink-500">{description}</p>
      ) : null}
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  );
}

/** Bloc gris animé, aux dimensions du contenu qu'il remplace. */
export function Skeleton({ className }: { className?: string }) {
  return <span className={cn("block rounded-lg shimmer", className)} aria-hidden />;
}

export function SkeletonList({ rows = 3 }: { rows?: number }) {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="Chargement">
      {Array.from({ length: rows }, (_, index) => (
        <div key={index} className="flex items-center gap-3 rounded-card bg-surface p-4 ring-1 ring-line">
          <Skeleton className="size-10 shrink-0 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-3.5 w-2/5" />
            <Skeleton className="h-3 w-1/4" />
          </div>
          <Skeleton className="h-4 w-20" />
        </div>
      ))}
    </div>
  );
}

/**
 * Barre d'erreur de chargement, avec un bouton pour réessayer.
 *
 * Réessayer est presque toujours la bonne conduite (le cas le plus fréquent est
 * un backend qui n'était pas encore démarré) : ne pas offrir le bouton oblige à
 * recharger la page entière et à reperdre l'état de l'écran.
 */
export function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <Alert tone="danger" title="Chargement impossible">
      <p>{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-2 font-semibold underline underline-offset-2"
      >
        Réessayer
      </button>
    </Alert>
  );
}

/* ---------------------------------------------------------------------------
   Barre de progression
   ------------------------------------------------------------------------ */

export function ProgressBar({
  ratio,
  tone = "brand",
  className,
}: {
  /** Entre 0 et 1. Déjà borné par `progressRatio`. */
  ratio: number;
  tone?: Tone;
  className?: string;
}) {
  const fill: Record<Tone, string> = {
    neutral: "bg-ink-400",
    brand: "bg-kola-600",
    positive: "bg-positive-500",
    warning: "bg-warning-500",
    danger: "bg-danger-500",
    info: "bg-info-500",
  };
  const percent = Math.round(Math.min(1, Math.max(0, ratio)) * 100);

  return (
    <div
      role="progressbar"
      aria-valuenow={percent}
      aria-valuemin={0}
      aria-valuemax={100}
      className={cn("h-2 w-full overflow-hidden rounded-full bg-ink-100", className)}
    >
      <div
        className={cn("h-full rounded-full transition-[width] duration-500", fill[tone])}
        style={{ width: `${percent}%` }}
      />
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Fenêtre modale
   ------------------------------------------------------------------------ */

/**
 * Modale bâtie sur `<dialog>`.
 *
 * L'élément natif apporte gratuitement ce qu'une `<div>` obligerait à
 * réimplémenter : le piège de focus, la fermeture par Échap, l'inertie du
 * contenu situé derrière. On ne garde à sa charge que la fermeture au clic sur
 * le fond, que le natif ne fournit pas.
 */
export function Modal({
  open,
  onClose,
  title,
  children,
  footer,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        /* Le clic sur le fond atteint le `<dialog>` lui-même ; un clic dans le
           panneau atteint un enfant. Comparer la cible distingue les deux sans
           avoir à mesurer des coordonnées. */
        if (event.target === ref.current) onClose();
      }}
      className="m-auto w-[min(30rem,calc(100vw-2rem))] rounded-panel bg-surface p-0 text-ink-900 shadow-raised backdrop:bg-ink-950/40 backdrop:backdrop-blur-sm"
    >
      <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
        <h2 id={titleId} className="font-display text-lg font-semibold">
          {title}
        </h2>
        <IconButton label="Fermer" onClick={onClose} className="-mr-2 -mt-1">
          <CloseIcon />
        </IconButton>
      </div>
      <div className="px-5 py-4">{children}</div>
      {footer ? (
        <div className="flex justify-end gap-2 border-t border-line px-5 py-4">
          {footer}
        </div>
      ) : null}
    </dialog>
  );
}

/* ---------------------------------------------------------------------------
   Divers
   ------------------------------------------------------------------------ */

/** Ligne « libellé → valeur » des écrans de détail. */
export function DetailRow({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: ReactNode;
  mono?: boolean;
}) {
  return (
    <div className="flex items-baseline justify-between gap-4 border-b border-line py-2.5 last:border-0">
      <dt className="text-sm text-ink-500">{label}</dt>
      <dd
        className={cn(
          "text-right text-sm font-medium text-ink-900",
          mono && "tabular font-mono text-xs"
        )}
      >
        {value}
      </dd>
    </div>
  );
}
