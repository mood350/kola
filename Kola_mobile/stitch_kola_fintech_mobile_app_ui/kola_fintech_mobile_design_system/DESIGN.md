---
name: KOLA Fintech Mobile Design System
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434750'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#747781'
  outline-variant: '#c4c6d2'
  surface-tint: '#3c5d9c'
  primary: '#002353'
  on-primary: '#ffffff'
  primary-container: '#0f3875'
  on-primary-container: '#83a3e7'
  inverse-primary: '#adc6ff'
  secondary: '#745b00'
  on-secondary: '#ffffff'
  secondary-container: '#fecb00'
  on-secondary-container: '#6e5700'
  tertiary: '#002b1b'
  on-tertiary: '#ffffff'
  tertiary-container: '#00432c'
  on-tertiary-container: '#14ba82'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#214583'
  secondary-fixed: '#ffe08b'
  secondary-fixed-dim: '#f1c100'
  on-secondary-fixed: '#241a00'
  on-secondary-fixed-variant: '#584400'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-currency:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 38px
    letterSpacing: -0.02em
  display-currency-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '800'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter-xs: 0.25rem
  gutter-sm: 0.5rem
  gutter-md: 1rem
  gutter-lg: 1.5rem
  gutter-xl: 2rem
  margin-screen: 1.25rem
  card-padding: 1.25rem
---

## Brand & Style

