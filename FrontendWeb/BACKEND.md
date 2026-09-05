# BACKEND.md — Contrat API attendu par le back-office admin (FrontendWeb)

> Ce document est la source de vérité pour brancher un vrai backend derrière le
> back-office admin React. Il est généré à partir d'une lecture complète du
> code frontend (`src/models`, `src/repositories`, `src/services`, `src/hooks`,
> `src/admin/pages`) au 2026-09-04, sur la branche `Bienvenu`.
>
> **Le backend Spring implémente désormais les sections 4 à 13** — voir §14 pour
> la carte des classes, les écarts restants et les arbitrages rendus. Les sections
> qui suivent gardent le point de vue d'origine (« ce que le mock fait, ce qu'un vrai
> backend doit corriger ») parce qu'elles restent le contrat ; les corrections
> effectivement apportées sont annotées **✅**.
>
> Tant que `VITE_API_BASE_URL` (voir `.env.example`) n'est pas défini,
> l'application tourne entièrement sur des données en mémoire
> (`Mock*Repository`). Dès que la variable est définie, **chaque** repository
> bascule automatiquement vers son implémentation `Http*Repository` — voir
> `src/repositories/index.js`. Aucune autre ligne de code frontend n'a besoin
> de changer : il suffit que le backend respecte exactement les contrats
> ci-dessous.

## 1. Comment lire ce document

Pour chaque domaine métier :
- **Endpoints** : méthode HTTP + chemin (déjà figés dans `src/api/endpoints.js`, ne pas les renommer sans mettre à jour ce fichier).
- **Contrat** : forme exacte du JSON attendu en entrée/sortie (dérivée des `@typedef` dans `src/models/*.js`).
- **Règles métier** : déduites du comportement des `Mock*Repository` (qui font aujourd'hui office de spec exécutable) et des pages qui les consomment.
- **Notes d'implémentation** : ce qui manque au mock (idempotence, contrôle d'accès réel, double validation réelle...) et qu'un vrai backend doit corriger.

## 2. Architecture du frontend (pour comprendre où brancher)

```
pages/*.jsx  →  hooks/use*.js  →  services/*.js  →  repositories/index.js  →  Mock*Repository | Http*Repository  →  api/httpClient.js  →  fetch()
```

- **`models/*.js`** — types JSDoc uniquement (`@typedef`), aucune logique. C'est le contrat de données pur.
- **`repositories/*.js`** — chaque domaine expose une paire `Mock*Repository` / `Http*Repository` implémentant *exactement* la même interface. `Http*Repository` appelle `httpClient` + `endpoints`, sans aucune transformation : **la réponse JSON du backend doit correspondre 1:1 à la forme documentée ici**, aucun mapping n'est fait côté frontend.
- **`services/*.js`** — orchestrent plusieurs appels repository en parallèle (`Promise.all`) pour construire la vue "overview" d'une page, et portent les quelques règles de présentation pures (ex. filtrage/recherche côté client sur `users`, cycle de statut marchand).
- **`hooks/use*.js`** — bookkeeping React (`loading/error/data`, `reload()` après mutation) via `useAsync`/`useActionRunner` génériques. Aucune règle métier ici.
- **`admin/presentation.js`** — mappe des valeurs de domaine (enums) vers du style visuel. Confirme que le backend ne doit **jamais** renvoyer de champs de style (`stateStyle`, `dotColor`...), seulement des valeurs brutes (`'Actif'`, `'critical'`, `'reconciled'`...).
- **`admin/permissions.js`** — matrice de contrôle d'accès **côté client uniquement** (cache les liens de nav). ⚠️ Le backend doit ré-implémenter cette même matrice côté serveur (section 4).

## 3. Conventions API générales

