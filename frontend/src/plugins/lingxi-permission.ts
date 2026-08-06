import { registerPlugin } from '@capacitor/core';
import type { LingxiPermissionPlugin } from '../types/bridge';
import { LingxiPermissionWeb } from '../mocks/lingxi-permission-web';

/**
 * Typed wrapper for native plugin `LingxiPermission`.
 * Pages must not call window.Capacitor directly — use this module.
 */
const LingxiPermission = registerPlugin<LingxiPermissionPlugin>(
  'LingxiPermission',
  {
    web: () => Promise.resolve(new LingxiPermissionWeb()),
  },
);

export { LingxiPermission };
export type { LingxiPermissionPlugin };
