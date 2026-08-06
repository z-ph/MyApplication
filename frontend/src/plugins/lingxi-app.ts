import { Capacitor, registerPlugin } from '@capacitor/core';
import type { LingxiAppPlugin } from '../types/bridge';
import { LingxiAppWeb } from '../mocks/lingxi-app-web';

/**
 * Typed wrapper for native plugin `LingxiApp`.
 * Pages must not call window.Capacitor directly — use this module.
 *
 * Native: com.example.myapplication.plugins.LingxiAppPlugin
 * Web / DEV: LingxiAppWeb mock (always available so npm run dev works).
 */
const LingxiApp = registerPlugin<LingxiAppPlugin>('LingxiApp', {
  web: () => Promise.resolve(new LingxiAppWeb()),
});

/** Prefer mock when running pure browser without native bridge (even outside DEV). */
export function isNativePlatform(): boolean {
  return Capacitor.isNativePlatform();
}

export { LingxiApp };
export type { LingxiAppPlugin };
