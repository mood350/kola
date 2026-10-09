import { ImageResponse } from "next/og";
import { BRAND } from "@/lib/business";

/**
 * Image de partage (Open Graph / Twitter Card).
 *
 * GÉNÉRÉE PAR LE CODE, PAS DÉPOSÉE EN PNG. Un fichier exporté depuis un outil
 * de design se désynchronise dès la première évolution de la marque, et
 * personne ne pense à le régénérer — c'est l'actif le plus vite périmé d'un
 * site. Ici l'image est reconstruite à chaque build à partir des mêmes
 * constantes que le reste du site.
 *
 * CONTRAINTES DU MOTEUR (Satori) : seuls flexbox et un sous-ensemble de CSS
 * sont gérés. Pas de `grid`, pas de variables CSS — les couleurs sont donc
 * écrites en hexadécimal littéral, et doivent rester synchrones avec les tokens
 * de `globals.css`. Tout conteneur à plusieurs enfants exige un `display: flex`
 * explicite : sans lui, le rendu échoue au build plutôt qu'à l'affichage.
 *
 * FORMAT : 1200 × 630 est le seul ratio (1,91:1) recadré proprement par
 * l'ensemble des plateformes. En deçà de 600 px de large, plusieurs d'entre
 * elles refusent l'image et retombent sur une vignette générique.
 *
 * Le texte alternatif exporté ci-dessous alimente `og:image:alt`, lu par les
 * lecteurs d'écran là où l'aperçu est déplié — dans un fil de discussion, par
 * exemple.
 */

export const alt =
  "Kola — portefeuille mobile money et score de confiance pour l'Afrique de l'Ouest";

export const size = { width: 1200, height: 630 };

export const contentType = "image/png";

/* Tokens de `globals.css`, en littéral : Satori ne résout pas `var()`. */
const COLOR = {
  canvas: "#f5f3fd",
  brand: "#2e32c7",
  brandLight: "#7b7fec",
  ink: "#1e1834",
  inkMuted: "#5c5288",
  ochre: "#e9a23b",
};

export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          backgroundColor: COLOR.canvas,
          padding: 72,
          /* La trame technique du site, reproduite en dégradés répétés — même
             procédé que l'utilitaire `tech-grid`, aucun octet d'image. */
          backgroundImage:
            "linear-gradient(to right, rgba(46,50,199,0.06) 1px, transparent 1px), linear-gradient(to bottom, rgba(46,50,199,0.06) 1px, transparent 1px)",
          backgroundSize: "64px 64px",
        }}
      >
        {/* Marque : trois pétales en rosace, approximés en cercles. Le tracé
            vectoriel complet du logo n'apporterait rien à cette taille et
            alourdirait un rendu contraint à 500 Ko. */}
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div style={{ display: "flex", position: "relative", width: 56, height: 56 }}>
            <div
              style={{
                position: "absolute",
                top: 0,
                left: 6,
                width: 30,
                height: 30,
                borderRadius: 15,
                backgroundColor: COLOR.brand,
              }}
            />
            <div
              style={{
                position: "absolute",
                top: 14,
                left: 26,
                width: 30,
                height: 30,
                borderRadius: 15,
                backgroundColor: COLOR.brandLight,
              }}
            />
            <div
              style={{
                position: "absolute",
                top: 30,
                left: 2,
                width: 30,
                height: 30,
                borderRadius: 15,
                backgroundColor: COLOR.brand,
              }}
            />
          </div>
          <div
            style={{
              fontSize: 40,
              fontWeight: 700,
              color: COLOR.ink,
              letterSpacing: "-0.02em",
            }}
          >
            {BRAND.name}
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column" }}>
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              fontSize: 76,
              fontWeight: 700,
              color: COLOR.ink,
              letterSpacing: "-0.035em",
              lineHeight: 1.05,
            }}
          >
            <span style={{ marginRight: 18 }}>Votre argent construit votre</span>
            {/* Le bloc plein du hero, repris tel quel : l'aperçu partagé et la
                page d'arrivée doivent se reconnaître au premier coup d'œil. */}
            <span
              style={{
                display: "flex",
                backgroundColor: COLOR.brand,
                color: "#ffffff",
                borderRadius: 18,
                padding: "0 20px 10px 20px",
              }}
            >
              score
            </span>
          </div>

          <div
            style={{
              marginTop: 28,
              fontSize: 30,
              color: COLOR.inkMuted,
              lineHeight: 1.4,
            }}
          >
            Payez, épargnez, transférez. Kola note votre fiabilité de 0 à 100 et
            vous prête en conséquence.
          </div>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 16,
            fontSize: 24,
            color: COLOR.inkMuted,
          }}
        >
          <div
            style={{
              width: 12,
              height: 12,
              borderRadius: 6,
              backgroundColor: COLOR.ochre,
            }}
          />
          <div style={{ display: "flex" }}>
            Mobile money · Afrique de l&apos;Ouest · XOF
          </div>
        </div>
      </div>
    ),
    size
  );
}
