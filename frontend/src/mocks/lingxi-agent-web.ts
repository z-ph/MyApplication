import type {
  AgentStateDto,
  ConfiguredResult,
  LingxiAgentPlugin,
  ReconfigureResult,
} from '../types/bridge';

type Listener = (data: AgentStateDto) => void;

/**
 * Browser mock for LingxiAgent.
 */
export class LingxiAgentWeb implements LingxiAgentPlugin {
  private state: AgentStateDto = {
    state: 'READY',
    step: '',
    action: '',
    thinking: '',
    result: null,
    error: null,
  };
  private listeners = new Set<Listener>();

  async getState(): Promise<AgentStateDto> {
    return { ...this.state };
  }

  async reconfigure(): Promise<ReconfigureResult> {
    this.state = {
      state: 'READY',
      step: '',
      action: '',
      thinking: '',
      result: null,
      error: null,
    };
    this.emit();
    return { ok: true };
  }

  async isConfigured(): Promise<ConfiguredResult> {
    return { configured: true };
  }

  async addListener(
    eventName: 'stateChanged',
    listener: Listener,
  ): Promise<{ remove: () => Promise<void> }> {
    if (eventName !== 'stateChanged') {
      return { remove: async () => undefined };
    }
    this.listeners.add(listener);
    // Push current state once
    listener({ ...this.state });
    return {
      remove: async () => {
        this.listeners.delete(listener);
      },
    };
  }

  async removeAllListeners(): Promise<void> {
    this.listeners.clear();
  }

  /** Test helper: push a state */
  setMockState(partial: Partial<AgentStateDto>) {
    this.state = { ...this.state, ...partial };
    this.emit();
  }

  private emit() {
    const snap = { ...this.state };
    this.listeners.forEach((fn) => fn(snap));
  }
}