- **Base URL** : `VITE_API_BASE_URL` (préfixe brut, sans slash final) + chemin ci-dessous. Ex. `https://api.dogaa.io/admin` + `/users` → `GET https://api.dogaa.io/admin/users`.
- **Format** : JSON partout, `Content-Type: application/json` envoyé sur chaque requête (même GET).
- **Auth** : `Authorization: Bearer <token>` sur chaque requête après login (`src/api/httpClient.js`). Le token est lu depuis `localStorage['dogaa_admin_token']`.
- **401 global** : toute réponse HTTP 401 déclenche un événement `dogaa:unauthorized` → le frontend efface le token et redirige vers `/login` immédiatement, sur **n'importe quel** appel. Donc : renvoyer 401 uniquement pour "token absent/expiré/invalide", jamais pour un refus de permission métier (utiliser 403 pour ça — voir section 4).
- **Erreurs** : le body d'erreur doit contenir `{ "message": "texte affichable à l'admin" }`. C'est ce message qui est montré tel quel dans les bandeaux d'erreur de l'UI (`ErrorState`, `actionError`).
- **Requêtes de mutation sans payload** : le frontend envoie quand même un body `{}` (ex. `unblock`, `takeCharge`). Le backend doit accepter un body vide/`{}` sur ces routes.
- **Pas de pagination actuellement** : toutes les listes (`GET /users`, `GET /disputes`, `GET /support/tickets`, etc.) sont récupérées en une fois ; le filtrage/la recherche sont faits **côté client** sur la liste complète (voir `userService.filterUsers/searchUsers`). Si le volume réel impose une pagination serveur, c'est un changement de contrat qui devra être coordonné avec le frontend (pas juste une addition rétrocompatible, puisque le filtrage client suppose la liste complète).

## 4. Authentification & contrôle d'accès

### 4.1 Endpoints

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| POST | `/auth/login` | `{ email, password }` | `{ token: string, user: AdminIdentity }` |
| GET | `/auth/me` | — (Bearer) | `AdminIdentity` |
| POST | `/auth/logout` | `{}` | `void` |
| POST | `/auth/password-reset-request` | `{ email }` | `void` (toujours 200, ne jamais révéler si l'email existe) |
| POST | `/auth/change-password` | `{ currentPassword, newPassword }` | `void` |

**`AdminIdentity`**
```ts
{
  id: string,
  name: string,
  email: string,
  role: 'Super-admin' | 'Agent conformité' | 'Analyste crédit' | 'Support',
  scope: string   // résumé libre affiché tel quel dans le profil, ex. "KYC, litiges, chargebacks (2e validation)"
}
```

### 4.2 Comptes admin de référence (seed actuel côté mock)

| Email | Nom | Rôle | Scope affiché |
|---|---|---|---|
| sena.ametepe@dogaa.io | Sena Amétépé | Super-admin | Accès total · configuration produit |
| koffi.messan@dogaa.io | Koffi Messan | Agent conformité | KYC, litiges, chargebacks (2e validation) |
| aya.djobo@dogaa.io | Aya Djobo | Analyste crédit | Scoring, paliers de prêt, défauts |
| prisca.lawson@dogaa.io | Prisca Lawson | Support | Tickets, consultation comptes (lecture seule) |

Ces 4 comptes doivent probablement devenir les premières lignes d'une table `admin_account` réelle (avec mot de passe haché — le mock ne vérifie **aucun** mot de passe, ce n'est évidemment pas acceptable en prod).

✅ **Fait** : `AdminAccountSeeder` crée ces 4 lignes au premier démarrage si la table est vide, mot de passe BCrypt commun `DogaaAdmin2026!`. Désactiver avec `app.admin.seed.enabled=false` avant la prod.

### 4.3 Matrice de permissions par rôle — ✅ appliquée côté serveur

C'est `admin/permissions.js` côté client (elle cache des liens de menu, rien de plus). ✅ Le backend reproduit désormais **exactement** cette matrice en autorisation serveur — 403 si le rôle n'a pas accès au module, jamais 401, pour que la redirection vers `/login` ne se déclenche pas à tort :

| Rôle | Modules autorisés |
|---|---|
| Super-admin | dashboard, users, credit, finance, disputes, config, audit, roles, support (tout) |
| Agent conformité | dashboard, users, disputes, audit, support |
| Analyste crédit | dashboard, credit, finance |
| Support | dashboard, users, support |

`profile` (changement de mot de passe, consultation de son propre compte) est accessible à tous les rôles, sans exception.

