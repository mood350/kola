/**
 * Identité de l'entreprise — source unique de vérité.
 *
 * Tout ce qui décrit Kola en tant qu'organisation vit ici : URL canonique,
 * raison sociale, zone desservie, délais de réponse, implantation physique.
 * Ces valeurs alimentent trois consommateurs qui doivent impérativement dire
 * la même chose, sous peine d'incohérence sanctionnée au référencement :
 *
 *   1. les données structurées Schema.org (`lib/schema.ts`) ;
 *   2. le texte visible des pages (contact, à propos, pied de page) ;
 *   3. le sitemap et les URL absolues des métadonnées.
 *
 * Dupliquer une adresse ou un délai dans un composant, c'est garantir qu'un
 * jour l'un des trois mentira.
 */

import { CONTACT_EMAIL } from "@/lib/content";

/** Origine canonique. Pas de barre oblique finale : tout est concaténé après. */
export const SITE_URL = "https://kola.africa";

export const BRAND = {
  name: "Kola",
  legalName: "Kola",
  /** Une phrase, telle qu'un moteur ou un assistant la citera. */
  tagline: "Portefeuille mobile money et score de confiance pour l'Afrique de l'Ouest.",
  foundingDate: "2026",
} as const;

/* ---------------------------------------------------------------------------
   Implantation physique
   ------------------------------------------------------------------------ */

export type Office = {
  street: string;
  postalCode: string;
  city: string;
  region: string;
  /** Code ISO 3166-1 alpha-2. */
  country: string;
  /** Coordonnées du point exact, pour la carte et le champ `geo` du schema. */
  latitude: number;
  longitude: number;
  /** Format Schema.org : « Mo-Fr 08:00-18:00 ». */
  openingHours: string[];
  telephone?: string;
};

/**
 * Kola n'a pas de guichet ouvert au public : le service est intégralement en
 * ligne, et un visiteur n'a aucune raison de se déplacer.
 *
 * `null` est donc la valeur exacte, pas un trou à combler. Elle a des
 * conséquences délibérées et automatiques dans tout le site :
 *
 *   - `lib/schema.ts` émet `Organization` + `areaServed` au lieu de
 *     `FinancialService` + `postalAddress`. Déclarer un établissement local
 *     sans local est précisément ce que les moteurs sanctionnent, et un faux
 *     `geo` enverrait des gens sonner à une porte qui n'existe pas ;
 *   - `CoverageMap` affiche la zone desservie plutôt qu'un point, et le bloc
 *     « itinéraire » cède la place à la liste des pays.
 *
 * LE JOUR OÙ UN BUREAU OUVRE : renseigner l'objet ci-dessous (le type `Office`
 * décrit les champs attendus) et rien d'autre. Carte centrée sur le point,
 * liens d'itinéraire Google / Apple / OSM, `FinancialService` avec adresse,
 * horaires et coordonnées : tout bascule sans toucher à un composant.
 *
 * @example
 * export const OFFICE: Office | null = {
 *   street: "12 boulevard du 13 Janvier",
 *   postalCode: "BP 1234",
 *   city: "Lomé",
 *   region: "Maritime",
 *   country: "TG",
 *   latitude: 6.1725,
 *   longitude: 1.2314,
 *   openingHours: ["Mo-Fr 08:00-18:00"],
 *   telephone: "+228 00 00 00 00",
 * };
 */
export const OFFICE: Office | null = null;

/* ---------------------------------------------------------------------------
   Zone desservie
   ------------------------------------------------------------------------ */

/**
 * Les huit États de l'UEMOA — l'union monétaire dont le franc CFA (XOF) est la
 * devise. C'est la définition rigoureuse de « où Kola fonctionne » : le produit
 * ne manipule que des XOF, il s'arrête donc exactement où s'arrête la zone.
 *
 * Les codes ISO alimentent `areaServed` dans les données structurées ; les noms
 * alimentent le texte visible. Une seule liste pour les deux.
 */
export const COVERAGE = [
  { code: "BJ", name: "Bénin" },
  { code: "BF", name: "Burkina Faso" },
  { code: "CI", name: "Côte d'Ivoire" },
  { code: "GW", name: "Guinée-Bissau" },
  { code: "ML", name: "Mali" },
  { code: "NE", name: "Niger" },
  { code: "SN", name: "Sénégal" },
  { code: "TG", name: "Togo" },
] as const;

/**
 * Cadre géographique de la zone UEMOA : ouest, sud, est, nord.
 *
 * Sert à cadrer la carte quand il n'y a pas de point unique à montrer. Valeurs
 * arrondies au degré — la précision n'a aucun intérêt pour un cadrage.
 */
export const COVERAGE_BBOX = {
  west: -17.6,
  south: 4.2,
  east: 4.4,
  north: 25.0,
} as const;

/* ---------------------------------------------------------------------------
   Promesse de délai de réponse
   ------------------------------------------------------------------------ */

/**
 * Délais de réponse affichés publiquement.
 *
 * Une promesse de délai n'a de valeur que si elle est tenable : celles-ci sont
 * volontairement prudentes et exprimées en jours ouvrés, jamais en heures.
 * Annoncer « réponse en 2 h » sans astreinte organisée produit une déception
 * mesurable à chaque message envoyé un vendredi soir.
 *
 * `hours` est la traduction machine du même engagement, pour le champ
 * `Schema.org` de l'organisation. Les deux doivent bouger ensemble.
 */
export const RESPONSE_PROMISE = {
  /** Engagement principal, celui qu'on affiche partout. */
  headline: "Réponse sous 2 jours ouvrés",
  /** Version longue, pour les blocs qui ont la place. */
  detail:
    "Chaque message reçoit une réponse humaine sous deux jours ouvrés au maximum. Les demandes de support urgentes — compte verrouillé, opération contestée — sont traitées en priorité, généralement le jour même.",
  /** Traduction ISO 8601 pour les données structurées. */
  iso: "P2D",
  /** Créneau d'astreinte réel de l'équipe support. */
  hours: "Du lundi au vendredi, 8 h – 18 h (GMT)",
} as const;

/* ---------------------------------------------------------------------------
   Coordonnées et présence
   ------------------------------------------------------------------------ */

export const CONTACT = {
  email: CONTACT_EMAIL,
  support: "aide@kola.africa",
  press: "presse@kola.africa",
} as const;

/**
 * Profils officiels, injectés dans `sameAs` — le champ qui permet à un moteur
 * de relier ce site à une entité déjà connue ailleurs.
 *
 * Vide tant qu'aucun compte n'est ouvert : un `sameAs` pointant vers une page
 * inexistante ou vers le compte de quelqu'un d'autre est activement nuisible.
 * Ajouter les URL réelles ici les fait apparaître dans le schema, sans autre
 * modification.
 */
export const SOCIAL_PROFILES: string[] = [];
