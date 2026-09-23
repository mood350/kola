# frontend-web — le site public et l'espace client Kola

**Une seule application, un seul port, un seul domaine.** Les pages marketing
(`/`, `/a-propos`, `/etudes-de-cas`…) et l'espace client authentifié
(`/mon-compte`, `/coffres`, `/credit`…) sont servis par le même build depuis la
fusion de `landing/`.

L'espace client est la version web de l'application Flutter (`mobile/`) : même
compte, même API, mêmes opérations.

## Démarrer

```bash
npm install
cp .env.example .env.local   # NEXT_PUBLIC_API_URL si le backend n'est pas sur :8081
npm run dev                  # http://localhost:3002
```

Le backend doit tourner (`cd backend && mvn spring-boot:run`, port 8081, avec
Postgres et Redis).

**Le port 3002 n'est pas négociable en développement.** Les origines autorisées
sont listées une à une dans `SecurityConfig.corsConfigurationSource()` : depuis
n'importe quel autre port, chaque requête échoue en CORS avant d'atteindre un
contrôleur. Le tableau des ports du monorepo :

| Application | Port | Rôle |
|---|---|---|
| `frontend-admin/` | 3000 | console d'administration |
| **`frontend-web/`** | **3002** | **site public + espace client (cette application)** |
| `backend/` | 8081 | API Spring Boot |

Le port 3001 est libre : `landing/` ne tourne plus séparément. Le relancer
placerait les pages publiques sur une origine que l'API refuse, et couperait le
domaine en deux.

## Commandes

```bash
npm run dev      # serveur de développement sur :3002
npm run build    # build de production + vérification TypeScript
npm run start    # sert le build de production
npm run lint     # ce que vérifie la CI
```

## Les trois groupes de routes

| Groupe | Routes | Coque | Indexé |
|---|---|---|---|
| `(marketing)` | `/`, `/a-propos`, `/etudes-de-cas` (+ `[slug]`), `/contact`, `/merci`, `/cgu`, `/confidentialite`, `/cookies` | en-tête, pied de page, défilement Lenis, bandeau cookies, GA | oui |
| `(private)/(app)` | espace client (voir ci-dessous) | coque applicative, garde d'authentification | **non** |
| `(private)/(auth)` | `/connexion`, `/inscription`, `/confirmation`, `/mot-de-passe-oublie`, `/reinitialiser` | coque centrée | **non** |

**La racine `/` appartient au site public** ; le tableau de bord vit à
`/mon-compte`. Deux groupes ne peuvent pas définir la même URL — c'était la
seule collision à résoudre.

## Couverture fonctionnelle de l'espace client

Tout ce que fait `mobile/`, écran pour écran :

| Domaine | Écrans web | API |
|---|---|---|
| Authentification | `/connexion`, `/inscription`, `/confirmation`, `/mot-de-passe-oublie`, `/reinitialiser` | `/api/auth/**` |
| Accueil | `/mon-compte` — solde, actions rapides, dernières opérations, épargne, score | `/api/wallets`, `/api/transactions/**`, `/api/vaults`, `/api/credit/score` |
| Opérations | `/operations/depot`, `/retrait`, `/envoi`, `/paiement` | `/api/transactions/**`, `/api/merchants/{code}` |
| Historique | `/transactions`, `/transactions/{reference}` | `/api/transactions/**` |
| Coffres | `/coffres`, `/coffres/{id}` | `/api/vaults/**` |
| Crédit | `/credit`, `/credit/demande`, `/credit/prets/{id}` | `/api/credit/**` |
| Virements programmés | `/virements-programmes` | `/api/scheduled-transfers/**` |
| Bénéficiaires | `/beneficiaires` | `/api/beneficiaries/**` |
| Notifications | `/notifications` | `/api/notifications/**` |
| Profil | `/profil`, `/profil/modifier`, `/profil/mot-de-passe` | `/api/users/**` |
| Comptes | `/comptes` | `/api/wallets/**` |

## Ce qu'il faut savoir avant de modifier le code

### Next.js 16, et la documentation qui fait foi

