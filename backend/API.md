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

### Compte épargne

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/wallets/savings/deposit` | `{ currency, amount }` |
| `POST` | `/wallets/savings/withdraw` | `{ currency, amount }` |

Les deux renvoient **la liste des deux comptes** après le mouvement, pas un seul côté.

Le virement courant → épargne est **gratuit** et **ne consomme aucun plafond d'envoi** : déplacer
son propre argent n'est pas une dépense. C'est ce compte qui garantit les prêts, donc l'alimenter
est le préalable à toute demande de crédit (§11).

> Tant qu'un prêt est en cours, l'épargne est bloquée en garantie et le retrait répond
> `INSUFFICIENT_FUNDS`. Les versements, eux, continuent de passer.

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

### Vérifier le destinataire avant d'envoyer

`GET /transactions/recipient?phone=90333444`

**À appeler dès que l'utilisateur a saisi le numéro, avant l'écran de confirmation.** Un virement
P2P ne se défait pas sans ouvrir un litige : c'est la dernière occasion d'attraper un chiffre de
travers.

```json
{ "success": true, "data": {
    "phone": "+22890333444",
    "phoneMasked": "+228 90 ** ** 44",
    "registered": true,
    "name": "Ama Kossi",
    "self": false } }
```

| Champ | À quoi ça sert |
|---|---|
| `phone` | le numéro **normalisé** — renvoyez celui-ci dans `/transfer`, pas ce que l'utilisateur a tapé |
| `name` | à afficher en gros sur l'écran de confirmation. `null` si le compte n'affiche pas de nom |
| `registered` | `false` = pas de compte Dogaa. Le virement marche quand même, mais il part par Mobile Money et **il n'y a aucun nom à vérifier** — prévenez-en l'utilisateur |
| `self` | `true` = c'est son propre numéro. Le virement à soi-même est refusé ; dites-le ici plutôt que de laisser échouer après confirmation |

> Un numéro inconnu répond **200 avec `registered: false`**, pas 404 : c'est une réponse, pas un
> échec.

> **Limité en débit** (60 vérifications par heure et par compte, `429` au-delà). Un point d'entrée
> numéro → nom parcouru en boucle sert à moissonner les noms d'un plan de numérotation. N'appelez
> pas la route à chaque frappe : attendez que le numéro soit complet.

Le nom est aussi enregistré sur la transaction (`counterpartyName` dans `/transactions`), figé au
moment du virement — l'historique continue de nommer qui a été payé même si la personne renomme
son compte ensuite.

### Clé d'idempotence — à envoyer sur chaque paiement

Ajoutez un en-tête `Idempotency-Key` sur `POST /transactions/{transfer,merchant-payment,cash-out,bill-payment}`.

```
Idempotency-Key: 7f3a9c02-1b4e-4d55-9a10-2c8e6f0b1d33
```

Un téléphone sur réseau faible ne distingue pas une réponse perdue d'un paiement refusé : il
réessaie. **Sans clé, ce second appel est un second paiement.** Avec la même clé, le premier appel
exécute et les suivants renvoient **la même transaction** avec un succès — pas une erreur, car un
client qui reçoit un conflit ne sait pas si l'argent est parti.

Règle : un UUID généré **une fois par intention de paiement**, conservé à travers les tentatives.
Un nouveau paiement = une nouvelle clé. L'en-tête est optionnel pour ne pas casser les clients
existants ; considérez-le comme obligatoire.

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

### Modifier un coffre

`PATCH /vaults/{id}` — un champ absent est laissé tel quel.

```json
{ "name": "Apport logement", "targetAmount": 900000,
  "targetDate": "2027-06-30", "description": "acompte",
  "clearTargetAmount": false, "clearTargetDate": false }
