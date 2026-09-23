/**
 * Contenu des conditions générales.
 *
 * Rédaction préparatoire, alignée sur ce que le backend fait réellement
 * (plafonds KYC, verrouillage après cinq échecs, barème de score à huit
 * critères, paliers de prêt, non-suppression des portefeuilles). Écrire ici des
 * clauses que le code ne tient pas transformerait un document juridique en
 * fausse déclaration.
 *
 * Ce texte n'a pas été relu par un juriste — l'avertissement affiché en bas de
 * la page le dit explicitement au visiteur.
 */

export const CGU_UPDATED_AT = "2 août 2026";

export type CguArticle = {
  id: string;
  title: string;
  body: string[];
  list?: string[];
};

export const CGU_ARTICLES: CguArticle[] = [
  {
    id: "objet",
    title: "Objet et acceptation",
    body: [
      "Les présentes conditions régissent l'accès et l'utilisation de Kola, service de portefeuille électronique et de micro-crédit destiné aux particuliers d'Afrique de l'Ouest. Elles s'appliquent dès la création d'un compte.",
      "L'ouverture d'un compte vaut acceptation sans réserve. En cas de désaccord avec l'une de ces clauses, il convient de ne pas utiliser le service.",
    ],
  },
  {
    id: "compte",
    title: "Ouverture et activation du compte",
    body: [
      "L'inscription requiert une adresse e-mail valide et un numéro de téléphone au format international. Le numéro de téléphone identifie le compte de façon unique et sert de référence pour les transferts entrants.",
      "Le compte n'est pas actif à l'inscription : il le devient après saisie du code de confirmation à six chiffres envoyé par e-mail. Ce code expire au bout de quinze minutes.",
    ],
  },
  {
    id: "verification",
    title: "Niveaux de vérification et plafonds",
    body: [
      "Chaque compte relève d'un niveau de vérification qui détermine ses plafonds journaliers, en entrée comme en sortie. Les montants sont exprimés en francs CFA (XOF), seule devise prise en charge.",
      "Les plafonds d'envoi cumulent transferts, retraits et paiements marchands sur une journée calendaire :",
    ],
    list: [
      "Niveau 0 (téléphone confirmé) : 50 000 XOF par jour en sortie, 100 000 XOF en rechargement.",
      "Niveau 1 (e-mail vérifié) : 200 000 XOF par jour en sortie, 500 000 XOF en rechargement.",
      "Niveau 2 (pièce d'identité fournie) : 1 000 000 XOF par jour en sortie, 2 000 000 XOF en rechargement.",
      "Niveau 3 (identité validée) : plafonds étendus, définis au cas par cas.",
    ],
  },
  {
    id: "score",
    title: "Score de confiance",
    body: [
      "Kola calcule un score de 0 à 100 à partir de huit critères pondérés : régularité des dépôts, niveau de vérification, historique de remboursement, discipline d'épargne, ratio dépenses/revenus, ancienneté du compte, volume d'activité et diversité du réseau de bénéficiaires.",
      "Le détail du calcul est consultable à tout moment dans l'application, critère par critère. Le score est recalculé au fil des opérations et reste valable trente jours avant réévaluation.",
      "Un défaut de paiement passé demeure visible dans l'historique de scoring même après régularisation. Aucune décision de crédit n'est prise sans que l'utilisateur puisse en consulter les motifs chiffrés.",
    ],
  },
  {
    id: "credit",
    title: "Micro-crédit",
    body: [
      "L'accès au crédit est ouvert à partir d'un score de 40. Le score détermine le plafond empruntable et le taux mensuel appliqué, connus avant toute souscription.",
      "Un seul prêt non soldé est autorisé à la fois. Le montant total dû est fixé à l'octroi et n'évolue plus ensuite : principal augmenté des intérêts sur la durée choisie.",
      "Une échéance dépassée place le prêt en défaut. Il reste remboursable après cette bascule, mais aucun nouveau prêt ne peut être souscrit tant qu'il n'est pas régularisé.",
    ],
    list: [
      "Palier Basique (score 40 à 59) : jusqu'à 25 000 XOF à 3 % par mois.",
      "Palier Standard (60 à 74) : jusqu'à 100 000 XOF à 2 % par mois.",
      "Palier Premium (75 à 89) : jusqu'à 500 000 XOF à 1,5 % par mois.",
      "Palier Élite (90 à 100) : jusqu'à 2 000 000 XOF à 1 % par mois.",
    ],
  },
  {
    id: "frais",
    title: "Frais",
    body: [
      "Les frais sont annoncés avant validation de chaque opération, jamais après. Ils sont prélevés sur le portefeuille émetteur et tracés séparément dans l'historique.",
      "Les rechargements et les paiements marchands ne supportent aucun frais côté client. Les transferts et les retraits sont soumis à une commission proportionnelle, indiquée à l'écran de confirmation.",
    ],
  },
  {
    id: "securite",
    title: "Sécurité du compte",
    body: [
      "L'utilisateur est responsable de la confidentialité de ses identifiants. Cinq tentatives de connexion infructueuses consécutives entraînent le verrouillage automatique du compte pendant trente minutes.",
      "Toute connexion depuis un appareil ou une adresse réseau inconnus déclenche une notification. Il appartient à l'utilisateur de signaler sans délai toute activité qu'il ne reconnaît pas.",
    ],
  },
  {
    id: "surveillance",
    title: "Surveillance des opérations",
    body: [
      "Conformément aux obligations de lutte contre le blanchiment et le financement du terrorisme, les opérations font l'objet d'une analyse automatisée continue.",
      "Kola peut suspendre une opération, geler un compte ou transmettre un signalement aux autorités compétentes lorsque la réglementation l'impose. Ces mesures peuvent être appliquées sans préavis lorsque la loi l'exige.",
    ],
  },
  {
    id: "donnees",
    title: "Données personnelles",
    body: [
      "Les données collectées servent à fournir le service, satisfaire aux obligations réglementaires et calculer le score de confiance. Elles ne sont ni vendues ni cédées à des fins publicitaires.",
      "Les portefeuilles et les opérations ne sont jamais supprimés de la base : ils sont désactivés. Cette conservation répond aux exigences de traçabilité applicables aux services de paiement, et garantit qu'un historique reste vérifiable.",
      "Toute demande d'accès, de rectification ou d'information sur le traitement peut être adressée via la page de contact.",
    ],
  },
  {
    id: "responsabilite",
    title: "Responsabilité et disponibilité",
    body: [
      "Kola s'engage à mettre en œuvre les moyens raisonnables pour assurer la disponibilité du service, sans garantie d'un fonctionnement ininterrompu. Les interruptions liées à la maintenance, aux réseaux des opérateurs partenaires ou à un cas de force majeure n'ouvrent pas droit à indemnisation.",
      "La responsabilité de Kola ne saurait être engagée en cas d'usage du compte par un tiers résultant d'un défaut de vigilance de l'utilisateur sur ses identifiants.",
    ],
  },
  {
    id: "evolution",
    title: "Évolution des conditions",
    body: [
      "Ces conditions peuvent être modifiées pour tenir compte d'évolutions légales, réglementaires ou fonctionnelles. Toute modification substantielle est notifiée dans l'application avant son entrée en vigueur.",
      "La poursuite de l'utilisation du service après cette notification vaut acceptation de la version révisée.",
    ],
  },
];
