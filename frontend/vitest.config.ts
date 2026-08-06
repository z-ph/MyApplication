import { defineConfig } from 'vitest/config';

/**
 * Separate from vite.config.ts so `tsc -b` (vite 8 types) is not coupled to
 * vitest's nested vite peer types.
 */
export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
  },
});
