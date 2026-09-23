/**
 * Contenu et données de la landing page.
 *
 * Les règles de scoring et les paliers de prêt reproduisent fidèlement le
 * moteur réel du backend Kola (`ScoringRule`, `CreditTier`) : la page promet
 * exactement ce que le produit calcule. Toute divergence ici deviendrait une
 * promesse commerciale que le code ne tient pas.
 */

/**
 * Ancres absolues (« /#section ») et non relatives : depuis /cgu ou /contact,
 * un « #score » nu chercherait la section sur la page courante et ne ferait
 * rien.
 */

/**
 * Navigation principale — QUATRE ENTRÉES, PAS UNE DE PLUS.
 *
 * La barre doit tenir sur une ligne à 1024 px avec le logo et le bouton
 * d'action ; au-delà de quatre libellés, elle passe à la ligne ou comprime le
 * bouton. Le tri retenu mélange volontairement deux ancres de la page
 * d'accueil et deux pages entières : ce sont ces dernières qui ont besoin
 * d'être atteignables depuis N'IMPORTE OÙ sur le site, une ancre restant
 * accessible en faisant défiler.
 *
 * « Fonctionnalités » et « Sécurité » sortent d'ici et vivent dans le pied de
 * page : elles se rencontrent naturellement en descendant l'accueil.
 */
export const NAV_LINKS = [
  { label: "Comment ça marche", href: "/#probleme" },
  { label: "Score de confiance", href: "/#score" },
  { label: "Études de cas", href: "/etudes-de-cas" },
  { label: "À propos", href: "/a-propos" },
] as const;

/**
 * Maillage du pied de page.
 *
 * Il ne recopie pas la navigation : il l'ÉTEND. Le pied de page est le seul
 * endroit présent sur toutes les routes où l'on peut lister l'intégralité du
 * site sans encombrer la lecture — ce qui en fait le point d'entrée par lequel
 * un robot d'indexation découvre les pages profondes, et le filet de secours
 * d'un visiteur arrivé au bas d'une page sans avoir trouvé ce qu'il cherchait.
 *
 * Trois colonnes, trois intentions distinctes : parcourir l'accueil, changer de
 * page, consulter un document légal. Un pied de page qui mélange les trois se
 * lit comme une liste de liens et ne guide personne.
 */
export const FOOTER_DISCOVER = [
  { label: "Comment ça marche", href: "/#probleme" },
  { label: "Fonctionnalités", href: "/#fonctionnalites" },
  { label: "Score de confiance", href: "/#score" },
  { label: "Sécurité", href: "/#securite" },
  { label: "Avis", href: "/#avis" },
  { label: "Questions fréquentes", href: "/#faq" },
] as const;

export const FOOTER_EXPLORE = [
  { label: "Études de cas", href: "/etudes-de-cas" },
  { label: "À propos", href: "/a-propos" },
  { label: "Nous contacter", href: "/contact" },
] as const;

export const FOOTER_LEGAL = [
  { label: "Conditions générales", href: "/cgu" },
  { label: "Confidentialité", href: "/confidentialite" },
  { label: "Cookies", href: "/cookies" },
] as const;

/* ---------------------------------------------------------------------------
   Problème / solution
   ------------------------------------------------------------------------ */

/**
 * Le parcours produit, en trois étapes.
 *
 * Remplace l'ancien diptyque problème/solution, qui faisait près de 120 mots
 * de prose avant qu'on comprenne à quoi sert Kola. Ici chaque étape tient en
 * une phrase et s'appuie sur une illustration : le lecteur saisit le
 * mécanisme sans avoir à lire un paragraphe.
 */
export const STEPS = [
  {
    title: "Vous utilisez Kola",
    body: "Rechargez, payez, épargnez. Rien à déclarer, rien à remplir.",
  },
  {
    title: "Votre score monte",
    body: "Huit critères mesurent votre régularité et vous notent de 0 à 100.",
  },
  {
    title: "Le crédit s'ouvre",
    body: "Votre score fixe votre plafond, jusqu'à 2 000 000 XOF.",
  },
] as const;

