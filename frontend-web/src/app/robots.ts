import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/business";

/**
 * Directives d'exploration.
 *
 * ═══ CE QUI A CHANGÉ AVEC LA FUSION ═══
 *
 * Le site public et l'espace client partagent désormais un domaine. Les routes
 * privées portent déjà `noindex` (cf. `(private)/layout.tsx`), qui est le
 * signal fort — il empêche l'INDEXATION. Ce fichier ajoute le second étage :
 * empêcher l'EXPLORATION. Les deux ne font pas la même chose, et l'un sans
 * l'autre laisse un trou : `noindex` seul suppose que le robot charge la page
 * pour lire la balise, ce qui expose des écrans de compte au trafic
 * d'exploration et fait apparaître leurs URL dans les journaux de tiers.
 *
 * `/merci` reste écarté pour une autre raison : une page de confirmation
 * atteinte directement depuis un moteur ne veut rien dire, et elle fausserait
 * la mesure des conversions.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [
        "/api/",
        "/merci",
        // Espace client : rien à y explorer sans être connecté, et tout y est
        // personnel. La liste suit les routes de `(private)`.
        "/mon-compte",
        "/comptes",
        "/transactions",
        "/operations/",
        "/coffres",
        "/credit",
        "/virements-programmes",
        "/beneficiaires",
        "/notifications",
        "/profil",
        // Écrans d'authentification : ils n'apportent aucun contenu et une
        // page de connexion indexée n'attire que du bruit.
        "/connexion",
        "/inscription",
        "/confirmation",
        "/mot-de-passe-oublie",
        "/reinitialiser",
      ],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
    host: SITE_URL,
  };
}
