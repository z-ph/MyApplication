import type { WebPlugin } from '@capacitor/core';
import type {
  AppVersionInfo,
  EchoOptions,
  EchoResult,
  LingxiAppPlugin,
  OpenUrlOptions,
} from '../types/bridge';

/**
 * Browser / web implementation of LingxiApp.
 * Used when Capacitor native bridge is unavailable (npm run dev).
 */
export class LingxiAppWeb implements LingxiAppPlugin {
  async echo(options: EchoOptions): Promise<EchoResult> {
    const value = options?.value ?? '';
    // Phase 1 smoke: echo "ok" style probe returns the value as-is.
    return { value };
  }

  async getVersion(): Promise<AppVersionInfo> {
    return {
      appName: '灵犀',
      packageName: 'com.example.myapplication',
      versionName: '1.0.0-web',
      versionCode: 0,
      mock: true,
    };
  }

  async openUrl(options: OpenUrlOptions): Promise<void> {
    if (options?.url) {
      window.open(options.url, '_blank', 'noopener,noreferrer');
    }
  }
}

// Satisfy Capacitor WebPlugin shape when registered via registerPlugin web option.
export type { WebPlugin };
