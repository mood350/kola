"use client";

import { useState } from "react";
import { COVERAGE, COVERAGE_BBOX, OFFICE } from "@/lib/business";

/**
 * Carte et itinéraire.
 *
 * CHARGEMENT AU CLIC, PAS AU RENDU. Une carte embarquée est une page tierce
 * complète : elle dépose ses cookies, lit l'adresse IP et charge plusieurs
 * centaines de kilo-octets, pour tout visiteur, y compris ceux qui ne
 * regarderont jamais la carte. Sur une page qui promet par ailleurs qu'aucun
 * script tiers n'est chargé sans consentement, l'embarquer d'office
 * contredirait le bandeau de cookies dans le même écran.
 *
 * L'aperçu ci-dessous est donc entièrement local, et l'iframe n'est insérée
 * qu'après une action explicite. Bénéfice secondaire : la page ne paie aucun
 * coût réseau pour une carte que la plupart des visiteurs ne dérouleront pas.
 *
 * OPENSTREETMAP PLUTÔT QUE GOOGLE MAPS : l'embarquement OSM ne dépose aucun
 * cookie et ne demande pas de clé d'API. Les liens d'itinéraire, eux, ouvrent
 * l'application de navigation du visiteur — Google, Apple ou OSM, à son choix.
 *
 * DEUX ÉTATS, PILOTÉS PAR `OFFICE` (`lib/business.ts`) :
 *
 *   - `OFFICE === null` — aucun guichet ouvert au public. La carte cadre la
 *     zone desservie et le bloc d'itinéraire cède la place à la liste des pays.
 *     Afficher un itinéraire vers un lieu où personne ne peut se rendre serait
 *     une invitation à un déplacement inutile.
 *   - `OFFICE` renseigné — la carte se centre sur le point avec un marqueur, et
 *     les trois liens d'itinéraire apparaissent. Aucun composant à modifier.
 */

const OSM_EMBED = "https://www.openstreetmap.org/export/embed.html";

