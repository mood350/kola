# BACKEND.md — Contrat API attendu par le back-office admin (FrontendWeb)

> Ce document est la source de vérité pour brancher un vrai backend derrière le
> back-office admin React. Il est généré à partir d'une lecture complète du
> code frontend (`src/models`, `src/repositories`, `src/services`, `src/hooks`,
> `src/admin/pages`) au 2026-09-04, sur la branche `Bienvenu`.
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

### 4.3 Matrice de permissions par rôle (à réimplémenter côté serveur !)

C'est aujourd'hui `admin/permissions.js`, appliquée **uniquement côté client** (elle cache des liens de menu, rien de plus — un rôle non autorisé qui appelle directement l'API aujourd'hui ne serait bloqué par rien). Le backend doit reproduire **exactement** cette matrice en autorisation serveur (403 si le rôle n'a pas accès au module) :

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
- Export : le mock renvoie `{ url: null }` — un vrai backend générera probablement un fichier (CSV/PDF) et une URL de téléchargement signée/temporaire.
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

## 14. Récapitulatif — état du backend Spring existant

Le module `backend/src/main/java/com/dogaa/backend/modules/admin/` existe déjà mais ne couvre **que** :
- `AdminController` / `AdminService(Impl)` / `AdminDashboardResponse` — un point d'entrée dashboard, à comparer/aligner avec la section 5 ci-dessus.
- Aucune entité admin (`entity/`, `repository/`, `mapper/` sont vides — juste des `.gitkeep`).

Tout le reste (users admin actions, credit tier-config, finance, disputes, config, audit, roles, support) **reste à construire**. Bonne nouvelle : les entités métier sous-jacentes existent déjà ailleurs dans le monolithe et doivent être **réutilisées**, pas dupliquées :

| Domaine admin | Entité(s) backend déjà existante(s) à brancher |
|---|---|
| Utilisateurs / KYC | `modules/user/entity/User.java`, `modules/kyc/entity/KycDocument.java` (+`KycDocumentStatus`, `KycDocumentType`) |
| Crédit | `modules/credit/entity/Loan.java`, `modules/scoring/entity/CreditScore.java` |
| Coffres (force-close-vault) | `modules/vault/entity/Vault.java` (+`VaultStatus`) |
| Finance / liquidité | `modules/wallet/entity/Wallet.java` (+`WalletStatus`), `modules/vault`, `modules/credit` |
| Transactions / litiges | `modules/transaction/entity/Transaction.java` |
| Notifications (relances, KYC) | `modules/notification` |

À créer entièrement (pas d'équivalent existant) :
- `AdminAccount` / rôles & permissions admin (section 4 & 12).
- `Dispute` / `DisputeValidation` (avec identité de l'admin par validation — section 9).
- `Merchant` + `FeeConfig` (section 10).
- `AuditLogEntry` (transverse — section 11), idéalement introduit tôt puisque presque tous les autres modules doivent y écrire.
- `SupportTicket` / `ManualAction` (section 13).

## 15. Ordre d'implémentation suggéré

1. **Auth admin réel** (table `AdminAccount`, hash de mot de passe, JWT, `/auth/*`) — bloque tout le reste, y compris les tests manuels du front avec `VITE_API_BASE_URL` renseigné.
2. **Journal d'audit** (`/audit/log` + service interne `record(...)`) — transverse, à brancher au fur et à mesure dans les modules suivants plutôt qu'après coup.
3. **Utilisateurs + KYC** (`/users*`) — plus gros volume de règles métier, réutilise `User`/`KycDocument` déjà existants.
4. **Crédit** (`/credit/*`) — réutilise `Loan`/`CreditScore`.
5. **Litiges** (`/disputes/*`) — nouveau, avec la vraie règle de double validation par 2 admins distincts.
6. **Configuration** (`/config/*`), **Support** (`/support/*`), **Rôles** (`/roles/*`) — CRUD plus simples.
7. **Dashboard** (`/dashboard/*`) et **Finance** (`/finance/*`) — agrégations en lecture seule, naturellement en dernier puisqu'elles peuvent lire tout ce qui a été construit avant.

## 16. Points ouverts à trancher avant/pendant l'implémentation

- **Double validation des chargebacks** : imposer explicitement 2 admins distincts (section 9) — actuellement non garanti même dans l'intention du mock.
- **`force-close-vault` sans `vaultId`** : le contrat actuel ferme "un coffre" sans le désigner ; à clarifier avec le produit avant d'exposer un vrai bouton par coffre (section 6).
- **Granularité des permissions "Support" (lecture seule)** : la matrice de nav actuelle ne distingue pas lecture/écriture (section 4.3).
- **Motif de rejet KYC** : actuellement aucun champ de raison côté UI/API — à ajouter si le compliance l'exige.
- **Filtrage serveur** de `/users`, `/disputes`, `/support/tickets` : pas nécessaire tant que le volume reste raisonnable (tout est filtré côté client aujourd'hui), mais à revisiter si la base d'utilisateurs grossit.
