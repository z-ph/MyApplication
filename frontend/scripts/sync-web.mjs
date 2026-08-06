#!/usr/bin/env node
/**
 * Sync Vite `dist/` → existing Android app assets (no second android/ tree).
 *
 * Writes:
 *   app/src/main/assets/public/     ← web bundle
 *   app/src/main/assets/capacitor.config.json
 *   app/src/main/assets/capacitor.plugins.json
 */
import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(__dirname, '..');
const repoRoot = resolve(frontendRoot, '..');
const distDir = join(frontendRoot, 'dist');
const assetsDir = join(repoRoot, 'app/src/main/assets');
const publicDir = join(assetsDir, 'public');

if (!existsSync(distDir)) {
  console.error('[sync-web] dist/ missing — run `npm run build` first');
  process.exit(1);
}

mkdirSync(assetsDir, { recursive: true });
if (existsSync(publicDir)) {
  rmSync(publicDir, { recursive: true, force: true });
}
mkdirSync(publicDir, { recursive: true });
cpSync(distDir, publicDir, { recursive: true });

// Prefer built capacitor.config.json from CLI if present; else emit from TS defaults.
const capConfigPath = join(frontendRoot, 'capacitor.config.ts');
const capJsonFromCli = join(distDir, '..', 'capacitor.config.json'); // not always generated
void capConfigPath;
void capJsonFromCli;

const capacitorConfig = {
  appId: 'com.example.myapplication',
  appName: '灵犀',
  webDir: 'public',
  android: {
    allowMixedContent: false,
  },
  server: {
    androidScheme: 'https',
  },
};

// Custom Lingxi* plugins are registered in MainActivity.registerPlugin.
// capacitor.plugins.json is reserved for npm Cordova/Capacitor packages.
const capacitorPlugins = [];

writeFileSync(
  join(assetsDir, 'capacitor.config.json'),
  JSON.stringify(capacitorConfig, null, 2) + '\n',
  'utf8',
);
writeFileSync(
  join(assetsDir, 'capacitor.plugins.json'),
  JSON.stringify(capacitorPlugins, null, 2) + '\n',
  'utf8',
);

// Optional: copy package version stamp for debugging
try {
  const pkg = JSON.parse(readFileSync(join(frontendRoot, 'package.json'), 'utf8'));
  writeFileSync(
    join(assetsDir, 'public', '.web-build.json'),
    JSON.stringify({ name: pkg.name, builtAt: new Date().toISOString() }, null, 2) + '\n',
    'utf8',
  );
} catch {
  /* ignore */
}

console.log('[sync-web] synced dist → app/src/main/assets/public');
console.log('[sync-web] wrote capacitor.config.json + capacitor.plugins.json');