```

> **Il n'y a pas de champ `balance`, et il n'y en aura pas.** L'argent entre et sort d'un coffre
> uniquement par un dépôt, un retrait ou une planification — chacun laissant une transaction.
> La devise n'est pas modifiable non plus : elle est fixée par le portefeuille sur lequel
> l'argent est bloqué.

Refusé : un objectif **inférieur** à ce qui est déjà épargné (400), une échéance dans le passé
(400), un coffre clôturé (400).

---

## 9. Transactions programmées

Enveloppe `ApiResponse<T>`. Le propriétaire vient du token : **il n'y a pas de `userId`**.

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/scheduling/tasks` | voir ci-dessous |
| `PATCH` | `/scheduling/tasks/{id}` | **modifier** — voir ci-dessous |
| `PATCH` | `/scheduling/tasks/{id}/pause` | — |
| `PATCH` | `/scheduling/tasks/{id}/resume` | — |
| `PATCH` | `/scheduling/tasks/{id}/cancel` | — |
| `GET` | `/scheduling/tasks/me` | — |
| `GET` | `/scheduling/tasks/billers` | — → services facturables |

```json
{ "type": "P2P_TRANSFER", "frequency": "MONTHLY",
  "amount": 50000, "currency": "XOF", "beneficiaryReference": "+22890111222",
  "firstRunAt": "2026-10-05T00:00:00Z", "dayOfMonth": 5,
  "fundingVaultId": "uuid-du-coffre",
  "endDate": null, "maxOccurrences": null }
```

Types : `P2P_TRANSFER`, `MERCHANT_PAYMENT`, `VAULT_DEPOSIT`, `BILL_PAYMENT`
Fréquences : `ONCE`, `DAILY`, `WEEKLY`, `MONTHLY`
Statuts : `ACTIVE`, `PAUSED`, `FAILED`, `COMPLETED`, `CANCELLED`

### Modifier une planification

`PATCH /scheduling/tasks/{id}` — **un champ absent est laissé tel quel.**

```json
{ "amount": 65000,
  "beneficiaryReference": "+22890999888",
  "biller": "canal_plus",
  "fundingVaultId": "uuid-d-un-autre-coffre",
  "frequency": "MONTHLY",
  "dayOfMonth": 28,
  "nextRunAt": "2026-11-28T00:00:00Z",
  "endDate": "2027-12-31T00:00:00Z",
  "maxOccurrences": 12,
  "clearEndDate": false,
  "clearMaxOccurrences": false }
```

> Comme `null` signifie « inchangé », **supprimer** une date de fin ou un nombre maximum
> d'exécutions se fait avec `clearEndDate: true` / `clearMaxOccurrences: true`. Envoyer `null` ne
> les efface pas.

Ce qui n'est **pas** modifiable, et pourquoi :

| Champ | Raison |
|---|---|
| `type`, `currency` | le bénéficiaire, le facturier et le coffre en dépendent — annulez et recréez |
| `status` | passe par `/pause`, `/resume`, `/cancel`, pour que ça reste des événements distincts |

Une planification `CANCELLED` ou `COMPLETED` répond **409** : c'est de l'historique.
Une planification `PAUSED` reste modifiable — on ajuste avant de reprendre.

Changer de coffre refait les mêmes contrôles qu'à la création (vous appartient, actif, même
devise). Changer de facturier revalide le numéro d'abonné : un numéro de carte Canal+ ne veut
rien dire une fois le facturier passé à Togocom.

### Le coffre de financement est obligatoire

`fundingVaultId` est **requis** pour tous les types sauf `VAULT_DEPOSIT`. Une planification ne
puise jamais dans le compte courant : l'argent doit avoir été mis de côté exprès. Le coffre est
vérifié à la création — il doit vous appartenir, être actif, et être dans la **même devise** que la
planification.

Au moment de l'exécution, le coffre est débité du **montant + la commission**. Prévoyez donc les
frais dans le coffre : un coffre à 50 000 F ne couvre pas un virement de 50 000 F.

### `dayOfMonth`

Pour une fréquence `MONTHLY`, le jour du mois (1-31). Omis, il est déduit de `firstRunAt`.
Un 29, 30 ou 31 tombe sur le dernier jour d'un mois plus court **puis revient** au jour choisi :
31 janvier → 28 février → 31 mars.

### Factures à montant fixe

| Méthode | Route |
|---|---|
| `GET` | `/scheduling/tasks/billers` |

