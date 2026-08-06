import type {
  ApiConfigDto,
  ConfigEnvelope,
  ConfigsEnvelope,
  CreateApiConfigOptions,
  LingxiApiConfigPlugin,
  ModelsEnvelope,
  ProvidersEnvelope,
  TestConnectionResult,
  UpdateApiConfigOptions,
} from '../types/bridge';

type Listener = (data: ConfigsEnvelope) => void;

function mask(key: string): string {
  if (!key) return '';
  if (key.length <= 4) return '***';
  return `${key.slice(0, 3)}***`;
}

/**
 * Browser mock for LingxiApiConfig — in-memory configs, no secrets persisted.
 */
export class LingxiApiConfigWeb implements LingxiApiConfigPlugin {
  private configs: ApiConfigDto[] = [
    {
      id: 'mock-1',
      name: 'Mock OpenAI',
      provider: 'OPENAI',
      apiKeyMasked: 'sk-***',
      baseUrl: 'https://api.openai.com/v1',
      modelId: 'gpt-4o-mini',
      isActive: true,
    },
  ];
  private secrets = new Map<string, string>([['mock-1', 'sk-mock-key']]);
  private listeners = new Set<Listener>();

  async list(): Promise<ConfigsEnvelope> {
    return { configs: this.configs.map((c) => ({ ...c })) };
  }

  async create(options: CreateApiConfigOptions): Promise<ConfigEnvelope> {
    const id = `mock-${Date.now()}`;
    const isFirst = this.configs.length === 0;
    if (isFirst || options) {
      // first becomes active
    }
    if (isFirst) {
      this.configs = this.configs.map((c) => ({ ...c, isActive: false }));
    }
    const config: ApiConfigDto = {
      id,
      name: options.name || options.provider,
      provider: options.provider.toUpperCase(),
      apiKeyMasked: mask(options.apiKey),
      baseUrl: options.baseUrl || '',
      modelId: options.modelId || '',
      isActive: isFirst,
    };
    this.secrets.set(id, options.apiKey);
    this.configs = [...this.configs, config];
    this.emit();
    return { config: { ...config } };
  }

  async update(options: UpdateApiConfigOptions): Promise<ConfigEnvelope> {
    const idx = this.configs.findIndex((c) => c.id === options.id);
    if (idx < 0) throw new Error('Config not found');
    const prev = this.configs[idx];
    if (options.apiKey) {
      this.secrets.set(options.id, options.apiKey);
    }
    const config: ApiConfigDto = {
      ...prev,
      name: options.name ?? prev.name,
      provider: options.provider.toUpperCase(),
      apiKeyMasked: options.apiKey ? mask(options.apiKey) : prev.apiKeyMasked,
      baseUrl: options.baseUrl ?? prev.baseUrl,
      modelId: options.modelId ?? prev.modelId,
    };
    this.configs = this.configs.map((c, i) => (i === idx ? config : c));
    this.emit();
    return { config: { ...config } };
  }

  async delete(options: { id: string }): Promise<void> {
    this.configs = this.configs.filter((c) => c.id !== options.id);
    this.secrets.delete(options.id);
    this.emit();
  }

  async setActive(options: { id: string }): Promise<void> {
    this.configs = this.configs.map((c) => ({
      ...c,
      isActive: c.id === options.id,
    }));
    this.emit();
  }

  async fetchModels(): Promise<ModelsEnvelope> {
    return {
      models: ['gpt-4o-mini', 'gpt-4o', 'glm-4', 'deepseek-chat'],
    };
  }

  async testConnection(): Promise<TestConnectionResult> {
    return {
      success: true,
      message: '连接成功（Web mock）',
      details: 'mock',
    };
  }

  async listProviders(): Promise<ProvidersEnvelope> {
    return {
      providers: [
        {
          id: 'OPENAI',
          displayName: 'OpenAI',
          defaultBaseUrl: 'https://api.openai.com/v1',
          defaultModel: 'gpt-4o',
        },
        {
          id: 'ZHIPU',
          displayName: '智谱 AI',
          defaultBaseUrl: 'https://open.bigmodel.cn/api/paas/v4',
          defaultModel: 'glm-4',
        },
        {
          id: 'DEEPSEEK',
          displayName: 'DeepSeek',
          defaultBaseUrl: 'https://api.deepseek.com/v1',
          defaultModel: 'deepseek-chat',
        },
        {
          id: 'CUSTOM',
          displayName: '自定义 API',
          defaultBaseUrl: '',
          defaultModel: '',
        },
      ],
    };
  }

  async addListener(
    eventName: 'configsChanged',
    listener: Listener,
  ): Promise<{ remove: () => Promise<void> }> {
    if (eventName !== 'configsChanged') {
      return { remove: async () => undefined };
    }
    this.listeners.add(listener);
    listener(await this.list());
    return {
      remove: async () => {
        this.listeners.delete(listener);
      },
    };
  }

  async removeAllListeners(): Promise<void> {
    this.listeners.clear();
  }

  private emit() {
    const payload = { configs: this.configs.map((c) => ({ ...c })) };
    this.listeners.forEach((fn) => fn(payload));
  }
}
