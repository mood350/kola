/**
 * Études de cas.
 *
 * ══════════════════════════════════════════════════════════════════════════
 *   SCÉNARIOS ILLUSTRATIFS — AUCUN N'EST UN CLIENT RÉEL.
 * ══════════════════════════════════════════════════════════════════════════
 *
 * Même statut que `lib/reviews.ts`, et pour la même raison : le service n'est
 * pas ouvert, il n'y a donc pas de résultat client à raconter. Chaque page
 * affiche `CASE_STUDIES_DISCLAIMER` en tête, et aucun balisage `Article` ou
 * `Review` n'est émis — voir `lib/schema.ts`.
 *
 * CE QUI EST VRAI DANS CES SCÉNARIOS : la mécanique. Les scores, paliers,
 * plafonds, taux et délais cités sont ceux que le backend calcule réellement
 * (`ScoringRule`, `CreditTier`, `LoanService`). Un lecteur qui refait le
 * raisonnement doit tomber sur les mêmes chiffres — sinon l'étude de cas
 * devient une promesse que le produit ne tient pas.
 *
 * CE QUI EST INVENTÉ : les personnes, les entreprises et les montants
 * particuliers.
 */

export const CASE_STUDIES_DISCLAIMER =
  "Scénario illustratif. Les personnes et les montants sont fictifs ; le fonctionnement du score, les paliers et les taux cités sont ceux réellement appliqués par Kola.";

export type CaseStudyMetric = {
  label: string;
  value: string;
  /** Précision facultative sous le chiffre — l'unité, la période, la base. */
  note?: string;
};

export type CaseStudy = {
  /** Segment d'URL. Immuable une fois publié : le changer casse les liens. */
  slug: string;
  /** Titre court, pour les cartes et le fil d'Ariane. */
  title: string;
  /** Titre long, pour la balise <title> et le H1 de la page de détail. */
  headline: string;
  /** Une phrase de résumé, reprise en méta-description. */
  summary: string;
  sector: string;
  city: string;
  /** Durée couverte par le scénario. */
  duration: string;
  metrics: CaseStudyMetric[];
  /** Le blocage de départ. */
  challenge: string[];
  /** Ce que Kola change, mécanisme par mécanisme. */
  approach: string[];
  /** Où on aboutit, et à quel coût. */
  outcome: string[];
  quote: {
    text: string;
    author: string;
    role: string;
  };
};