```json
{ "success": true, "data": [
  { "code": "canal_plus", "displayName": "CANAL+",
    "identifierLabel": "Numéro de carte", "identifierKind": "DIGITS",
    "minLength": 14, "maxLength": 14, "fixedAmount": true },
  { "code": "togocom_fibre", "displayName": "Togocom — fibre optique",
    "identifierLabel": "Numéro de contrat", "identifierKind": "ALPHANUMERIC",
    "minLength": 4, "maxLength": 24, "fixedAmount": true } ] }
```

**Affichez `identifierLabel` au-dessus du champ de saisie**, ne mettez pas « numéro » en dur :
la question n'est pas la même selon le service. Canal+ demande le numéro de carte imprimé sous le
décodeur, Cash Power le numéro du compteur, CEET une référence client. `identifierKind` indique le
clavier à ouvrir (`DIGITS` → pavé numérique).

Pour programmer une facture : `type: "BILL_PAYMENT"`, `biller: "canal_plus"`, et le numéro
d'abonné dans `beneficiaryReference`. Les espaces et tirets copiés depuis une facture papier sont
retirés automatiquement.

> Seules les factures à **montant invariable** (abonnements) sont programmables. CEET, TdE et
> Cash Power se facturent à la consommation : les programmer pour une somme fixe est refusé (400),
> car cela paierait trop ou trop peu tous les mois.

Le job s'exécute **à minuit**. En cas de coffre insuffisant, la tâche passe `FAILED` avec un
`lastFailureReason` qui nomme le coffre.

> La tâche d'un autre utilisateur répond **404**, pas 403 : un 403 confirmerait que l'identifiant existe.

---

## 9 bis. Notifications

Enveloppe `ApiResponse<T>`.

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/notifications/me` | — |
| `POST` | `/notifications` | `{ userId, channel, title, body }` — **réservé aux administrateurs** |

`channel` : `EMAIL`, `SMS`, `PUSH`. Les trois canaux **écrivent dans les logs** au lieu d'envoyer quoi que ce soit — aucune passerelle n'est branchée.

> L'envoi est `ROLE_ADMIN` : un utilisateur qui pouvait envoyer une notification à n'importe qui disposait d'un outil de phishing, pas d'une fonctionnalité.

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

⚠️ **Ce tableau est la valeur de départ, pas une constante.** Le back-office édite l'échelle
(`PUT /admin/credit/tier-config`, §12) et la version enregistrée prend effet immédiatement pour
tous les prêts accordés ensuite. Lisez `GET /credit/eligibility` plutôt que de recopier ces
chiffres dans le front.

### Ce qui se passe au décaissement

Le montant arrive sur le **compte courant**, et **tout le compte épargne passe en `lockedBalance`** :

- ❌ retrait de l'épargne impossible
- ✅ dépôt sur l'épargne toujours possible

Remboursement intégral → épargne débloquée. `POST /loans/{id}/repay` avec un corps vide rembourse tout le solde restant.

Retard : 3 jours de grâce, puis 0,5 %/jour plafonné à 15 %. À 30 jours, saisie de la garantie.

Statuts : `ACTIVE`, `OVERDUE`, `REPAID`, `DEFAULTED`.

---

## 11 bis. Assistant

Un chat qui répond aux questions de l'utilisateur sur l'application **et sur son propre compte**.
Enveloppe `ApiResponse<T>`. Toutes les routes portent sur l'appelant : aucun `userId` nulle part.

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/assistant/messages` | `{ conversationId?, message }` |
| `GET` | `/assistant/conversations` | — |
| `GET` | `/assistant/conversations/{id}` | — |
| `DELETE` | `/assistant/conversations/{id}` | — |

`conversationId` absent ou `null` ouvre un nouveau fil ; sinon le fil est poursuivi. Le titre du fil
est tiré de la première question.

Réponse de `POST /assistant/messages` :

```json
{ "success": true, "data": {
    "conversationId": "…", "title": "Combien je peux emprunter ?",
    "answer": { "id": "…", "role": "ASSISTANT", "content": "…", "createdAt": "…" },
    "remainingToday": 39 } }
```

