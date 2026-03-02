import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
    exclude: ['**/e2e/**', '**/node_modules/**'],
    coverage: {
      exclude: [
        '**/*.css',
        '**/e2e/**',
        '**/node_modules/**',
        '**/test/**',
        '**/__tests__/**',
        '**/main.tsx',
        '**/App.tsx',
        '**/vite-env.d.ts',
      ],
    },
  },
});
