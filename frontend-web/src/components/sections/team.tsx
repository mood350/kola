import Image from "next/image";
import { Container, Section, SectionHeading } from "@/components/ui/section";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import { TEAM, TEAM_PHOTO } from "@/lib/team";

/**
 * L'équipe.
 *
 * DEUX ÉTATS, SELON QUE LA PHOTO EXISTE OU NON (`lib/team.ts`) :
 *
 *   - photo renseignée — rendue par `next/image`, qui produit les variantes
 *     AVIF/WebP, sert la taille adaptée à l'écran via `sizes`, et réserve le
 *     ratio avant l'arrivée du fichier ;
 *   - photo absente — un visuel dessiné prend sa place. Il ne représente
 *     personne et ne cherche pas à le faire croire.
 *
 * `priority` n'est PAS posé : cette section est loin sous la ligne de
 * flottaison. Le réserver aux images réellement visibles au chargement est ce
 * qui lui donne son effet — appliqué partout, il ne hiérarchise plus rien et
 * retarde ce qui compte vraiment.
 */
export function Team() {
  return (
    <Section id="equipe">
      <Container>
        <SectionHeading
          eyebrow="L'équipe"
          title="Qui construit Kola"
          lead="Un service financier engage la confiance de ceux qui le confient leur argent. Savoir qui est derrière fait partie de cette confiance."
        />

        <Reveal className="mt-12">
          {TEAM_PHOTO ? (
            <figure>
              <Image
                src={TEAM_PHOTO.src}
                width={TEAM_PHOTO.width}
                height={TEAM_PHOTO.height}
                /* Le texte alternatif décrit la scène, il ne la nomme pas.
                   « Photo de l'équipe » n'apprend rien à quelqu'un qui ne voit
                   pas l'image ; la composition, le lieu et le nombre de
                   personnes, si. */
                alt={TEAM_PHOTO.alt}
                /* `sizes` décrit la largeur d'AFFICHAGE, pas celle du fichier.
                   Sans lui, le navigateur suppose la pleine largeur du viewport
                   et télécharge systématiquement la plus grande variante. */
                sizes="(min-width: 1024px) 1024px, 100vw"
                className="w-full rounded-panel object-cover border border-hairline shadow-card"
              />
              <figcaption className="mt-4 text-[0.8125rem] text-ink-400">
                {TEAM_PHOTO.alt}
              </figcaption>
            </figure>
          ) : (
            <TeamPlaceholder />
          )}
        </Reveal>

        <RevealGroup className="mt-4 grid gap-px overflow-hidden rounded-card bg-ink-200 sm:grid-cols-2 lg:grid-cols-4">
          {TEAM.map((member) => (
            <div key={member.id} data-animate className="bg-surface p-7">
              <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
                {member.role}
              </p>
              <p className="font-headline mt-2.5 text-lg font-semibold text-ink-950">
                {member.name}
              </p>
              <p className="mt-3 text-[0.875rem] leading-relaxed text-ink-500">
                {member.focus}
              </p>
            </div>
          ))}
        </RevealGroup>
      </Container>
    </Section>
  );
}

/**
 * Visuel de remplacement, en attendant la photo réelle.
 *
 * Quatre silhouettes abstraites sur la trame technique du site — assez pour
 * occuper l'emplacement et en donner l'échelle, pas assez pour être prises pour
 * des personnes. Dessiné en SVG selon le même vocabulaire que
 * `ui/illustrations.tsx` : aucun octet réseau, net sur écran haute densité,
 * couleurs suivant les tokens de marque.
 *
 * `role="img"` avec un `aria-label` explicite : le lecteur d'écran annonce
 * qu'il s'agit d'un visuel d'attente, plutôt que de rester silencieux sur un
 * grand bloc vide au milieu de la page.
 */
function TeamPlaceholder() {
  return (
    <div
      role="img"
      aria-label="Emplacement réservé à la photo de l'équipe Kola — illustration provisoire, aucune personne réelle n'y est représentée."
      className="relative aspect-[3/1.4] overflow-hidden rounded-panel bg-surface/50"
    >
      <div aria-hidden="true" className="tech-grid absolute inset-0" />
      <div
        aria-hidden="true"
        className="absolute inset-x-0 -top-24 h-[24rem] bg-[radial-gradient(ellipse_60%_60%_at_50%_0%,rgb(158_161_246/0.35)_0%,transparent_70%)]"
      />

      <svg
        viewBox="0 0 420 196"
        fill="none"
        aria-hidden="true"
        className="absolute inset-0 h-full w-full"
      >
        {[
          { x: 74, scale: 1, fill: "var(--color-kola-300)" },
          { x: 158, scale: 1.14, fill: "var(--color-kola-500)" },
          { x: 250, scale: 1.14, fill: "var(--color-kola-600)" },
          { x: 334, scale: 1, fill: "var(--color-kola-300)" },
        ].map((figure) => (
          <g key={figure.x} transform={`translate(${figure.x} 196) scale(${figure.scale})`}>
            {/* Buste : un arc posé sur la ligne de base. */}
            <path
              d="M-34 0v-30a34 34 0 0 1 68 0V0z"
              fill={figure.fill}
              opacity="0.9"
            />
            {/* Tête */}
            <circle cx="0" cy="-52" r="19" fill={figure.fill} />
          </g>
        ))}
      </svg>

      <p className="absolute inset-x-0 bottom-5 text-center text-[0.75rem] text-ink-500">
        Photo d&apos;équipe à venir
      </p>
    </div>
  );
}
