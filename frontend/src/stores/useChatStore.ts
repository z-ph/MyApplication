import { create } from 'zustand';
import type { PluginListenerHandle } from '@capacitor/core';
import { LingxiChat } from '../plugins/lingxi-chat';
import { LingxiAgent } from '../plugins/lingxi-agent';
import type {
  AgentStateDto,
  ChatMessageDto,
  ChatSessionDto,
} from '../types/bridge';

interface ChatState {
  sessions: ChatSessionDto[];
  currentSessionId: string | null;
  messages: ChatMessageDto[];
  agentState: AgentStateDto | null;
  sending: boolean;
  ready: boolean;
  error: string | null;
  /** Bootstrap + subscribe once */
  init: () => Promise<void>;
  dispose: () => void;
  createSession: (title?: string) => Promise<void>;
  selectSession: (sessionId: string) => Promise<void>;
  deleteSession: (sessionId: string) => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  cancelTask: () => Promise<void>;
  clearMessages: () => Promise<void>;
}

let handles: PluginListenerHandle[] = [];
let initPromise: Promise<void> | null = null;

export const useChatStore = create<ChatState>((set, get) => ({
  sessions: [],
  currentSessionId: null,
  messages: [],
  agentState: null,
  sending: false,
  ready: false,
  error: null,

  init: async () => {
    if (initPromise) return initPromise;
    initPromise = (async () => {
      try {
        // Clear previous listeners if re-init
        for (const h of handles) {
          try {
            await h.remove();
          } catch {
            /* ignore */
          }
        }
        handles = [];

        const [sessionsRes, agentState] = await Promise.all([
          LingxiChat.listSessions(),
          LingxiAgent.getState().catch(() => null),
        ]);

        const sessions = sessionsRes.sessions ?? [];
        let currentSessionId = get().currentSessionId;
        if (!currentSessionId && sessions[0]) {
          currentSessionId = sessions[0].id;
        }

        let messages: ChatMessageDto[] = [];
        if (currentSessionId) {
          const msgRes = await LingxiChat.listMessages({
            sessionId: currentSessionId,
          });
          messages = msgRes.messages ?? [];
        }

        set({
          sessions,
          currentSessionId,
          messages,
          agentState,
          ready: true,
          error: null,
        });

        handles.push(
          await LingxiChat.addListener('sessionsChanged', (raw) => {
            const data = raw as { sessions?: ChatSessionDto[] };
            set({ sessions: data.sessions ?? [] });
          }),
        );

        handles.push(
          await LingxiChat.addListener('messagesChanged', (raw) => {
            const data = raw as {
              sessionId?: string;
              messages?: ChatMessageDto[];
            };
            const sid = get().currentSessionId;
            if (!sid || data.sessionId === sid) {
              set({
                currentSessionId: data.sessionId ?? sid,
                messages: data.messages ?? [],
              });
            }
          }),
        );

        handles.push(
          await LingxiChat.addListener('taskProgress', (raw) => {
            const data = raw as {
              state: string;
              step?: string;
              action?: string;
              thinking?: string;
              result?: string | null;
              error?: string | null;
            };
            set({
              agentState: {
                state: data.state,
                step: data.step ?? '',
                action: data.action ?? '',
                thinking: data.thinking ?? '',
                result: data.result ?? null,
                error: data.error ?? null,
              },
              sending: data.state === 'RUNNING',
            });
          }),
        );

        handles.push(
          await LingxiAgent.addListener('stateChanged', (data) => {
            set({
              agentState: data,
              sending: data.state === 'RUNNING',
            });
          }),
        );
      } catch (e) {
        const message = e instanceof Error ? e.message : String(e);
        set({ error: message, ready: true });
      }
    })();
    return initPromise;
  },

  dispose: () => {
    void (async () => {
      for (const h of handles) {
        try {
          await h.remove();
        } catch {
          /* ignore */
        }
      }
      handles = [];
      initPromise = null;
    })();
  },

  createSession: async (title?: string) => {
    const { session } = await LingxiChat.createSession(
      title ? { title } : undefined,
    );
    set({
      currentSessionId: session.id,
      messages: [],
    });
    // sessions list comes via sessionsChanged; refresh optimistically
    const listRes = await LingxiChat.listSessions();
    set({ sessions: listRes.sessions });
  },

  selectSession: async (sessionId: string) => {
    await LingxiChat.selectSession({ sessionId });
    const msgRes = await LingxiChat.listMessages({ sessionId });
    set({
      currentSessionId: sessionId,
      messages: msgRes.messages ?? [],
    });
  },

  deleteSession: async (sessionId: string) => {
    await LingxiChat.deleteSession({ sessionId });
    const listRes = await LingxiChat.listSessions();
    const list = listRes.sessions;
    const nextId =
      get().currentSessionId === sessionId
        ? (list[0]?.id ?? null)
        : get().currentSessionId;
    let messages: ChatMessageDto[] = [];
    if (nextId) {
      const msgRes = await LingxiChat.listMessages({ sessionId: nextId });
      messages = msgRes.messages ?? [];
    }
    set({
      sessions: list,
      currentSessionId: nextId,
      messages,
    });
  },

  sendMessage: async (content: string) => {
    const trimmed = content.trim();
    if (!trimmed) return;
    set({ sending: true, error: null });
    try {
      await LingxiChat.sendMessage({ content: trimmed });
      // Kickoff returned. Events clear sending when leaving RUNNING; if agent never
      // entered RUNNING (early reject) or already terminal, clear here so UI unsticks.
      try {
        // Brief yield so native job can transition to RUNNING on the happy path
        await new Promise((r) => setTimeout(r, 150));
        if (!get().sending) return;
        const st = await LingxiAgent.getState();
        if (st.state !== 'RUNNING') {
          set({ sending: false, agentState: st });
        }
      } catch {
        /* events / cancel may still clear */
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e);
      set({ error: message, sending: false });
      throw e;
    }
  },

  cancelTask: async () => {
    try {
      await LingxiChat.cancelTask();
    } finally {
      set({ sending: false });
    }
  },

  clearMessages: async () => {
    await LingxiChat.clearMessages();
    set({ messages: [] });
  },
}));