⚠️ **Écart à trancher** : le scope texte de "Support" dit *"consultation comptes (lecture seule)"*, mais la matrice ci-dessus donne accès à la page `users` en entier, y compris ses actions (`Débloquer le compte`, `Forcer fermeture d'un coffre`, approuver/rejeter le KYC). Le mock actuel ne distingue pas lecture/écriture — c'est un problème à résoudre avant d'exposer un vrai rôle "Support" en production : soit on ajoute un contrôle d'accès **par action** (au-delà de la simple visibilité de page), soit on assume que "Support" peut aussi agir sur `users` et on met à jour le texte de scope. Recommandation : ajouter cette granularité côté backend (ex. permissions `users:read` / `users:write` par rôle) plutôt que de la câbler en dur, car `roles.jsx` permet déjà de changer le `role` et le `scope` d'un admin dynamiquement.

## 5. Dashboard

| Méthode | Chemin | Réponse |
|---|---|---|
| GET | `/dashboard/metrics` | `Metric[]` |
| GET | `/dashboard/transaction-volume?period=14d\|30d` | `ChartPoint[]` |
| GET | `/dashboard/alerts` | `Alert[]` |
| GET | `/dashboard/loan-book-summary` | `LoanBookSummary` |

```ts
Metric        { label: string, value: string, delta: string, up: boolean }
ChartPoint    { day: string, h: number /* 0-100, hauteur de barre en % */, last: boolean }
Alert         { title: string, detail: string, severity: 'critical' | 'warning' }
LoanBookSummary {
  outstanding: string, allocatedPct: number /* 0-100 */,
  defaultRate: string, defaultRateNote: string,
  userGrowth: string, activeUsersTotal: string
}
```

**Notes** :
- Les 5 metrics affichées sont, dans l'ordre : volume 24h, solde global, croissance utilisateurs, encours de prêts, taux de défaut. Le frontend affiche les items dans l'ordre du tableau renvoyé (5 colonnes fixes) — respecter cet ordre ou le frontend n'a pas de logique de tri/label-matching.
- `/dashboard/alerts` alimente **aussi** la cloche de notifications du header (`useNotifications` réutilise `dashboardService.getOverview()`), pas seulement la page Dashboard. Le nombre d'items non lus détermine le badge rouge.
- Le bouton "14 j / 30 j" existe visuellement mais n'est **pas encore câblé** à `getTransactionVolume(period)` dans la page actuelle (toujours "14d" par défaut) — prévoir l'endpoint quand même, il est dans `endpoints.js`.

## 6. Utilisateurs (comptes clients + file KYC)

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| GET | `/users` | — | `ClientUser[]` |
| GET | `/users/:id` | — | `ClientUser` (404 si absent) |
| POST | `/users/:id/unblock` | `{}` | `ClientUser` |
| POST | `/users/:id/force-close-vault` | `{}` | `ClientUser` |
| GET | `/users/kyc-queue` | — | `KycSubmission[]` |
| POST | `/users/kyc-queue/:submissionId/approve` | `{}` | `void` |
| POST | `/users/kyc-queue/:submissionId/reject` | `{}` | `void` |

```ts
ClientUser {
  id: number,
  initials: string, name: string, phone: string,
  tier: 'TIER_0' | 'TIER_1' | 'TIER_2' | 'TIER_3',
  score: number,               // 0-100
  age: string,                 // ancienneté humaine, ex. "14 mois"
  state: 'Actif' | 'Gelé' | 'Litige',
  vaults: number,               // nb de coffres actifs
  loan: string                  // "100 000 XOF" ou "Aucun"
}
KycSubmission { id: string, name: string, fromTier: string, toTier: string, receivedAt: string }
```

