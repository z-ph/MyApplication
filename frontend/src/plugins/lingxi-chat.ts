import { registerPlugin } from '@capacitor/core';
import type { LingxiChatPlugin } from '../types/bridge';
import { LingxiChatWeb } from '../mocks/lingxi-chat-web';

/**
 * Typed wrapper for native plugin `LingxiChat`.
 * Pages must not call window.Capacitor directly — use this module.
 */
const LingxiChat = registerPlugin<LingxiChatPlugin>('LingxiChat', {
  web: () => Promise.resolve(new LingxiChatWeb()),
});

export { LingxiChat };
export type { LingxiChatPlugin };
