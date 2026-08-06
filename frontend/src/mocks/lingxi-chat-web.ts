import type {
  ChatMessageDto,
  ChatSessionDto,
  LingxiChatPlugin,
  MessagesChangedEvent,
  MessagesEnvelope,
  SessionEnvelope,
  SessionsEnvelope,
  TaskProgressEvent,
} from '../types/bridge';

type Listener = (data: unknown) => void;

function uuid(): string {
  return `mock-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

/**
 * In-memory browser mock for LingxiChat (npm run dev).
 */
export class LingxiChatWeb implements LingxiChatPlugin {
  private sessions: ChatSessionDto[] = [];
  private messages = new Map<string, ChatMessageDto[]>();
  private currentSessionId: string | null = null;
  private listeners = new Map<string, Set<Listener>>();

  constructor() {
    const id = uuid();
    const now = Date.now();
    const session: ChatSessionDto = {
      id,
      title: '新会话（mock）',
      createdAt: now,
      updatedAt: now,
    };
    this.sessions = [session];
    this.messages.set(id, []);
    this.currentSessionId = id;
  }

  private emit(event: string, data: unknown) {
    this.listeners.get(event)?.forEach((fn) => fn(data));
  }

  async listSessions(): Promise<SessionsEnvelope> {
    return { sessions: [...this.sessions] };
  }

  async createSession(options?: { title?: string }): Promise<SessionEnvelope> {
    const now = Date.now();
    const session: ChatSessionDto = {
      id: uuid(),
      title: options?.title?.trim() || '新会话',
      createdAt: now,
      updatedAt: now,
    };
    this.sessions = [session, ...this.sessions];
    this.messages.set(session.id, []);
    this.currentSessionId = session.id;
    this.emit('sessionsChanged', { sessions: [...this.sessions] });
    this.emit('messagesChanged', {
      sessionId: session.id,
      messages: [],
    } satisfies MessagesChangedEvent);
    return { session };
  }

  async selectSession(options: { sessionId: string }): Promise<void> {
    if (!this.sessions.some((s) => s.id === options.sessionId)) {
      throw new Error('session not found');
    }
    this.currentSessionId = options.sessionId;
    this.emit('messagesChanged', {
      sessionId: options.sessionId,
      messages: [...(this.messages.get(options.sessionId) ?? [])],
    });
  }

  async deleteSession(options: { sessionId: string }): Promise<void> {
    this.sessions = this.sessions.filter((s) => s.id !== options.sessionId);
    this.messages.delete(options.sessionId);
    if (this.currentSessionId === options.sessionId) {
      if (this.sessions[0]) {
        this.currentSessionId = this.sessions[0].id;
      } else {
        await this.createSession();
        return;
      }
    }
    this.emit('sessionsChanged', { sessions: [...this.sessions] });
    if (this.currentSessionId) {
      this.emit('messagesChanged', {
        sessionId: this.currentSessionId,
        messages: [...(this.messages.get(this.currentSessionId) ?? [])],
      });
    }
  }

  async listMessages(options: {
    sessionId: string;
  }): Promise<MessagesEnvelope> {
    return { messages: [...(this.messages.get(options.sessionId) ?? [])] };
  }

  async sendMessage(options: { content: string }): Promise<void> {
    const content = options.content?.trim();
    if (!content) throw new Error('content is required');
    let sid = this.currentSessionId;
    if (!sid) {
      const { session } = await this.createSession();
      sid = session.id;
    }
    const user: ChatMessageDto = {
      id: uuid(),
      timestamp: Date.now(),
      type: 'user',
      content,
    };
    const list = [...(this.messages.get(sid) ?? []), user];
    this.messages.set(sid, list);

    // Auto title on first message
    const sess = this.sessions.find((s) => s.id === sid);
    if (sess && list.length === 1) {
      sess.title =
        content.length > 30 ? `${content.slice(0, 30)}...` : content;
      sess.updatedAt = Date.now();
      this.emit('sessionsChanged', { sessions: [...this.sessions] });
    }

    this.emit('messagesChanged', { sessionId: sid, messages: list });
    this.emit('taskProgress', {
      sessionId: sid,
      state: 'RUNNING',
      step: '',
      action: '',
      thinking: '',
    } satisfies TaskProgressEvent);

    // Mock AI reply
    await delay(400);
    const ai: ChatMessageDto = {
      id: uuid(),
      timestamp: Date.now(),
      type: 'ai',
      content: `（mock）已收到：${content}`,
      isSuccess: true,
    };
    const list2 = [...(this.messages.get(sid) ?? []), ai];
    this.messages.set(sid, list2);
    this.emit('messagesChanged', { sessionId: sid, messages: list2 });
    this.emit('taskProgress', {
      sessionId: sid,
      state: 'COMPLETED',
      step: '',
      action: '',
      thinking: '',
      result: ai.content,
    });
  }

  async cancelTask(): Promise<void> {
    const sid = this.currentSessionId;
    if (!sid) return;
    const status: ChatMessageDto = {
      id: uuid(),
      timestamp: Date.now(),
      type: 'status',
      content: '⏹️ 任务已取消',
      isRunning: false,
    };
    const list = [...(this.messages.get(sid) ?? []), status];
    this.messages.set(sid, list);
    this.emit('messagesChanged', { sessionId: sid, messages: list });
    this.emit('taskProgress', {
      sessionId: sid,
      state: 'CANCELLED',
      step: '',
      action: '',
      thinking: '',
    });
  }

  async clearMessages(): Promise<void> {
    const sid = this.currentSessionId;
    if (!sid) return;
    this.messages.set(sid, []);
    this.emit('messagesChanged', { sessionId: sid, messages: [] });
  }

  async addListener(
    eventName: string,
    listener: (data: SessionsEnvelope | MessagesChangedEvent | TaskProgressEvent) => void,
  ): Promise<{ remove: () => Promise<void> }> {
    const wrapped: Listener = (data) => {
      listener(
        data as SessionsEnvelope | MessagesChangedEvent | TaskProgressEvent,
      );
    };
    if (!this.listeners.has(eventName)) {
      this.listeners.set(eventName, new Set());
    }
    this.listeners.get(eventName)!.add(wrapped);
    return {
      remove: async () => {
        this.listeners.get(eventName)?.delete(wrapped);
      },
    };
  }

  async removeAllListeners(): Promise<void> {
    this.listeners.clear();
  }
}

function delay(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}
