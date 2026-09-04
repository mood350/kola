# API Dogaa — référence pour le front (mobile & web)

Backend Spring Boot. Toutes les routes sont préfixées par `/api/v1`.

| | |
|---|---|
| **Base URL (dev)** | `http://localhost:8081` |
| **Swagger** | `http://localhost:8081/swagger-ui.html` |
| **Santé** | `GET /actuator/health` (public) |
| **Devises** | `XOF` (0 décimale), `GHS`, `NGN`, `USD` (2 décimales) |

> **XOF n'a pas de décimales.** Envoyez `50000`, pas `50000.00`. Les montants sont des nombres JSON, jamais des chaînes.

---

## 1. Conventions

### Authentification

Toutes les routes exigent un jeton, **sauf** :

```
POST /api/v1/auth/register/request-otp
POST /api/v1/auth/register/verify-otp
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

Partout ailleurs :

```http
Authorization: Bearer <accessToken>
```

L'`accessToken` vit **15 minutes**. Passé ce délai, appelez `/auth/refresh` avec le `refreshToken` : vous recevez une **nouvelle paire**. L'ancien `refreshToken` est immédiatement invalidé — stockez toujours celui qui vient d'arriver, sinon la session est perdue.

### Enveloppe de réponse

La plupart des routes renvoient :

```json
{
  "success": true,
  "message": "Logged in",
  "data": { },
  "timestamp": "2026-09-04T18:24:46Z"
}
```

⚠️ **Quatre groupes de routes renvoient le DTO brut, sans enveloppe** : `/admin/**`, `/notifications/**`, `/scheduling/**`, et `/admin/scoring/**`. Pour celles-là, lisez la racine de la réponse et non `data`. C'est une incohérence héritée d'une fusion, à uniformiser côté backend — prévoyez un helper qui gère les deux formes.

### Erreurs

Toujours cette forme, quel que soit le code HTTP :

```json
{
  "success": false,
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid phone number or PIN",
  "fieldErrors": { "pin": "PIN must be 4 to 6 digits" },
  "path": "/api/v1/auth/login",
  "timestamp": "2026-09-04T18:24:46Z"
}
```

`fieldErrors` n'est présent que sur `VALIDATION_ERROR`.

| Code | HTTP | Sens |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Payload invalide — détail dans `fieldErrors` |
| `BAD_REQUEST` | 400 | Règle métier violée |
| `UNAUTHORIZED` | 401 | Jeton absent, expiré ou invalide |
| `INVALID_CREDENTIALS` | 401 | Mauvais téléphone **ou** mauvais PIN (volontairement indistinct) |
| `INVALID_REFRESH_TOKEN` | 401 | Refresh expiré, révoqué ou déjà utilisé |
| `ACCOUNT_SUSPENDED` / `ACCOUNT_CLOSED` | 401 | Compte bloqué |
| `FORBIDDEN` | 403 | Authentifié mais pas le bon rôle |
| `KYC_LIMIT_EXCEEDED` | 403 | Plafond du niveau KYC dépassé |
| `NOT_FOUND` | 404 | Ressource inexistante |
| `CONFLICT` | 409 | Doublon, ou état incompatible |
| `ACCOUNT_LOCKED` | 423 | Trop de PIN erronés — 15 min de blocage |
| `TOO_MANY_REQUESTS` | 429 | Cooldown OTP non écoulé |
| `INTERNAL_ERROR` | 500 | Anomalie serveur |

### Téléphones

Envoyez-les comme l'utilisateur les tape : `90123456`, `+228 90 12 34 56`, `0022890123456`. Le backend normalise en E.164 (`+22890123456`) et **renvoie toujours cette forme**. Indicatif par défaut : Togo (228).

### CORS

Origines autorisées : `http://localhost:3000`, `http://127.0.0.1:3000`, `http://10.0.2.2:8081` (hôte vu depuis l'émulateur Android). Pour ajouter la vôtre, demandez au backend de compléter `app.security.cors.allowed-origins`.

---

## 2. Inscription — 3 étapes obligatoires

> ⚠️ **L'envoi de SMS n'est pas branché.** Le code OTP s'affiche **dans les logs du serveur** :
> `[DEV OTP] +228******56 -> code 384912 (valid 5 minutes)`
> Il n'est jamais renvoyé par l'API. En démo, gardez la console du backend ouverte.

### Étape 1 — demander le code

```http
POST /api/v1/auth/register/request-otp
{ "phone": "90123456" }
```

```json
{ "phone": "+228******56", "codeExpiresInSeconds": 300,
  "resendAvailableAt": "2026-09-04T18:25:46Z" }
```

Cooldown de **60 s** entre deux envois (`429` sinon). Un numéro déjà inscrit renvoie `409`.

### Étape 2 — vérifier le code

```http
POST /api/v1/auth/register/verify-otp
{ "phone": "90123456", "code": "384912" }
```

```json
{ "verificationToken": "hK3s...", "expiresInSeconds": 900 }
```

5 codes erronés brûlent le défi : il faut en redemander un. Le token vaut **15 minutes** et ne sert **qu'une fois**.

### Étape 3 — créer le compte

```http
POST /api/v1/auth/register
{
  "verificationToken": "hK3s...",
  "firstName": "Kossi", "lastName": "Adjo",
  "dateOfBirth": "1995-04-12",
  "pin": "8305", "confirmPin": "8305",
  "acceptedPrivacyPolicy": true,
  "email": "kossi@example.com",
  "address": "Rue de la Paix", "city": "Lomé", "country": "TG"
}
```

Champs obligatoires : `verificationToken`, `firstName`, `lastName`, `dateOfBirth`, `pin`, `confirmPin`, `acceptedPrivacyPolicy`.
Optionnels : `email`, `address`, `city`, `country` (ISO 2 lettres majuscules).

**Il n'y a pas de champ `phone`** — c'est volontaire. Le numéro est lu dans le `verificationToken`, ce qui rend impossible la création d'un compte sur un numéro non vérifié.

**Règles du PIN :** 4 à 6 chiffres, ni tous identiques (`0000`), ni consécutifs (`1234`, `4321`). Validez-les côté client pour éviter un aller-retour.

**Âge minimum : 18 ans.**

Réponse `201` — `AuthResponse` (voir §3), et **les comptes courant et épargne XOF sont créés automatiquement**.

---

## 3. Authentification

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/auth/login` | `{ phone, pin }` |
| `POST` | `/auth/refresh` | `{ refreshToken }` |
| `POST` | `/auth/logout` | `{ refreshToken }` |
| `POST` | `/auth/logout-all` | — (jeton requis) |
| `POST` | `/auth/change-pin` | `{ currentPin, newPin, confirmPin }` |
| `POST` | `/auth/email/request-code` | — (jeton requis) |
| `POST` | `/auth/email/verify` | `{ code }` |

**`AuthResponse`** (login, register, refresh) :

```json
{
  "accessToken": "eyJhbG...",
  "refreshToken": "9xK2...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": { "id": "...", "firstName": "Kossi", "phone": "+22890123456",
            "kycTier": "TIER_0", "role": "USER", "status": "ACTIVE", "...": "..." }
}
```

**Blocage :** 5 PIN erronés → `423 ACCOUNT_LOCKED` pendant 15 minutes. Le 5ᵉ essai renvoie directement `423`, pas `401`.

**`change-pin` révoque toutes les sessions** : il faut se reconnecter. Prévoyez la redirection.

**L'email est facultatif** et ne débloque aucun niveau KYC ni plafond — uniquement les reçus et notifications.

---

## 4. Profil

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/users/me` | — |
| `PATCH` | `/users/me` | champs à modifier uniquement |

Champs modifiables : `firstName`, `lastName`, `email`, `dateOfBirth`, `address`, `city`, `country`. Tout champ absent ou `null` est ignoré.

> Renseigner `address` + `city` + `country` fait **automatiquement passer l'utilisateur en TIER_1**. Bon levier d'UX : proposez-le juste après l'inscription.

Changer l'email remet `emailVerified` à `false`.

---

## 5. KYC — 4 niveaux

| Niveau | Condition | Plafond/jour | Crédit |
|---|---|---|---|
| `TIER_0` | Téléphone vérifié (inscription) | 50 000 XOF | non |
| `TIER_1` | + adresse, ville, pays renseignés | 300 000 | non |
| `TIER_2` | + pièce d'identité approuvée | 3 000 000 | **oui** |
| `TIER_3` | + selfie et justificatif de domicile | illimité | oui |

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/kyc/status` | — |
| `POST` | `/kyc/documents` | **multipart** : `type` + `file` |
| `GET` | `/kyc/documents` | — |

**`GET /kyc/status`** renvoie tout ce qu'il faut pour dessiner l'écran de vérification :

```json
{
  "tier": "TIER_1", "nextTier": "TIER_2",
  "phoneVerified": true, "profileComplete": true,
  "identityDocumentApproved": false,
  "requirementsForNextTier": ["Submit an identity document ..."],
  "limits": { "perTransaction": 100000, "daily": 300000, "monthly": 1500000,
              "balanceCap": 1000000, "creditEligible": false },
  "documents": [ ]
}
```

Affichez `requirementsForNextTier` tel quel : c'est la liste exacte de ce qui manque.

**Envoi de document** — `multipart/form-data` :

```
type: NATIONAL_ID | PASSPORT | DRIVING_LICENCE | VOTER_CARD | SELFIE | PROOF_OF_ADDRESS
file: <image/jpeg | image/png | image/webp | application/pdf, 5 Mo max>
```

Statuts : `PENDING` → `APPROVED` | `REJECTED` (avec `rejectionReason` à afficher). Le niveau ne monte qu'après **approbation par un administrateur** — un document en attente ne change rien.

Réenvoyer un document du même type **remplace** celui en attente.

---

## 6. Comptes (wallets)

Chaque utilisateur a **deux comptes XOF** créés à l'inscription : `CURRENT` (transactions) et `SAVINGS` (garantie des prêts).

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/wallets` | — |
| `POST` | `/wallets` | `{ currency }` |
| `GET` | `/wallets/{currency}` | — |
| `POST` | `/wallets/{currency}/deposit` | `{ amount }` |

**`WalletResponse`** :

```json
{ "id": "...", "currency": "XOF", "availableBalance": 120000,
  "lockedBalance": 50000, "totalBalance": 170000,
  "status": "ACTIVE", "createdAt": "..." }
```

`availableBalance` = dépensable · `lockedBalance` = bloqué (coffres, ou garantie d'un prêt en cours). **Affichez toujours les deux** : un utilisateur avec un prêt en cours voit son épargne entièrement en `locked` et doit comprendre pourquoi.

---

## 7. Transactions

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/transactions/quote` | `{ type, currency, amount }` |
| `POST` | `/transactions/transfer` | `{ currency, amount, recipientPhone, description? }` |
| `POST` | `/transactions/merchant-payment` | `{ currency, amount, merchantCode, description? }` |
| `POST` | `/transactions/cash-out` | `{ currency, amount, phoneNumber, provider? }` |
| `POST` | `/transactions/bill-payment` | `{ currency, amount, billerReference, description? }` |
| `GET` | `/transactions` | filtres en query |
| `GET` | `/transactions/{reference}` | — |
| `GET` | `/transactions/stream` | flux SSE |

**Toujours appeler `/quote` avant d'exécuter** et afficher les frais. Ils dépendent du type :

```json
{ "currency": "XOF", "amount": 100000, "fee": 1500, "total": 101500 }
```

Barème : dépôt gratuit · P2P ~1,5 % · retrait ~1 % · marchand et facture selon paramétrage.

**Historique** — `GET /transactions?type=&status=&from=&to=&page=0&size=20`
`from`/`to` en ISO-8601 (`2026-09-01T00:00:00Z`). Réponse paginée Spring (`content`, `totalElements`, `totalPages`, `number`).

**Temps réel** — `GET /transactions/stream` en `text/event-stream`. Les transactions de l'utilisateur arrivent au fil de l'eau. Sur mobile, préférez un rafraîchissement à l'ouverture de l'écran : le SSE tient mal les coupures réseau.

Types : `CASH_IN`, `CASH_OUT`, `P2P_TRANSFER`, `MERCHANT_PAYMENT`, `BILL_PAYMENT`, `VAULT_DEPOSIT`, `VAULT_WITHDRAWAL`, `LOAN_DISBURSEMENT`, `LOAN_REPAYMENT`, `CHARGEBACK`
Statuts : `PENDING`, `COMPLETED`, `FAILED` (avec `failureReason`)

---

## 8. Coffres-forts (vaults)

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/vaults` | — |
| `POST` | `/vaults` | `{ name, currency, targetAmount?, targetDate?, description? }` |
| `GET` | `/vaults/{id}` | — |
| `POST` | `/vaults/{id}/deposit` | `{ amount }` |
| `POST` | `/vaults/{id}/withdraw` | `{ amount }` |
| `POST` | `/vaults/{id}/close` | — |

Déposer déplace l'argent du solde disponible vers le solde bloqué. `progressPercent` et `goalReached` sont calculés côté serveur — utilisez-les directement pour la barre de progression.

---

## 9. Transactions programmées

⚠️ Ces routes renvoient le **DTO brut**, sans enveloppe.

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/scheduling/tasks` | voir ci-dessous |
| `PATCH` | `/scheduling/tasks/{id}/pause` | — |
| `PATCH` | `/scheduling/tasks/{id}/resume` | — |
| `PATCH` | `/scheduling/tasks/{id}/cancel` | — |
| `GET` | `/scheduling/tasks/users/{userId}` | — |

```json
{ "userId": "...", "type": "VAULT_DEPOSIT", "frequency": "MONTHLY",
  "amount": 10000, "currency": "XOF", "beneficiaryReference": "vault-id-ou-numero",
  "firstRunAt": "2026-10-05T00:00:00Z", "endDate": null, "maxOccurrences": 12 }
```

Types : `P2P_TRANSFER`, `MERCHANT_PAYMENT`, `VAULT_DEPOSIT`, `BILL_PAYMENT`
Fréquences : `ONCE`, `DAILY`, `WEEKLY`, `MONTHLY`
Statuts : `ACTIVE`, `PAUSED`, `FAILED`, `COMPLETED`, `CANCELLED`

Le job s'exécute **à minuit**. En cas de solde insuffisant, la tâche passe `FAILED` avec `lastFailureReason`.

> ⚠️ `userId` est dans le corps et le chemin. À sécuriser côté backend (un utilisateur peut lire les tâches d'un autre) — ne construisez pas de fonctionnalité qui en dépende.

---

## 9 bis. Notifications

⚠️ DTO brut, sans enveloppe.

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/notifications` | `{ userId, channel, title, body }` |
| `GET` | `/notifications/users/{userId}` | — |

`channel` : `EMAIL`, `SMS`, `PUSH`. Les trois canaux **écrivent dans les logs** au lieu d'envoyer quoi que ce soit — aucune passerelle n'est branchée.

> ⚠️ Comme pour le scheduling, ces routes acceptent n'importe quel `userId` sans vérifier qu'il correspond à l'appelant. À sécuriser côté backend.

---

## 10. Score de crédit

| Méthode | Route |
|---|---|
| `GET` | `/scoring/me` |
| `POST` | `/scoring/me/recalculate` |

```json
{ "userId": "...", "scoreValue": 72, "rawScoreValue": 78,
  "kycTier": "TIER_2", "windowDays": 30,
  "breakdown": { "savingsDiscipline": 22, "financialStability": 19,
                 "inflowRegularity": 14, "usageIntensity": 11,
                 "creditHistory": 5, "total": 78 },
  "calculatedAt": "..." }
```

`scoreValue` = score publié (lissé) — **c'est celui à afficher**. `rawScoreValue` = valeur brute du jour.

Les 5 axes et leur maximum : épargne 30, stabilité 25, régularité des entrées 20, usage 15, historique de crédit 10.

Le score est recalculé **chaque nuit à 00h30**. `POST /scoring/me/recalculate` force le recalcul immédiat — pratique en démo.

---

## 11. Crédit

Le prêt est **garanti par le compte épargne**. Pas d'épargne, pas de prêt.

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/credit/eligibility?currency=XOF` | — |
| `POST` | `/credit/loans` | `{ currency, amount }` |
| `GET` | `/credit/loans` | — |
| `POST` | `/credit/loans/{loanId}/repay` | `{ amount }` ou corps vide |

**`GET /credit/eligibility`** — un seul appel pour tout l'écran :

```json
{
  "eligible": false, "score": 45, "minimumScore": 60,
  "kycTier": "TIER_1", "breakdown": { },
  "currency": "XOF", "savingsBalance": 500000,
  "leverageRatio": 0, "maxLoanAmount": 0,
  "monthlyRatePercent": 0, "totalRepayable": 0,
  "termDays": 30, "loansRepaid": 0,
  "blockers": [
    "Credit requires KYC level TIER_2: submit an identity document",
    "Your score is 45/100, 60 is required"
  ]
}
```

**Affichez `blockers` tel quel** : c'est la liste exacte de ce qui manque, rédigée pour l'utilisateur. Quand `eligible` est `true`, la liste est vide et `maxLoanAmount` porte le plafond.

### Conditions d'éligibilité

1. KYC **TIER_2**
2. Compte `ACTIVE`
3. Épargne ≥ **5 000 XOF**
4. Score ≥ **60/100**
5. **Aucun prêt en cours**
6. Ancienneté du compte (0 jour en config démo)

### Montant et taux — échelle progressive

| Prêts remboursés | Score requis | Montant | Taux/mois |
|---|---|---|---|
| 0 | 60 | **1,0 ×** épargne | 8 % |
| 1 | 60 | 1,2 × | 7 % |
| 2 | 75 | 1,4 × | 6,5 % |
| 3+ | 85 | **1,6 ×** | 6 % |

Un premier prêt est plafonné au montant de l'épargne. Le levier se gagne en remboursant.

### Ce qui se passe au décaissement

Le montant arrive sur le **compte courant**, et **tout le compte épargne passe en `lockedBalance`** :

- ❌ retrait de l'épargne impossible
- ✅ dépôt sur l'épargne toujours possible

Remboursement intégral → épargne débloquée. `POST /loans/{id}/repay` avec un corps vide rembourse tout le solde restant.

Retard : 3 jours de grâce, puis 0,5 %/jour plafonné à 15 %. À 30 jours, saisie de la garantie.

Statuts : `ACTIVE`, `OVERDUE`, `REPAID`, `DEFAULTED`.

---

## 12. Routes administrateur

> **Base URL du back-office** : pointez `VITE_API_BASE_URL` sur `http://localhost:8081/api/v1/admin`.
> Les chemins du contrat (`/auth/login`, `/audit/log`…) tombent alors juste.
> Ces routes renvoient le **DTO brut**, sans enveloppe — sauf `/admin/kyc/**`, qui emballe encore.

### Authentification back-office

Comptes séparés des utilisateurs de l'application : **email + mot de passe**, session de **8 h**
(pas de refresh token — le front n'en gère pas).

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| `POST` | `/admin/auth/login` | `{ email, password }` | `{ token, user }` |
| `GET` | `/admin/auth/me` | — | `AdminIdentity` |
| `POST` | `/admin/auth/logout` | — | `204` |
| `POST` | `/admin/auth/password-reset-request` | `{ email }` | `204` toujours |
| `POST` | `/admin/auth/change-password` | `{ currentPassword, newPassword }` | `204` |

`role` circule avec son **libellé exact** : `"Super-admin"`, `"Agent conformité"`, `"Analyste crédit"`, `"Support"`.

**Comptes de démarrage** (créés au premier lancement si la table est vide) :
`sena.ametepe@dogaa.io`, `koffi.messan@dogaa.io`, `aya.djobo@dogaa.io`, `prisca.lawson@dogaa.io` —
mot de passe commun `DogaaAdmin2026!`, à changer et à désactiver (`app.admin.seed.enabled=false`) avant la prod.

### Matrice de permissions — appliquée côté serveur

| Rôle | Modules |
|---|---|
| Super-admin | tous |
| Agent conformité | dashboard, users, disputes, audit, support |
| Analyste crédit | dashboard, credit, finance |
| Support | dashboard, users, support |

`profile` est ouvert à tous. Un module interdit renvoie **403**, jamais 401 — le 401 est réservé au
jeton absent ou expiré, pour que votre redirection vers `/login` ne se déclenche pas à tort.

### Rôles et audit

| Méthode | Route |
|---|---|
| `GET` | `/admin/roles/admins` |
| `PATCH` | `/admin/roles/admins/{id}/permissions` — `{ role, scope }` |
| `GET` | `/admin/audit/log?admin=&module=&from=&to=` |
| `POST` | `/admin/audit/log/export` → `{ url: null }` |
| `GET` | `/admin/audit/reports` |
| `POST` | `/admin/audit/reports/{id}/export` → `{ url: null }` |

Le journal renvoie `{ id, admin, action, diff, time }`, plus récent d'abord. `diff` est déjà formaté
`"avant → après"` côté serveur. Les exports renvoient `url: null` : la génération de fichier n'est
pas implémentée.

### Autres routes admin


Nécessitent `role = ADMIN`. ⚠️ Renvoient le **DTO brut** sauf les routes KYC.

| Méthode | Route |
|---|---|
| `GET` | `/admin/dashboard` |
| `GET` | `/admin/scheduling/tasks` |
| `GET` | `/admin/notifications` |
| `GET` | `/admin/scoring/users/{userId}` |
| `GET` | `/admin/kyc/documents/pending?page=0&size=20` |
| `GET` | `/admin/kyc/documents/{id}/file` |
| `POST` | `/admin/kyc/documents/{id}/review` — `{ approved, rejectionReason? }` |
| `POST` | `/admin/kyc/documents/{id}/revoke?reason=...` |

Un rejet **doit** porter un `rejectionReason`, sinon `400`.

---

## 13. Parcours type (mobile)

```
1.  POST /auth/register/request-otp        { phone }
2.  ── lire le code dans les logs serveur ──
3.  POST /auth/register/verify-otp         { phone, code }      → verificationToken
4.  POST /auth/register                    { verificationToken, ... }  → tokens
5.  GET  /wallets                          → courant + épargne (déjà créés)
6.  PATCH /users/me                        { address, city, country }  → TIER_1
7.  POST /kyc/documents                    multipart NATIONAL_ID
    ── attente de validation admin ──                            → TIER_2
8.  POST /wallets/XOF/deposit              { amount: 500000 }
9.  POST /vaults + /vaults/{id}/deposit    (alimente l'épargne, monte le score)
10. GET  /credit/eligibility               → maxLoanAmount
11. POST /credit/loans                     { currency, amount }
12. POST /credit/loans/{id}/repay          { }  → épargne débloquée
```

---

## 14. À savoir avant de coder

- **L'OTP n'est pas envoyé par SMS** — code visible uniquement dans les logs du serveur (§2).
- **L'enveloppe de réponse n'est pas uniforme** — `/admin`, `/notifications`, `/scheduling` renvoient le DTO brut (§1).
- **Le port de dev est 8081**, pas 8080.
- **Rotation du refresh token** : conservez systématiquement le dernier reçu.
- **`ddl-auto=update`** : le schéma peut bouger entre deux versions du backend.
- Aucun endpoint de **suppression de compte** ni de **réinitialisation du PIN oublié** n'existe encore.

Questions ou champ manquant → ouvrez une issue sur le dépôt, ou consultez Swagger qui reflète toujours le code déployé.
