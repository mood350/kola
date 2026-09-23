/**
 * Données structurées Schema.org (JSON-LD).
 *
 * Ce que ces objets décrivent est destiné à être consommé sans que personne ne
 * revienne lire la page : un moteur peut afficher une réponse FAQ telle quelle,
 * un assistant peut citer l'entité. La règle qui gouverne tout ce fichier en
 * découle — ON NE BALISE QUE CE QUI EST VRAI ET VISIBLE SUR LA PAGE.
 *
 * Deux conséquences concrètes, appliquées plus bas :
 *
 *   - pas d'adresse postale tant que `OFFICE` est `null` (`business.ts`) ;
 *   - pas de `Review` ni d'`AggregateRating` tant que `REVIEWS_ARE_REAL` est
 *     `false` (`reviews.ts`).
 *
 * Le balisage n'est pas une couche décorative ajoutée après coup : il reflète
 * l'état réel du produit, et suit automatiquement les constantes qui décrivent
 * cet état.
 */

import {
  BRAND,
  COVERAGE,
  OFFICE,
  RESPONSE_PROMISE,
  SITE_URL,
  SOCIAL_PROFILES,
  CONTACT,
} from "@/lib/business";
import { FAQ_ITEMS } from "@/lib/faq";
import { REVIEWS, REVIEWS_ARE_REAL, REVIEWS_AVERAGE } from "@/lib/reviews";

/** Identifiant stable de l'entité, réutilisé pour la relier depuis d'autres nœuds. */
const ORGANIZATION_ID = `${SITE_URL}/#organisation`;

/* ---------------------------------------------------------------------------
   Organisation
   ------------------------------------------------------------------------ */

/**
 * L'entité Kola.
 *
 * LE CHOIX DE TYPE EST LE POINT DÉLICAT. `FinancialService` est un sous-type de
 * `LocalBusiness` : il engage un établissement joignable à une adresse, et les
 * résultats locaux qu'il vise supposent `postalAddress` et `geo`. Déclarer ce
 * type sans local revient à revendiquer une présence physique inexistante —
 * c'est exactement le motif d'action manuelle « établissement non éligible »,
 * et ça enverrait des gens à une adresse qui n'existe pas.
 *
 * Tant que `OFFICE` vaut `null`, on émet donc une `Organization` avec
 * `areaServed` : une entreprise réellement active sur un territoire, sans
 * guichet. C'est la description exacte de la situation.
 *
 * Renseigner `OFFICE` bascule automatiquement sur `FinancialService` complété
 * de l'adresse, des coordonnées et des horaires.
 */
export function organizationSchema() {
  const base = {
    "@context": "https://schema.org",
    "@id": ORGANIZATION_ID,
    name: BRAND.name,
    legalName: BRAND.legalName,
    description: BRAND.tagline,
    url: SITE_URL,
    /* Le logo est l'image de partage générée par `app/opengraph-image.tsx` :
       une seule image à maintenir, et jamais de lien mort vers un fichier
       supprimé. */
    logo: `${SITE_URL}/opengraph-image`,
    image: `${SITE_URL}/opengraph-image`,
    foundingDate: BRAND.foundingDate,
    /* `sameAs` vide n'est pas émis : un tableau vide n'apporte rien et laisse
       croire à un oubli. */
    ...(SOCIAL_PROFILES.length > 0 ? { sameAs: SOCIAL_PROFILES } : {}),
    contactPoint: [
      {
        "@type": "ContactPoint",
        contactType: "customer support",
        email: CONTACT.support,
        availableLanguage: ["fr"],
        areaServed: COVERAGE.map((country) => country.code),
        hoursAvailable: {
          "@type": "OpeningHoursSpecification",
          dayOfWeek: [
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
          ],
          opens: "08:00",
          closes: "18:00",
        },
      },
      {
        "@type": "ContactPoint",
        contactType: "public relations",
        email: CONTACT.press,
        availableLanguage: ["fr"],
      },
    ],
    /* La promesse de délai, sous une forme lisible par une machine. Elle doit
       rester synchrone avec `RESPONSE_PROMISE.headline`, affiché sur le site —
       les deux viennent de la même constante. */
    slogan: RESPONSE_PROMISE.headline,
  };

  if (!OFFICE) {
    return {
      ...base,
      "@type": "Organization",
      areaServed: COVERAGE.map((country) => ({
        "@type": "Country",
        name: country.name,
        identifier: country.code,
      })),
      currenciesAccepted: "XOF",
    };
  }

  return {
    ...base,
    "@type": "FinancialService",
    priceRange: "Gratuit à l'ouverture",
    currenciesAccepted: "XOF",
    address: {
      "@type": "PostalAddress",
      streetAddress: OFFICE.street,
      postalCode: OFFICE.postalCode,
      addressLocality: OFFICE.city,
      addressRegion: OFFICE.region,
      addressCountry: OFFICE.country,
    },
    geo: {
      "@type": "GeoCoordinates",
      latitude: OFFICE.latitude,
      longitude: OFFICE.longitude,
    },
    openingHours: OFFICE.openingHours,
    ...(OFFICE.telephone ? { telephone: OFFICE.telephone } : {}),
    areaServed: COVERAGE.map((country) => ({
      "@type": "Country",
      name: country.name,
      identifier: country.code,
    })),
    ...aggregateRatingSchema(),
  };
}

