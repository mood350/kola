import Link from "next/link";

/**
 * Page introuvable.
 *
 * Volontairement sobre et sans détail : cette page est atteignable sans être
 * connecté, et une adresse mal tapée ne doit rien apprendre sur ce qui existe
 * derrière l'authentification.
 */
export default function NotFound() {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center px-6 text-center">
      <p className="font-display text-6xl font-semibold text-kola-600">404</p>
      <h1 className="mt-4 font-display text-2xl font-semibold text-ink-950">
        Page introuvable
      </h1>
      <p className="mt-2 max-w-sm text-ink-500">
        Cette adresse ne correspond à aucune page de votre espace Kola.
      </p>
      <Link
        href="/"
        className="mt-6 inline-flex h-11 items-center justify-center rounded-full bg-kola-600 px-6 text-sm font-semibold text-white transition-colors hover:bg-kola-700"
      >
        Retour à l&apos;accueil
      </Link>
    </div>
  );
}
