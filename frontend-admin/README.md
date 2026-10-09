# Console d'administration Kola

Console React + Vite, volontairement minimaliste, branchée sur
`/api/v1/admin/console/**` (contrat : `backend/BACKEND.md` §17).

## Démarrer

```
npm install
npm run dev      # http://localhost:3000 — backend attendu sur http://localhost:8081
npm run lint
npm run build
```

Le port 3000 est épinglé (`strictPort`) : c'est une origine acceptée par le CORS
du backend. `VITE_API_BASE_URL` (`.env.development`, versionné) pointe sur
`http://localhost:8081/api/v1/admin`. Sans elle, la console refuse de démarrer
plutôt que d'afficher des données inventées.

## Deux règles

1. **Rien que l'application mobile ne propose.** Utilisateurs (fiche : comptes,
   coffres, prêts, paiements programmés, transactions, pièces), vérifications KYC,
   transactions, prêts — plus le compte de l'administrateur. Litiges, support,
   finance, frais, audit, rôles : absents.
2. **L'API sert des valeurs brutes, la console les met en forme** — et seulement
   `src/lib/format.js` : montants, dates, libellés d'enum, tons de badge.

## Structure

```
src/lib/api.js          fetch, jeton (localStorage), 401 → déconnexion, téléchargement
src/lib/session.jsx     identité (/auth/me), rôle → sections visibles
src/lib/useResource.js  chargement annulable, données conservées pendant un rechargement
src/lib/format.js       nombres, dates, libellés français
src/components/         coque (Layout) et briques (ui.jsx)
src/pages/              un fichier par écran
src/styles.css          toute la mise en forme, en tokens (seules les hauteurs
                        calculées du graphique passent en style en ligne)
src/components/icons.jsx icônes SVG au trait, sans bibliothèque
```

- Un 403 (module non accordé au rôle) affiche « Accès refusé » et garde la session ;
  seul un 401 déconnecte.
- Les pièces KYC se **téléchargent**, jamais ne s'ouvrent dans un onglet : une URL
  `blob:` hérite de l'origine de la console, un fichier piégé y lirait le jeton.
- Police Manrope empaquetée par Vite (`@fontsource-variable/manrope`) : aucune
  requête vers un service tiers, aucun script extérieur.
- Sobre dans l'information, pas dans la forme : tableau de bord en quatre blocs
  (volume + graphique, utilisateurs, KYC, prêts), cartes arrondies, icônes, avatars.
