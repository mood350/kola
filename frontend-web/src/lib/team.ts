/**
 * L'équipe.
 *
 * PHOTO — CE QUI EST ATTENDU ET COMMENT L'INSTALLER
 *
 * `TEAM_PHOTO` vaut `null` : aucune photo réelle n'est disponible, et une
 * photo d'équipe achetée en banque d'images est le contraire de ce qu'une
 * section « qui nous sommes » est censée produire. Un visiteur qui reconnaît un
 * visage de stock sur la page d'un service financier en tire exactement la
 * conclusion inverse de celle recherchée.
 *
 * En attendant, `components/sections/team.tsx` affiche un visuel dessiné, dans
 * le vocabulaire graphique du site, sans jamais prétendre montrer des
 * personnes.
 *
 * POUR INSTALLER LA VRAIE PHOTO :
 *
 *   1. déposer le fichier dans `public/equipe/` ;
 *   2. renseigner l'objet ci-dessous — `width` et `height` sont les dimensions
 *      RÉELLES du fichier, en pixels. Elles ne redimensionnent rien : elles
 *      donnent au navigateur le ratio à réserver avant que l'image n'arrive,
 *      ce qui évite que le texte situé en dessous ne saute au chargement. Des
 *      valeurs fausses reproduisent précisément le décalage qu'elles sont
 *      censées empêcher ;
 *   3. écrire un `alt` qui DÉCRIT la scène. Pas « photo de l'équipe » — ça
 *      n'apprend rien à quelqu'un qui ne voit pas l'image. Combien de
 *      personnes, où, en train de quoi.
 *
 * @example
 * export const TEAM_PHOTO: TeamPhoto | null = {
 *   src: "/equipe/equipe-kola.jpg",
 *   width: 2400,
 *   height: 1600,
 *   alt: "Les six membres de l'équipe Kola réunis autour d'une table de travail dans leur bureau de Lomé, devant un tableau couvert de schémas du parcours de score.",
 * };
 */

export type TeamPhoto = {
  src: string;
  width: number;
  height: number;
  alt: string;
};

export const TEAM_PHOTO: TeamPhoto | null = null;

export type TeamMember = {
  id: string;
  name: string;
  role: string;
  focus: string;
};

/**
 * Les rôles décrivent les responsabilités réelles couvertes par le projet
 * (API Spring Boot, application Flutter, moteur de score, conformité). Les noms
 * sont à remplacer par ceux des personnes qui les tiennent — laisser un nom
 * inventé sur une page « à propos » est la façon la plus rapide de perdre la
 * confiance qu'elle cherche à établir.
 */
export const TEAM: TeamMember[] = [
  {
    id: "produit",
    name: "À compléter",
    role: "Produit et conformité",
    focus:
      "Définit les plafonds KYC, les règles de surveillance et ce que le service peut promettre sans dépasser le cadre réglementaire de l'UEMOA.",
  },
  {
    id: "backend",
    name: "À compléter",
    role: "Ingénierie back-end",
    focus:
      "Tient l'API Spring Boot : registre des opérations, moteur de score à huit critères, décision de prêt et journal inaltérable.",
  },
  {
    id: "mobile",
    name: "À compléter",
    role: "Ingénierie mobile",
    focus:
      "Développe l'application Flutter, dont l'écran qui décompose le score critère par critère plutôt que d'afficher un chiffre nu.",
  },
  {
    id: "support",
    name: "À compléter",
    role: "Support et relation client",
    focus:
      "Répond aux messages sous deux jours ouvrés et traite en priorité les comptes verrouillés et les opérations contestées.",
  },
];
