import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Capacitor config for 灵犀.
 * Web assets are built to `dist` and synced into the existing Android `app`
 * module at `app/src/main/assets/public` (see scripts/sync-web.mjs).
 * We intentionally do NOT run `cap add android` (would create a second tree).
 */
const config: CapacitorConfig = {
  appId: 'com.example.myapplication',
  appName: '灵犀',
  webDir: 'dist',
  android: {
    // Relative to this config file (frontend/)
    path: '../app',
  },
  server: {
    androidScheme: 'https',
  },
};

export default config;