export const PROBLEM_LEAD =
  "En Afrique de l'Ouest, un adulte sur deux n'a pas de compte bancaire. Sans historique, pas de crédit — et sans crédit, jamais d'historique. Kola casse cette boucle.";


/* ---------------------------------------------------------------------------
   Fonctionnalités
   ------------------------------------------------------------------------ */

export type Feature = {
  id: string;
  title: string;
  description: string;
};

/** Une ligne par fonction : la page en compte six, elles doivent se balayer. */
export const FEATURES: Feature[] = [
  {
    id: "wallet",
    title: "Portefeuille XOF",
    description: "Rechargez depuis votre mobile money habituel.",
  },
  {
    id: "transfert",
    title: "Transferts instantanés",
    description: "Envoyez par simple numéro, reçu immédiatement.",
  },
  {
    id: "coffres",
    title: "Coffres d'épargne",
    description: "Bloquez une somme jusqu'à la date de votre choix.",
  },
  {
    id: "programmes",
    title: "Virements programmés",
    description: "Épargnez chaque semaine sans y penser.",
  },
  {
    id: "marchands",
    title: "Paiement marchand",
    description: "Scannez, payez. Sans commission.",
  },
  {
    id: "credit",
    title: "Micro-crédit intégré",
    description:
      "Empruntez dans la limite que votre score débloque, à un taux connu d'avance. Décision immédiate, versement direct sur votre portefeuille.",
  },
];

/* ---------------------------------------------------------------------------
   Score de confiance — miroir exact de ScoringRule (backend)
   ------------------------------------------------------------------------ */

export type ScoreRule = {
  label: string;
  weight: number;
};

/** Somme des poids = 100. */
export const SCORE_RULES: ScoreRule[] = [
  {
    label: "Régularité des dépôts",
    weight: 15,
  },
  {
    label: "Niveau de vérification",
    weight: 15,
  },
  {
    label: "Historique de remboursement",
    weight: 15,
  },
  {
    label: "Discipline d'épargne",
    weight: 15,
  },
  {
    label: "Ratio dépenses / revenus",
    weight: 15,
  },
  {
    label: "Ancienneté du compte",
    weight: 10,
  },
  {
    label: "Volume d'activité",
    weight: 10,
  },
  {
    label: "Diversité du réseau",
    weight: 5,
  },
];

export type CreditTier = {
  name: string;
  range: string;
  min: number;
  ceiling: string;
  rate: string;
  /**
   * Rampe séquentielle d'une seule teinte : le palier est une magnitude
   * ordonnée, pas une catégorie. Un dégradé rouge → vert suggérerait à tort
   * un axe « mauvais / bon », alors qu'il s'agit d'un niveau d'accès.
   * La couleur n'est jamais seule porteuse d'information : le nom du palier
   * et la plage chiffrée sont toujours affichés à côté.
   */
  swatch: string;
};

export const CREDIT_TIERS: CreditTier[] = [
  {
    name: "Basique",
    range: "40 – 59",
    min: 40,
    ceiling: "25 000 XOF",
    rate: "3 % / mois",
    swatch: "var(--color-kola-200)",
  },
  {
    name: "Standard",
    range: "60 – 74",
    min: 60,
    ceiling: "100 000 XOF",
    rate: "2 % / mois",
    swatch: "var(--color-kola-400)",
  },
  {
    name: "Premium",
    range: "75 – 89",
    min: 75,
    ceiling: "500 000 XOF",
    rate: "1,5 % / mois",
    swatch: "var(--color-kola-600)",
  },
  {
    name: "Élite",
    range: "90 – 100",
    min: 90,
    ceiling: "2 000 000 XOF",
    rate: "1 % / mois",
    swatch: "var(--color-kola-800)",
  },
];

