import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev proxy: requests to /api are forwarded to the API gateway on :8085 (the
// microservices stack), NOT the monolith. The SPA authenticates with a JWT in the
// Authorization header (stateless, no cookies/CSRF), so same-origin is no longer
// required for a CSRF cookie — the proxy just avoids CORS in dev. See CLAUDE.md §24.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: false,
      },
    },
  },
});