**Règles métier (extraites du mock + de la page `Users.jsx`)** :
- `unblock(id)` → passe `state` à `'Actif'`, quel que soit l'état de départ.
- `force-close-vault(id)` → dans le mock, ferme **un** coffre parmi ceux de l'utilisateur (`vaults - 1`, jamais < 0) sans préciser lequel. Le bouton est désactivé côté UI si `vaults === 0`. ⚠️ **À affiner en vrai** : un vrai backend a une table `Vault` avec des IDs (`modules/vault/entity/Vault.java` existe déjà côté backend) — il faudra probablement faire évoluer ce endpoint pour accepter un `vaultId` explicite plutôt que de fermer "un coffre au hasard". Le contrat actuel (`POST /users/:id/force-close-vault` sans body) est celui que le frontend appelle aujourd'hui ; si vous changez la signature, il faudra aussi modifier `Users.jsx` pour choisir le coffre.
- `approveKyc(submissionId)` → fait passer `tier` de l'utilisateur cible de `fromTier` à `toTier`, puis retire l'entrée de la file KYC.
- `rejectKyc(submissionId)` → retire simplement l'entrée de la file, sans toucher au `tier` de l'utilisateur. (Le mock ne capture pas de motif de rejet — la page n'a pas de champ "raison" ; à ajouter si le métier l'exige, ce serait un changement de contrat côté formulaire aussi.)
- Filtrage : la page propose 4 filtres fixes — `Tous`, `TIER_2`, `TIER_3`, `Litige` — appliqués **côté client** sur le tableau complet (`u.tier === filter || u.state === filter`). Le backend n'a pas besoin de paramètre de filtre pour `GET /users` tant que le volume reste gérable côté client.
- Recherche : la barre de recherche du header (`AdminLayout`) redirige vers `/admin/users?q=...` ; la recherche `name`/`phone` (insensible à la casse, substring) est faite côté client sur la liste complète.
- Chaque KYC queue entry correspond très probablement à une soumission de document dans `modules/kyc/entity/KycDocument.java` côté backend — c'est le point d'intégration naturel plutôt qu'une nouvelle table.

## 7. Crédit (portefeuille de prêts, scoring, défauts)

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| GET | `/credit/stats` | — | `CreditStats` |
| GET | `/credit/tier-config` | — | `TierConfig[]` |
| PUT | `/credit/tier-config` | `{ tiers: TierConfig[] }` | `TierConfig[]` |
| GET | `/credit/defaults` | — | `LoanDefault[]` |
| POST | `/credit/defaults/:loanId/remind` | `{}` | `void` |

```ts
CreditStats  { outstandingTotal: string, defaultRate: string, lateLoans: number }
TierConfig   { name: string /* "TIER 1" */, minScore: number, maxAmount: string, monthlyRate: string }
LoanDefault  { id: string, borrowerName: string, amount: string, daysLate: number }
```

**Règles métier / notes** :
- La page affiche explicitement *"Réservé au rôle super-admin · version historisée"* sous l'éditeur de `tierConfig` → `PUT /credit/tier-config` doit être (a) restreint au rôle Super-admin côté serveur, (b) **historisé** (garder les versions précédentes, probablement pour l'audit/compliance) plutôt qu'un simple `UPDATE` en place.
- `remind(loanId)` déclenche une relance (SMS/notification) — aucune réponse attendue côté UI au-delà d'un succès HTTP ; à brancher sur `modules/notification`.
- `TierConfig[]` correspond au paramétrage utilisé par `modules/scoring` (paliers de score / plafond de prêt) — probablement à stocker comme config versionnée plutôt que dans l'entité `CreditScore` elle-même.

✅ **Fait** : `CreditTierConfig` + `CreditLadderService`. L'édition est réservée au Super-admin (403 sinon), refuse une échelle qui n'a pas le bon nombre de paliers (400) et **empile une version au lieu d'écraser** — les conditions d'un prêt déjà accordé restent lisibles. Le service charge la dernière version au démarrage dans `CreditProperties`, si bien que `CreditPolicy` reste de l'arithmétique pure qui ignore l'existence d'une base. Tant que rien n'est enregistré, `application.properties` fait foi : la version 1 est la première sauvegarde back-office, pas le barème de départ. `remind(loanId)` envoie un SMS via `modules/notification` et écrit au journal — une relance est un contact client.

## 8. Suivi financier (lecture seule)

