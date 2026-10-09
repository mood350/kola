import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/business";

/**
 * `robots.txt`, généré plutôt que déposé en fichier statique.
 *
 * L'URL du sitemap est ainsi dérivée de `SITE_URL` : un changement de domaine
 * ne peut pas laisser derrière lui un `robots.txt` pointant vers l'ancien —
 * c'est l'erreur classique, silencieuse, et qui ne se voit qu'au moment où
 * l'indexation s'effondre.
 *
 * CE QUI EST INTERDIT, ET POURQUOI :
 *
 *   - `/api/` : des points d'entrée qui ne renvoient pas de page. Les explorer
 *     consomme du budget d'exploration pour rien, et le formulaire de contact
 *     recevrait des requêtes de robots.
 *   - `/merci` : la page de remerciement n'a de sens qu'atteinte après l'envoi
 *     d'un message. Indexée, elle attirerait un trafic arrivant sur une
 *     confirmation vide de tout contexte — et fausserait la mesure des
 *     conversions, puisqu'une visite directe compterait comme un envoi.
 *
 * Rien d'autre n'est bloqué. En particulier, la présence d'une page dans ce
 * fichier n'empêche PAS son indexation si elle est liée depuis ailleurs :
 * `Disallow` interdit l'exploration, pas l'affichage dans les résultats. Le
 * `noindex` posé dans les métadonnées de `/merci` est l'instruction qui compte
 * réellement — les deux se complètent.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/api/", "/merci"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
    host: SITE_URL,
  };
}
