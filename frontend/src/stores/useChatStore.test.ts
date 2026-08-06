import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../plugins/lingxi-chat', () => ({
  LingxiChat: {
    listSessions: vi.fn(async () => ({ sessions: [] })),
    listMessages: vi.fn(async () => ({ messages: [] })),
    createSession: vi.fn(async () => ({
      session: {
        id: 's1',
        title: '新会话',
        createdAt: 1,
        updatedAt: 1,
      },
    })),
    selectSession: vi.fn(async () => ({})),
    deleteSession: vi.fn(async () => ({})),
    sendMessage: vi.fn(async () => ({})),
    cancelTask: vi.fn(async () => ({})),
    clearMessages: vi.fn(async () => ({})),
    addListener: vi.fn(async () => ({ remove: vi.fn(async () => {}) })),
  },
}));

vi.mock('../plugins/lingxi-agent', () => ({
  LingxiAgent: {
    getState: vi.fn(async () => ({
      state: 'READY',
      step: '',
      action: '',
      thinking: '',
      result: null,
      error: null,
    })),
    addListener: vi.fn(async () => ({ remove: vi.fn(async () => {}) })),
  },
}));

import { LingxiChat } from '../plugins/lingxi-chat';
import { useChatStore } from './useChatStore';

describe('useChatStore', () => {
  beforeEach(() => {
    useChatStore.setState({
      sessions: [],
      currentSessionId: null,
      messages: [],
      agentState: null,
      sending: false,
      ready: false,
      error: null,
    });
    useChatStore.getState().dispose();
    vi.clearAllMocks();
  });

  it('sendMessage ignores blank content', async () => {
    await useChatStore.getState().sendMessage('   ');
    expect(LingxiChat.sendMessage).not.toHaveBeenCalled();
    expect(useChatStore.getState().sending).toBe(false);
  });

  it('sendMessage forwards trimmed content', async () => {
    await useChatStore.getState().sendMessage('  hello  ');
    expect(LingxiChat.sendMessage).toHaveBeenCalledWith({ content: 'hello' });
  });

  it('createSession sets currentSessionId', async () => {
    await useChatStore.getState().createSession();
    expect(LingxiChat.createSession).toHaveBeenCalled();
    expect(useChatStore.getState().currentSessionId).toBe('s1');
    expect(useChatStore.getState().messages).toEqual([]);
  });
});
