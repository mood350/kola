import { AVATAR_COLOR, AVATAR_EMOJI } from "@/lib/labels";
import { cn } from "@/lib/cn";

/**
 * Avatar de l'utilisateur.
 *
 * Le serveur ne stocke qu'un identifiant (« avatar_03 ») : aucun fichier n'est
 * téléversé, aucun binaire n'est à servir, et le rendu est entièrement local —
 * exactement comme sur mobile (`KolaAvatars`). C'est ce qui fait que l'avatar
 * choisi sur le téléphone apparaît ici sans qu'une seule image ne transite.
 *
 * Sans avatar choisi, on affiche les initiales plutôt qu'une silhouette
 * générique : c'est la seule variante qui distingue encore deux comptes.
 */
export function Avatar({
  avatarId,
  initials,
  size = 40,
  className,
}: {
  avatarId: string | null | undefined;
  initials: string;
  size?: number;
  className?: string;
}) {
  const color = avatarId ? AVATAR_COLOR[avatarId] : undefined;
  const emoji = avatarId ? AVATAR_EMOJI[avatarId] : undefined;

  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full font-semibold select-none",
        !color && "bg-kola-100 text-kola-700",
        className
      )}
      style={{
        width: size,
        height: size,
        fontSize: size * 0.42,
        /* Fond à 15 % d'opacité, comme sur mobile : à pleine saturation, huit
           avatars côte à côte transforment une liste en nuancier. */
        ...(color ? { backgroundColor: `${color}26`, color } : null),
      }}
      aria-hidden
    >
      {emoji ?? initials}
    </span>
  );
}
