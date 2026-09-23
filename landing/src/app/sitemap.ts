import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/business";
import { CASE_STUDIES } from "@/lib/case-studies";

/**
 * Plan du site.
 *
 * Les études de cas sont dérivées de `CASE_STUDIES` et non recopiées : ajouter
 * une étude suffit à la faire apparaître ici. Une liste tenue à la main finit
 * toujours par référencer une page supprimée — ce que les moteurs traitent
 * comme un signal de qualité négatif sur l'ensemble du plan.
 *
 * `/merci` est volontairement absente : elle est en `noindex`, et déclarer dans
 * un sitemap une page qu'on demande par ailleurs de ne pas indexer est une
 * contradiction que la Search Console signale.
 *
 * `priority` n'est qu'une indication relative à l'intérieur de CE site — elle
 * ne se compare pas à celle d'un autre domaine et n'améliore aucun classement.
 * Elle sert seulement à dire quelles pages comptent le plus quand tout ne peut
 * pas être exploré.
 */
export default function sitemap(): MetadataRoute.Sitemap {
  const lastModified = new Date();

  /* Le type est porté par CE tableau, avant le `.map`. Annoter le résultat de
     la projection ne suffirait pas : TypeScript élargirait `"weekly"` en
     `string` en construisant les littéraux, et `changeFrequency` n'accepte que
     l'union fermée. */
  const routes: MetadataRoute.Sitemap = [
    { url: SITE_URL, changeFrequency: "weekly", priority: 1 },
    {
      url: `${SITE_URL}/etudes-de-cas`,
      changeFrequency: "monthly",
      priority: 0.8,
    },
    { url: `${SITE_URL}/a-propos`, changeFrequency: "monthly", priority: 0.7 },
    { url: `${SITE_URL}/contact`, changeFrequency: "monthly", priority: 0.7 },
    { url: `${SITE_URL}/cgu`, changeFrequency: "yearly", priority: 0.3 },
    {
      url: `${SITE_URL}/confidentialite`,
      changeFrequency: "yearly",
      priority: 0.3,
    },
    { url: `${SITE_URL}/cookies`, changeFrequency: "yearly", priority: 0.3 },
  ];

  const pages: MetadataRoute.Sitemap = routes.map((route) => ({
    ...route,
    lastModified,
  }));

  const caseStudies: MetadataRoute.Sitemap = CASE_STUDIES.map((study) => ({
    url: `${SITE_URL}/etudes-de-cas/${study.slug}`,
    lastModified,
    changeFrequency: "monthly",
    priority: 0.6,
  }));

  return [...pages, ...caseStudies];
}