| Méthode | Chemin | Réponse |
|---|---|---|
| GET | `/finance/liquidity` | `LiquidityBucket[]` |
| GET | `/finance/revenue` | `{ lines: RevenueLine[], total: string }` |
| GET | `/finance/operator-reconciliation` | `OperatorStatus[]` |

```ts
LiquidityBucket { label: string, value: string, note: string, pct: number /* 0-100, doit sommer à 100 sur l'ensemble */ }
RevenueLine     { label: string, value: string, pct: number /* 0-100 */ }
OperatorStatus  { name: string, status: 'reconciled' | 'discrepancy' }
```

Aucune mutation sur ce module — 100% lecture, agrégations calculées à partir de `Wallet`/`Vault`/`Loan`/`Transaction`. Les opérateurs mobile money référencés dans le seed (`Moov Money`, `Orange Money`, `MTN Mobile Money`) donnent une idée des intégrations à réconcilier.

## 9. Litiges & fraude (chargebacks à double validation)

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| GET | `/disputes` | — | `Dispute[]` |
| GET | `/disputes/:ref` | — | `DisputeDetail` |
| POST | `/disputes/:ref/chargeback` | `{}` | `Dispute` |
| POST | `/disputes/:ref/reject` | `{}` | `Dispute` |
| POST | `/disputes/:ref/validate` | `{}` | `DisputeDetail` |

```ts
Dispute {
  ref: string,                 // référence de transaction, ex. "TX-99C41A"
  tag: 'fraud' | 'double_debit' | 'p2p',
  tagLabel: string,
  amount: string,
  title: string,
  meta: string,                 // texte libre déjà formaté, ex. "Ouvert il y a 2 h · TIER_2 · Lomé"
  status: 'open' | 'chargeback_pending' | 'resolved' | 'rejected'
}
DisputeDetail {
  ref: string, debitedAccount: string, creditedAccount: string, amount: string,
  validationsRequired: number, validationsDone: number, lastValidationNote: string
}
```

**Règles métier** :
- `chargeback(ref)` → `status: 'open' → 'chargeback_pending'`.
- `reject(ref)` → `status → 'rejected'` (classé sans suite).
- `validate(ref)` → incrémente `validationsDone` (jamais au-delà de `validationsRequired`) ; quand `validationsDone >= validationsRequired`, le litige passe `status → 'resolved'` et `lastValidationNote` doit indiquer que le chargeback a été exécuté. Le bouton "Valider" est désactivé côté UI dès que le seuil est atteint.
- L'UI affiche systématiquement *"⭑ double validation requise"* sur chaque litige — **le mock ne vérifie absolument pas que les deux validations viennent de deux admins différents** (`validationsDone` est juste un compteur). ⚠️ **C'est le point le plus important à corriger dans le vrai backend** : la 2ᵉ validation doit être refusée (403 ou erreur métier) si elle vient du même compte admin que la 1ʳᵉ. `lastValidationNote` doit citer l'identité de l'admin qui vient de valider (le seed montre le format attendu : `"1re validation : Sena A. — en attente d'un 2e admin conformité"`).

  ✅ **Fait, et structurellement** : `DisputeValidation` porte l'identité du signataire avec une
  contrainte d'unicité `(disputeId, adminId)` — le même admin qui resigne reçoit un **403** qui le
  dit. Le renversement des fonds s'exécute dans la transaction de la validation qui l'atteint : si
  le portefeuille du bénéficiaire ne peut plus le couvrir, la validation est annulée avec lui,
  plutôt que de laisser un litige « résolu » sans mouvement d'argent. Valider sans chargeback en
  cours renvoie **409**.
- Chaque validation/chargeback/rejet est une action sensible → doit alimenter le journal d'audit (`AuditLogEntry`).

## 10. Configuration (frais & marchands)

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| GET | `/config/fees` | — | `FeeConfig[]` |
| PUT | `/config/fees` | `{ fees: FeeConfig[] }` | `FeeConfig[]` |
| GET | `/config/merchants` | — | `Merchant[]` |
| PATCH | `/config/merchants/:id/status` | `{ status }` | `Merchant` |

