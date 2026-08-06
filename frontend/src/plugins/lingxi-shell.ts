import { registerPlugin } from '@capacitor/core';
import type { LingxiShellPlugin } from '../types/bridge';
import { LingxiShellWeb } from '../mocks/lingxi-shell-web';

/**
 * Typed wrapper for native plugin `LingxiShell` (debug).
 * Pages must not call window.Capacitor directly — use this module.
 */
const LingxiShell = registerPlugin<LingxiShellPlugin>('LingxiShell', {
  web: () => Promise.resolve(new LingxiShellWeb()),
});

export { LingxiShell };
export type { LingxiShellPlugin };
