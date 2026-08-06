import { registerPlugin } from '@capacitor/core';
import type { LingxiAgentPlugin } from '../types/bridge';
import { LingxiAgentWeb } from '../mocks/lingxi-agent-web';

/**
 * Typed wrapper for native plugin `LingxiAgent`.
 * getState / stateChanged return bare AgentStateDto.
 */
const LingxiAgent = registerPlugin<LingxiAgentPlugin>('LingxiAgent', {
  web: () => Promise.resolve(new LingxiAgentWeb()),
});

export { LingxiAgent };
export type { LingxiAgentPlugin };
