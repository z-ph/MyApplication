import type {
  LogEntryDto,
  LogExportResult,
  LingxiLogPlugin,
  LogsEnvelope,
} from '../types/bridge';
import type { PluginListenerHandle } from '@capacitor/core';

/**
 * Browser mock for LingxiLog — in-memory sample buffer.
 */
export class LingxiLogWeb implements LingxiLogPlugin {
  private logs: LogEntryDto[] = [
    {
      id: 'mock-1',
      timestamp: '12:00:00.000',
      tag: 'Mock',
      level: 'INFO',
      message: '浏览器 mock 日志 — 真机上显示原生 Logger 缓冲',
      throwable: null,
    },
  ];
  private listeners = new Set<(data: LogsEnvelope) => void>();

  async list(): Promise<LogsEnvelope> {
    return { logs: [...this.logs] };
  }

  async clear(): Promise<void> {
    this.logs = [];
    this.emit();
  }

  async export(): Promise<LogExportResult> {
    const lines = this.logs.map(
      (e) => `${e.timestamp} ${e.level}/${e.tag}: ${e.message}`,
    );
    return {
      text: `=== App Logs Export (mock) ===\n${lines.join('\n')}`,
    };
  }

  async addListener(
    eventName: 'logAppended',
    listener: (data: LogsEnvelope) => void,
  ): Promise<PluginListenerHandle> {
    if (eventName === 'logAppended') {
      this.listeners.add(listener);
    }
    return {
      remove: async () => {
        this.listeners.delete(listener);
      },
    };
  }

  private emit() {
    const payload = { logs: [...this.logs] };
    this.listeners.forEach((l) => l(payload));
  }
}