**Ce que l'assistant sait.** Les règles du produit (frais, niveaux KYC et plafonds, barème de prêt,
axes du score, litiges) sont construites à partir de la configuration réelle du serveur, donc elles
suivent automatiquement un changement de tarif. S'y ajoute la situation de l'appelant : niveau KYC
et ce qui manque pour monter, soldes disponibles et bloqués des deux comptes, coffres, score et son
détail, éligibilité au crédit avec les blocages nommés, prêt en cours, opérations programmées et
dernières transactions.

**Ce que l'assistant ne fait pas.** Il n'exécute aucune opération — pas de virement, pas de
déblocage, pas de changement de niveau. Il explique et renvoie vers le bon écran. Il ne voit aucun
autre compte que celui de l'appelant, et ne demande jamais un code PIN ni un OTP.

**Codes à gérer côté client :**

| Code | Quand |
|---|---|
| `429` | quota quotidien atteint — `remainingToday` permet de prévenir avant |
| `503` | assistant non configuré ou fournisseur injoignable — masquez l'entrée du menu |
| `404` | `conversationId` inconnu, ou appartenant à quelqu'un d'autre |

---

## 11 ter. QR codes

Encaisser sans dicter son numéro, payer sans le taper. Enveloppe `ApiResponse<T>`.
Le bénéficiaire d'un code est toujours son créateur : **aucune route n'accepte de bénéficiaire**.

### Ses propres codes

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/qr/me` | — (crée le code permanent à la première demande) |
| `POST` | `/qr/me/rotate` | — (révoque l'ancien) |
| `POST` | `/qr/me/requests` | `{ currency, amount, label?, expiresInMinutes? }` |
| `GET` | `/qr/me/requests` | — |
| `DELETE` | `/qr/{code}` | — (annuler) |
| `GET` | `/qr/{code}/image?size=512` | — → **`image/png`**, propriétaire uniquement |

Deux types de codes :

- **`STATIC`** — la carte de visite. Pas de montant, n'expire pas, reste payable après usage.
  C'est le payeur qui saisit le montant.
- **`PAYMENT_REQUEST`** — une demande pour un montant précis. Expire (24 h par défaut, 7 jours
  maximum) et **n'est payable qu'une fois** : un reçu photographié ne doit pas être payé deux fois.

La réponse contient `payload` : c'est exactement ce que l'image encode
(`https://dogaa.app/p/{code}`). **Dessinez le QR côté mobile à partir de ce champ** ; l'endpoint
PNG ne sert qu'au partage ou à l'impression.

### Payer un code scanné

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/qr/{code}` | — → écran de confirmation |
| `POST` | `/qr/{code}/pay` | `{ currency?, amount?, description? }` |

`GET /qr/{code}` renvoie :

```json
{ "success": true, "data": {
    "code": "…", "type": "PAYMENT_REQUEST",
    "payable": true, "reason": null,
    "recipientName": "Ama Kossi", "recipientPhoneMasked": "+228 90 ** ** 56",
    "amountFixed": true, "amount": 2500, "currency": "XOF",
    "label": "Table 4", "expiresAt": "…" } }
```

> Un code expiré, annulé ou déjà payé répond **200 avec `payable: false`** et un `reason` lisible.
> Affichez ce message : l'utilisateur est devant un commerçant et doit savoir lequel des trois cas
> s'applique. Seul un code **inconnu** renvoie 404.

Sur `POST /qr/{code}/pay` :

- code `STATIC` → `currency` et `amount` sont **obligatoires** ;
- code `PAYMENT_REQUEST` → laissez-les vides. Si vous les envoyez et qu'ils diffèrent du code, la
  requête est **refusée (400)**, jamais silencieusement corrigée.

Le paiement emprunte le chemin de transfert normal : mêmes frais (1,5 %), mêmes plafonds KYC, même
écriture au registre. La réponse porte `transactionReference` pour retrouver l'opération dans
`/transactions`.

**Le QR ne contient pas le numéro de téléphone**, seulement une référence aléatoire révocable. Un
code se photographie et se transfère : y mettre le numéro reviendrait à le donner définitivement.

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

### Utilisateurs et file KYC

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/admin/users` | `ClientUser[]` — table complète, filtrage côté client |
| `GET` | `/admin/users/{id}` | `ClientUser` |
| `POST` | `/admin/users/{id}/unblock` | `ClientUser` |
| `POST` | `/admin/users/{id}/force-close-vault` | `ClientUser` |
| `GET` | `/admin/users/kyc-queue` | `KycSubmission[]` |
| `POST` | `/admin/users/kyc-queue/{id}/approve` | `204` |
| `POST` | `/admin/users/kyc-queue/{id}/reject` | `204` |

