import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Épinglé, et strict exprès. Par défaut Vite glisse vers le port libre suivant
    // quand celui-ci est pris : il démarrerait alors sur une origine que le CORS du
    // backend ne connaît pas, et chaque appel échouerait en « Failed to fetch » — ce
    // qui, sur l'écran de connexion, ressemble à un mauvais mot de passe. Refuser de
    // démarrer est l'issue honnête : cela dit qu'un serveur tourne déjà.
    //
    // 3000 : le port de la console dans Kola (voir le tableau des ports de CLAUDE.md),
    // accepté par `app.security.cors.allowed-origins`.
    port: 3000,
    strictPort: true,
  },
  // Même origine pour `npm run preview` (défaut Vite : 4173, inconnu du CORS).
  preview: {
    port: 3000,
    strictPort: true,
  },
})
