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

// —— ApiConfig §2.4 / LingxiApiConfig §3.3 ——

export interface ApiConfigDto {
  id: string;
  name: string;
  /** Bridge uppercase enum style: OPENAI, ZHIPU, … */
  provider: string;
  apiKeyMasked: string;
  baseUrl: string;
  modelId: string;
  isActive: boolean;
}

export interface ConfigsEnvelope {
  configs: ApiConfigDto[];
}

export interface ConfigEnvelope {
  config: ApiConfigDto;
}

export interface CreateApiConfigOptions {
  name?: string;
  provider: string;
  apiKey: string;
  baseUrl?: string;
  modelId?: string;
}

export interface UpdateApiConfigOptions {
  id: string;
  name?: string;
  provider: string;
  /** Omit or blank to keep existing key */
  apiKey?: string;
  baseUrl?: string;
  modelId?: string;
}

export interface ModelsEnvelope {
  models: string[];
}

export interface TestConnectionResult {
  success: boolean;
  message: string;
  details?: string | null;
}

export interface ProviderInfo {
  id: string;
  displayName: string;
  defaultBaseUrl: string;
  defaultModel: string;
}

export interface ProvidersEnvelope {
  providers: ProviderInfo[];
}

export interface LingxiApiConfigPlugin {
  list(): Promise<ConfigsEnvelope>;
  create(options: CreateApiConfigOptions): Promise<ConfigEnvelope>;
  update(options: UpdateApiConfigOptions): Promise<ConfigEnvelope>;
  delete(options: { id: string }): Promise<void>;
  setActive(options: { id: string }): Promise<void>;
  fetchModels(options: {
    provider: string;
    apiKey?: string;
    baseUrl?: string;
  }): Promise<ModelsEnvelope>;
  testConnection(options: {
    provider: string;
    apiKey?: string;
    baseUrl?: string;
    modelId?: string;
  }): Promise<TestConnectionResult>;
  listProviders?(): Promise<ProvidersEnvelope>;
  addListener(
    eventName: 'configsChanged',
    listener: (data: ConfigsEnvelope) => void,
  ): Promise<PluginListenerHandle>;
  removeAllListeners?(): Promise<void>;
}

// —— Permission §3.4 ——

export interface PermissionStatusDto {
  accessibility: boolean;
  overlay: boolean;
  screenCapture: boolean;
  appList: boolean;
  notification: boolean;
  apiConfigured: boolean;
  shizuku: boolean;
  /** accessibility && overlay && screenCapture && appList && apiConfigured */
  allReady: boolean;
}

export interface LingxiPermissionPlugin {
  getStatus(): Promise<PermissionStatusDto>;
  openAccessibilitySettings(): Promise<void>;
  requestOverlay(): Promise<void>;
  /** MediaProjection via Activity Result; resolves updated status or rejects */
  requestScreenCapture(): Promise<PermissionStatusDto>;
  refresh(): Promise<PermissionStatusDto>;
  addListener(
    eventName: 'statusChanged',
    listener: (data: PermissionStatusDto) => void,
  ): Promise<PluginListenerHandle>;
  removeAllListeners?(): Promise<void>;
}