Tout est **pré-formaté** : `age` en `"14 mois"`, `loan` en `"100 000 XOF"` ou `"Aucun"`,
`state` en `"Actif"` / `"Gelé"`. Le front n'a aucune logique de formatage.

⚠️ **`id` est un UUID (chaîne), pas un `number`** comme le typedef l'annonce. Nos utilisateurs
n'ont jamais eu d'identifiant numérique. Ça fonctionne tel quel pour les clés React et les URL ;
seule une opération arithmétique sur l'id casserait. À corriger dans `models/*.js`.

Deux écarts assumés : `force-close-vault` ferme **le plus ancien coffre ouvert** faute de
`vaultId` dans le contrat, et `state` ne renvoie toujours que `"Actif"` / `"Gelé"` — le module
litiges existe désormais (voir plus bas) mais n'est pas encore branché sur l'état du compte, donc
`"Litige"` n'apparaît jamais.

`toTier` de la file KYC est **calculé par les règles de niveau**, pas supposé être « le suivant » :
un selfie seul renvoie `fromTier == toTier`, ce qui évite d'annoncer une promotion qui n'aura pas lieu.

### Dashboard

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/admin/dashboard/metrics` | `Metric[]` — 5 tuiles, dans l'ordre |
| `GET` | `/admin/dashboard/transaction-volume?period=14d\|30d` | `ChartPoint[]` |
| `GET` | `/admin/dashboard/alerts` | `Alert[]` |
| `GET` | `/admin/dashboard/loan-book-summary` | `LoanBookSummary` |

Ordre imposé des tuiles : volume 24 h, solde global, croissance utilisateurs, encours de prêts,
taux de défaut.

Tout est calculé sur les tables réelles. **Une base vide renvoie des zéros, jamais des valeurs
inventées** — et `alerts` renvoie une liste vide quand rien ne va mal, ce qui est une réponse
valide et non une erreur. Le « solde global » ne concerne que le XOF : additionner des devises
différentes produirait un nombre sans signification.

### Litiges et chargebacks

Côté client, contester une de ses transactions (enveloppe `ApiResponse` habituelle) :

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/api/v1/disputes` | `{ transactionReference, tag, title }` |

`tag` : `fraud`, `double_debit`, `p2p`.

Côté back-office (DTO brut) :
### Crédit — portefeuille, barème et défauts

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| `GET` | `/admin/credit/stats` | — | `{ outstandingTotal, defaultRate, lateLoans }` |
| `GET` | `/admin/credit/tier-config` | — | `TierConfig[]` — l'échelle, du bas vers le haut |
| `PUT` | `/admin/credit/tier-config` | `{ tiers: TierConfig[] }` | l'échelle enregistrée |
| `GET` | `/admin/credit/defaults` | — | `LoanDefault[]` — prêts en retard ou en défaut |
| `POST` | `/admin/credit/defaults/{loanId}/remind` | — | `204` |

```jsonc
// TierConfig — montants et taux voyagent en chaînes d'affichage,
// l'écran les édite en texte libre et le serveur les reparse.
{ "name": "TIER 1", "minScore": 60, "maxAmount": "150 000 XOF", "monthlyRate": "8 %/mois" }

// LoanDefault
{ "id": "uuid", "borrowerName": "Koffi M.", "amount": "120 000 XOF", "daysLate": 17 }
```

**`PUT /tier-config` est réservé au Super-admin** (403 sinon) et l'échelle doit garder son nombre
de paliers — en envoyer plus ou moins renvoie 400. Un montant ou un taux illisible renvoie 400 avec
la valeur fautive citée, plutôt qu'un zéro enregistré en silence.

**L'édition n'écrase jamais : elle empile une version.** `CreditLadderService` charge la dernière
version au démarrage et remplace le barème en vigueur à chaque sauvegarde, si bien que les prêts
déjà accordés restent lisibles avec les conditions de leur époque. La version 1 est la première
sauvegarde back-office, pas le barème de configuration — celui-ci reste dans
`application.properties` et sert tant que personne n'a rien enregistré.

