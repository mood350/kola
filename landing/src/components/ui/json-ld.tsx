/**
 * Injection d'un bloc de données structurées.
 *
 * POURQUOI UNE BALISE `<script>` NATIVE ET PAS `next/script` : `next/script`
 * orchestre le CHARGEMENT et l'EXÉCUTION de code JavaScript (priorités,
 * stratégies, hydratation). Le JSON-LD n'est pas exécuté : c'est une donnée
 * inerte que les robots lisent dans le HTML. Le faire passer par `next/script`
 * ajoute une couche cliente pour rien, et peut retarder son apparition dans le
 * document — au moment précis où un robot le cherche.
 *
 * POURQUOI L'ÉCHAPPEMENT DE `<` : `JSON.stringify` ne neutralise pas ce
 * caractère. Une chaîne contenant `</script>` — dans un avis, un titre d'étude
 * de cas — fermerait la balise depuis l'intérieur et laisserait le reste de la
 * donnée s'exécuter comme du HTML. Le remplacer par son équivalent Unicode
 * `<` est sans effet sur le JSON analysé, et referme la brèche.
 */
export function JsonLd({ data }: { data: object }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{
        __html: JSON.stringify(data).replace(/</g, "\\u003c"),
      }}
    />
  );
}
