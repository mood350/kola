# Dogaa — support de présentation

> Contenu source pour générer une présentation (Gamma, Canva, Beautiful.ai, PowerPoint…).
> Tous les chiffres de ce document sont mesurés sur le dépôt au 5 septembre 2026, pas estimés.
> La section finale contient un prompt prêt à copier-coller.

---

## Slide 1 — Titre

**Dogaa**
Le portefeuille mobile qui transforme l'épargne en pouvoir d'emprunt

Zone UEMOA — Togo, Sénégal, Côte d'Ivoire, Ghana

---

## Slide 2 — Le problème

En Afrique de l'Ouest, des millions de personnes ont un téléphone et de l'argent mobile,
mais **pas d'accès au crédit**.

- Pas d'historique bancaire → pas de score → pas de prêt.
- Les prêteurs informels pratiquent des taux prohibitifs.
- L'épargne existe, mais elle dort : elle ne donne accès à rien.

**Le paradoxe :** quelqu'un qui épargne 500 000 F avec discipline pendant six mois
n'a toujours aucun moyen de prouver qu'il est solvable.

---

## Slide 3 — Notre réponse

Dogaa réunit trois choses que le marché sépare :

| | |
|---|---|
| 💳 **Un portefeuille** | envoyer, recevoir, payer ses factures |
| 🔒 **Une épargne programmée** | des coffres, des versements automatiques |
| 📈 **Un microcrédit algorithmique** | adossé à l'épargne, sans garant ni paperasse |

**L'épargne devient la garantie.** Pas de dossier, pas de caution : votre comportement
sur l'application *est* votre dossier.

---

## Slide 4 — Comment ça marche pour l'utilisateur

1. **Je m'inscris** avec mon numéro et un code PIN. Pas de mot de passe, pas d'e-mail obligatoire.
2. **J'utilise mon compte** : je reçois, j'envoie, je paie mes factures.
3. **J'épargne** : je vire vers mon compte épargne, j'ouvre des coffres avec un objectif.
4. **Un score se construit** automatiquement, sur mon comportement réel des 30 derniers jours.
5. **J'emprunte** jusqu'à **1,6 × mon épargne**, sans jamais parler à personne.
6. **Je rembourse** — et mon levier augmente au prêt suivant.

---

## Slide 5 — Le modèle de crédit, en une image

> **Le levier se gagne en remboursant, jamais avec le score seul.**

| Prêts remboursés | Score requis | Levier | Taux mensuel |
|---|---|---|---|
| 0 | 60 | **×1,0** | 8,0 % |
| 1 | 60 | ×1,2 | 7,0 % |
| 2 | 75 | ×1,4 | 6,5 % |
| 3+ | 85 | **×1,6** | 6,0 % |

**Pourquoi c'est solide :** au premier prêt, l'épargne couvre exactement le capital.
Le premier prêt ne peut donc pas perdre d'argent, et faire défaut coûte plus cher à
l'emprunteur que ça ne lui rapporte. Au-delà, le risque est financé par un historique prouvé.

---

## Slide 6 — Le score : 5 axes sur 30 jours

| Axe | Points | Ce qu'il mesure |
|---|---|---|
| Discipline d'épargne | 30 | la part des entrées qui part vers l'épargne |
| Stabilité financière | 25 | un solde qui tient, sans découvert |
| Régularité des entrées | 20 | de l'argent qui rentre souvent, pas d'un coup |
| Intensité d'usage | 15 | un compte réellement utilisé |
| Historique de crédit | 10 | les prêts déjà remboursés |

---

## Slide 7 — Le score est difficile à truquer

Trois mécanismes, pensés ensemble :

1. **Chaque signal est une proportion**, jamais un nombre d'opérations.
   → Faire cent virements d'un franc ne vaut rien.
2. **Seuil de matérialité + facteur d'activité.**
   → Les petits montants sont ignorés, et les ratios parfaits sur un compte vide ne comptent presque pas.
3. **Moyenne mobile exponentielle.**
   → Une bonne soirée ne fait pas bondir le score ; une mauvaise ne l'effondre pas.

**Résultat mesuré :** un compte qui met en scène 500 F pour obtenir 100/100
obtient en réalité **moins de 10/100**. Un test automatisé verrouille ce comportement.

---

## Slide 8 — La sécurité, par construction

Nous avons cherché des garanties **structurelles** plutôt que des règles qu'on peut oublier d'écrire.

| Garantie | Comment |
|---|---|
| L'OTP ne peut pas être contourné | le DTO d'inscription **n'a pas de champ téléphone** — sauter l'OTP laisse l'endpoint sans numéro à inscrire |
| Le niveau KYC ne s'attribue pas | il se **déduit** des pièces fournies ; révoquer une pièce rétrograde le compte gratuitement |
| L'épargne gagée est intouchable | le prêt la déplace en solde bloqué, et le code de débit ne dépense que le disponible |
| Un remboursement litigieux exige deux validateurs | un **index unique** (litige, admin) : la même personne ne peut pas valider deux fois |
| Un paiement ne part jamais deux fois | clé d'idempotence en **index unique**, pas en vérification applicative |

