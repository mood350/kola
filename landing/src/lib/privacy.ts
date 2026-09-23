/**
 * Politique de confidentialité.
 *
 * Rédigée à partir de ce que le code TRAITE RÉELLEMENT, et non d'un modèle
 * générique. Chaque section correspond à un traitement identifiable dans le
 * backend : `AuthenticationService` (verrouillage après cinq échecs, alerte
 * nouvel appareil), `JwtService` (jetons), `CreditScoringService` (les huit
 * critères et la conservation trente jours), `RateLimitingService` (adresse IP
 * en cache Redis), `Transaction` (registre non supprimable).
 *
 * Une politique qui promet moins que ce que le code fait est fausse ; une qui
 * promet plus est invérifiable. Les deux se découvrent au premier contrôle.
 *
 * Ce texte n'a pas été relu par un juriste — la page l'affiche explicitement au
 * visiteur, comme le fait déjà `lib/legal.ts` pour les CGU.
 */

export const PRIVACY_UPDATED_AT = "11 août 2026";

export type PrivacySection = {
  id: string;
  title: string;
  body: string[];
  list?: string[];
};

export const PRIVACY_SECTIONS: PrivacySection[] = [
  {
    id: "responsable",
    title: "Qui traite vos données",
    body: [
      "Kola est responsable du traitement des données collectées via l'application et ce site. Le service s'adresse aux résidents des huit États de l'UEMOA et n'est pas commercialisé ailleurs.",
      "Pour toute question relative à vos données, ou pour exercer l'un des droits décrits plus bas, écrivez à aide@kola.africa. Une réponse vous est adressée sous deux jours ouvrés.",
    ],
  },
  {
    id: "donnees",
    title: "Ce que nous collectons, et pourquoi",
    body: [
      "Aucune donnée n'est collectée « au cas où ». Chaque catégorie ci-dessous correspond à une fonction précise du service, et disparaîtrait avec elle.",
    ],
    list: [
      "Adresse e-mail et numéro de téléphone : identifient le compte et permettent l'activation. Le numéro sert de référence pour les transferts entrants — c'est par lui que d'autres utilisateurs vous envoient de l'argent.",
      "Mot de passe : jamais conservé en clair. Seule une empreinte cryptographique irréversible est enregistrée ; elle ne permet pas de retrouver le mot de passe d'origine.",
      "Opérations : montant, date, type et contrepartie de chaque dépôt, retrait, transfert ou paiement. C'est le registre du service, et la matière première du score.",
      "Tentatives de connexion : nombre d'échecs consécutifs et date de verrouillage, afin de bloquer une attaque par essais successifs.",
      "Adresse IP et empreinte d'appareil : comparées à vos connexions précédentes pour vous alerter d'un accès inconnu, et pour appliquer les limites de fréquence qui protègent les points d'entrée sensibles.",
      "Niveau de vérification d'identité (KYC) : détermine vos plafonds d'opération et pèse dans le score.",
    ],
  },
  {
    id: "score",
    title: "Le score de confiance et la décision de crédit",
    body: [
      "Votre score est calculé à partir de huit critères pondérés, tous issus de votre usage du service : régularité des dépôts, niveau de vérification, historique de remboursement, discipline d'épargne, ratio dépenses / revenus, ancienneté du compte, volume d'activité et diversité du réseau. Aucune donnée extérieure à Kola n'entre dans ce calcul — ni fichier bancaire, ni source tierce.",
      "Ce traitement est automatisé et produit un effet concret : il détermine le montant que vous pouvez emprunter et le taux appliqué. Vous disposez à ce titre de trois garanties, exerçables à tout moment.",
    ],
    list: [
      "Explication : l'application affiche les huit critères, leur poids et le niveau que vous atteignez sur chacun. Aucune part du calcul n'est masquée.",
      "Contestation : vous pouvez demander le réexamen d'une décision de crédit par une personne, en écrivant au support.",
      "Recalcul : un score reste valable trente jours, mais vous pouvez en déclencher le recalcul immédiat depuis l'écran du score.",
    ],
  },
  {
    id: "conservation",
    title: "Combien de temps nous les gardons",
    body: [
      "Les durées ci-dessous ne sont pas choisies pour notre confort : elles découlent soit d'une obligation légale, soit d'une nécessité technique explicite.",
    ],
    list: [
      "Compte et données d'identification : le temps de la relation, puis cinq ans après la clôture, au titre des obligations de lutte contre le blanchiment.",
      "Registre des opérations : dix ans, durée de conservation des pièces comptables. Les portefeuilles et opérations ne sont jamais supprimés, seulement désactivés — un historique financier tronqué est inexploitable en cas de litige, y compris pour vous défendre.",
      "Score de confiance : chaque calcul expire au bout de trente jours et est remplacé par le suivant.",
      "Tentatives de connexion échouées : remises à zéro dès la première connexion réussie.",
      "Jetons d'activation et de réinitialisation : quinze minutes, puis suppression.",
    ],
  },
  {
    id: "partage",
    title: "Avec qui elles sont partagées",
    body: [
      "Vos données ne sont ni vendues, ni louées, ni cédées à des fins publicitaires. Elles ne sont transmises qu'à trois catégories de destinataires, et uniquement pour ce qui les concerne.",
    ],
    list: [
      "Le prestataire d'envoi d'e-mails, pour les seuls messages du service : code d'activation, alerte de connexion inconnue, réinitialisation de mot de passe.",
      "L'hébergeur de l'infrastructure, qui stocke la base de données. Il n'y accède pas et est contractuellement tenu à la confidentialité.",
      "Les autorités compétentes, sur réquisition régulière et dans le seul périmètre de leur demande.",
    ],
  },
  {
    id: "site",
    title: "Ce que fait ce site, distinct de l'application",
    body: [
      "Ce site vitrine ne donne accès à aucun compte et n'affiche aucune donnée personnelle. Les polices de caractères sont auto-hébergées : aucune requête n'est adressée à un domaine tiers au chargement de la page.",
      "La mesure d'audience n'est chargée qu'après acceptation explicite depuis le bandeau de cookies. En cas de refus, ou tant qu'aucun choix n'est fait, aucun script de mesure n'est téléchargé — pas même de manière anonyme. Le détail exhaustif de ce qui est déposé figure sur la page Cookies.",
      "Le formulaire de contact transmet les informations que vous y saisissez pour traiter votre demande, et rien d'autre. Un champ invisible piège les robots ; son contenu n'est jamais conservé.",
    ],
  },
  {
    id: "droits",
    title: "Vos droits",
    body: [
      "Vous pouvez à tout moment demander l'accès à vos données, leur rectification, leur effacement dans les limites des durées légales ci-dessus, la limitation d'un traitement, ou vous opposer à un traitement fondé sur notre intérêt légitime.",
      "Vous pouvez également obtenir une copie de vos données dans un format lisible par machine, et demander qu'une décision de crédit automatisée soit réexaminée par une personne.",
      "Toute demande adressée à aide@kola.africa reçoit une réponse sous deux jours ouvrés, et un traitement complet sous un mois au plus.",
    ],
  },
  {
    id: "securite",
    title: "Comment elles sont protégées",
    body: [
      "Les échanges entre votre appareil et nos serveurs sont chiffrés. Les mots de passe ne sont conservés que sous forme d'empreinte irréversible, et l'accès à un compte requiert un jeton signé, de durée limitée.",
      "Cinq tentatives de connexion échouées verrouillent le compte pendant trente minutes. Toute connexion depuis un appareil ou une adresse inconnue déclenche une notification immédiate. Les points d'entrée sensibles sont soumis à des limites de fréquence qui rendent une attaque massive impraticable.",
      "Aucun système n'est invulnérable. En cas de violation susceptible d'engendrer un risque pour vos droits, les personnes concernées et l'autorité compétente sont informées dans les délais prévus par la réglementation applicable.",
    ],
  },
];
