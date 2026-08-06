import { registerPlugin } from '@capacitor/core';
import type { LingxiLogPlugin } from '../types/bridge';
import { LingxiLogWeb } from '../mocks/lingxi-log-web';

/**
 * Typed wrapper for native plugin `LingxiLog`.
 * Pages must not call window.Capacitor directly — use this module.
 */
const LingxiLog = registerPlugin<LingxiLogPlugin>('LingxiLog', {
  web: () => Promise.resolve(new LingxiLogWeb()),
});

export { LingxiLog };
export type { LingxiLogPlugin };