`remind` envoie un SMS au retardataire et l'inscrit au journal d'audit : une relance est un contact
client. Un prêt déjà soldé renvoie 400.

### Litiges & chargebacks
### Litiges et chargebacks

Côté client, contester une de ses transactions (enveloppe `ApiResponse` habituelle) :

| Méthode | Route | Corps |
|---|---|---|
| `POST` | `/api/v1/disputes` | `{ transactionReference, tag, title }` |

`tag` : `fraud`, `double_debit`, `p2p`.

Côté back-office (DTO brut) :

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/admin/disputes` | `Dispute[]` |
| `GET` | `/admin/disputes/{ref}` | `DisputeDetail` |
| `POST` | `/admin/disputes/{ref}/chargeback` | `Dispute` — passe en `chargeback_pending` |
| `POST` | `/admin/disputes/{ref}/reject` | `Dispute` — classé sans suite |
| `POST` | `/admin/disputes/{ref}/validate` | `DisputeDetail` |

**La double validation est réelle.** `validate` est refusé (`409`) si l'administrateur a déjà validé
ce litige — un index unique `(dispute_id, admin_id)` l'empêche même en cas de requêtes simultanées.
Il faut donc bien **deux administrateurs distincts**, et `lastValidationNote` les nomme tous les deux.

Rien ne bouge tant que le quota n'est pas atteint. La dernière validation exécute le chargeback :
le plaignant est **remboursé intégralement**, la récupération auprès du bénéficiaire est limitée à ce
qu'il détient encore, et le manque éventuel apparaît dans `lastValidationNote`.

Nombre de validations réglable par `app.disputes.validations-required` (défaut 2).
| `POST` | `/admin/disputes/{ref}/chargeback` | `Dispute` — ouvre la procédure |
| `POST` | `/admin/disputes/{ref}/reject` | `Dispute` — classe sans suite |
| `POST` | `/admin/disputes/{ref}/validate` | `DisputeDetail` — signe une validation |

```jsonc
// Dispute
{ "ref": "TX-8821", "tag": "fraud", "tagLabel": "Fraude", "amount": "450 000 XOF",
  "title": "Débit contesté vers un marchand inconnu",
  "meta": "Ouvert il y a 2 h · TIER_2 · Lomé", "status": "chargeback_pending" }

// DisputeDetail
{ "ref": "TX-8821", "debitedAccount": "…", "creditedAccount": "…", "amount": "450 000 XOF",
  "validationsRequired": 2, "validationsDone": 1,
  "lastValidationNote": "1re validation : Sena A. — en attente d'un 2e admin conformité" }
```

`tag` vaut `fraud`, `double_debit` ou `p2p` ; `status` vaut `open`, `chargeback_pending`,
`resolved` ou `rejected`. Ce sont les valeurs de fil, en minuscules — la console s'en sert pour
choisir ses styles.

**La double validation est réelle.** `validate` est refusé (`409`) si l'administrateur a déjà validé
ce litige — un index unique `(dispute_id, admin_id)` l'empêche même en cas de requêtes simultanées.
Il faut donc bien **deux administrateurs distincts**, et `lastValidationNote` les nomme tous les deux.

Rien ne bouge tant que le quota n'est pas atteint. La dernière validation exécute le chargeback :
le plaignant est **remboursé intégralement**, la récupération auprès du bénéficiaire est limitée à ce
qu'il détient encore, et le manque éventuel apparaît dans `lastValidationNote`.

Nombre de validations réglable par `app.disputes.validations-required` (défaut 2).

### Suivi financier

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/admin/finance/liquidity` | `LiquidityBucket[]` — les parts totalisent 100 |
| `GET` | `/admin/finance/revenue` | `{ lines: RevenueLine[], total }` |
| `GET` | `/admin/finance/operator-reconciliation` | `OperatorStatus[]` |

```jsonc
{ "label": "Portefeuilles clients", "value": "48 200 000 XOF", "note": "…", "pct": 62 }
{ "label": "Commissions P2P", "value": "820 000 XOF", "pct": 41 }
{ "name": "Moov Money", "status": "reconciled" }   // ou "discrepancy"
```

