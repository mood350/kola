import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Pinned, and strict on purpose. Vite's default is to slide to the next free
    // port when 5173 is taken, which starts a second dev server on an origin the
    // backend's CORS allow-list does not know — every API call then fails with an
    // opaque "Failed to fetch" that reads like wrong credentials. Failing to start
    // is the honest outcome: it says a server is already running.
    port: 5173,
    strictPort: true,
  },
})