export function CoverageMap() {
  const [loaded, setLoaded] = useState(false);

  const { src, title } = OFFICE
    ? {
        /* Cadre serré autour du point : ±0,02° ≈ 2 km, l'échelle à laquelle on
           reconnaît un quartier. */
        src: `${OSM_EMBED}?bbox=${OFFICE.longitude - 0.02}%2C${OFFICE.latitude - 0.02}%2C${OFFICE.longitude + 0.02}%2C${OFFICE.latitude + 0.02}&layer=mapnik&marker=${OFFICE.latitude}%2C${OFFICE.longitude}`,
        title: `Carte de l'emplacement des bureaux Kola à ${OFFICE.city}`,
      }
    : {
        src: `${OSM_EMBED}?bbox=${COVERAGE_BBOX.west}%2C${COVERAGE_BBOX.south}%2C${COVERAGE_BBOX.east}%2C${COVERAGE_BBOX.north}&layer=mapnik`,
        title:
          "Carte de la zone desservie par Kola : les huit États de l'UEMOA, en Afrique de l'Ouest",
      };

  return (
    <div className="rounded-card bg-surface p-7 shadow-soft sm:p-9">
      <h2 className="font-display text-lg font-semibold text-ink-950">
        {OFFICE ? "Nous trouver" : "Où Kola fonctionne"}
      </h2>

      <p className="mt-2 text-[0.9375rem] leading-relaxed text-ink-600">
        {OFFICE ? (
          <>
            {OFFICE.street}, {OFFICE.postalCode} {OFFICE.city}. Ouvert{" "}
            {OFFICE.openingHours.join(", ")}.
          </>
        ) : (
          <>
            Kola n&apos;a pas de guichet ouvert au public : tout se fait depuis
            l&apos;application, il n&apos;y a donc aucun déplacement à prévoir.
            Le service couvre les huit États de l&apos;Union économique et
            monétaire ouest-africaine — la zone du franc CFA (XOF), seule devise
            que le portefeuille manipule.
          </>
        )}
      </p>

      {/* Cadre de la carte. Ratio fixe des deux côtés du chargement : sans lui,
          l'insertion de l'iframe déplacerait tout le contenu situé en dessous. */}
      <div className="relative mt-6 aspect-[16/10] overflow-hidden rounded-2xl bg-sunken">
        {loaded ? (
          <iframe
            src={src}
            title={title}
            loading="lazy"
            /* `referrerPolicy` : l'URL de la page consultée n'est pas transmise
               au fournisseur de la carte. */
            referrerPolicy="no-referrer"
            className="absolute inset-0 h-full w-full border-0"
          />
        ) : (
          <button
            type="button"
            onClick={() => setLoaded(true)}
            className="group absolute inset-0 flex cursor-pointer flex-col items-center justify-center gap-4 p-6 text-center"
          >
            <MapPreview />
            <span className="relative flex flex-col items-center gap-1.5">
              <span className="inline-flex h-11 items-center rounded-full bg-kola-600 px-5 text-[0.875rem] font-medium text-white transition-colors duration-200 group-hover:bg-kola-700">
                Afficher la carte
              </span>
              <span className="text-[0.75rem] text-ink-500">
                Charge OpenStreetMap · aucun cookie déposé
              </span>
            </span>
          </button>
        )}
      </div>

      {OFFICE ? (
        <div className="mt-6 border-t border-ink-200 pt-6">
          <h3 className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
            Itinéraire
          </h3>
          <ul className="mt-4 flex flex-wrap gap-2.5">
            {[
              {
                label: "Google Maps",
                href: `https://www.google.com/maps/dir/?api=1&destination=${OFFICE.latitude},${OFFICE.longitude}`,
              },
              {
                label: "Plans (Apple)",
                href: `https://maps.apple.com/?daddr=${OFFICE.latitude},${OFFICE.longitude}`,
              },
              {
                label: "OpenStreetMap",
                href: `https://www.openstreetmap.org/directions?to=${OFFICE.latitude},${OFFICE.longitude}`,
              },
            ].map((service) => (
              <li key={service.label}>
                {/* `noopener` est impératif sur une cible externe ouverte dans
                    un nouvel onglet : sans lui, la page ouverte garde une
                    référence vers la nôtre via `window.opener`. */}
                <a
                  href={service.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex h-10 items-center rounded-full bg-kola-100 px-4 text-[0.875rem] font-medium text-kola-700 transition-colors hover:bg-kola-200"
                >
                  {service.label}
                  <span className="sr-only"> (nouvel onglet)</span>
                </a>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <div className="mt-6 border-t border-ink-200 pt-6">
          <h3 className="text-[0.6875rem] font-medium tracking-[0.16em] text-ink-400 uppercase">
            Pays couverts
          </h3>
          <ul className="mt-4 flex flex-wrap gap-2">
            {COVERAGE.map((country) => (
              <li
                key={country.code}
                className="inline-flex h-9 items-center rounded-full bg-sunken px-3.5 text-[0.8125rem] font-medium text-ink-700"
              >
                {country.name}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

/**
 * Aperçu dessiné localement, affiché à la place de la carte tant qu'elle n'a
 * pas été demandée.
 *
 * Volontairement schématique — un semis de points sur une trame — plutôt qu'une
 * silhouette de côte approximative : un contour géographique faux est pire
 * qu'une abstraction assumée, parce qu'il se donne pour une information.
 * Purement décoratif, donc masqué aux technologies d'assistance : le texte du
 * bouton et celui de la section portent seuls le sens.
 */
function MapPreview() {
  return (
    <span
      aria-hidden="true"
      className="tech-grid pointer-events-none absolute inset-0 opacity-70"
    >
      <span className="absolute inset-0 bg-[radial-gradient(ellipse_50%_45%_at_50%_50%,rgb(158_161_246/0.35)_0%,transparent_70%)]" />
    </span>
  );
}