Lecture seule, tout agrégé sur les tables réelles. Comme le dashboard, une base vide renvoie des
zéros.

### Configuration — frais & marchands

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| `GET` | `/admin/config/fees` | — | `FeeConfig[]` — une ligne par palier KYC |
| `PUT` | `/admin/config/fees` | `{ fees: FeeConfig[] }` | la grille enregistrée |
| `GET` | `/admin/config/merchants` | — | `Merchant[]` |
| `PATCH` | `/admin/config/merchants/{id}/status` | `{ status }` | `Merchant` |

```jsonc
{ "tier": "TIER_2", "p2p": "1,20 %", "merchant": "0,80 %", "cashout": "1,00 %" }
{ "id": "uuid", "name": "Alimentation Adjo", "category": "Commerce", "status": "active" }
```

**`PUT /config/fees` est réservé au Super-admin** (403 sinon) et exige une ligne par palier KYC —
ni plus ni moins, sinon 400. Un taux illisible ou hors bornes renvoie 400.

Une grille enregistrée ici **prime sur le taux de base configuré** : elle est déjà exprimée par
palier, donc la remise de palier ne s'applique pas une seconde fois par-dessus. Tant que rien n'est
enregistré, le calcul retombe sur `app.fees.*` × multiplicateur de palier.

Statuts marchands : `pending`, `active`, `suspended`. Les transitions sont contraintes —
`pending → active|suspended`, `active → suspended`, `suspended → active` ; tout le reste renvoie
**409**, comme le fait de réappliquer le statut courant.

### Support client

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/admin/support/tickets` | `SupportTicket[]` |
| `GET` | `/admin/support/manual-actions` | `ManualAction[]` |
| `POST` | `/admin/support/tickets/{ref}/take-charge` | `SupportTicket` |
| `POST` | `/admin/support/tickets/{ref}/resolve` | `SupportTicket` |

```jsonc
{ "ref": "#8821", "subject": "Retrait bloqué", "userName": "Aya D.", "status": "open" }
{ "id": "uuid", "action": "Déblocage de compte", "by": "Prisca L.", "time": "Il y a 2 h" }
```

`status` vaut `open`, `in_progress` ou `resolved`. Prendre en charge un ticket déjà pris, ou
résoudre un ticket déjà résolu, renvoie **409** : deux agents qui cliquent en même temps ne doivent
pas se voler le ticket en silence.

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
- **L'enveloppe de réponse n'est pas uniforme** — seules les routes `/admin` renvoient le DTO brut, volontairement : le back-office mappe le JSON tel quel. Les routes client sont toutes sous `ApiResponse<T>` (§1).
- **Le port de dev est 8081**, pas 8080.
- **Rotation du refresh token** : conservez systématiquement le dernier reçu.
- **`ddl-auto=update`** : le schéma peut bouger entre deux versions du backend.
- Aucun endpoint de **suppression de compte** ni de **réinitialisation du PIN oublié** n'existe encore.
- **L'assistant (§11 bis) répond 503 tant que `app.assistant.api-key` n'est pas renseignée** côté serveur. Le reste de l'application fonctionne normalement.
- **Les barèmes sont modifiables à chaud** : l'échelle de prêt (§12) et la grille de frais (§12)
  vivent en base et priment sur `application.properties`. Ne figez ni les taux ni les plafonds dans le front.
- **Les exports d'audit renvoient `url: null`** — la génération de fichier n'est pas implémentée (§12).

- **Les barèmes sont modifiables à chaud** : l'échelle de prêt (§12) et la grille de frais (§12)
  vivent en base et priment sur `application.properties`. Ne figez ni les taux ni les plafonds dans le front.
- **Les exports d'audit renvoient `url: null`** — la génération de fichier n'est pas implémentée (§12).
- **L'assistant (§11 bis) répond 503 tant que `app.assistant.api-key` n'est pas renseignée** côté serveur. Le reste de l'application fonctionne normalement.
Questions ou champ manquant → ouvrez une issue sur le dépôt, ou consultez Swagger qui reflète toujours le code déployé.
