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
  /** Present when agent reconfigure was attempted after mutation; false = failed quietly */
  reconfigured?: boolean | null;
}

export interface ReconfiguredResult {
  reconfigured: boolean;
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
  delete(options: { id: string }): Promise<void | ReconfiguredResult>;
  setActive(options: { id: string }): Promise<void | ReconfiguredResult>;
  fetchModels(options: {
    provider: string;
    apiKey?: string;
    baseUrl?: string;
    /** When apiKey blank, native loads stored secret for this config id */
    configId?: string;
  }): Promise<ModelsEnvelope>;
  testConnection(options: {
    provider: string;
    apiKey?: string;
    baseUrl?: string;
    modelId?: string;
    /** When apiKey blank, native loads stored secret for this config id */
    configId?: string;
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

// —— Log §3.5 LingxiLog ——

export type LogLevelName = 'ERROR' | 'WARN' | 'INFO' | 'DEBUG' | 'VERBOSE';

export interface LogEntryDto {
  id: string;
  /** Display time string (e.g. HH:mm:ss.SSS) from native Logger */
  timestamp: string;
  tag: string;
  level: LogLevelName | string;
  message: string;
  throwable?: string | null;
}

export interface LogsEnvelope {
  logs: LogEntryDto[];
}

export interface LogExportResult {
  text: string;
}

export interface LingxiLogPlugin {
  list(): Promise<LogsEnvelope>;
  clear(): Promise<void>;
  export(): Promise<LogExportResult>;
  addListener(
    eventName: 'logAppended',
    listener: (data: LogsEnvelope) => void,
  ): Promise<PluginListenerHandle>;
  removeAllListeners?(): Promise<void>;
}

// —— Shell §3.6 LingxiShell (debug) ——

export interface ShizukuStatusDto {
  ready: boolean;
  available: boolean;
  /** ready | available | unavailable */
  status: string;
}

export interface ShellCommandResult {
  success: boolean;
  output: string;
  error?: string | null;
  exitCode?: number | null;
}

export interface PackageInfoDto {
  packageName: string;
  label: string;
  isSystem?: boolean;
  hasLaunchIntent?: boolean;
}

export interface PackagesEnvelope {
  packages: PackageInfoDto[];
}

export interface ShellTestResultDto {
  name: string;
  success: boolean;
  message: string;
  durationMs: number;
}

export interface ShellTestResultsEnvelope {
  results: ShellTestResultDto[];
}

export interface LingxiShellPlugin {
  getShizukuStatus(): Promise<ShizukuStatusDto>;
  runCommand(options: { command: string }): Promise<ShellCommandResult>;
  listPackages(options?: {
    includeSystem?: boolean;
    limit?: number;
  }): Promise<PackagesEnvelope>;
  launchApp(options: {
    nameOrPackage?: string;
    name?: string;
  }): Promise<ShellCommandResult>;
  /** AutoService.inputText — focus a field first (type tool test) */
  inputText(options: { text: string }): Promise<ShellCommandResult>;
  runPackageTests(): Promise<ShellTestResultsEnvelope>;
}