---

## Slide 9 — Deux détails qui font la différence

**Le QR ne contient jamais le numéro de téléphone.**
Un QR se photographie, s'imprime, se transfère. Y encoder le numéro reviendrait à le
donner définitivement. Dogaa encode une référence aléatoire, révocable en un clic.

**Une planification ne puise jamais dans le compte courant.**
L'utilisateur désigne le coffre qui la finance. L'argent doit avoir été mis de côté
exprès — frais compris. Pas de prélèvement surprise à une date choisie six semaines plus tôt.

---

## Slide 10 — L'assistant IA

Un chat intégré qui répond sur l'application **et sur le compte de l'utilisateur**.

- Il connaît les règles du produit — **dérivées de la configuration réelle du serveur**,
  donc elles suivent automatiquement un changement de tarif. Aucun texte recopié qui pourrait mentir.
- Il voit la situation de l'appelant : niveau KYC, soldes, score, ce qui bloque son crédit.
- **Il ne peut exécuter aucune opération.** Aucun outil ne lui est donné : il explique et oriente.
  Mettre un modèle de langage sur le chemin du paiement n'est pas une chose que le prompt rend sûre.

---

## Slide 11 — Ce qui est construit

| | |
|---|---|
| **15 modules** métier | auth, KYC, wallet, transaction, vault, scheduling, credit, scoring, dispute, QR, assistant, notification, audit, admin, user |
| **103 endpoints** REST | sur 22 contrôleurs |
| **22 entités** persistées | PostgreSQL |
| **~15 800 lignes** de Java | Spring Boot 4.1.1, Java 17 |
| **271 tests** automatisés | tous au vert |

Back-office administrateur, journal d'audit, gestion des litiges avec double validation,
tableau de bord temps réel.

---

## Slide 12 — Architecture

**Découpage vertical par métier**, pas par couche technique :

```
modules/<métier>/{entity, dto, mapper, repository, service, controller}
```

Règles tenues :
- Un module ne touche jamais le *repository* d'un autre — l'accès passe par son service.
- Les modules communiquent par **événements** quand une dépendance créerait un cycle.
- Tout ce qui est transverse (sécurité, exceptions, configuration) vit **hors** des modules.

**Effet :** ajouter une fonctionnalité touche un dossier, pas six couches.

---

## Slide 13 — Stack technique

| Couche | Choix |
|---|---|
| Runtime | Spring Boot 4.1.1 · Java 17 |
| Données | PostgreSQL 18 · Spring Data JPA · H2 pour les tests |
| Sécurité | Spring Security · JWT HS256 · refresh tokens opaques et rotatifs · BCrypt 12 |
| API | REST · OpenAPI / Swagger |
| IA | API Anthropic (Claude) derrière une interface remplaçable |
| QR | ZXing |

Chaque intégration externe (SMS, e-mail, push, Mobile Money, stockage KYC) passe par une
**interface**, avec aujourd'hui une implémentation de développement. Brancher un vrai
fournisseur touche une classe.

---

## Slide 14 — Ce qui reste avant la production

Nous le disons franchement :

- **Aucune passerelle réelle n'est branchée** — SMS, e-mail, push et paiement Mobile Money
  journalisent au lieu d'envoyer. Les interfaces sont prêtes.
- Le stockage des pièces KYC est local, sans chiffrement au repos.
- 11 routes du back-office administrateur restent à écrire (finance, configuration, support).
- Conversion multi-devises : pas de source de taux de change.

---

## Slide 15 — La suite

**Court terme** — brancher un agrégateur SMS et une passerelle Mobile Money, chiffrer les
pièces KYC, finir le back-office.

**Moyen terme** — ouvrir le crédit au-delà du levier adossé, marchands partenaires,
support multi-devises réel.

**Ambition** — devenir l'historique de crédit que la zone UEMOA n'a pas.

---

## Slide 16 — Clôture

**Dogaa**

> L'épargne comme preuve. Le comportement comme dossier.

---
---

# Prompt prêt à copier-coller

À coller dans Gamma, ChatGPT, Claude, ou tout générateur de présentation.