```ts
FeeConfig { tier: string /* "TIER_0".."TIER_3" */, p2p: string, merchant: string, cashout: string }
Merchant  { id: string, name: string, category: string, status: 'active' | 'suspended' | 'pending' }
```

**Règle métier — cycle de statut marchand** (aujourd'hui calculé **côté frontend**, dans `configService.toggleMerchantStatus`, un seul bouton dont le libellé dépend du statut courant) :

| Statut actuel | Libellé bouton | Nouveau statut envoyé au PATCH |
|---|---|---|
| `active` | "Désactiver" | `suspended` |
| `suspended` | "Réactiver" | `active` |
| `pending` | "Approuver" | `active` |

Le frontend envoie directement le statut cible calculé — le backend doit néanmoins **revalider** cette transition côté serveur (ne pas faire confiance au client), et rejeter toute valeur de `status` hors de l'énum.

L'UI marque la page frais comme *"super-admin uniquement"* → `PUT /config/fees` doit être restreint au rôle Super-admin côté serveur (comme `/credit/tier-config`).

✅ **Fait** : `MerchantStatus.canTransitionTo` revalide la transition côté serveur — `pending → active|suspended`, `active → suspended`, `suspended → active`, tout le reste renvoie **409**, y compris le fait de réappliquer le statut courant. `PUT /config/fees` exige le Super-admin (403) et une ligne par palier KYC, ni plus ni moins (400).

✅ **Et la grille sert vraiment** : `FeeScheduleEntry` + `FeeScheduleService` alimentent `FeeCalculator`. Une grille enregistrée ici **prime sur `app.fees.*`** — elle est déjà exprimée par palier, donc le multiplicateur de palier ne s'applique pas une seconde fois par-dessus. Tant que rien n'est enregistré, le calcul retombe sur le taux de base × multiplicateur.

## 11. Conformité & Audit

| Méthode | Chemin | Réponse |
|---|---|---|
| GET | `/audit/log` | `AuditLogEntry[]` |
| POST | `/audit/log/export` | `{ url: string \| null }` |
| GET | `/audit/reports` | `ComplianceReport[]` |
| POST | `/audit/reports/:id/export` | `{ url: string \| null }` |

```ts
AuditLogEntry    { id: string, admin: string, action: string, diff: string /* "avant → après" */, time: string }
ComplianceReport { id: string, name: string, period: string }
```

**Notes** :
- Le journal est explicitement décrit comme **immuable** et *"filtrable par admin, action ou période"* dans l'UI (le filtrage n'est pas encore câblé côté frontend actuel, mais l'intention est claire — prévoir les paramètres de requête si vous les ajoutez).
- `diff` est une chaîne déjà formatée "avant → après" (ex. `"Gelé → Actif"`, `"8 % → 7 %/mois"`) — c'est le backend qui doit la construire, le frontend ne fait aucune transformation.
- **Ce log est le journal central référencé par tout le reste de l'admin** : chaque badge "⭑ action tracée" (déblocage compte, fermeture coffre forcée, modification des paliers de crédit, chargeback/validation, changement de statut marchand, modification de permissions admin, prise en charge/résolution de ticket) doit produire une entrée ici. C'est donc un composant transverse à construire tôt (ex. un `AuditService.record(admin, action, diff)` appelé par tous les autres services), pas un module isolé.

  ✅ **Fait** : `modules/audit` expose exactement ce `AuditService.record(...)`, et il a été branché en deuxième, avant les modules qui y écrivent. `/support/manual-actions` est d'ailleurs **dérivé** de ce journal plutôt que stocké à part : une intervention manuelle *est* une ligne d'audit.
- Export : le mock renvoie `{ url: null }` — un vrai backend générera probablement un fichier (CSV/PDF) et une URL de téléchargement signée/temporaire. ⚠️ **Toujours ouvert** : le backend renvoie lui aussi `{ url: null }`, la génération de fichier n'est pas implémentée.
- Les rapports de conformité seedés (*"Rapport mensuel AML"*, *"Déclarations de soupçon"*, *"Seuils de transactions suspectes"*) donnent le type de rapports réglementaires UEMOA/BCEAO attendus.

## 12. Rôles admin

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| GET | `/roles/admins` | — | `AdminAccount[]` |
| PATCH | `/roles/admins/:id/permissions` | `{ role, scope }` | `AdminAccount` |

```ts
AdminAccount { id: string, initials: string, name: string, role: AdminRole, scope: string }
```

La page `Roles.jsx` permet d'éditer `role` (select fermé sur les 4 valeurs) et `scope` (texte libre) pour n'importe quel admin listé. Cet endpoint devrait être réservé au Super-admin côté serveur (l'UI ne le restreint pas explicitement aujourd'hui — c'est un gap à combler). Toute modification doit aussi produire une entrée dans le journal d'audit (section 11).

## 13. Support client

| Méthode | Chemin | Body | Réponse |
|---|---|---|---|
| GET | `/support/tickets` | — | `SupportTicket[]` |
| GET | `/support/manual-actions` | — | `ManualAction[]` |
| POST | `/support/tickets/:ref/take-charge` | `{}` | `SupportTicket` |
| POST | `/support/tickets/:ref/resolve` | `{}` | `SupportTicket` |

```ts
SupportTicket { ref: string /* "#8821" */, subject: string, userName: string, status: 'open' | 'in_progress' | 'resolved' }
ManualAction  { id: string, action: string, by: string, time: string }
```

**Règles métier** :
- `take-charge(ref)` → `status: 'open' → 'in_progress'`, et doit créer une `ManualAction` (le mock génère `"Ticket {ref} pris en charge"`, `by: <admin courant>`).
- `resolve(ref)` → `status → 'resolved'`, et crée une `ManualAction` (`"Ticket {ref} marqué résolu"`).
- Le bouton "Prendre en charge" n'apparaît que si `status === 'open'` ; "Marquer résolu" apparaît tant que `status !== 'resolved'` (donc utilisable directement depuis `open`, sans passer par `in_progress`).
- `manual-actions` est un flux d'audit **local à Support**, distinct du journal d'audit global (section 11) mais avec le même esprit — les deux pourraient à terme être unifiés côté backend (même table, filtrée par module) plutôt que dupliqués.

✅ **Unifiés, comme suggéré** : `manual-actions` est une **projection du journal d'audit**, pas une seconde table. Une intervention manuelle *est* une ligne d'audit ; en stocker une copie aurait créé deux vérités qui divergent. Côté concurrence, prendre en charge un ticket déjà pris ou résoudre un ticket déjà résolu renvoie **409** — deux agents qui cliquent en même temps ne se volent pas le ticket en silence.

## 14. État d'implémentation côté backend Spring

**Les sections 4 à 13 sont livrées.** Ce document reste le contrat de référence, mais il n'est plus
une liste de choses à construire : il décrit ce que le backend expose aujourd'hui. Les écarts
résiduels sont signalés en fin de section.

| Section | Statut | Où |
|---|---|---|
| 4 — Auth & permissions | ✅ | `admin/{entity/AdminAccount,security/CurrentAdmin,service/AdminAccountService}`, `AdminAuthController` |
| 5 — Dashboard | ✅ | `AdminDashboardController`, `AdminDashboardMetricsService` |
| 6 — Utilisateurs & file KYC | ✅ | `AdminUserController`, `AdminUserService` |
| 7 — Crédit | ✅ | `AdminCreditController`, `AdminCreditService`, `credit/service/CreditLadderService` |
| 8 — Suivi financier | ✅ | `AdminFinanceController`, `AdminFinanceService` |
| 9 — Litiges & chargebacks | ✅ | `AdminDisputeController`, `AdminDisputeService` |
| 10 — Configuration | ✅ | `AdminConfigController`, `AdminConfigService`, `transaction/service/FeeScheduleService` |
| 11 — Conformité & audit | ✅ | `modules/audit/*` |
| 12 — Rôles admin | ✅ | `AdminRolesController` |
| 13 — Support client | ✅ | `AdminSupportController`, `AdminSupportService` |

### Entités créées pour le back-office

| Entité | Module | Rôle |
|---|---|---|
| `AdminAccount`, `AdminRole`, `AdminModule` | `admin/entity` | comptes back-office et matrice de permissions |
| `Dispute`, `DisputeStatus`, `DisputeTag`, `DisputeValidation` | `admin/entity` | litiges et signatures de chargeback |
| `Merchant`, `MerchantStatus` | `admin/entity` | marchands partenaires et machine à états |
| `SupportTicket`, `SupportTicketStatus` | `admin/entity` | file de tickets |
| `AuditLogEntry` | `audit/entity` | journal transverse |
| `CreditTierConfig` | `credit/entity` | échelle de prêt **versionnée** |
| `FeeScheduleEntry` | `transaction/entity` | grille de frais par palier KYC |

Les entités métier existantes ont bien été **réutilisées et non dupliquées** : `User`, `KycDocument`,
`Loan`, `CreditScore`, `Vault`, `Wallet`, `Transaction`, `modules/notification`.

### Trois choses que le mock ne disait pas et que le backend fait

- **La double validation est structurelle.** `DisputeValidation` porte l'identité de l'admin
  signataire et une contrainte d'unicité `(disputeId, adminId)` : le même admin ne peut pas signer
  deux fois, la seconde tentative renvoie 403. Le renversement des fonds s'exécute dans la
  transaction de la validation qui le déclenche, donc un litige ne peut pas être marqué résolu
  pendant que l'argent reste en place.
- **Les barèmes s'empilent, ils ne s'écrasent pas.** `CreditLadderService` enregistre une nouvelle
  version à chaque édition et charge la dernière au démarrage ; les conditions d'un prêt accordé
  restent lisibles après coup. Tant qu'aucune sauvegarde n'existe, c'est `application.properties`
  qui fait foi — la version 1 est la première sauvegarde back-office, pas le barème de départ.
- **`manual-actions` n'a pas d'entité.** La liste est dérivée du journal d'audit : une intervention
  manuelle *est* une ligne d'audit, en stocker une copie aurait créé deux vérités.

### Ce qui reste ouvert

- **`ClientUser.state` ne renvoie jamais `"Litige"`** — le module litiges existe mais n'est pas
  branché sur l'état du compte (`AdminUserService.state`).
- **Les exports** (`/audit/log/export`, `/audit/reports/{id}/export`) renvoient `{ url: null }` : la
  génération de fichier n'est pas implémentée.
- **`force-close-vault`** ferme le plus ancien coffre ouvert, faute de `vaultId` dans le contrat.
- **Le filtrage reste côté client** pour `/users`, `/disputes` et `/support/tickets`.
- **Les comptes de seed** (`app.admin.seed.enabled`) partagent un mot de passe commun : à désactiver
  avant la prod.

## 15. Ordre d'implémentation — historique

L'ordre suivi a été celui-ci, et il s'est vérifié : auth admin → journal d'audit → utilisateurs/KYC
→ crédit → litiges → configuration/support/rôles → dashboard/finance. Brancher l'audit en deuxième
plutôt qu'en dernier était le bon pari : presque tous les modules suivants y écrivent, et le faire
après coup aurait voulu dire repasser dans chacun d'eux.

## 16. Points ouverts — arbitrages rendus

| Point | Décision |
|---|---|
| Double validation des chargebacks | **Tranché** : 2 admins distincts, garanti par l'unicité `(disputeId, adminId)` et un 403 explicite. |
| `force-close-vault` sans `vaultId` | **Non tranché** : le backend ferme le plus ancien coffre ouvert, en attendant un contrat par coffre. |
| Granularité des permissions « Support » | **Non tranché** : la matrice reste par module, sans distinction lecture/écriture. Les écritures sensibles (barème de prêt, grille de frais) sont en revanche réservées au Super-admin. |
| Motif de rejet KYC | **Non tranché côté back-office** : `/admin/kyc/documents/{id}/review` exige un `rejectionReason`, mais `/admin/users/kyc-queue/{id}/reject` n'en prend pas. |
| Filtrage serveur | **Non tranché** : filtrage client conservé, à revisiter quand le volume l'imposera. |
