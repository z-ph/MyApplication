/** DTO / plugin types aligned with docs/bridge-api.md */

import type { PluginListenerHandle } from '@capacitor/core';

// —— LingxiApp (Phase 1) ——

export interface EchoOptions {
  value: string;
}

export interface EchoResult {
  value: string;
}

export interface AppVersionInfo {
  appName: string;
  packageName: string;
  versionName: string;
  versionCode: number;
  /** true when served by browser mock, false on native */
  mock?: boolean;
}

export interface OpenUrlOptions {
  url: string;
}

export interface LingxiAppPlugin {
  echo(options: EchoOptions): Promise<EchoResult>;
  getVersion(): Promise<AppVersionInfo>;
  openUrl(options: OpenUrlOptions): Promise<void>;
}

// —— DTOs §2 ——

export type ChatMessageType =
  | 'user'
  | 'ai'
  | 'toolCall'
  | 'screenshot'
  | 'status';

export interface ChatMessageDto {
  id: string;
  timestamp: number;
  type: ChatMessageType | string;
  content: string;
  isSuccess?: boolean | null;
  errorMessage?: string | null;
  toolName?: string | null;
  parameters?: Record<string, unknown> | null;
  result?: string | null;
  imageBase64?: string | null;
  isRunning?: boolean | null;
}

export interface ChatSessionDto {
  id: string;
  title: string;
  createdAt: number;
  updatedAt: number;
}

export type AgentStateName =
  | 'IDLE'
  | 'READY'
  | 'RUNNING'
  | 'COMPLETED'
  | 'ERROR'
  | 'CANCELLED';

export interface AgentStateDto {
  state: AgentStateName | string;
  step: string;
  action: string;
  thinking: string;
  result?: string | null;
  error?: string | null;
}

// —— Envelopes ——

export interface SessionsEnvelope {
  sessions: ChatSessionDto[];
}

export interface SessionEnvelope {
  session: ChatSessionDto;
}

export interface MessagesEnvelope {
  messages: ChatMessageDto[];
}

export interface MessagesChangedEvent {
  sessionId: string;
  messages: ChatMessageDto[];
}

export interface TaskProgressEvent {
  sessionId?: string | null;
  state: string;
  step?: string;
  action?: string;
  thinking?: string;
  result?: string | null;
  error?: string | null;
  message?: string | null;
}

export interface ConfiguredResult {
  configured: boolean;
}

export interface ReconfigureResult {
  ok?: boolean;
}

// —— LingxiChat §3.1 ——

export type LingxiChatEventName =
  | 'sessionsChanged'
  | 'messagesChanged'
  | 'taskProgress';

export interface LingxiChatPlugin {
  listSessions(): Promise<SessionsEnvelope>;
  createSession(options?: { title?: string }): Promise<SessionEnvelope>;
  selectSession(options: { sessionId: string }): Promise<void>;
  deleteSession(options: { sessionId: string }): Promise<void>;
  listMessages(options: { sessionId: string }): Promise<MessagesEnvelope>;
  sendMessage(options: { content: string }): Promise<void>;
  cancelTask(): Promise<void>;
  clearMessages(): Promise<void>;
  addListener(
    eventName: LingxiChatEventName,
    listener: (
      data: SessionsEnvelope | MessagesChangedEvent | TaskProgressEvent,
    ) => void,
  ): Promise<PluginListenerHandle>;
  removeAllListeners?(): Promise<void>;
}

// —— LingxiAgent §3.2 ——

export interface LingxiAgentPlugin {
  /** Bare AgentStateDto — no { state: dto } wrapper */
  getState(): Promise<AgentStateDto>;
  reconfigure(): Promise<ReconfigureResult | void>;
  isConfigured(): Promise<ConfiguredResult>;
  addListener(
    eventName: 'stateChanged',
    listener: (data: AgentStateDto) => void,
  ): Promise<PluginListenerHandle>;
  removeAllListeners?(): Promise<void>;
}