Ce système de design s'adresse à l'écosystème financier digital ouest-africain (zone UEMOA : Côte d'Ivoire, Sénégal, Mali, Burkina Faso, Togo, Bénin, Niger, Guinée-Bissau). Il incarne la confiance institutionnelle d'une banque moderne alliée à la réactivité et l'accessibilité du Mobile Money local.

Le style visuel repose sur le **Corporate / Modern dynamique à forte lisibilité**. Il combine :
- Une structure modulaire claire et tactile adaptée aux usages mobiles quotidiens en mobilité et sous fort ensoleillement.
- Des contrastes affirmés entre le bleu cobalt/roi sécurisant et le jaune solaire chaleureux, créant une signature visuelle mémorable et rassurante.
- Une clarté monétaire absolue pour l'affichage des montants en Franc CFA (XOF / FCFA), éliminant toute ambiguïté de conversion ou de virgule.
- Des indicateurs visuels immédiats pour le suivi de KYC progressif, la santé financière (score de crédit) et les objectifs d'épargne collective ou personnelle (tontines et coffres sécurisés).

## Colors

La palette chromatique est articulée autour de quatre piliers :

- **Primaire (Bleu Cobalt Profond - `#0F3875` & Accent `#0047BA`) :** Couleur de base des interfaces de compte, des cartes maîtres, des en-têtes et des boutons d'action de confirmation. Elle projette stabilité, sécurité bancaire et autorité institutionnelle.
- **Secondaire (Jaune Solaire Énergique - `#FFCC00` & `#F59E0B`) :** Utilisé pour les déclencheurs clés (boutons de scan QR central, points de contact majeurs, alertes transactionnelles et icônes d'activation). Ce jaune dynamique attire instantanément l'attention sans agresser l'œil.
- **Tertiaire (Vert Émeraude - `#10B981`) :** Dédié aux flux positifs (crédits reçus, cash-in, progression des coffres d'épargne, scores de crédit excellents et badges KYC validés).
- **Surfaces et Fonds (Gris Ardoise Neutres - `#F8FAFC`, `#F1F5F9`, `#E2E8F0`) :** Des fonds froids et lumineux qui permettent aux cartes et conteneurs bancaires de se détacher nettement tout en reposant la vue.
- **Texte Neutre (`#0F172A` et `#475569`) :** Assure un contraste WCAG AAA sur l'ensemble des écrans tactiles.

## Typography

Le système utilise **Plus Jakarta Sans** pour sa clarté géométrique, ses compte-poinçons ouverts et sa lisibilité irréprochable sur des dalles OLED comme LCD d'entrée de gamme.

Règles typographiques clés :
- **Soldes et Montants FCFA :** Utiliser systématiquement `display-currency` avec espacement des milliers par espace insécable (`2 500 000 FCFA`). Le suffixe `FCFA` ou `Fcfa` est affiché soit à taille égale, soit en `headline-sm` pour alléger la lecture sans masquer l'unité.
- **Remplacement monétaire :** En mode confidentialité (yeux masqués), le texte est remplacé par des puces grasses centrées verticalement (`•••••••• FCFA`).
- **Niveaux de Titres :** Toujours en graisses semi-bold (600) ou bold (700) pour ancrer les modules d'action rapide et séparer les sections sans surcharge de bordures.

## Layout & Spacing

Le layout repose sur une structure **Mobile-First fluide** :
- **Marges externes :** 20px (`1.25rem`) sur terminaux compacts (360px - 414px de large) pour maximiser la surface utile tout en évitant les déclenchements accidentels sur les bords incurvés.
- **Grille de raccourcis rapides (Quick Actions) :** Grille 4 colonnes équidistantes pour les fonctions vitales (Transfert, Crédit/Pass, Retrait, Factures/Paiements), avec espacement de 8px entre cellules.
- **Rythme vertical :** Grille de base basée sur un incrément strict de 4px / 8px. Espacement standard de 16px entre sections logiques, et 24px entre blocs de contexte distincts.
- **Bottom Navigation Bar :** Hauteur fixe de 68px (hors zone de sécurité iOS/Android) avec un bouton central d'action flottant surélevé de 12px pour la lecture QR et paiement marchand instantané.

## Elevation & Depth

La hiérarchie repose sur un modèle hybride de **couches tonales et d'ombres diffuses douces teintées de cobalt** :

1. **Niveau 0 (Toile de fond) :** Arrière-plan global `#F8FAFC`.
2. **Niveau 1 (Cartes de contenu & Listes) :** Fond blanc pur (`#FFFFFF`) avec une ombre ultra-douce `0 2px 8px -2px rgba(15, 56, 117, 0.06)` et bordure légère de 1px `#E2E8F0`.
3. **Niveau 2 (Cartes bancaires actives & Portefeuilles) :** Dégradés saturés cobalt (`linear-gradient(135deg, #0F3875 0%, #0047BA 100%)`) avec ombre portée profonde `0 10px 25px -5px rgba(15, 56, 117, 0.25)`.
4. **Niveau 3 (Éléments d'action clés & QR FAB) :** Le bouton central QR jaune solaire utilise un rayonnement diffus `0 6px 16px rgba(255, 204, 0, 0.40)` qui le détache immédiatement de la barre de navigation.
5. **Niveau 4 (Modales & Sheets de transaction) :** Panneaux coulissants du bas ancrés avec un voile noir opacifié à 40% et un rayon de courbure supérieur de 24px.

## Shapes

La grammaire géométrique du système adopte un standard arrondi et tactile (`roundedness: 2`) :
- **Cartes bancaires et conteneurs de soldes :** Rayon de courbure généreux de 24px (`rounded-2xl`) rappelant la tenue physique d'une carte plastique bancaire.
- **Tuiles d'action rapide & Icônes :** Carrés adoucis de 16px (`rounded-xl`) pour les fonds d'icônes, créant une surface tactile évidente pour le pouce.
- **Badges & Boutons d'action :** Boutons principaux en capsule complète (`rounded-full` ou `rounded-pill`) pour signifier l'interactivité sans hésitation.
- **Barres de progression des coffres :** Embouts totalement arrondis pour refléter la progression continue de l'épargne.

## Components

### 1. Carte Bancaire & Solde Principal
- **Visuel :** Rectangle au ratio 1.58:1, fond dégradé cobalt profond (`#0F3875` vers `#0047BA`), motif filigrane texturé en opacité 8%.
- **Contenu :** Logo KOLA en haut à gauche, QR code de réception rapide à droite, solde affiché en caractères gras 26px en jaune solaire (`#FFCC00`), suivi de contrôles discrets (bouton œil pour masquer, bouton d'actualisation rapide et flèche d'historique).
- **Pagination :** Indicateurs à tirets horizontaux arrondis (`rounded-full`), le tiret actif étant en bleu primaire étendu (largeur 24px vs 6px pour les inactifs).

### 2. Boutons d'Action
- **Primaire (Bleu Cobalt) :** Fond `#0F3875`, texte blanc ou `#FFCC00`, forme capsule (`rounded-full`), padding `12px 24px`, micro-interaction d'échelle à la pression (0.98).
- **Accent (Jaune Solaire) :** Fond `#FFCC00`, texte `#0F3875` (poids 700), utilisé pour les conversions prioritaires (ex: "Valider le transfert").
- **Secondaire / Tertiaire :** Fond `#F1F5F9`, texte `#0F3875`, bordure 1px invisible ou `#E2E8F0`.

### 3. Badges KYC Progressifs
- **Structure :** Badge compact en pilule (`rounded-full`) situé sous le profil utilisateur.
- **États :**
  - *Niveau 1 (Base) :* Contour ambre `#F59E0B` avec icône sablier.
  - *Niveau 2 (Vérifié / CNI) :* Fond `#ECFDF5`, bordure `#10B981`, texte `#065F46`, icône bouclier coché vert.
  - *Progression :* Mini jauge circulaire ou barre linéaire indiquant le pourcentage d'élévation de plafond journalier.

### 4. Jauge de Score de Crédit Interactif
- **Style :** Demi-arc semi-circulaire néo-moderne avec 4 segments de couleur (Rouge corail, Ambre, Vert lime, Émeraude).
- **Curseur :** Aiguille arrondie ou marqueur lumineux pointant la valeur actuelle (ex: "740 - Excellent"), accompagné du plafond d'emprunt débloqué en FCFA.

### 5. Coffres d'Épargne & Tontines Digitales
- **Carte Coffre :** Fond blanc, icône de cadenas ou tirelire sur fond teinté, nom du projet (ex: "Terrain", "Scolarité"), montant actuel vs montant cible.
- **Progression :** Barre d'avancement de 8px de hauteur, fond `#F1F5F9`, remplissage en dégradé `#10B981` à `#059669`. Tag de pourcentage dynamique au bout du curseur.

### 6. Champs de Saisie & Sélecteur d'Opérateur
- **Montant Input :** Champ géant sans bordure inférieure agressive, typographie 28px, affichage fixe du libellé "FCFA" à droite en couleur secondaire ou neutre clair.
- **Clavier Numérique Custom :** Pavé tactile optimisé pour la saisie à une main avec touche d'effacement rapide et validation haptique.
- **Sélecteur Télécom/Banque :** Pastilles circulaires avec logos des réseaux partenaires (Orange Money, Moov, MTN, Wave, Cartes GIM-UEMOA) entourées d'un anneau de sélection cobalt de 2px.