/** Valeur mise en scène par la jauge animée. */
export const DEMO_SCORE = 78;

/* ---------------------------------------------------------------------------
   Sécurité
   ------------------------------------------------------------------------ */

export const SECURITY_ITEMS = [
  {
    title: "Verrouillage après 5 échecs",
    body: "Le compte se ferme 30 minutes après cinq tentatives de connexion ratées.",
  },
  {
    title: "Vérification en deux temps",
    body: "Aucun compte n'est actif sans confirmation par code e-mail.",
  },
  {
    title: "Alerte nouvel appareil",
    body: "Toute connexion inconnue déclenche une notification immédiate.",
  },
  {
    title: "Surveillance anti-blanchiment",
    body: "Neuf règles analysent chaque opération, sans jamais la ralentir.",
  },
  {
    title: "Plafonds proportionnés",
    body: "Vos limites dépendent de votre niveau de vérification, dans les deux sens.",
  },
  {
    title: "Historique inaltérable",
    body: "Portefeuilles et opérations sont désactivés, jamais supprimés.",
  },
] as const;

/* ---------------------------------------------------------------------------
   Chiffres
   ------------------------------------------------------------------------ */

export type Stat = {
  value: number;
  suffix: string;
  prefix?: string;
  label: string;
  decimals?: number;
};

export const STATS: Stat[] = [
  { value: 100, suffix: "", label: "points de score, 8 critères explicités" },
  { value: 8, suffix: " s", label: "pour une décision de prêt, versement inclus" },
  { value: 0, suffix: " %", label: "de commission sur les paiements marchands" },
  { value: 30, suffix: " j", label: "de validité d'un score avant recalcul" },
];

export const TESTIMONIAL = {
  quote:
    "Je vendais déjà, mais sur le papier je n'existais pas. Mon score a rendu visible ce que je faisais depuis six ans.",
  author: "Aminata D.",
  role: "Commerçante, Lomé",
} as const;

/* ---------------------------------------------------------------------------
   CTA
   ------------------------------------------------------------------------ */

export const FINAL_CTA = {
  title: "Votre historique commence à votre premier dépôt.",
  body: "Ouvrez un compte en quelques minutes. Le score se construit ensuite, tout seul, à mesure que vous utilisez Kola.",
} as const;

/* ---------------------------------------------------------------------------
   Contact
   ------------------------------------------------------------------------ */

export const CONTACT_EMAIL = "contact@kola.africa";

export const CONTACT_CHANNELS = [
  {
    id: "email",
    label: "E-mail",
    value: CONTACT_EMAIL,
    href: `mailto:${CONTACT_EMAIL}`,
    note: "Réponse sous deux jours ouvrés.",
  },
  {
    id: "support",
    label: "Support utilisateur",
    value: "aide@kola.africa",
    href: "mailto:aide@kola.africa",
    note: "Compte bloqué, opération contestée, question sur un prêt.",
  },
  {
    id: "presse",
    label: "Presse et partenariats",
    value: "presse@kola.africa",
    href: "mailto:presse@kola.africa",
    note: "Demandes média, intégrations, partenariats marchands.",
  },
] as const;

export const CONTACT_FAQ = [
  {
    q: "Mon compte est verrouillé, que faire ?",
    a: "Le verrouillage après cinq tentatives de connexion échouées est automatique et dure trente minutes. Passé ce délai, la connexion redevient possible sans intervention.",
  },
  {
    q: "Pourquoi mon score n'a-t-il pas bougé ?",
    a: "Un score reste valable trente jours avant réévaluation. L'application permet de forcer un recalcul immédiat depuis l'écran du score.",
  },
  {
    q: "Comment contester une opération ?",
    a: "Écrivez à l'adresse du support en indiquant la référence de la transaction, visible dans son détail au format KLA-AAAA-XXXXXXXX.",
  },
] as const;