export const CASE_STUDIES: CaseStudy[] = [
  {
    slug: "commerce-de-detail-lome",
    title: "Commerce de détail",
    headline:
      "Financer un stock saisonnier sans historique bancaire",
    summary:
      "Comment une commerçante sans compte bancaire atteint le palier Premium en huit mois et finance son stock de saison à 1,5 % par mois.",
    sector: "Commerce de détail",
    city: "Lomé, Togo",
    duration: "8 mois",
    metrics: [
      { label: "Score atteint", value: "78 / 100", note: "palier Premium" },
      { label: "Plafond débloqué", value: "500 000 XOF", note: "contre 25 000 au départ" },
      { label: "Taux obtenu", value: "1,5 % / mois", note: "contre 3 % au palier Basique" },
      { label: "Décision de prêt", value: "Immédiate", note: "versement sur le portefeuille" },
    ],
    challenge: [
      "Une commerçante en textile réalise l'essentiel de son chiffre d'affaires sur deux pics saisonniers. Pour en profiter, il lui faut acheter le stock deux mois à l'avance — au moment précis où sa trésorerie est au plus bas.",
      "Six ans d'activité, aucune trace exploitable : ventes en espèces, pas de compte bancaire, pas de bulletin de salaire à présenter. Chaque demande de financement bute sur la même impasse — sans historique, pas de crédit ; sans crédit, jamais d'historique.",
      "La seule option restante est le crédit informel, à des taux mensuels qui absorbent la marge de la saison qu'il était censé financer.",
    ],
    approach: [
      "Le portefeuille Kola devient le point de passage des encaissements quotidiens. Aucune déclaration, aucun formulaire : c'est l'usage lui-même qui produit la trace.",
      "La régularité des dépôts pèse 15 points sur 100. Des versements fréquents, même modestes, la font monter plus vite qu'un dépôt unique et important — le critère mesure la constance, pas le volume.",
      "Un coffre d'épargne bloque une somme fixe chaque semaine jusqu'à la date d'achat du stock. La discipline d'épargne vaut 15 points supplémentaires, et le coffre constitue en parallèle un apport propre.",
      "La vérification d'identité est complétée dès l'ouverture : 15 points sont conditionnés au seul niveau de vérification, disponibles immédiatement et sans attendre.",
    ],
    outcome: [
      "Au bout de huit mois, le score atteint 78 points : ancienneté du compte, régularité des dépôts, discipline d'épargne et vérification complète se cumulent. Le palier Premium ouvre un plafond de 500 000 XOF à 1,5 % par mois.",
      "Le stock est financé avant le pic de saison, à un taux connu avant même de valider la demande. La décision est immédiate et le montant versé directement sur le portefeuille.",
      "Effet le plus durable : l'historique existe désormais. Le remboursement alimente à son tour le critère correspondant, qui pèse 15 points — le prochain cycle démarre plus haut.",
    ],
    quote: {
      text: "Je vendais déjà, mais sur le papier je n'existais pas. Mon score a rendu visible ce que je faisais depuis six ans.",
      author: "Aminata D.",
      role: "Commerçante en textile, Lomé",
    },
  },
  {
    slug: "transport-abidjan",
    title: "Transport urbain",
    headline: "Lisser des revenus quotidiens irréguliers",
    summary:
      "Comment un chauffeur de taxi transforme des recettes journalières variables en épargne régulière, et double son plafond de crédit en six mois.",
    sector: "Transport de personnes",
    city: "Abidjan, Côte d'Ivoire",
    duration: "6 mois",
    metrics: [
      { label: "Score atteint", value: "64 / 100", note: "palier Standard" },
      { label: "Plafond débloqué", value: "100 000 XOF", note: "×4 par rapport au départ" },
      { label: "Épargne constituée", value: "En coffre bloqué", note: "virements programmés hebdomadaires" },
      { label: "Commission marchand", value: "0 %", note: "sur les paiements encaissés" },
    ],
    challenge: [
      "Un chauffeur encaisse chaque jour un montant différent, en espèces. Les bonnes journées compensent les mauvaises, mais rien ne le montre : vu de l'extérieur, le revenu paraît instable, donc non finançable.",
      "L'entretien du véhicule, lui, ne s'étale pas : une panne de boîte de vitesses tombe d'un coup et immobilise l'outil de travail — donc supprime le revenu au moment exact où il faudrait payer la réparation.",
      "Épargner sur un compte courant échoue systématiquement : une somme disponible finit toujours par servir à autre chose.",
    ],
    approach: [
      "Les recettes de la journée sont déposées le soir même. Le ratio dépenses / revenus, qui vaut 15 points, mesure la part conservée — pas le montant encaissé. Un revenu modeste mais bien tenu note mieux qu'un revenu élevé intégralement dépensé.",
      "Un virement programmé hebdomadaire alimente automatiquement un coffre. L'automatisation est ce qui rend l'épargne effective : elle ne dépend plus d'une décision à reprendre chaque semaine.",
      "Les courses réglées par paiement marchand passent par le portefeuille, sans commission. Elles nourrissent le volume d'activité (10 points) et la diversité du réseau (5 points).",
    ],
    outcome: [
      "Six mois plus tard, le score s'établit à 64 points. Le palier Standard porte le plafond à 100 000 XOF à 2 % par mois, contre 25 000 à 3 % au palier Basique.",
      "La réparation imprévue est couverte sans immobiliser le véhicule, et sans recourir au crédit informel.",
      "Le coffre reste intact : il n'a pas servi d'amortisseur, c'est le crédit qui a joué ce rôle. L'épargne continue donc de courir, et avec elle la discipline d'épargne comptée dans le score.",
    ],
    quote: {
      text: "Bloquer l'argent était la seule méthode qui a marché. Le reste a suivi tout seul.",
      author: "Koffi A.",
      role: "Chauffeur de taxi, Abidjan",
    },
  },
  {
    slug: "services-domicile-dakar",
    title: "Services à domicile",
    headline: "Passer de la clientèle de quartier au réseau étendu",
    summary:
      "Comment une activité de services à domicile élargit sa base de clients réguliers et atteint le palier Standard en cinq mois.",
    sector: "Services à la personne",
    city: "Dakar, Sénégal",
    duration: "5 mois",
    metrics: [
      { label: "Score atteint", value: "61 / 100", note: "palier Standard" },
      { label: "Critère décisif", value: "Diversité du réseau", note: "5 points, souvent les derniers manquants" },
      { label: "Taux obtenu", value: "2 % / mois", note: "palier Standard" },
      { label: "Score recalculé", value: "Tous les 30 jours", note: "ou à la demande" },
    ],
    challenge: [
      "Une coiffeuse à domicile travaille pour une clientèle fidèle mais étroite, réglée en espèces. Son activité est stable ; sa trace financière, inexistante.",
      "Acheter du matériel professionnel suppose une avance de trésorerie qu'aucun organisme n'accorde sans justificatif de revenus — document qu'une activité indépendante non déclarée ne peut pas produire.",
      "Premier score obtenu : 38 points, soit deux points sous le seuil de 40 qui ouvre le premier palier de crédit. Assez pour être frustrant, pas assez pour servir à quelque chose.",
    ],
    approach: [
      "Les règlements passent au paiement marchand plutôt qu'aux espèces. Chaque client réglant via Kola compte dans la diversité du réseau — un critère à 5 points, précisément la marge qui manquait.",
      "L'écran de score affiche les huit critères avec leur poids et le niveau atteint sur chacun. Le diagnostic est immédiat : ce n'était pas le volume qui bloquait, mais la concentration sur trop peu de contreparties.",
      "Un score reste valable trente jours, mais un recalcul peut être déclenché manuellement depuis l'application — inutile d'attendre l'expiration pour constater l'effet d'un changement d'habitude.",
    ],
    outcome: [
      "Cinq mois plus tard, le score atteint 61 points : le palier Standard ouvre 100 000 XOF à 2 % par mois. Le matériel est financé, remboursé sur le cycle suivant.",
      "L'enseignement dépasse le cas particulier : c'est le critère le plus léger du barème — 5 points sur 100 — qui a fait basculer le dossier. Un score décomposé permet de le voir ; un score opaque ne l'aurait jamais laissé apparaître.",
    ],
    quote: {
      text: "Il me manquait deux points. Je ne l'aurais jamais su avec une réponse qui dit seulement « refusé ».",
      author: "Fatou S.",
      role: "Coiffeuse à domicile, Dakar",
    },
  },
];

/** Recherche par segment d'URL, utilisée par la route dynamique. */
export function findCaseStudy(slug: string): CaseStudy | undefined {
  return CASE_STUDIES.find((study) => study.slug === slug);
}