/* ---------------------------------------------------------------------------
   Site
   ------------------------------------------------------------------------ */

export function webSiteSchema() {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "@id": `${SITE_URL}/#site`,
    url: SITE_URL,
    name: BRAND.name,
    inLanguage: "fr-FR",
    publisher: { "@id": ORGANIZATION_ID },
  };
}

/* ---------------------------------------------------------------------------
   Questions fréquentes
   ------------------------------------------------------------------------ */

/**
 * `FAQPage` — le seul balisage de ce fichier qui vise réellement un affichage
 * enrichi, et il est légitime : chaque réponse décrit le comportement effectif
 * du backend, et le texte balisé est mot pour mot celui rendu dans la page.
 * Google exige cette identité : une réponse balisée absente du HTML visible est
 * traitée comme du contenu masqué.
 */
export function faqSchema() {
  return {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    "@id": `${SITE_URL}/#faq`,
    mainEntity: FAQ_ITEMS.map((item) => ({
      "@type": "Question",
      name: item.question,
      acceptedAnswer: {
        "@type": "Answer",
        text: item.answer,
      },
    })),
  };
}

/* ---------------------------------------------------------------------------
   Fil d'Ariane
   ------------------------------------------------------------------------ */

export type Crumb = { label: string; href: string };

/**
 * `BreadcrumbList` remplace l'URL brute par le chemin de navigation dans les
 * résultats de recherche. Les positions commencent à 1 et doivent être
 * continues : une numérotation trouée invalide la liste entière.
 *
 * Le dernier élément — la page courante — porte quand même son `item` : c'est
 * ce que recommande Google, et ça évite un nœud incomplet en fin de liste.
 */
export function breadcrumbSchema(crumbs: Crumb[]) {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: crumbs.map((crumb, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: crumb.label,
      item: `${SITE_URL}${crumb.href}`,
    })),
  };
}

/* ---------------------------------------------------------------------------
   Avis
   ------------------------------------------------------------------------ */

/**
 * Note agrégée — n'est émise QUE si les avis sont authentiques.
 *
 * Tant que `REVIEWS_ARE_REAL` vaut `false`, cette fonction renvoie un objet
 * vide, absorbé par l'étalement dans le nœud parent : aucune étoile n'est
 * revendiquée. C'est le garde-fou central du fichier — le supprimer ferait
 * publier une note calculée sur des textes inventés, ce qui expose tout le
 * domaine à une action manuelle « avis frauduleux ».
 */
function aggregateRatingSchema() {
  if (!REVIEWS_ARE_REAL || REVIEWS.length === 0) return {};

  return {
    aggregateRating: {
      "@type": "AggregateRating",
      ratingValue: REVIEWS_AVERAGE,
      reviewCount: REVIEWS.length,
      bestRating: 5,
      worstRating: 1,
    },
    review: REVIEWS.map((review) => ({
      "@type": "Review",
      name: review.headline,
      reviewBody: review.body,
      datePublished: review.date,
      author: { "@type": "Person", name: review.author },
      reviewRating: {
        "@type": "Rating",
        ratingValue: review.rating,
        bestRating: 5,
        worstRating: 1,
      },
    })),
  };
}
