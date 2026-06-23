import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev proxy: requests to /api are forwarded to the Spring backend on :8081.
// This makes the SPA same-origin with the API in development, so the JS-readable
// XSRF-TOKEN cookie works (the recommended CSRF setup — see CLAUDE.md §14).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: false,
      },
    },
  },
});