```
Crée une présentation professionnelle de 16 slides pour "Dogaa", une fintech de la
zone UEMOA (Togo, Sénégal, Côte d'Ivoire, Ghana).

TON ET STYLE
Sobre, crédible, orienté produit et ingénierie. Pas de jargon startup creux, pas de
superlatifs. Chaque affirmation doit être appuyée par un chiffre ou un mécanisme concret.
Palette : bleu nuit et vert, typographie moderne, beaucoup de blanc. Utilise des tableaux
et des schémas simples plutôt que des listes à puces partout.

LE PRODUIT
Dogaa est un portefeuille mobile money qui réunit trois choses habituellement séparées :
un portefeuille du quotidien, une épargne programmée par coffres, et un microcrédit
algorithmique adossé à cette épargne.

LE PROBLÈME
Des millions de personnes en Afrique de l'Ouest ont un téléphone et de l'argent mobile
mais aucun accès au crédit : pas d'historique bancaire, donc pas de score, donc pas de
prêt. Leur épargne existe mais ne leur donne accès à rien.

LA PROPOSITION
L'épargne devient la garantie. Le comportement sur l'application remplace le dossier
bancaire. Pas de garant, pas de paperasse.

LE MODÈLE DE CRÉDIT (barème progressif)
- 0 prêt remboursé, score 60  → emprunt jusqu'à 1,0 × l'épargne, 8,0 %/mois
- 1 prêt remboursé, score 60  → 1,2 ×, 7,0 %/mois
- 2 prêts remboursés, score 75 → 1,4 ×, 6,5 %/mois
- 3+ prêts remboursés, score 85 → 1,6 ×, 6,0 %/mois
Point clé à mettre en valeur : au premier prêt l'épargne couvre exactement le capital,
donc ce prêt ne peut pas perdre d'argent et faire défaut coûte plus à l'emprunteur que
ça ne lui rapporte. Le levier se gagne en remboursant, jamais avec le score seul.

LE SCORE DE CRÉDIT (0-100, recalculé chaque nuit sur 30 jours)
Cinq axes : discipline d'épargne 30 points, stabilité financière 25, régularité des
entrées 20, intensité d'usage 15, historique de crédit 10.
Trois protections anti-triche : chaque signal est une proportion et non un nombre
d'opérations ; les petits montants sont ignorés et les ratios pondérés par le volume
réel ; le score publié est lissé dans le temps. Résultat mesuré : un compte qui met en
scène 500 F pour viser 100/100 obtient moins de 10/100.

SÉCURITÉ PAR CONSTRUCTION (insister là-dessus, c'est notre différenciateur)
- L'OTP d'inscription est structurellement incontournable : le formulaire n'a pas de
  champ téléphone, le numéro vient du jeton de vérification.
- Le niveau KYC n'est jamais attribué à la main, il se déduit des pièces fournies.
- L'épargne gagée par un prêt devient intouchable sans règle supplémentaire.
- Un remboursement de litige exige deux administrateurs différents, garanti par un
  index unique en base.
- Aucun paiement ne peut partir deux fois : clé d'idempotence en index unique.
- Un QR code de paiement ne contient jamais le numéro de téléphone, seulement une
  référence aléatoire révocable.
- Une transaction programmée ne puise jamais dans le compte courant : l'utilisateur
  désigne le coffre qui la finance, frais compris.

ASSISTANT IA INTÉGRÉ
Un chat qui répond sur l'application et sur le compte de l'utilisateur. Ses
connaissances produit sont dérivées de la configuration réelle du serveur, donc elles
ne peuvent pas devenir fausses après un changement de tarif. Il ne peut exécuter aucune
opération : il explique et oriente.

CHIFFRES RÉELS (ne pas arrondir vers le haut)
15 modules métier, 103 endpoints REST, 22 contrôleurs, 22 entités persistées,
environ 15 800 lignes de Java, 271 tests automatisés tous au vert.

STACK
Spring Boot 4.1.1, Java 17, PostgreSQL 18, Spring Security avec JWT et refresh tokens
rotatifs, BCrypt, OpenAPI/Swagger, ZXing pour les QR, API Anthropic pour l'assistant.
Architecture en tranches verticales par métier plutôt qu'en couches techniques.

CE QUI RESTE À FAIRE (à présenter honnêtement, une slide dédiée)
Aucune passerelle réelle n'est encore branchée : SMS, e-mail, push et paiement Mobile
Money journalisent au lieu d'envoyer, mais les interfaces sont prêtes. Le stockage des
pièces KYC est local et non chiffré. 11 routes du back-office administrateur restent à
écrire. Pas encore de source de taux de change pour le multi-devises.

PLAN DES SLIDES
1. Titre
2. Le problème
3. Notre réponse
4. Le parcours utilisateur en 6 étapes
5. Le modèle de crédit (tableau du barème)
6. Le score en 5 axes (tableau)
7. Pourquoi le score est difficile à truquer
8. La sécurité par construction (tableau)
9. Deux détails qui font la différence (QR et planification)
10. L'assistant IA
11. Ce qui est construit (chiffres)
12. Architecture
13. Stack technique
14. Ce qui reste avant la production
15. La suite
16. Clôture : "L'épargne comme preuve. Le comportement comme dossier."
```
