/**
 * Avis utilisateurs.
 *
 * ══════════════════════════════════════════════════════════════════════════
 *   CES AVIS SONT DES SCÉNARIOS ILLUSTRATIFS, PAS DES TÉMOIGNAGES RECUEILLIS.
 * ══════════════════════════════════════════════════════════════════════════
 *
 * Kola n'est pas encore en service : personne n'a pu s'en servir, donc personne
 * n'a pu en témoigner. Ces textes montrent à quoi ressemblerait la section une
 * fois de vrais retours collectés.
 *
 * DEUX CONSÉQUENCES, DÉLIBÉRÉES, À NE PAS DÉFAIRE PAR INADVERTANCE :
 *
 * 1. La mention `REVIEWS_DISCLAIMER` est affichée dans la section, pas enfouie
 *    dans une note de bas de page. Un visiteur ne doit pas pouvoir croire qu'il
 *    lit l'expérience de quelqu'un.
 *
 * 2. AUCUN balisage `Review` ni `AggregateRating` n'est émis pour ces
 *    contenus — voir `lib/schema.ts`. Publier des avis inventés en données
 *    structurées revient à demander à Google d'afficher des étoiles fondées sur
 *    rien : c'est une violation caractérisée de ses règles sur les avis, passible
 *    d'une action manuelle sur tout le domaine. Le gain d'affichage est nul, le
 *    risque porte sur l'ensemble du site.
 *
 * QUAND DE VRAIS AVIS EXISTERONT : remplacer les entrées ci-dessous, passer
 * `REVIEWS_ARE_REAL` à `true` (ce qui masque la mention et active le balisage
 * `Review` + `AggregateRating` dans `lib/schema.ts`), et conserver une trace
 * vérifiable du consentement de chaque personne citée.
 */

export const REVIEWS_ARE_REAL = false;

export const REVIEWS_DISCLAIMER =
  "Kola n'est pas encore ouvert au public. Les retours ci-dessous sont des scénarios illustratifs rédigés par l'équipe, et non des témoignages d'utilisateurs. Ils seront remplacés par de vrais avis au lancement.";

export type Review = {
  id: string;
  /** Note sur 5. Conservée même en mode illustratif : la section la met en forme. */
  rating: number;
  /** Une phrase d'accroche — c'est elle qu'on lit en balayant la page. */
  headline: string;
  body: string;
  author: string;
  role: string;
  city: string;
  /** Date au format ISO, pour un tri stable et un futur balisage `datePublished`. */
  date: string;
};

export const REVIEWS: Review[] = [
  {
    id: "aminata",
    rating: 5,
    headline: "Mon activité est enfin devenue lisible",
    body: "Je vends du tissu depuis six ans et aucune banque n'avait jamais rien vu de cette activité. En quatre mois d'utilisation, mon score est monté à 71 et j'ai pu emprunter de quoi acheter un stock avant la saison. Ce que j'ai apprécié, c'est de voir précisément quel critère me faisait gagner des points.",
    author: "Aminata D.",
    role: "Commerçante en textile",
    city: "Lomé",
    date: "2026-05-14",
  },
  {
    id: "koffi",
    rating: 5,
    headline: "Les coffres m'ont forcé à mettre de côté",
    body: "Je n'arrivais jamais à épargner : l'argent disponible finissait toujours par partir. Bloquer une somme jusqu'à une date choisie a réglé le problème en une fois. Effet secondaire que je n'attendais pas : la discipline d'épargne compte dans le score, donc mon plafond de crédit a suivi.",
    author: "Koffi A.",
    role: "Chauffeur de taxi",
    city: "Abidjan",
    date: "2026-04-02",
  },
  {
    id: "fatou",
    rating: 4,
    headline: "Une décision de prêt sans dossier à monter",
    body: "Demande faite un dimanche soir, réponse immédiate, montant sur le portefeuille dans la foulée. Aucun papier, aucun garant. Je retire une étoile parce que j'aurais aimé pouvoir rembourser par anticipation depuis l'application, ce qui n'est pas encore possible.",
    author: "Fatou S.",
    role: "Coiffeuse à domicile",
    city: "Dakar",
    date: "2026-06-21",
  },
  {
    id: "ibrahim",
    rating: 5,
    headline: "Je comprends enfin comment je suis noté",
    body: "J'ai déjà été refusé par un organisme de crédit sans jamais savoir pourquoi. Ici, les huit critères sont affichés avec leur poids et mon niveau sur chacun. Je sais exactement quoi faire pour progresser, et ça change tout par rapport à une décision qui tombe sans explication.",
    author: "Ibrahim T.",
    role: "Mécanicien",
    city: "Ouagadougou",
    date: "2026-03-09",
  },
];

/**
 * Moyenne calculée, jamais saisie à la main : une note affichée qui ne
 * correspond pas aux avis listés juste en dessous se repère immédiatement.
 */
export const REVIEWS_AVERAGE =
  Math.round(
    (REVIEWS.reduce((total, review) => total + review.rating, 0) /
      REVIEWS.length) *
      10
  ) / 10;
