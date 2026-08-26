# frontend-web — l'espace client Kola dans le navigateur

Version web de l'application mobile Flutter (`mobile/`). Même compte, même API,
mêmes opérations : ce que l'on peut faire sur le téléphone, on peut le faire
ici.

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
| `landing/` | 3001 | site vitrine (n'appelle pas l'API) |
| **`frontend-web/`** | **3002** | **espace client (cette application)** |
| `backend/` | 8081 | API Spring Boot |

## Commandes

```bash
npm run dev      # serveur de développement sur :3002
npm run build    # build de production + vérification TypeScript
npm run start    # sert le build de production
npm run lint     # ce que vérifie la CI
```

## Couverture fonctionnelle

Tout ce que fait `mobile/`, écran pour écran :

| Domaine | Écrans web | API |
|---|---|---|
| Authentification | `/connexion`, `/inscription`, `/confirmation`, `/mot-de-passe-oublie`, `/reinitialiser` | `/api/auth/**` |
| Accueil | `/` — solde, actions rapides, dernières opérations, épargne, score | `/api/wallets`, `/api/transactions/**`, `/api/vaults`, `/api/credit/score` |
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