Comme pour `landing/` et `frontend-admin/` : cette version a des ruptures par
rapport aux conventions plus anciennes de l'App Router. La documentation de
référence est celle qui est livrée avec la dépendance —
`node_modules/next/dist/docs/` — pas un souvenir de version antérieure.
Notamment, `params` est une **promesse** : dans un composant client, on la dénoue
avec `use()`.

### Où vit quoi

- `src/lib/api.ts` — client HTTP : jeton, renouvellement mutualisé sur 401,
  traduction des erreurs, clés d'idempotence. **Aucun `fetch` ailleurs.**
- `src/lib/services.ts` — un module par domaine, une fonction par endpoint. Un
  chemin d'API qui n'apparaît pas ici n'est utilisé nulle part.
- `src/lib/types.ts` — miroir des DTOs Java ; chaque bloc nomme sa source.
- `src/lib/session.tsx` — qui est connecté, et sur quel compte il travaille.
- `src/lib/labels.ts` — traduction et couleur des énumérations. Un statut = une
  couleur, partout.
- `src/components/ui/primitives.tsx` — toute la base d'interface, en un module.

### Ce que la fusion impose

1. **Le layout racine ne porte que ce qui vaut pour les deux publics** — langue,
   polices, JSON-LD d'entité, lien d'évitement. L'en-tête, le pied de page,
   Lenis et le consentement vivent dans `(marketing)/layout.tsx` ; le
   fournisseur de session dans `(private)/layout.tsx`. Le remonter à la racine
   ferait interroger `/users/me` depuis la page d'accueil publique.
2. **`(private)/layout.tsx` est un composant serveur** : c'est ce qui lui permet
   d'exporter `robots: noindex`. `robots.ts` interdit en plus l'exploration —
   `noindex` empêche l'indexation, `Disallow` empêche la visite, et l'un sans
   l'autre laisse un trou.
3. **Google Analytics n'est monté que dans le groupe marketing.** Les jetons
   vivent dans le `localStorage` d'une origine désormais partagée : aucune page
   authentifiée ne doit charger de script tiers.
4. **Deux familles de rayons, volontairement.** `rounded-card`/`rounded-panel`
   (1,75/2,25rem) au marketing ; `rounded-surface`/`rounded-sheet`/`rounded-field`
   (20/24/12px) au produit, alignés sur `AppRadius` du Flutter.

### Trois règles qui tiennent le reste

1. **Toute opération d'argent porte une clé d'idempotence, créée avec
   l'intention et non à l'envoi.** Elle ne change qu'après un succès ou un
   changement de montant/destinataire. C'est ce qui fait qu'un rechargement au
   mauvais moment ne débite pas deux fois.
2. **Après une opération, `reloadWallets()`.** Sans lui, l'accueil continue
   d'afficher le solde d'avant, ce qui est le pire défaut possible ici.
3. **Le formulaire disparaît quand l'opération réussit** (`OperationResult`).
   Une bannière verte au-dessus d'un formulaire encore rempli invite à renvoyer
   le même virement.

### Tout 200 n'est pas du JSON

`POST /auth/confirm` et `POST /auth/reset-password` sont déclarés
`ResponseEntity<String>` côté Spring : ils répondent 200 avec une phrase en
texte brut. `apiFetch` se fie donc au `Content-Type` annoncé plutôt que
d'appeler `JSON.parse` systématiquement — sans quoi une activation **réussie**
lève une `SyntaxError` et l'écran annonce un échec imaginaire. Tout nouvel
endpoint qui renvoie `ResponseEntity<String>` passe par la même branche.

L'e-mail d'activation pointe vers `/confirmation` de cette application
(`application.mailing.frontend.activation-url`, surchargeable par
`FRONTEND_ACTIVATION_URL`). Le lien ne porte pas le code : l'OTP figure
uniquement dans le corps du message.

### Jetons et XSS

Les jetons vivent dans le `localStorage` — c'est ce que documente
`AuthController.logout`, l'API ne posant aucun cookie. Conséquence assumée :
un script injecté dans cette page peut les lire. L'atténuation retenue est
qu'aucun script tiers, aucune police distante et aucune balise de mesure ne sont
chargés (les polices sont auto-hébergées par `next/font`). Passer à des cookies
`HttpOnly` demanderait du travail côté backend.
