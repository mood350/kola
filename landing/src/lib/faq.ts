/**
 * Questions fréquentes.
 *
 * Chaque réponse décrit le comportement RÉEL du backend (`CreditScoringService`,
 * `AuthenticationService`, `CreditTier`, `LoanService`). C'est la condition pour
 * qu'elles puissent être publiées en données structurées `FAQPage` : une
 * réponse balisée est susceptible d'être affichée telle quelle dans les
 * résultats de recherche ou récitée par un assistant, sans que personne ne
 * revienne vérifier sur la page. Une approximation ici devient une promesse
 * commerciale opposable.
 *
 * Les réponses sont volontairement courtes et autoportantes : hors contexte,
 * elles doivent rester exactes.
 */

export type FaqItem = {
  /** Sert d'ancre et de clé React — stable, jamais dérivé du texte. */
  id: string;
  question: string;
  answer: string;
};

export const FAQ_ITEMS: FaqItem[] = [
  {
    id: "score-calcul",
    question: "Comment mon score de confiance est-il calculé ?",
    answer:
      "Huit critères pondérés produisent une note de 0 à 100 : régularité des dépôts, niveau de vérification, historique de remboursement, discipline d'épargne, ratio dépenses / revenus, ancienneté du compte, volume d'activité et diversité du réseau. Chaque critère est consultable séparément dans l'application, avec le nombre de points qu'il vous rapporte et pourquoi. Aucune décision n'est prise sur un chiffre que vous ne pouvez pas décomposer.",
  },
  {
    id: "score-delai",
    question: "Combien de temps faut-il pour obtenir un score exploitable ?",
    answer:
      "Le score existe dès le premier dépôt, mais il reste bas tant que l'historique est court : l'ancienneté du compte et la régularité des dépôts pèsent à eux deux 25 points, et ne se construisent qu'avec le temps. En pratique, il faut environ trois mois d'usage régulier pour franchir le seuil de 40 points qui ouvre le premier palier de crédit.",
  },
  {
    id: "credit-montant",
    question: "Quel montant puis-je emprunter, et à quel taux ?",
    answer:
      "Le plafond et le taux dépendent uniquement de votre palier : Basique (score 40 à 59) donne 25 000 XOF à 3 % par mois, Standard (60 à 74) 100 000 XOF à 2 %, Premium (75 à 89) 500 000 XOF à 1,5 %, et Élite (90 à 100) 2 000 000 XOF à 1 % par mois. Le taux est connu avant de valider la demande, et un seul prêt peut être actif à la fois.",
  },
  {
    id: "sans-historique",
    question: "Puis-je ouvrir un compte sans historique bancaire ?",
    answer:
      "Oui — c'est la raison d'être du service. L'inscription demande une adresse e-mail et un numéro de téléphone, rien de plus : aucun bulletin de salaire, aucun justificatif de revenus, aucun garant. Votre historique se construit ensuite à partir de votre usage réel de Kola, pas d'un passé bancaire que vous n'avez peut-être jamais eu.",
  },
  {
    id: "securite-fonds",
    question: "Que se passe-t-il si mon compte est compromis ?",
    answer:
      "Cinq tentatives de connexion échouées verrouillent automatiquement le compte pendant trente minutes, et toute connexion depuis un appareil ou une adresse inconnue déclenche immédiatement une notification par e-mail. Aucun compte n'est actif sans confirmation par code envoyé par e-mail. Enfin, portefeuilles et opérations ne sont jamais supprimés, seulement désactivés : l'historique reste intégralement reconstituable en cas de litige.",
  },
];
