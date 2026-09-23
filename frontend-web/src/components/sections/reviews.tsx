import Link from "next/link";
import { Container, Section, SectionHeading } from "@/components/ui/section";
import { Reveal, RevealGroup } from "@/components/motion/reveal";
import {
  REVIEWS,
  REVIEWS_ARE_REAL,
  REVIEWS_AVERAGE,
  REVIEWS_DISCLAIMER,
} from "@/lib/reviews";

/**
 * Avis utilisateurs.
 *
 * LE BLOC D'AVERTISSEMENT N'EST PAS OPTIONNEL tant que `REVIEWS_ARE_REAL` vaut
 * `false`. Il est placé AVANT les avis, à taille lisible, et non en note de bas
 * de section : un visiteur doit savoir qu'il lit des scénarios avant de les
 * lire, pas après. C'est aussi ce qui distingue une maquette honnête d'un faux
 * témoignage — la seule différence entre les deux tient à cette mention.
 *
 * Aucune donnée structurée n'est émise ici : `lib/schema.ts` retient le
 * balisage `Review` et `AggregateRating` sur la même constante. Voir le
 * commentaire de `lib/reviews.ts` pour le détail du risque encouru.
 */
export function Reviews() {
  return (
    <Section id="avis">
      <Container>
        <SectionHeading
          eyebrow="Ce qu'en disent les utilisateurs"
          title="Le score vu par ceux qui s'en servent"
          lead={
            REVIEWS_ARE_REAL
              ? `Note moyenne de ${REVIEWS_AVERAGE.toString().replace(".", ",")} sur 5, sur ${REVIEWS.length} avis vérifiés.`
              : "Quatre parcours représentatifs de ce que le produit rend possible."
          }
        />

        {!REVIEWS_ARE_REAL ? (
          <Reveal>
            <p className="mt-8 rounded-card border border-ochre-300 bg-ochre-200/40 p-6 text-[0.875rem] leading-relaxed text-ink-700">
              <strong className="font-semibold text-ink-950">
                Scénarios illustratifs.
              </strong>{" "}
              {REVIEWS_DISCLAIMER}
            </p>
          </Reveal>
        ) : null}

        <RevealGroup className="mt-10 grid gap-4 sm:grid-cols-2">
          {REVIEWS.map((review) => (
            <article
              key={review.id}
              data-animate
              className="flex flex-col rounded-card bg-surface p-7 shadow-soft sm:p-8"
            >
              <Stars rating={review.rating} />

              <h3 className="font-display mt-5 text-lg font-semibold text-ink-950">
                {review.headline}
              </h3>

              <p className="mt-3 flex-1 text-[0.9375rem] leading-relaxed text-ink-600">
                {review.body}
              </p>

              <footer className="mt-6 flex items-center gap-3 border-t border-ink-200 pt-5">
                {/* Initiale en pastille plutôt qu'une photo : nous n'avons pas
                    de portrait de ces personnes, et un visage tiré d'une banque
                    d'images transformerait un scénario assumé en imposture. */}
                <span
                  aria-hidden="true"
                  className="font-display flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-kola-100 text-[0.9375rem] font-semibold text-kola-700"
                >
                  {review.author.charAt(0)}
                </span>
                <div className="min-w-0">
                  <p className="text-[0.875rem] font-medium text-ink-800">
                    {review.author}
                  </p>
                  <p className="text-[0.8125rem] text-ink-500">
                    {review.role} · {review.city}
                  </p>
                </div>
              </footer>
            </article>
          ))}
        </RevealGroup>

        <Reveal delay={0.1}>
          <p className="mt-8 text-[0.9375rem] text-ink-500">
            Chacun de ces parcours est détaillé, chiffre par chiffre, dans{" "}
            <Link
              href="/etudes-de-cas"
              className="font-medium text-kola-700 underline underline-offset-2 transition-colors hover:text-kola-600"
            >
              les études de cas
            </Link>
            .
          </p>
        </Reveal>
      </Container>
    </Section>
  );
}

/**
 * Note en étoiles.
 *
 * Les étoiles sont décoratives : la note est donnée en toutes lettres dans le
 * texte masqué visuellement qui les accompagne. Compter des icônes n'est pas
 * une tâche qu'on impose à un lecteur d'écran, et une note rendue uniquement
 * par une couleur ou une forme n'est pas perceptible par tous.
 */
function Stars({ rating }: { rating: number }) {
  return (
    <p className="flex items-center gap-1">
      <span className="sr-only">{rating} étoiles sur 5</span>
      {[1, 2, 3, 4, 5].map((position) => (
        <svg
          key={position}
          viewBox="0 0 20 20"
          aria-hidden="true"
          className={
            position <= rating
              ? "h-4 w-4 fill-ochre-400"
              : "h-4 w-4 fill-ink-200"
          }
        >
          <path d="M10 1.5l2.47 5.01 5.53.8-4 3.9.94 5.5L10 14.12l-4.94 2.6.94-5.5-4-3.9 5.53-.8L10 1.5z" />
        </svg>
      ))}
    </p>
  );
}
