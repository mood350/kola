import { Hero } from "@/components/sections/hero";
import { Problem } from "@/components/sections/problem";
import { HowItWorks } from "@/components/sections/how-it-works";
import { Comparison } from "@/components/sections/comparison";
import { Features } from "@/components/sections/features";
import { CreditScore } from "@/components/sections/credit-score";
import { Security } from "@/components/sections/security";
import { Proof } from "@/components/sections/proof";
import { Reviews } from "@/components/sections/reviews";
import { Faq } from "@/components/sections/faq";
import { FinalCta } from "@/components/sections/final-cta";

/**
 * La page est un Server Component : seules les sections qui animent réellement
 * quelque chose passent en "use client". Les blocs purement statiques
 * (sécurité, questions fréquentes, pied de page) restent rendus côté serveur et
 * n'envoient aucun JavaScript au navigateur.
 *
 * ORDRE DES SECTIONS. Il suit une objection après l'autre, dans celui où elles
 * viennent : à quoi ça sert (hero), pourquoi c'est nécessaire (constat),
 * comment ça marche (parcours), en quoi c'est différent (comparatif), ce qu'on
 * peut faire (fonctions),
 * sur quoi repose la promesse (score), est-ce que c'est sûr (sécurité),
 * qu'est-ce qui le prouve (chiffres, avis), et enfin ce qui reste en travers
 * (questions fréquentes) — juste avant l'appel à l'action final.
 *
 * Les questions fréquentes sont placées EN DERNIER et non en annexe : elles
 * traitent les derniers doutes au moment où la décision se prend. Reléguées sur
 * une page séparée, elles ne seraient lues que par ceux qui doutent déjà assez
 * pour aller les chercher.
 *
 * Les métadonnées de cette page sont celles du layout racine — titre par
 * défaut, description, canonique « / ». Les redéclarer ici produirait deux
 * sources pour la même information, avec la certitude qu'elles divergent.
 */
export default function Home() {
  return (
    <main id="contenu">
      <Hero />
      <Problem />
      <HowItWorks />
      <Comparison />
      <Features />
      <CreditScore />
      <Security />
      <Proof />
      <Reviews />
      <Faq />
      <FinalCta />
    </main>
  );
}
