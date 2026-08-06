import { registerPlugin } from '@capacitor/core';
import type { LingxiApiConfigPlugin } from '../types/bridge';
import { LingxiApiConfigWeb } from '../mocks/lingxi-api-config-web';

/**
 * Typed wrapper for native plugin `LingxiApiConfig`.
 * List returns apiKeyMasked only; write paths send full apiKey once.
 */
const LingxiApiConfig = registerPlugin<LingxiApiConfigPlugin>(
  'LingxiApiConfig',
  {
    web: () => Promise.resolve(new LingxiApiConfigWeb()),
  },
);

export { LingxiApiConfig };
export type { LingxiApiConfigPlugin };
